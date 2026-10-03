package com.example.ui.history

import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.data.model.HistoryMeeting
import com.example.data.model.HistorySession
import com.example.data.repository.F1Repository
import com.example.data.model.MergedHistoryClassification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Year

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Success(val selectedYear: Int, val availableYears: List<Int>, val meetings: List<HistoryMeeting>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}
sealed interface HistorySessionDetailUiState {
    data object Idle : HistorySessionDetailUiState
    data object Loading : HistorySessionDetailUiState
    data class Success(val meetingName: String, val session: HistorySession, val classification: List<MergedHistoryClassification>) : HistorySessionDetailUiState
    data class Error(val message: String) : HistorySessionDetailUiState
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {\n    private val repository = F1Repository(context = application)
    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private val _sessionDetailState = MutableStateFlow<HistorySessionDetailUiState>(HistorySessionDetailUiState.Idle)
    val sessionDetailState: StateFlow<HistorySessionDetailUiState> = _sessionDetailState.asStateFlow()

    private var selectedYear = Year.now().value
    private var loadJob: kotlinx.coroutines.Job? = null

    init { loadHistoryYear(selectedYear) }

    fun selectYear(year: Int) {
        if (year == selectedYear && _uiState.value is HistoryUiState.Success) return
        selectedYear = year
        loadHistoryYear(year)
    }

    fun refresh() = loadHistoryYear(selectedYear)

    private fun loadHistoryYear(year: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            val yearsResult = repository.getHistoryYears()
            val years = yearsResult.getOrNull().orEmpty()
            val effectiveYear = when {
                year in years -> year
                years.isNotEmpty() -> years.first()
                else -> year
            }
            if (effectiveYear != selectedYear) selectedYear = effectiveYear

            val meetingsResult = repository.getHistoryMeetings(effectiveYear)
            val meetings = meetingsResult.getOrNull()
            if (meetings != null) {
                _uiState.value = HistoryUiState.Success(
                    selectedYear = effectiveYear,
                    availableYears = years.ifEmpty { listOf(effectiveYear) },
                    meetings = meetings
                )
            } else {
                _uiState.value = HistoryUiState.Error(
                    meetingsResult.exceptionOrNull()?.localizedMessage ?: "Failed to load archive for $effectiveYear"
                )
            }
        }
    }

    fun openSessionDetail(meetingName: String, session: HistorySession) {
        val path = session.path ?: return
        viewModelScope.launch {
            _sessionDetailState.value = HistorySessionDetailUiState.Loading
            val result = repository.getHistorySessionClassification(path)
            result.fold(
                onSuccess = { classification ->
                    _sessionDetailState.value = HistorySessionDetailUiState.Success(meetingName, session, classification)
                },
                onFailure = {
                    _sessionDetailState.value = HistorySessionDetailUiState.Error(
                        it.localizedMessage ?: "Failed to load session timing"
                    )
                }
            )
        }
    }

    fun closeSessionDetail() { _sessionDetailState.value = HistorySessionDetailUiState.Idle }
}