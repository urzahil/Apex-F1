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

            val status = statusResult.getOrNull()
            if (status != null) {
                val snapshot = snapshotResult.getOrNull()
                val timing = timingResult.getOrNull()
                val calendar = calendarResult.getOrNull()

                // Find current or matching round from calendar
                val currentRound = calendar?.find { round ->
                    val meetingName = status.session?.meeting?.name ?: ""
                    round.name?.contains(meetingName, ignoreCase = true) == true ||
                            meetingName.contains(round.name ?: "", ignoreCase = true)
                } ?: calendar?.firstOrNull()

                // Load genuine classification from the API:
                // 1. If live timing stream has drivers, use them.
                // 2. Otherwise fetch the real session classification using session.path.
                // 3. Fallback to status.topThree. Never invent fake positions!
                val leaderboard = mutableListOf<TimingDriverLine>()

                if (!timing?.drivers.isNullOrEmpty() && timing.drivers.any { it.position != null }) {
                    leaderboard.addAll(timing.drivers)
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

                    // Fallback to topThree if path classification was not available
                    if (!loadedFromSessionPath && !status.topThree?.lines.isNullOrEmpty()) {
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

                val isLiveSession = status.live == true

                _uiState.value = LiveUiState.Success(
                    status = status,
                    snapshot = snapshot,
                    leaderboard = sortedLeaderboard,
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
