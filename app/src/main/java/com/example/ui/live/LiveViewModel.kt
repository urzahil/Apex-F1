package com.example.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CalendarRound
import com.example.data.model.SnapshotResponse
import com.example.data.model.StatusResponse
import com.example.data.model.TimingDriverLine
import com.example.data.repository.F1Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface LiveUiState {
    data object Loading : LiveUiState
    data class Success(
        val status: StatusResponse, val snapshot: SnapshotResponse?, val leaderboard: List<TimingDriverLine>,
        val currentRoundCalendar: CalendarRound?, val isLive: Boolean, val autoRefresh: Boolean
    ) : LiveUiState
    data class Error(val message: String) : LiveUiState
}

class LiveViewModel(private val repository: F1Repository = F1Repository()) : ViewModel() {
    private val _uiState = MutableStateFlow<LiveUiState>(LiveUiState.Loading)
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()
    private var pollingJob: Job? = null
    private var loadJob: Job? = null
    private var isAutoRefreshEnabled = true
    private var isScreenVisible = false
    private var cachedCalendar: List<CalendarRound>? = null
    private var cachedSessionPath: String? = null
    private var cachedSessionDrivers = emptyList<com.example.data.model.SessionDriver>()
    private var cachedHistoryPath: String? = null

    fun setScreenVisible(visible: Boolean) {
        if (isScreenVisible == visible) return
        isScreenVisible = visible
        if (visible) { refresh(); startPolling() }
        else { pollingJob?.cancel(); pollingJob = null; loadJob?.cancel(); loadJob = null }
    }

    fun refresh() {
        if (!isScreenVisible) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadLiveData(false) }
    }

    fun toggleAutoRefresh() {
        isAutoRefreshEnabled = !isAutoRefreshEnabled
        if (isAutoRefreshEnabled && isScreenVisible) startPolling() else pollingJob?.cancel()
        (_uiState.value as? LiveUiState.Success)?.let { _uiState.value = it.copy(autoRefresh = isAutoRefreshEnabled) }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive && isScreenVisible) {
                if (isAutoRefreshEnabled && loadJob?.isActive != true) {
                    loadJob = launch { loadLiveData(true) }
                    loadJob?.join()
                }
                delay(
                    when (val state = _uiState.value) {
                        is LiveUiState.Success -> when {
                            state.isLive && state.status.stale != true -> 3_000L
                            state.isLive -> 10_000L
                            else -> 60_000L
                        }
                        else -> 30_000L
                    }
                )
            }
        }
    }

    private suspend fun loadLiveData(isSilent: Boolean) = coroutineScope {
        if (!isSilent && _uiState.value !is LiveUiState.Success) _uiState.value = LiveUiState.Loading
        val statusResult = repository.getStatus()
        val status = statusResult.getOrNull()
        if (status == null) {
            if (_uiState.value !is LiveUiState.Success) {
                _uiState.value = LiveUiState.Error(statusResult.exceptionOrNull()?.localizedMessage ?: "Failed to connect to F1 Live API")
            }
            return@coroutineScope
        }

        val timingDeferred = async { repository.getTiming() }
        val snapshotDeferred = async { repository.getSnapshot() }
        val calendarDeferred = async {
            if (cachedCalendar == null) repository.getCalendar().also { cachedCalendar = it.getOrNull() }
            else Result.success(cachedCalendar!!)
        }
        val sessionPath = status.session?.path
        val driversDeferred = async {
            if (!sessionPath.isNullOrBlank() && sessionPath != cachedSessionPath) {
                repository.getSessionDrivers().also {
                    cachedSessionDrivers = it.getOrNull().orEmpty()
                    cachedSessionPath = sessionPath
                }
            } else Result.success(cachedSessionDrivers)
        }

        val timing = timingDeferred.await().getOrNull()
        val snapshot = snapshotDeferred.await().getOrNull()
        val calendar = calendarDeferred.await().getOrNull()
        val sessionDrivers = driversDeferred.await().getOrNull().orEmpty()

        val currentRound = calendar?.find { round ->
            val meeting = status.session?.meeting
            val meetingName = meeting?.name.orEmpty()
            val circuit = meeting?.circuit?.shortName.orEmpty()
            (meetingName.isNotBlank() && (round.name?.contains(meetingName, true) == true || meetingName.contains(round.name.orEmpty(), true))) ||
                (circuit.isNotBlank() && round.circuit?.contains(circuit, true) == true)
        }

        val driverMap = sessionDrivers.associateBy { it.driverNumber }
        val topThree = status.topThree?.lines.orEmpty()
        val topMap = topThree.associateBy { it.racingNumber }
        val leaderboard = buildLeaderboard(timing, snapshot, driverMap, topMap, status, sessionPath)
        val sorted = leaderboard.sortedBy { it.getDisplayPosition().toIntOrNull() ?: 999 }
        val p1 = sorted.firstOrNull { it.getDisplayPosition() == "1" || it.getDisplayPosition() == "P1" } ?: sorted.firstOrNull()
        val p1LapTime = p1?.bestLapTime ?: p1?.lapTime ?: p1?.lastLapTime

        val finalLeaderboard = sorted.map { driver ->
            if (driver == p1 || driver.getDisplayPosition() == "1" || driver.getDisplayPosition() == "P1") driver
            else {
                val currentGap = driver.gapToLeader ?: driver.gap
                val gap = currentGap?.takeIf(String::isNotBlank)?.let {
                    if (!it.startsWith("+") && !it.contains("LAP", true)) "+$it" else it
                } ?: LiveTimeUtils.calculateLapGap(p1LapTime, driver.bestLapTime ?: driver.lapTime ?: driver.lastLapTime)
                    ?: driver.interval?.let { if (!it.startsWith("+")) "+$it" else it }
                driver.copy(gapToLeader = gap)
            }
        }

        val enrichedTop = topThree.mapIndexed { index, top ->
            val matching = finalLeaderboard.firstOrNull {
                it.driverNumber == top.racingNumber || it.racingNumber == top.racingNumber ||
                    it.tla.equals(top.tla, true) || it.acronym.equals(top.tla, true)
            }
            top.copy(
                position = top.position?.takeIf(String::isNotBlank) ?: (index + 1).toString(),
                lapTime = top.lapTime?.takeIf(String::isNotBlank) ?: matching?.bestLapTime ?: matching?.lapTime,
                diffToLeader = top.diffToLeader?.takeIf(String::isNotBlank) ?: matching?.gapToLeader ?: matching?.gap
                    ?: LiveTimeUtils.calculateLapGap(p1LapTime, matching?.bestLapTime ?: matching?.lapTime)
            )
        }
        val enrichedStatus = if (enrichedTop.isNotEmpty()) status.copy(topThree = status.topThree?.copy(lines = enrichedTop)) else status
        val isLiveSession = status.live == true || status.session?.sessionStatus?.let {
            it.equals("Started", true) || it.equals("Active", true) || it.equals("Running", true)
        } == true
        val previous = _uiState.value as? LiveUiState.Success

        _uiState.value = LiveUiState.Success(
            status = enrichedStatus,
            snapshot = snapshot ?: previous?.snapshot,
            leaderboard = finalLeaderboard.ifEmpty { previous?.leaderboard.orEmpty() },
            currentRoundCalendar = currentRound,
            isLive = isLiveSession,
            autoRefresh = isAutoRefreshEnabled
        )
    }

    private suspend fun buildLeaderboard(
        timing: com.example.data.model.TimingResponse?, snapshot: SnapshotResponse?,
        sessionDrivers: Map<String?, com.example.data.model.SessionDriver>,
        topThree: Map<String?, com.example.data.model.TopThreeDriver>,
        status: StatusResponse, sessionPath: String?
    ): List<TimingDriverLine> {
        if (!timing?.drivers.isNullOrEmpty() && timing!!.drivers!!.any { it.position != null }) return timing.drivers.orEmpty()
        val snapshotLines = snapshot?.timing?.lines.orEmpty()
        if (snapshotLines.isNotEmpty()) {
            return snapshotLines.map { line ->
                val driver = sessionDrivers[line.racingNumber]
                val top = topThree[line.racingNumber]
                val tla = driver?.acronym ?: top?.tla.orEmpty()
                TimingDriverLine(
                    position = line.position, racingNumber = line.racingNumber, driverNumber = line.racingNumber,
                    name = driver?.name ?: top?.fullName ?: top?.broadcastName ?: ("Car #" + line.racingNumber),
                    tla = tla, acronym = tla, team = driver?.team ?: top?.team, teamColour = driver?.teamColour ?: top?.teamColour,
                    gap = line.gapToLeader ?: top?.diffToLeader, gapToLeader = line.gapToLeader ?: top?.diffToLeader,
                    interval = line.intervalToAhead ?: top?.diffToAhead, lapTime = line.bestLapTime ?: top?.lapTime,
                    bestLapTime = line.bestLapTime ?: top?.lapTime, lastLapTime = line.lastLapTime,
                    inPit = line.inPit, pitOut = line.pitOut, retired = line.retired, stopped = line.stopped, knockOut = line.knockedOut
                )
            }
        }

        if (!sessionPath.isNullOrBlank() && sessionPath != cachedHistoryPath) {
            cachedHistoryPath = sessionPath
            repository.getHistorySessionClassification(sessionPath, status.session?.type).getOrNull()?.let { history ->
                if (history.isNotEmpty()) {
                    val isPractice = status.session?.type?.contains("Practice", true) == true
                    return history.map { item ->
                        TimingDriverLine(
                            position = item.position, racingNumber = item.driverNumber, driverNumber = item.driverNumber,
                            name = item.fullName, tla = item.tla, team = item.teamName, teamColour = item.teamColour,
                            gap = if (isPractice) item.timeDiffToFastest ?: item.gapToLeader else item.gapToLeader,
                            gapToLeader = item.gapToLeader, interval = item.intervalToAhead,
                            lapTime = item.bestLapTime, bestLapTime = item.bestLapTime,
                            retired = item.isRetired, inPit = item.inPit, stopped = item.stopped
                        )
                    }
                }
            }
        }

        return if (sessionDrivers.isNotEmpty()) {
            sessionDrivers.values.mapIndexed { index, driver ->
                TimingDriverLine(
                    position = (index + 1).toString(), racingNumber = driver.driverNumber, driverNumber = driver.driverNumber,
                    name = driver.name, tla = driver.acronym, acronym = driver.acronym, team = driver.team, teamColour = driver.teamColour
                )
            }
        } else {
            topThree.values.map { top ->
                TimingDriverLine(
                    position = top.position, racingNumber = top.racingNumber, driverNumber = top.racingNumber,
                    name = top.fullName ?: top.broadcastName, tla = top.tla, acronym = top.tla,
                    team = top.team, teamColour = top.teamColour, gap = top.diffToLeader, gapToLeader = top.diffToLeader,
                    interval = top.diffToAhead, lapTime = top.lapTime
                )
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel(); loadJob?.cancel(); super.onCleared()
    }
}
