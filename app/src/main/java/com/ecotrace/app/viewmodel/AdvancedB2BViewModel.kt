package com.ecotrace.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecotrace.app.data.models.*
import com.ecotrace.app.data.repository.AdvancedB2BRepository
import com.ecotrace.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AdvancedB2BViewModel @Inject constructor(
    private val repository: AdvancedB2BRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════

    private val _currentCompany = MutableStateFlow<EnhancedCompanyProfile?>(null)
    val currentCompany: StateFlow<EnhancedCompanyProfile?> = _currentCompany.asStateFlow()

    private val _emissions = MutableStateFlow<List<FinancialEmissionEntry>>(emptyList())
    val emissions: StateFlow<List<FinancialEmissionEntry>> = _emissions.asStateFlow()

    private val _currentReport = MutableStateFlow<EnhancedCarbonReport?>(null)
    val currentReport: StateFlow<EnhancedCarbonReport?> = _currentReport.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Mode Consultant
    private val _consultantDashboard = MutableStateFlow<ConsultantDashboard?>(null)
    val consultantDashboard: StateFlow<ConsultantDashboard?> = _consultantDashboard.asStateFlow()

    private val _consultantClients = MutableStateFlow<List<EnhancedCompanyProfile>>(emptyList())
    val consultantClients: StateFlow<List<EnhancedCompanyProfile>> = _consultantClients.asStateFlow()

    // Métriques calculées
    val totalEmissions: StateFlow<Double> = _emissions.map { entries ->
        entries.sumOf { it.kgCo2e }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val totalSpending: StateFlow<Double> = _emissions.map { entries ->
        entries.sumOf { it.amountEuro }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val averageCarbonIntensity: StateFlow<Double> = combine(
        totalEmissions,
        totalSpending
    ) { emissions, spending ->
        if (spending > 0) emissions / spending else 0.0
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALISATION
    // ═══════════════════════════════════════════════════════════════════════════

    init {
        viewModelScope.launch {
            try {
                repository.initializeApp()
                loadCompany()
            } catch (e: Exception) {
                _error.value = "Erreur d'initialisation : ${e.message}"
            }
        }
    }

    private fun loadCompany() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            repository.getCompanyByUserId(userId).collect { company ->
                _currentCompany.value = company
                company?.let { loadEmissions(it.id) }
            }
        }
    }

    private fun loadEmissions(companyId: String) {
        viewModelScope.launch {
            repository.getEmissionsByCompany(companyId).collect { entries ->
                _emissions.value = entries
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GESTION ENTREPRISE
    // ═══════════════════════════════════════════════════════════════════════════

    fun saveCompanyProfile(
        companyName: String,
        sector: BusinessSector,
        employees: Int,
        annualRevenue: Double,
        fiscalYearStart: Int,
        fiscalYearEnd: Int,
        baselineYear: Int = 2024,
        reductionTargetPercent: Double = 10.0
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = authRepository.getCurrentUserId() ?: return@launch

                val profile = EnhancedCompanyProfile(
                    id = _currentCompany.value?.id ?: java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    companyName = companyName,
                    sector = sector,
                    employees = employees,
                    annualRevenue = annualRevenue,
                    fiscalYearStart = fiscalYearStart,
                    fiscalYearEnd = fiscalYearEnd,
                    baselineYear = baselineYear,
                    reductionTargetPercent = reductionTargetPercent
                )

                repository.createOrUpdateCompany(profile)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur sauvegarde : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AJOUT ÉMISSION MANUELLE
    // ═══════════════════════════════════════════════════════════════════════════

    fun addEmission(
        categoryName: String,
        amountEuro: Double,
        originalAmount: Double = amountEuro,
        originalCurrency: String = "EUR",
        transactionLabel: String,
        supplierName: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val companyId = _currentCompany.value?.id ?: return@launch

                repository.addFinancialEmission(
                    companyId = companyId,
                    date = LocalDate.now().toEpochDay(),
                    categoryName = categoryName,
                    amountEuro = amountEuro,
                    originalAmount = originalAmount,
                    originalCurrency = originalCurrency,
                    transactionLabel = transactionLabel,
                    supplierName = supplierName,
                    note = note
                )

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur ajout émission : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEmission(entryId: String) {
        viewModelScope.launch {
            try {
                repository.deleteEmission(entryId)
            } catch (e: Exception) {
                _error.value = "Erreur suppression : ${e.message}"
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IMPORT BANCAIRE
    // ═══════════════════════════════════════════════════════════════════════════

    fun importCsv(file: File) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val companyId = _currentCompany.value?.id ?: return@launch

                val result = repository.importBankTransactions(file, companyId)
                _importResult.value = result

                if (result.success.isNotEmpty()) {
                    // Auto-confirmer les transactions avec haute confiance (>90%)
                    val highConfidence = result.success.filter { it.confidence >= 0.90 }
                    if (highConfidence.isNotEmpty()) {
                        repository.confirmImportedTransactions(highConfidence, companyId)
                    }
                }

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur import : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmImportedTransactions(transactions: List<ImportedTransaction>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val companyId = _currentCompany.value?.id ?: return@launch

                val savedCount = repository.confirmImportedTransactions(transactions, companyId)
                _importResult.value = null // Réinitialiser après confirmation

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur confirmation : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GÉNÉRATION DE RAPPORT
    // ═══════════════════════════════════════════════════════════════════════════

    fun generateReport(periodStart: Long, periodEnd: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val companyId = _currentCompany.value?.id ?: return@launch

                val report = repository.generateReport(companyId, periodStart, periodEnd)
                _currentReport.value = report

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur génération rapport : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateMonthlyReport() {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1).toEpochDay()
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).toEpochDay()
        generateReport(startOfMonth, endOfMonth)
    }

    fun generateYearlyReport() {
        val now = LocalDate.now()
        val startOfYear = now.withDayOfYear(1).toEpochDay()
        val endOfYear = now.withDayOfYear(now.lengthOfYear()).toEpochDay()
        generateReport(startOfYear, endOfYear)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SIGNATURE DE RAPPORT (MODE CONSULTANT)
    // ═══════════════════════════════════════════════════════════════════════════

    fun signReport(consultantId: String, comments: String = "") {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val reportId = _currentReport.value?.id ?: return@launch

                repository.signReport(reportId, consultantId, comments)
                
                // Recharger le rapport pour voir la signature
                _currentReport.value = _currentReport.value?.copy(
                    verificationStatus = VerificationStatus.SIGNED
                )

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur signature : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markForReview(consultantId: String, revisionNotes: String) {
        viewModelScope.launch {
            try {
                val reportId = _currentReport.value?.id ?: return@launch
                repository.markReportForReview(reportId, consultantId, revisionNotes)
                
                _currentReport.value = _currentReport.value?.copy(
                    verificationStatus = VerificationStatus.UNDER_REVIEW
                )
            } catch (e: Exception) {
                _error.value = "Erreur révision : ${e.message}"
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD CONSULTANT
    // ═══════════════════════════════════════════════════════════════════════════

    fun loadConsultantDashboard(consultantId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val dashboard = repository.getConsultantDashboard(consultantId)
                _consultantDashboard.value = dashboard

                // Charger la liste des clients
                repository.getConsultantClients(consultantId).collect { clients ->
                    _consultantClients.value = clients
                }

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erreur chargement dashboard : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addClient(consultantId: String, companyId: String, monthlyFee: Double) {
        viewModelScope.launch {
            try {
                repository.addClient(consultantId, companyId, monthlyFee)
                loadConsultantDashboard(consultantId) // Recharger
            } catch (e: Exception) {
                _error.value = "Erreur ajout client : ${e.message}"
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT
    // ═══════════════════════════════════════════════════════════════════════════

    fun generatePdf(): File? {
        val companyId = _currentCompany.value?.id ?: return null
        val reportId = _currentReport.value?.id ?: return null

        return try {
            viewModelScope.launch {
                _isLoading.value = true
                repository.generatePdf(companyId, reportId)
                _isLoading.value = false
            }
            null // Retourner de manière asynchrone
        } catch (e: Exception) {
            _error.value = "Erreur génération PDF : ${e.message}"
            null
        }
    }

    fun exportCsv(outputFile: File) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val companyId = _currentCompany.value?.id ?: return@launch

                val success = repository.exportCsv(companyId, outputFile)
                if (!success) {
                    _error.value = "Erreur export CSV"
                }
            } catch (e: Exception) {
                _error.value = "Erreur export : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    fun clearError() {
        _error.value = null
    }

    fun getEmissionsByScope(scope: ScopeType): List<FinancialEmissionEntry> {
        return _emissions.value.filter { it.scope == scope }
    }

    fun getEmissionsByCategory(category: String): List<FinancialEmissionEntry> {
        return _emissions.value.filter { it.categoryName == category }
    }

    fun getTopSuppliers(limit: Int = 10): List<Pair<String, Double>> {
        return _emissions.value
            .filter { it.supplierName.isNotBlank() }
            .groupBy { it.supplierName }
            .map { (supplier, entries) -> supplier to entries.sumOf { it.kgCo2e } }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun getCarbonIntensityByCategory(): Map<String, Double> {
        return _emissions.value
            .groupBy { it.categoryName }
            .mapValues { (_, entries) ->
                val totalEmissions = entries.sumOf { it.kgCo2e }
                val totalSpending = entries.sumOf { it.amountEuro }
                if (totalSpending > 0) totalEmissions / totalSpending else 0.0
            }
    }
}
