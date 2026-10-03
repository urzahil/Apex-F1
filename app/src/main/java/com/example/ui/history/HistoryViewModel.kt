package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.HistoryMeeting
import com.example.data.model.HistorySession
import com.example.data.repository.F1Repository
import com.example.data.repository.MergedHistoryClassification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Success(
        val selectedYear: Int,
        val availableYears: List<Int>,
        val meetings: List<HistoryMeeting>
    ) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

sealed interface HistorySessionDetailUiState {
    data object Idle : HistorySessionDetailUiState
    data object Loading : HistorySessionDetailUiState
    data class Success(
        val meetingName: String,
        val session: HistorySession,
        val classification: List<MergedHistoryClassification>
    ) : HistorySessionDetailUiState
    data class Error(val message: String) : HistorySessionDetailUiState
}

class HistoryViewModel(
    private val repository: F1Repository = F1Repository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _sessionDetailState = MutableStateFlow<HistorySessionDetailUiState>(HistorySessionDetailUiState.Idle)
    val sessionDetailState: StateFlow<HistorySessionDetailUiState> = _sessionDetailState.asStateFlow()

    private val availableYears = listOf(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018)
    private var selectedYear = 2026

    init {
        loadHistoryYear(selectedYear)
    }

    fun selectYear(year: Int) {
        selectedYear = year
        loadHistoryYear(year)
    }

    fun refresh() {
        loadHistoryYear(selectedYear)
    }

    private fun loadHistoryYear(year: Int) {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            val meetingsResult = repository.getHistoryMeetings(year)
            val meetings = meetingsResult.getOrNull()

            if (meetings != null) {
                _uiState.value = HistoryUiState.Success(
                    selectedYear = year,
                    availableYears = availableYears,
                    meetings = meetings
                )
            } else {
                val err = meetingsResult.exceptionOrNull()?.localizedMessage
                    ?: "Failed to load archive for $year"
                _uiState.value = HistoryUiState.Error(err)
            }
        }
    }

    fun openSessionDetail(meetingName: String, session: HistorySession) {
        val path = session.path ?: return
        viewModelScope.launch {
            _sessionDetailState.value = HistorySessionDetailUiState.Loading
            val result = repository.getHistorySessionClassification(path)
            val classification = result.getOrNull()
            if (classification != null) {
                _sessionDetailState.value = HistorySessionDetailUiState.Success(
                    meetingName = meetingName,
                    session = session,
                    classification = classification
                )
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to load session timing"
                _sessionDetailState.value = HistorySessionDetailUiState.Error(err)
            }
        }
    }

    fun closeSessionDetail() {
        _sessionDetailState.value = HistorySessionDetailUiState.Idle
    }
}
