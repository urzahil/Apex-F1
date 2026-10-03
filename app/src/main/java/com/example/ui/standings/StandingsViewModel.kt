package com.example.ui.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConstructorStanding
import com.example.data.model.DriverStanding
import com.example.data.repository.F1Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StandingsCategory {
    DRIVERS, CONSTRUCTORS
}

sealed interface StandingsUiState {
    data object Loading : StandingsUiState
    data class Success(
        val season: Int,
        val category: StandingsCategory,
        val drivers: List<DriverStanding>,
        val constructors: List<ConstructorStanding>
    ) : StandingsUiState
    data class Error(val message: String) : StandingsUiState
}

class StandingsViewModel(
    private val repository: F1Repository = F1Repository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val uiState: StateFlow<StandingsUiState> = _uiState.asStateFlow()

    private var currentCategory = StandingsCategory.DRIVERS

    init {
        loadCurrentStandings()
    }

    fun selectCategory(category: StandingsCategory) {
        currentCategory = category
        val current = _uiState.value
        if (current is StandingsUiState.Success) {
            _uiState.value = current.copy(category = category)
        }
    }

    fun refresh() {
        loadCurrentStandings()
    }

    private fun loadCurrentStandings() {
        viewModelScope.launch {
            _uiState.value = StandingsUiState.Loading
            val driversResult = repository.getDriverStandings()
            val constructorsResult = repository.getConstructorStandings()

            val drivers = driversResult.getOrNull()
            val constructors = constructorsResult.getOrNull()

            if (drivers != null && constructors != null) {
                _uiState.value = StandingsUiState.Success(
                    season = 2026,
                    category = currentCategory,
                    drivers = drivers,
                    constructors = constructors
                )
            } else {
                val err = driversResult.exceptionOrNull()?.localizedMessage
                    ?: constructorsResult.exceptionOrNull()?.localizedMessage
                    ?: "Failed to load current championship standings"
                _uiState.value = StandingsUiState.Error(err)
            }
        }
    }
}
