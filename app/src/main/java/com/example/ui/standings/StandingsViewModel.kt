package com.example.ui.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConstructorStanding
import com.example.data.model.DriverStanding
import com.example.data.repository.F1Repository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Year

enum class StandingsCategory { DRIVERS, CONSTRUCTORS }

sealed interface StandingsUiState {
    data object Loading : StandingsUiState
    data class Success(val season: Int, val category: StandingsCategory, val drivers: List<DriverStanding>, val constructors: List<ConstructorStanding>) : StandingsUiState
    data class Error(val message: String) : StandingsUiState
}

class StandingsViewModel(private val repository: F1Repository = F1Repository()) : ViewModel() {
    private val _uiState = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val uiState: StateFlow<StandingsUiState> = _uiState.asStateFlow()
    private var currentCategory = StandingsCategory.DRIVERS

    init { loadCurrentStandings() }

    fun selectCategory(category: StandingsCategory) {
        currentCategory = category
        (_uiState.value as? StandingsUiState.Success)?.let { _uiState.value = it.copy(category = category) }
    }

    fun refresh() = loadCurrentStandings()

    private fun loadCurrentStandings() {
        viewModelScope.launch {
            _uiState.value = StandingsUiState.Loading
            val (driversResult, constructorsResult) = coroutineScope {
                val drivers = async { repository.getDriverStandings() }
                val constructors = async { repository.getConstructorStandings() }
                drivers.await() to constructors.await()
            }
            if (driversResult.isSuccess && constructorsResult.isSuccess) {
                _uiState.value = StandingsUiState.Success(
                    season = Year.now().value,
                    category = currentCategory,
                    drivers = driversResult.getOrThrow(),
                    constructors = constructorsResult.getOrThrow()
                )
            } else {
                _uiState.value = StandingsUiState.Error(
                    driversResult.exceptionOrNull()?.localizedMessage
                        ?: constructorsResult.exceptionOrNull()?.localizedMessage
                        ?: "Failed to load current championship standings"
                )
            }
        }
    }
}