package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.HistoryMeeting
import com.example.data.model.HistorySession
import com.example.data.repository.F1Repository
import com.example.data.repository.MergedHistoryClassification
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Success(val selectedYear: Int, val availableYears: List<Int>, val meetings: List<HistoryMeeting>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

sealed interface HistorySessionDetailUiState {
    data object Idle : HistorySessionDetailUiState
    data object Loading : HistorySessionDetailUiState
    data class Success(
        val meetingName: String, val session: HistorySession, val classification: List<MergedHistoryClassification>
    ) : HistorySessionDetailUiState
    data class Error(val message: String) : HistorySessionDetailUiState
}

class HistoryViewModel(private val repository: F1Repository = F1Repository()) : ViewModel() {
    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private val _sessionDetailState = MutableStateFlow<HistorySessionDetailUiState>(HistorySessionDetailUiState.Idle)
    val sessionDetailState: StateFlow<HistorySessionDetailUiState> = _sessionDetailState.asStateFlow()

    private val currentYear = java.time.Year.now().value
    private val availableYears = (currentYear downTo 2018).toList()
    private var selectedYear = currentYear
    private var loadJob: Job? = null
    private var detailJob: Job? = null

    init { loadHistoryYear(selectedYear) }

    fun selectYear(year: Int) {
        selectedYear = year
        loadHistoryYear(year)
    }

    fun refresh() = loadHistoryYear(selectedYear)

    private fun loadHistoryYear(year: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val previous = _uiState.value as? HistoryUiState.Success
            if (previous == null) _uiState.value = HistoryUiState.Loading
            repository.getHistoryMeetings(year).fold(
                onSuccess = { meetings ->
                    _uiState.value = HistoryUiState.Success(year, availableYears, meetings)
                },
                onFailure = { error ->
                    if (previous != null) {
                        _uiState.value = previous
                    } else {
                        _uiState.value = HistoryUiState.Error(error.localizedMessage ?: "Failed to load archive for $year")
                    }
                }
            )
        }
    }

    fun openSessionDetail(meetingName: String, session: HistorySession) {
        val path = session.path ?: return
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _sessionDetailState.value = HistorySessionDetailUiState.Loading
            repository.getHistorySessionClassification(path, session.type).fold(
                onSuccess = { classification ->
                    _sessionDetailState.value = HistorySessionDetailUiState.Success(meetingName, session, classification)
                },
                onFailure = { error ->
                    _sessionDetailState.value = HistorySessionDetailUiState.Error(
                        error.localizedMessage ?: "Failed to load session timing"
                    )
                }
            )
        }
    }

    fun closeSessionDetail() {
        detailJob?.cancel()
        _sessionDetailState.value = HistorySessionDetailUiState.Idle
    }

    override fun onCleared() {
        loadJob?.cancel()
        detailJob?.cancel()
        super.onCleared()
    }
}
