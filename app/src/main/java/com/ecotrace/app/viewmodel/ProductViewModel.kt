package com.ecotrace.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecotrace.app.data.models.ProductInfo
import com.ecotrace.app.data.models.ScannedProduct
import com.ecotrace.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class ProductUiState(
    val scannedProducts: List<ScannedProduct> = emptyList(),
    val totalKgCo2e: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProductUiState> = combine(
        _selectedMonth.flatMapLatest { ym ->
            repository.getScannedProductsForMonth(ym.year, ym.monthValue)
        },
        _error
    ) { products, error ->
        ProductUiState(
            scannedProducts = products,
            totalKgCo2e = products.sumOf { it.totalKgCo2e },
            isLoading = false,
            error = error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProductUiState()
    )

    val allScannedProducts: StateFlow<List<ScannedProduct>> = 
        repository.getAllScannedProductsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        initializeProductDatabase()
    }

    private fun initializeProductDatabase() {
        viewModelScope.launch {
            try {
                val count = repository.getProductDatabaseCount()
                if (count == 0) {
                    repository.getDefaultProductDatabase().forEach { product ->
                        repository.saveProductInfo(product)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Erreur d'initialisation: ${e.message}"
            }
        }
    }

    suspend fun getProductInfo(barcode: String): ProductInfo? {
        return try {
            repository.getProductInfoByBarcode(barcode)
        } catch (e: Exception) {
            _error.value = "Erreur de recherche: ${e.message}"
            null
        }
    }

    fun addScannedProduct(
        barcode: String,
        name: String,
        brand: String,
        category: String,
        kgCo2ePer100g: Double,
        weight: Double,
        imageUrl: String = ""
    ) {
        viewModelScope.launch {
            try {
                repository.addScannedProduct(
                    barcode = barcode,
                    name = name,
                    brand = brand,
                    category = category,
                    kgCo2ePer100g = kgCo2ePer100g,
                    weight = weight,
                    imageUrl = imageUrl
                )
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur d'ajout: ${e.message}"
            }
        }
    }

    fun deleteScannedProduct(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteScannedProduct(id)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur de suppression: ${e.message}"
            }
        }
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        val next = _selectedMonth.value.plusMonths(1)
        if (!next.isAfter(YearMonth.now())) _selectedMonth.value = next
    }

    fun clearError() {
        _error.value = null
    }
}
