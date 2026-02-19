package com.ecotrace.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecotrace.app.data.models.*
import com.ecotrace.app.data.repository.EmissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val summary: MonthlySummary? = null,
    val history: List<Pair<String, Double>> = emptyList(),
    val advices: List<Advice> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

@HiltViewModel
class EmissionViewModel @Inject constructor(
    private val repository: EmissionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedMonth.flatMapLatest { ym ->
            repository.getMonthlySummaryFlow(ym.year, ym.monthValue)
        },
        repository.getMonthlyHistoryFlow(),
        _error
    ) { summary, history, error ->
        HomeUiState(
            summary = summary,
            history = history,
            advices = repository.generateAdvices(summary),
            currentMonth = _selectedMonth.value,
            isLoading = false,
            error = error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    val allEntries: StateFlow<List<EmissionEntry>> = repository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun validateInput(category: Category, value: Double): ValidationResult {
        if (value <= 0) {
            return ValidationResult.Error("La valeur doit être supérieure à 0")
        }
        
        val maxValues = mapOf(
            Category.CAR_ESSENCE to 10000.0,
            Category.CAR_DIESEL to 10000.0,
            Category.CAR_ELECTRIQUE to 10000.0,
            Category.CHAUFFAGE_GAZ to 5000.0,
            Category.CHAUFFAGE_FIOUL to 5000.0,
            Category.ELECTRICITE to 10000.0,
            Category.AVION_COURT to 5000.0,
            Category.AVION_LONG to 20000.0,
            Category.TRAIN to 10000.0,
            Category.BOEUF to 100.0,
            Category.PORC_VOLAILLE to 100.0,
            Category.POISSON to 100.0,
            Category.VEGETARIEN to 300.0,
            Category.ACHATS_VETEMENTS to 10000.0,
            Category.ACHATS_ELECTRONIQUE to 50000.0,
            Category.STREAMING to 1000.0
        )
        
        val maxValue = maxValues[category] ?: 10000.0
        if (value > maxValue) {
            return ValidationResult.Error("Valeur trop élevée (max: ${maxValue.toInt()} ${category.unit})")
        }
        
        return ValidationResult.Success
    }

    fun addEntry(category: Category, value: Double, note: String = ""): Boolean {
        val validation = validateInput(category, value)
        if (validation is ValidationResult.Error) {
            _error.value = validation.message
            return false
        }
        
        viewModelScope.launch {
            try {
                repository.addEntry(category, value, note = note)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur lors de l'ajout: ${e.message}"
            }
        }
        return true
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteEntry(id)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur lors de la suppression: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        val next = _selectedMonth.value.plusMonths(1)
        if (!next.isAfter(YearMonth.now())) _selectedMonth.value = next
    }
}
