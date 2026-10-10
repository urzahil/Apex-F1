package com.example.ui.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConstructorStanding
import com.example.data.model.DriverStanding
import com.example.data.repository.F1Repository
import kotlinx.coroutines.Job
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
    data class Success(
        val season: Int, val category: StandingsCategory,
        val drivers: List<DriverStanding>, val constructors: List<ConstructorStanding>
    ) : StandingsUiState

    data class Error(val message: String) : StandingsUiState
}

class StandingsViewModel(private val repository: F1Repository = F1Repository()) : ViewModel() {
    private val _uiState = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val uiState: StateFlow<StandingsUiState> = _uiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private var currentCategory = StandingsCategory.DRIVERS
    private var loadJob: Job? = null

    init {
        loadCurrentStandings()
    }

    fun selectCategory(category: StandingsCategory) {
        currentCategory = category
        (_uiState.value as? StandingsUiState.Success)?.let {
            _uiState.value = it.copy(category = category)
        }
    }

    fun refresh() = loadCurrentStandings(forceRefresh = true, userInitiated = true)

    private fun loadCurrentStandings(forceRefresh: Boolean = false, userInitiated: Boolean = false) {
        loadJob?.cancel()
        _isRefreshing.value = userInitiated
        loadJob = viewModelScope.launch {
            val previous = _uiState.value as? StandingsUiState.Success
            if (previous == null) _uiState.value = StandingsUiState.Loading
            coroutineScope {
                val driversDeferred = async { repository.getDriverStandings(forceRefresh) }
                val constructorsDeferred = async { repository.getConstructorStandings(forceRefresh) }
                val sessionDriversDeferred = async { repository.getSessionDrivers() }
                val drivers = driversDeferred.await().getOrNull()
                val constructors = constructorsDeferred.await().getOrNull()
                val sessionDrivers = sessionDriversDeferred.await().getOrNull().orEmpty()
                val sessionDriverByNumber = sessionDrivers.associateBy { it.driverNumber }

                val enrichedDrivers = drivers?.map { driver ->
                    val sessionDriver = sessionDriverByNumber[driver.driverNumber]
                    driver.copy(headshotUrl = driver.headshotUrl ?: sessionDriver?.headshotUrl)
                }

                if (drivers != null || constructors != null || previous != null) {
                    _uiState.value = StandingsUiState.Success(
                        season = Year.now().value, category = currentCategory,
                        drivers = enrichedDrivers ?: previous?.drivers.orEmpty(),
                        constructors = constructors ?: previous?.constructors.orEmpty()
                    )
                } else {
                    val error = driversDeferred.await().exceptionOrNull()?.localizedMessage
                        ?: constructorsDeferred.await().exceptionOrNull()?.localizedMessage
                        ?: "Failed to load current championship standings"
                    _uiState.value = StandingsUiState.Error(error)
                }
                _isRefreshing.value = false
            }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        super.onCleared()
    }
}
