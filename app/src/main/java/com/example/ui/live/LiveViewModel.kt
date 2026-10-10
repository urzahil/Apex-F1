package com.example.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CalendarRound
import com.example.data.model.LapCountData
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
        val status: StatusResponse,
        val snapshot: SnapshotResponse?,
        val leaderboard: List<TimingDriverLine>,
        val currentRoundCalendar: CalendarRound?,
        val isLive: Boolean,
        val autoRefresh: Boolean,
        val lapCount: LapCountData? = null
    ) : LiveUiState

    data class Error(val message: String) : LiveUiState
}

private fun normalizeDriverKey(value: String?): String? {
    val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val number = trimmed.toDoubleOrNull()
    return if (number != null && number % 1.0 == 0.0) number.toInt().toString() else trimmed
}

internal fun shouldHideDeltasBeforeTiming(
    sessionStarted: Boolean,
    sessionFinished: Boolean,
    hasLapTimes: Boolean
): Boolean = !sessionStarted && !sessionFinished && !hasLapTimes

internal fun normalizeLeaderGap(value: String?): String? {
    val gap = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (gap.contains("LAP", ignoreCase = true)) return gap
    val unsigned = gap.removePrefix("+").removePrefix("-")
    return if (unsigned.isNotBlank()) "+$unsigned" else null
}

class LiveViewModel(private val repository: F1Repository = F1Repository()) : ViewModel() {
    private val _uiState = MutableStateFlow<LiveUiState>(LiveUiState.Loading)
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private var pollingJob: Job? = null
    private var loadJob: Job? = null
    private var refreshGeneration = 0L
    private var isAutoRefreshEnabled = true
    private val _autoRefreshEnabled = MutableStateFlow(true)
    val autoRefreshEnabled: StateFlow<Boolean> = _autoRefreshEnabled.asStateFlow()
    private var isScreenVisible = false

    fun setScreenVisible(visible: Boolean) {
        if (isScreenVisible == visible) return
        isScreenVisible = visible
        if (visible) {
            refresh(); startPolling()
        } else {
            pollingJob?.cancel(); pollingJob = null; loadJob?.cancel(); loadJob = null
        }
    }

    fun refresh() {
        if (!isScreenVisible) return
        loadJob?.cancel()
        _isRefreshing.value = true
        val thisRefresh = ++refreshGeneration
        loadJob = viewModelScope.launch {
            try {
                loadLiveData(false)
            } finally {
                if (thisRefresh == refreshGeneration) {
                    _isRefreshing.value = false
                    // Start a fresh 30-second polling countdown after manual refresh completes.
                    if (isScreenVisible && isAutoRefreshEnabled) startPolling()
                }
            }
        }
    }

    fun toggleAutoRefresh() {
        isAutoRefreshEnabled = !isAutoRefreshEnabled
        _autoRefreshEnabled.value = isAutoRefreshEnabled
        if (isAutoRefreshEnabled && isScreenVisible) startPolling() else pollingJob?.cancel()
        (_uiState.value as? LiveUiState.Success)?.let {
            _uiState.value = it.copy(autoRefresh = isAutoRefreshEnabled)
        }
    }

    private fun isRaceOrSprint(status: StatusResponse): Boolean {
        val type = status.session?.type.orEmpty()
        val name = status.session?.name.orEmpty()
        val isQualifying = type.contains("Qualifying", true) || name.contains("Qualifying", true)
        return !isQualifying && (
                type.contains("Race", true) || type.equals("Sprint", true) ||
                        name.contains("Race", true) || name.equals("Sprint", true)
                )
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive && isScreenVisible) {
                if (isAutoRefreshEnabled && loadJob?.isActive != true) {
                    loadJob = launch { loadLiveData(true) }
                    loadJob?.join()
                }
                // Keep the polling cadence constant across live, stale, finished,
                // loading, and error states. Failures do not increase the interval.
                delay(30_000L)
            }
        }
    }

    private suspend fun loadLiveData(isSilent: Boolean): Boolean = coroutineScope {
        if (!isSilent && _uiState.value !is LiveUiState.Success) _uiState.value =
            LiveUiState.Loading
        val statusDeferred = async { repository.getStatus() }
        val timingDeferred = async { repository.getTiming() }
        val snapshotDeferred = async { repository.getSnapshot() }
        val calendarDeferred = async {
            repository.getCalendar(forceRefresh = true, allowCacheFallback = false, cacheResults = false)
        }
        val statusResult = statusDeferred.await()
        val status = statusResult.getOrNull()
        if (status == null) {
            // Keep the last successful Live response visible if a refresh fails.
            // Only show an error when there has never been a successful response.
            if (_uiState.value !is LiveUiState.Success) {
                _uiState.value = LiveUiState.Error(
                    statusResult.exceptionOrNull()?.localizedMessage
                        ?: "Failed to connect to F1 Live API"
                )
            }
            return@coroutineScope false
        }

        val sessionPath = status.session?.path?.takeIf { it.isNotBlank() }
        val driversDeferred = async { repository.getSessionDrivers() }

        val timing = timingDeferred.await().getOrNull()
        val snapshot = snapshotDeferred.await().getOrNull()
        val calendar = calendarDeferred.await().getOrNull()
        val sessionDrivers = driversDeferred.await().getOrNull().orEmpty()

        val currentRound = calendar?.find { round ->
            val meeting = status.session?.meeting
            val meetingName = meeting?.name.orEmpty()
            val circuit = meeting?.circuit?.shortName.orEmpty()
            (meetingName.isNotBlank() && (round.name?.contains(
                meetingName,
                true
            ) == true || meetingName.contains(round.name.orEmpty(), true))) ||
                    (circuit.isNotBlank() && round.circuit?.contains(circuit, true) == true)
        }

        val driverMap = sessionDrivers.associateBy { it.driverNumber }
        val topThree = status.topThree?.lines.orEmpty()
        val topMap = topThree.associateBy { it.racingNumber }
        val builtLeaderboard =
            buildLeaderboard(timing, snapshot, driverMap, topMap, status, sessionPath)
        val sessionTypeOrName = listOf(status.session?.type, status.session?.name)
            .filterNotNull().joinToString(" ")
        val isClassificationSession = listOf("Practice", "Qualifying", "Race", "Sprint")
            .any { sessionTypeOrName.contains(it, true) }
        val isFinalised = status.session?.sessionStatus?.let {
            it.equals("Finalised", true) || it.equals("Finished", true) ||
                    it.equals("Ended", true) || it.equals("Completed", true)
        } == true
        // Once finalised, use a cached classification when available and retain it
        // as a fallback if a later API request fails.
        val finalSessionClassification =
            if (isClassificationSession && isFinalised && !sessionPath.isNullOrBlank()) {
                repository.getHistorySessionClassification(
                    sessionPath,
                    status.session?.type,
                    forceRefresh = false,
                    allowCacheFallback = true,
                    cacheResults = true,
                    cacheClassificationWithoutTyreData = true
                ).getOrNull().orEmpty()
            } else emptyList()
        val leaderboard = if (finalSessionClassification.isNotEmpty()) {
            mergeFinalSessionClassification(builtLeaderboard, finalSessionClassification)
        } else {
            builtLeaderboard
        }
        val sorted = leaderboard.sortedBy { it.getDisplayPosition().toIntOrNull() ?: 999 }
        val p1 =
            sorted.firstOrNull { it.getDisplayPosition() == "1" || it.getDisplayPosition() == "P1" }
                ?: sorted.firstOrNull()
        val p1LapTime = p1?.bestLapTime ?: p1?.lapTime ?: p1?.lastLapTime

        val sessionStatus = status.session?.sessionStatus.orEmpty()
        val sessionStarted = status.live == true || sessionStatus.equals("Started", true) ||
                sessionStatus.equals("Active", true) || sessionStatus.equals("Running", true)
        val sessionFinished = sessionStatus.equals("Finalised", true) ||
                sessionStatus.equals("Finished", true) || sessionStatus.equals("Ended", true) ||
                sessionStatus.equals("Completed", true)
        val hasLapTimes = builtLeaderboard.any { driver ->
            listOf(
                driver.bestLapTime,
                driver.lapTime,
                driver.lastLapTime
            ).any { !it.isNullOrBlank() }
        }
        val hideUninitializedDeltas = shouldHideDeltasBeforeTiming(
            sessionStarted = sessionStarted,
            sessionFinished = sessionFinished,
            hasLapTimes = hasLapTimes
        )
        val finalLeaderboard = sorted.map { driver ->
            if (hideUninitializedDeltas) {
                // Before this session has produced a lap time, gaps and intervals may
                // belong to a previous session or use different sign conventions.
                driver.copy(gap = null, gapToLeader = null, timeDiffToFastest = null)
            } else if (driver == p1 || driver.getDisplayPosition() == "1" || driver.getDisplayPosition() == "P1") driver
            else {
                val currentGap = driver.gapToLeader ?: driver.gap ?: driver.timeDiffToFastest
                val gap = normalizeLeaderGap(currentGap)
                    ?: LiveTimeUtils.calculateLapGap(
                        p1LapTime,
                        driver.bestLapTime ?: driver.lapTime ?: driver.lastLapTime
                    )
                // Interval-to-ahead is deliberately not used as a fallback for gap-to-leader.
                driver.copy(gap = gap, gapToLeader = gap)
            }
        }

        val enrichedTop = topThree.mapIndexed { index, top ->
            val matching = finalLeaderboard.firstOrNull {
                it.driverNumber == top.racingNumber || it.racingNumber == top.racingNumber ||
                        it.tla.equals(top.tla, true) || it.acronym.equals(top.tla, true)
            }
            top.copy(
                position = top.position?.takeIf(String::isNotBlank) ?: (index + 1).toString(),
                lapTime = top.lapTime?.takeIf(String::isNotBlank) ?: matching?.bestLapTime
                ?: matching?.lapTime,
                diffToLeader = if (hideUninitializedDeltas) null else normalizeLeaderGap(
                    top.diffToLeader?.takeIf(String::isNotBlank) ?: matching?.gapToLeader
                    ?: matching?.gap
                ) ?: LiveTimeUtils.calculateLapGap(
                    p1LapTime,
                    matching?.bestLapTime ?: matching?.lapTime
                )
            )
        }
        val enrichedStatus =
            if (enrichedTop.isNotEmpty()) status.copy(topThree = status.topThree?.copy(lines = enrichedTop)) else status
        val isLiveSession = status.live == true || status.session?.sessionStatus?.let {
            it.equals("Started", true) || it.equals("Active", true) || it.equals("Running", true)
        } == true
        val rawLapCount = snapshot?.lapCount ?: status.lapCount ?: timing?.lapCount
        val resolvedLapCount = if (rawLapCount != null) {
            if (rawLapCount.getTotalLapsInt() == null && currentRound?.track?.laps != null) {
                rawLapCount.copy(totalLaps = currentRound.track.laps)
            } else rawLapCount
        } else {
            val totalLaps = currentRound?.track?.laps
            val maxDriverLaps =
                finalLeaderboard.mapNotNull { it.lapsCompleted ?: it.laps }.maxOrNull()
            if (maxDriverLaps != null) LapCountData(
                currentLap = maxDriverLaps,
                totalLaps = totalLaps
            )
            else null
        }

        _uiState.value = LiveUiState.Success(
            status = enrichedStatus,
            snapshot = snapshot,
            leaderboard = finalLeaderboard,
            currentRoundCalendar = currentRound,
            isLive = isLiveSession,
            autoRefresh = isAutoRefreshEnabled,
            lapCount = resolvedLapCount
        )
        true
    }

    private suspend fun buildLeaderboard(
        timing: com.example.data.model.TimingResponse?, snapshot: SnapshotResponse?,
        sessionDrivers: Map<String?, com.example.data.model.SessionDriver>,
        topThree: Map<String?, com.example.data.model.TopThreeDriver>,
        status: StatusResponse, sessionPath: String?
    ): List<TimingDriverLine> {
        val snapshotLines =
            (snapshot?.timingApp?.lines.orEmpty() + snapshot?.timing?.lines.orEmpty())
                .groupBy { normalizeDriverKey(it.racingNumber) }
                .values
                .mapNotNull { candidates ->
                    candidates.firstOrNull { it.stints.isNotEmpty() } ?: candidates.firstOrNull()
                }
        val snapshotByDriver = snapshotLines.associateBy { normalizeDriverKey(it.racingNumber) }

        if (!timing?.drivers.isNullOrEmpty()) {
            val isRaceSession = isRaceOrSprint(status)
            val needsHistoryTyres = timing.drivers.orEmpty().any { line ->
                line.stints.isEmpty() &&
                        snapshotByDriver[line.driverNumber
                            ?: line.racingNumber]?.stints.isNullOrEmpty()
            }
            val needsHistoryData = isRaceSession || needsHistoryTyres
            val historyStintsByDriver = if (needsHistoryData && !sessionPath.isNullOrBlank()) {
                repository.getHistorySessionClassification(
                    sessionPath,
                    status.session?.type,
                    forceRefresh = true,
                    allowCacheFallback = false,
                    cacheResults = false
                ).getOrNull().orEmpty().associateBy { normalizeDriverKey(it.driverNumber) }
            } else {
                emptyMap()
            }

            val timingRows = timing.drivers.orEmpty().map { line ->
                val key = normalizeDriverKey(line.driverNumber ?: line.racingNumber)
                val snapshotLine = snapshotByDriver[key]
                val historyLine = historyStintsByDriver[normalizeDriverKey(line.driverNumber)]
                val driver =
                    sessionDrivers.entries.firstOrNull { normalizeDriverKey(it.key) == key }?.value
                val top = topThree.entries.firstOrNull { normalizeDriverKey(it.key) == key }?.value
                val url = line.getEffectiveHeadshotUrl() ?: driver?.headshotUrl
                ?: top?.getEffectiveHeadshotUrl()
                val stints = if (isRaceSession) {
                    when {
                        line.stints.isNotEmpty() -> line.stints
                        !snapshotLine?.stints.isNullOrEmpty() ->
                            snapshotLine?.stints?.mapIndexed { index, stint -> (index + 1).toString() to stint }
                                ?.toMap().orEmpty()

                        else -> historyLine?.stints.orEmpty()
                    }
                } else {
                    emptyMap()
                }
                line.copy(
                    // timing_app.Line is authoritative only for Race/Sprint. Practice
                    // and Qualifying keep the existing timing endpoint position/order.
                    position = if (isRaceSession) snapshotLine?.position
                        ?: line.position else line.position,
                    headshotUrl = line.headshotUrl ?: url,
                    // During a race the live timing feed may only provide GapToLeader
                    // for the top few drivers. Fill the remaining cars from the
                    // session classification/history feed instead of leaving the UI blank.
                    gapToLeader = line.gapToLeader
                        ?: line.gap
                        ?: snapshotLine?.gapToLeader
                        ?: historyLine?.gapToLeader
                        ?: historyLine?.timeDiffToFastest
                        ?: top?.diffToLeader,
                    totalRaceTime = if (isRaceSession && key == historyLine?.driverNumber) historyLine?.totalRaceTime else line.totalRaceTime,
                    stints = stints
                )
            }

            // After a session ends, the timing endpoint can shrink to only the top
            // three. The snapshot still contains the full classification, so append
            // snapshot drivers missing from the timing response.
            val sessionStatus = status.session?.sessionStatus.orEmpty()
            val isFinalised = listOf("Finalised", "Finished", "Ended", "Completed")
                .any { sessionStatus.equals(it, ignoreCase = true) }
            if (!isFinalised) return timingRows

            val timingKeys = timingRows.mapNotNull { row ->
                normalizeDriverKey(row.driverNumber ?: row.racingNumber)
            }.toSet()
            val snapshotOnlyRows = snapshotLines.mapNotNull { line ->
                val key = normalizeDriverKey(line.racingNumber) ?: return@mapNotNull null
                if (key in timingKeys) return@mapNotNull null
                val driver = sessionDrivers.entries
                    .firstOrNull { normalizeDriverKey(it.key) == key }?.value
                val top = topThree.entries
                    .firstOrNull { normalizeDriverKey(it.key) == key }?.value
                val history = historyStintsByDriver[key]
                val tla = driver?.acronym ?: top?.tla.orEmpty()
                TimingDriverLine(
                    position = line.position,
                    racingNumber = line.racingNumber,
                    driverNumber = line.racingNumber,
                    name = driver?.name ?: top?.fullName ?: top?.broadcastName
                        ?: ("Car #" + line.racingNumber),
                    tla = tla,
                    acronym = tla,
                    team = driver?.team ?: top?.team,
                    teamColour = driver?.teamColour ?: top?.teamColour,
                    headshotUrl = driver?.headshotUrl ?: top?.getEffectiveHeadshotUrl(),
                    gap = line.gapToLeader ?: history?.gapToLeader ?: history?.timeDiffToFastest,
                    gapToLeader = line.gapToLeader ?: history?.gapToLeader
                        ?: history?.timeDiffToFastest,
                    interval = line.intervalToAhead,
                    lapTime = line.bestLapTime,
                    bestLapTime = line.bestLapTime,
                    lastLapTime = line.lastLapTime,
                    totalRaceTime = history?.totalRaceTime,
                    inPit = line.inPit,
                    pitOut = line.pitOut,
                    retired = line.retired,
                    stopped = line.stopped,
                    knockOut = line.knockedOut,
                    stints = if (isRaceSession) {
                        if (line.stints.isNotEmpty()) {
                            line.stints.mapIndexed { index, stint -> (index + 1).toString() to stint }
                                .toMap()
                        } else history?.stints.orEmpty()
                    } else emptyMap()
                )
            }
            return timingRows + snapshotOnlyRows
        }

        if (snapshotLines.isNotEmpty()) {
            val isRaceSession = isRaceOrSprint(status)
            val historyByDriver =
                if (isRaceSession && !sessionPath.isNullOrBlank()) {
                    repository.getHistorySessionClassification(
                        sessionPath,
                        status.session?.type,
                        forceRefresh = true,
                        allowCacheFallback = false,
                        cacheResults = false
                    ).getOrNull()?.associateBy { it.driverNumber }.orEmpty()
                } else emptyMap()

            return snapshotLines.map { line ->
                val key = normalizeDriverKey(line.racingNumber)
                val driver =
                    sessionDrivers.entries.firstOrNull { normalizeDriverKey(it.key) == key }?.value
                val history = historyByDriver[key]
                val top = topThree.entries.firstOrNull { normalizeDriverKey(it.key) == key }?.value
                val tla = driver?.acronym ?: top?.tla.orEmpty()
                val url = driver?.headshotUrl ?: top?.getEffectiveHeadshotUrl()
                TimingDriverLine(
                    position = line.position,
                    racingNumber = line.racingNumber,
                    driverNumber = line.racingNumber,
                    name = driver?.name ?: top?.fullName ?: top?.broadcastName
                    ?: ("Car #" + line.racingNumber),
                    tla = tla,
                    acronym = tla,
                    team = driver?.team ?: top?.team,
                    teamColour = driver?.teamColour ?: top?.teamColour,
                    headshotUrl = url,
                    gap = line.gapToLeader ?: history?.gapToLeader ?: history?.timeDiffToFastest
                    ?: top?.diffToLeader,
                    gapToLeader = line.gapToLeader ?: history?.gapToLeader
                    ?: history?.timeDiffToFastest ?: top?.diffToLeader,
                    interval = line.intervalToAhead ?: top?.diffToAhead,
                    lapTime = line.bestLapTime ?: top?.lapTime,
                    totalRaceTime = if (isRaceSession) history?.totalRaceTime else null,
                    stints = if (isRaceSession) {
                        if (line.stints.isNotEmpty()) {
                            line.stints.mapIndexed { index, stint -> (index + 1).toString() to stint }
                                .toMap()
                        } else history?.stints.orEmpty()
                    } else {
                        emptyMap()
                    },
                    bestLapTime = line.bestLapTime ?: top?.lapTime,
                    lastLapTime = line.lastLapTime,
                    inPit = line.inPit,
                    pitOut = line.pitOut,
                    retired = line.retired,
                    stopped = line.stopped,
                    knockOut = line.knockedOut
                )
            }
        }

        if (!sessionPath.isNullOrBlank()) {
            repository.getHistorySessionClassification(
                sessionPath,
                status.session?.type,
                forceRefresh = true,
                allowCacheFallback = false,
                cacheResults = false
            ).getOrNull()?.let { history ->
                    if (history.isNotEmpty()) {
                        val isPractice = status.session?.type?.contains("Practice", true) == true
                        return history.map { item ->
                            TimingDriverLine(
                                position = item.position,
                                racingNumber = item.driverNumber,
                                driverNumber = item.driverNumber,
                                name = item.fullName,
                                tla = item.tla,
                                team = item.teamName,
                                teamColour = item.teamColour,
                                headshotUrl = item.headshotUrl,
                                gap = if (isPractice) item.timeDiffToFastest
                                    ?: item.gapToLeader else item.gapToLeader,
                                gapToLeader = item.gapToLeader,
                                interval = item.intervalToAhead,
                                lapTime = item.bestLapTime,
                                bestLapTime = item.bestLapTime,
                                totalRaceTime = if (isRaceOrSprint(status)) item.totalRaceTime else null,
                                retired = item.isRetired,
                                inPit = item.inPit,
                                stopped = item.stopped,
                                stints = if (isRaceOrSprint(status)) item.stints else emptyMap()
                            )
                        }
                    }
                }
        }

        return if (sessionDrivers.isNotEmpty()) {
            sessionDrivers.values.mapIndexed { index, driver ->
                TimingDriverLine(
                    position = (index + 1).toString(),
                    racingNumber = driver.driverNumber,
                    driverNumber = driver.driverNumber,
                    name = driver.name,
                    tla = driver.acronym,
                    acronym = driver.acronym,
                    team = driver.team,
                    teamColour = driver.teamColour,
                    headshotUrl = driver.headshotUrl
                )
            }
        } else {
            topThree.values.map { top ->
                TimingDriverLine(
                    position = top.position,
                    racingNumber = top.racingNumber,
                    driverNumber = top.racingNumber,
                    name = top.fullName ?: top.broadcastName,
                    tla = top.tla,
                    acronym = top.tla,
                    team = top.team,
                    teamColour = top.teamColour,
                    headshotUrl = top.getEffectiveHeadshotUrl(),
                    gap = top.diffToLeader,
                    gapToLeader = top.diffToLeader,
                    interval = top.diffToAhead,
                    lapTime = top.lapTime
                )
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel(); loadJob?.cancel(); super.onCleared()
    }
}
