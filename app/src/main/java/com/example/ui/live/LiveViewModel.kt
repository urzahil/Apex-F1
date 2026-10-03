package com.example.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CalendarRound
import com.example.data.model.SnapshotResponse
import com.example.data.model.StatusResponse
import com.example.data.model.TimingDriverLine
import com.example.data.repository.F1Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface LiveUiState {
    data object Loading : LiveUiState
    data class Success(
        val status: StatusResponse,
        val snapshot: SnapshotResponse?,
        val leaderboard: List<TimingDriverLine>,
        val currentRoundCalendar: CalendarRound?,
        val isLive: Boolean,
        val autoRefresh: Boolean
    ) : LiveUiState
    data class Error(val message: String) : LiveUiState
}

class LiveViewModel(
    private val repository: F1Repository = F1Repository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiveUiState>(LiveUiState.Loading)
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var isAutoRefreshEnabled = true

    init {
        loadLiveData()
        startPolling()
    }

    fun refresh() {
        loadLiveData()
    }

    fun toggleAutoRefresh() {
        isAutoRefreshEnabled = !isAutoRefreshEnabled
        if (isAutoRefreshEnabled) {
            startPolling()
        } else {
            pollingJob?.cancel()
        }
        val current = _uiState.value
        if (current is LiveUiState.Success) {
            _uiState.value = current.copy(autoRefresh = isAutoRefreshEnabled)
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(30000) // Live data refreshed every 30 seconds
                if (isAutoRefreshEnabled) {
                    loadLiveData(isSilent = true)
                }
            }
        }
    }

    private fun loadLiveData(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent && _uiState.value !is LiveUiState.Success) {
                _uiState.value = LiveUiState.Loading
            }

            val statusResult = repository.getStatus()
            val snapshotResult = repository.getSnapshot()
            val timingResult = repository.getTiming()
            val calendarResult = repository.getCalendar()
            val sessionDriversResult = repository.getSessionDrivers()

            val status = statusResult.getOrNull()
            if (status != null) {
                val snapshot = snapshotResult.getOrNull()
                val timing = timingResult.getOrNull()
                val calendar = calendarResult.getOrNull()
                val sessionDrivers = sessionDriversResult.getOrNull() ?: emptyList()

                // Find current or matching round from calendar
                val currentRound = calendar?.find { round ->
                    val meetingName = status.session?.meeting?.name ?: ""
                    round.name?.contains(meetingName, ignoreCase = true) == true ||
                            meetingName.contains(round.name ?: "", ignoreCase = true)
                } ?: calendar?.firstOrNull()

                // Load genuine classification from the API:
                // 1. If live timing stream has drivers, use them.
                // 2. If not live or timing feed is empty, load the full list from snapshot.timing.lines and sessionDrivers.
                // 3. Otherwise fetch the real session classification using session.path.
                // 4. Fallback to all session drivers or topThree.
                val leaderboard = mutableListOf<TimingDriverLine>()

                if (!timing?.drivers.isNullOrEmpty() && timing.drivers.any { it.position != null }) {
                    leaderboard.addAll(timing.drivers)
                } else if (!snapshot?.timing?.lines.isNullOrEmpty()) {
                    val topThreeList = status.topThree?.lines ?: emptyList()

                    snapshot.timing.lines.forEach { line ->
                        val matchingDriver = sessionDrivers.find { it.driverNumber == line.racingNumber }
                        val matchingTop = topThreeList.find { it.racingNumber == line.racingNumber }

                        val driverName = matchingDriver?.name
                            ?: matchingTop?.fullName
                            ?: matchingTop?.broadcastName
                            ?: "Car #${line.racingNumber}"

                        val tla = matchingDriver?.acronym
                            ?: matchingTop?.tla
                            ?: ""

                        val team = matchingDriver?.team
                            ?: matchingTop?.team

                        val teamColour = matchingDriver?.teamColour
                            ?: matchingTop?.teamColour

                        val bestLap = line.bestLapTime ?: matchingTop?.lapTime
                        val lastLap = line.lastLapTime
                        val gap = line.gapToLeader ?: matchingTop?.diffToLeader
                        val interval = line.intervalToAhead ?: matchingTop?.diffToAhead

                        leaderboard.add(
                            TimingDriverLine(
                                position = line.position,
                                racingNumber = line.racingNumber,
                                driverNumber = line.racingNumber,
                                name = driverName,
                                tla = tla,
                                acronym = tla,
                                team = team,
                                teamColour = teamColour,
                                gap = gap,
                                interval = interval,
                                bestLapTime = bestLap,
                                lapTime = bestLap ?: lastLap,
                                lastLapTime = lastLap,
                                inPit = line.inPit,
                                pitOut = line.pitOut,
                                retired = line.retired,
                                stopped = line.stopped,
                                knockOut = line.knockedOut
                            )
                        )
                    }
                } else {
                    val sessionPath = status.session?.path
                    var loadedFromSessionPath = false

                    if (!sessionPath.isNullOrBlank()) {
                        val sessionClassResult = repository.getHistorySessionClassification(sessionPath)
                        val sessionClass = sessionClassResult.getOrNull()
                        if (!sessionClass.isNullOrEmpty()) {
                            val isPractice = status.session?.type?.contains("Practice", ignoreCase = true) == true ||
                                    status.session?.name?.contains("Practice", ignoreCase = true) == true

                            sessionClass.forEach { item ->
                                val gap = if (isPractice) {
                                    item.timeDiffToFastest ?: item.gapToLeader
                                } else {
                                    item.gapToLeader ?: item.intervalToAhead
                                }

                                leaderboard.add(
                                    TimingDriverLine(
                                        position = item.position,
                                        racingNumber = item.driverNumber,
                                        driverNumber = item.driverNumber,
                                        name = item.fullName,
                                        tla = item.tla,
                                        team = item.teamName,
                                        teamColour = item.teamColour,
                                        gap = gap,
                                        interval = item.intervalToAhead,
                                        lapTime = item.bestLapTime,
                                        retired = item.isRetired,
                                        inPit = item.inPit,
                                        stopped = item.stopped
                                    )
                                )
                            }
                            loadedFromSessionPath = true
                        }
                    }

                    // Fallback to all session drivers if available
                    if (!loadedFromSessionPath && sessionDrivers.isNotEmpty()) {
                        sessionDrivers.forEachIndexed { idx, drv ->
                            leaderboard.add(
                                TimingDriverLine(
                                    position = (idx + 1).toString(),
                                    racingNumber = drv.driverNumber,
                                    driverNumber = drv.driverNumber,
                                    name = drv.name,
                                    tla = drv.acronym,
                                    team = drv.team,
                                    teamColour = drv.teamColour
                                )
                            )
                        }
                    } else if (!loadedFromSessionPath && !status.topThree?.lines.isNullOrEmpty()) {
                        status.topThree.lines.forEach { top ->
                            leaderboard.add(
                                TimingDriverLine(
                                    position = top.position,
                                    racingNumber = top.racingNumber,
                                    driverNumber = top.racingNumber,
                                    name = top.fullName ?: top.broadcastName,
                                    tla = top.tla,
                                    team = top.team,
                                    teamColour = top.teamColour,
                                    gap = top.diffToLeader,
                                    interval = top.diffToAhead,
                                    lapTime = top.lapTime
                                )
                            )
                        }
                    }
                }

                // Sort purely by numeric position
                val sortedLeaderboard = leaderboard.sortedBy {
                    it.getDisplayPosition().toIntOrNull() ?: 999
                }

                // Identify P1's best lap time
                val p1Driver = sortedLeaderboard.find {
                    it.getDisplayPosition() == "1" || it.getDisplayPosition() == "P1"
                } ?: sortedLeaderboard.firstOrNull()
                val p1LapTime = p1Driver?.bestLapTime ?: p1Driver?.lapTime ?: p1Driver?.lastLapTime

                // Calculate/ensure gap to leader for all other drivers (P2..P20)
                val finalLeaderboard = sortedLeaderboard.map { driver ->
                    val isP1 = driver == p1Driver || driver.getDisplayPosition() == "1" || driver.getDisplayPosition() == "P1"
                    if (isP1) {
                        driver
                    } else {
                        val currentGap = driver.gapToLeader ?: driver.gap
                        val resolvedGap = if (!currentGap.isNullOrBlank()) {
                            if (!currentGap.startsWith("+") && !currentGap.contains("LAP", ignoreCase = true)) {
                                "+$currentGap"
                            } else currentGap
                        } else {
                            val drvLap = driver.bestLapTime ?: driver.lapTime ?: driver.lastLapTime
                            LiveTimeUtils.calculateLapGap(p1LapTime, drvLap)
                                ?: driver.interval?.let { if (!it.startsWith("+")) "+$it" else it }
                        }
                        driver.copy(gapToLeader = resolvedGap)
                    }
                }

                // Enrich status.topThree with times and resolved positions from timing feed
                val enrichedStatus = if (!status.topThree?.lines.isNullOrEmpty()) {
                    val enrichedLines = status.topThree.lines.mapIndexed { idx, topDriver ->
                        val pos = topDriver.position?.takeIf { it.isNotBlank() } ?: (idx + 1).toString()
                        val matching = finalLeaderboard.find {
                            it.driverNumber == topDriver.racingNumber ||
                            it.racingNumber == topDriver.racingNumber ||
                            it.tla.equals(topDriver.tla, ignoreCase = true) ||
                            it.acronym.equals(topDriver.tla, ignoreCase = true)
                        }
                        val lapTime = topDriver.lapTime?.takeIf { it.isNotBlank() }
                            ?: matching?.bestLapTime
                            ?: matching?.lapTime
                        val diff = topDriver.diffToLeader?.takeIf { it.isNotBlank() }
                            ?: matching?.gapToLeader
                            ?: matching?.gap
                            ?: LiveTimeUtils.calculateLapGap(p1LapTime, lapTime)
                        topDriver.copy(
                            position = pos,
                            lapTime = lapTime,
                            diffToLeader = diff
                        )
                    }
                    status.copy(topThree = status.topThree.copy(lines = enrichedLines))
                } else status

                val isLiveSession = status.live == true

                _uiState.value = LiveUiState.Success(
                    status = enrichedStatus,
                    snapshot = snapshot,
                    leaderboard = finalLeaderboard,
                    currentRoundCalendar = currentRound,
                    isLive = isLiveSession,
                    autoRefresh = isAutoRefreshEnabled
                )
            } else {
                if (_uiState.value !is LiveUiState.Success) {
                    val err = statusResult.exceptionOrNull()?.localizedMessage ?: "Failed to connect to F1 Live API"
                    _uiState.value = LiveUiState.Error(err)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
