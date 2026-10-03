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
    private var refreshJob: Job? = null
    private var isAutoRefreshEnabled = true
    private var isScreenActive = false

    init { refresh() }

    fun setScreenActive(active: Boolean) {
        if (isScreenActive == active) return
        isScreenActive = active
        if (active && isAutoRefreshEnabled) startPolling() else pollingJob?.cancel()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = loadLiveData()
    }

    fun toggleAutoRefresh() {
        isAutoRefreshEnabled = !isAutoRefreshEnabled
        if (isAutoRefreshEnabled && isScreenActive) startPolling() else pollingJob?.cancel()
        (_uiState.value as? LiveUiState.Success)?.let { _uiState.value = it.copy(autoRefresh = isAutoRefreshEnabled) }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive && isScreenActive && isAutoRefreshEnabled) {
                delay(30_000)
                if (isActive && isScreenActive && isAutoRefreshEnabled) refresh()
            }
        }
    }

    private fun loadLiveData(isSilent: Boolean = false): Job = viewModelScope.launch {
        if (!isSilent && _uiState.value !is LiveUiState.Success) _uiState.value = LiveUiState.Loading

        val (statusResult, snapshotResult, timingResult, calendarResult) = coroutineScope {
            val status = async { repository.getStatus() }
            val snapshot = async { repository.getSnapshot() }
            val timing = async { repository.getTiming() }
            val calendar = async { repository.getCalendar() }
            Quad(status.await(), snapshot.await(), timing.await(), calendar.await())
        }

        val status = statusResult.getOrNull() ?: run {
            if (_uiState.value !is LiveUiState.Success) {
                _uiState.value = LiveUiState.Error(statusResult.exceptionOrNull()?.localizedMessage ?: "Failed to connect to F1 Live API")
            }
            return@launch
        }

        val snapshot = snapshotResult.getOrNull()
        val timing = timingResult.getOrNull()
        val calendar = calendarResult.getOrNull()
        val meetingName = status.session?.meeting?.name.orEmpty()
        val currentRound = calendar?.find { round ->
            val roundName = round.name.orEmpty()
            roundName.isNotEmpty() && (roundName.contains(meetingName, true) || meetingName.contains(roundName, true))
        } ?: calendar?.firstOrNull()

        val leaderboard = if (!timing?.drivers.isNullOrEmpty() && timing!!.drivers.any { it.position != null }) {
            timing.drivers
        } else {
            val sessionPath = status.session?.path
            val sessionClass = sessionPath?.takeIf { it.isNotBlank() }?.let { repository.getHistorySessionClassification(it).getOrNull() }
            if (!sessionClass.isNullOrEmpty()) {
                val isPractice = status.session?.type?.contains("Practice", true) == true || status.session?.name?.contains("Practice", true) == true
                sessionClass.map { item ->
                    val gap = if (isPractice) item.timeDiffToFastest ?: item.gapToLeader else item.gapToLeader ?: item.intervalToAhead
                    TimingDriverLine(
                        position = item.position, racingNumber = item.driverNumber, driverNumber = item.driverNumber,
                        name = item.fullName, tla = item.tla, team = item.teamName, teamColour = item.teamColour,
                        gap = gap, interval = item.intervalToAhead, lapTime = item.bestLapTime,
                        retired = item.isRetired, inPit = item.inPit, stopped = item.stopped
                    )
                }
            } else {
                status.topThree?.lines.orEmpty().map { top ->
                    TimingDriverLine(
                        position = top.position, racingNumber = top.racingNumber, driverNumber = top.racingNumber,
                        name = top.fullName ?: top.broadcastName, tla = top.tla, team = top.team,
                        teamColour = top.teamColour, gap = top.diffToLeader, interval = top.diffToAhead, lapTime = top.lapTime
                    )
                }
            }
        }.sortedBy { it.getDisplayPosition().toIntOrNull() ?: 999 }

        _uiState.value = LiveUiState.Success(status, snapshot, leaderboard, currentRound, status.live == true, isAutoRefreshEnabled)
    }

    private data class Quad(
        val status: Result<com.example.data.model.StatusResponse>,
        val snapshot: Result<com.example.data.model.SnapshotResponse>,
        val timing: Result<com.example.data.model.TimingResponse>,
        val calendar: Result<List<CalendarRound>>
    )

    override fun onCleared() {
        pollingJob?.cancel()
        refreshJob?.cancel()
        super.onCleared()
    }
}