package com.ecotrace.app.data.repository

import com.ecotrace.app.business.*
import com.ecotrace.app.data.models.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdvancedB2BRepository @Inject constructor(
    private val enhancedCompanyProfileDao: EnhancedCompanyProfileDao,
    private val financialEmissionDao: FinancialEmissionDao,
    private val enhancedCarbonReportDao: EnhancedCarbonReportDao,
    private val consultantProfileDao: ConsultantProfileDao,
    private val clientConsultantRelationDao: ClientConsultantRelationDao,
    private val reportSignatureDao: ReportSignatureDao,
    private val emissionFactorService: EmissionFactorService,
    private val advancedMappingEngine: AdvancedMappingEngine,
    private val currencyConverter: CurrencyConverter,
    private val transactionImporter: TransactionImporter,
    private val reportSignatureService: ReportSignatureService,
    private val consultantDashboardService: ConsultantDashboardService,
    private val pdfReportGenerator: PdfReportGenerator,
    private val csvExporter: CsvExporter
) {

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALISATION
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun initializeApp() {
        // Initialiser les facteurs d'émission par défaut
        emissionFactorService.initializeDefaultFactors()
        
        // Initialiser le dictionnaire de mapping (500+ mots-clés)
        advancedMappingEngine.initializeDictionary()
        
        // Synchroniser les taux de change
        currencyConverter.syncCommonCurrencies()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GESTION ENTREPRISE
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun createOrUpdateCompany(profile: EnhancedCompanyProfile) {
        enhancedCompanyProfileDao.insert(profile)
    }

    fun getCompanyByUserId(userId: String): Flow<EnhancedCompanyProfile?> {
        return enhancedCompanyProfileDao.getByUserIdFlow(userId)
    }

    suspend fun getCompanyById(companyId: String): EnhancedCompanyProfile? {
        return enhancedCompanyProfileDao.getById(companyId)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GESTION ÉMISSIONS AVEC RATIO MONÉTAIRE
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun addFinancialEmission(
        companyId: String,
        date: Long,
        categoryName: String,
        amountEuro: Double,
        originalAmount: Double,
        originalCurrency: String,
        transactionLabel: String,
        supplierName: String = "",
        note: String = ""
    ): FinancialEmissionEntry {
        
        // Convertir en EUR si nécessaire
        val conversion = if (originalCurrency != "EUR") {
            currencyConverter.convertToEur(originalAmount, originalCurrency)
        } else {
            ConversionResult(
                originalAmount = originalAmount,
                convertedAmount = originalAmount,
                fromCurrency = "EUR",
                toCurrency = "EUR",
                rate = 1.0,
                date = LocalDate.now().toEpochDay(),
                source = "direct"
            )
        }

        // Récupérer le facteur d'émission
        val factor = emissionFactorService.getFactorForCategory(categoryName)
            ?: throw IllegalArgumentException("Facteur d'émission non trouvé pour $categoryName")

        // Calculer les émissions
        val kgCo2e = conversion.convertedAmount * factor.kgCo2ePerUnit
        val carbonIntensityRatio = kgCo2e / conversion.convertedAmount

        val entry = FinancialEmissionEntry(
            id = UUID.randomUUID().toString(),
            companyId = companyId,
            date = date,
            categoryName = categoryName,
            scope = factor.scope,
            amountEuro = conversion.convertedAmount,
            originalAmount = originalAmount,
            originalCurrency = originalCurrency,
            exchangeRate = conversion.rate,
            valueInput = conversion.convertedAmount,
            unit = factor.unit,
            emissionFactorKgCo2e = factor.kgCo2ePerUnit,
            emissionFactorSource = factor.source,
            kgCo2e = kgCo2e,
            carbonIntensityRatio = carbonIntensityRatio,
            transactionLabel = transactionLabel,
            supplierName = supplierName,
            note = note,
            isAutoMapped = false,
            mappingConfidence = 1.0,
            suggestedBy = "manual"
        )

        financialEmissionDao.insert(entry)
        return entry
    }

    fun getEmissionsByCompany(companyId: String): Flow<List<FinancialEmissionEntry>> {
        return financialEmissionDao.getAllByCompanyFlow(companyId)
    }

    suspend fun deleteEmission(entryId: String) {
        financialEmissionDao.deleteById(entryId)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IMPORT BANCAIRE INTELLIGENT
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun importBankTransactions(
        csvFile: java.io.File,
        companyId: String
    ): ImportResult {
        return transactionImporter.importFromCsv(csvFile, companyId)
    }

    suspend fun confirmImportedTransactions(
        transactions: List<ImportedTransaction>,
        companyId: String
    ): Int {
        var savedCount = 0

        transactions.forEach { transaction ->
            if (transaction.suggestedCategory != null) {
                try {
                    val suggestion = advancedMappingEngine.suggestMappingAdvanced(
                        transaction.label,
                        transaction.amount,
                        companyId
                    )

                    if (suggestion?.emissionFactor != null) {
                        val conversion = currencyConverter.convertToEur(
                            transaction.amount,
                            "EUR" // Supposer EUR par défaut, à adapter
                        )

                        val kgCo2e = conversion.convertedAmount * suggestion.emissionFactor.kgCo2ePerUnit
                        val carbonIntensityRatio = kgCo2e / conversion.convertedAmount

                        val entry = FinancialEmissionEntry(
                            id = UUID.randomUUID().toString(),
                            companyId = companyId,
                            date = transaction.date,
                            categoryName = transaction.suggestedCategory,
                            scope = suggestion.emissionFactor.scope,
                            amountEuro = conversion.convertedAmount,
                            originalAmount = transaction.amount,
                            originalCurrency = "EUR",
                            exchangeRate = 1.0,
                            valueInput = conversion.convertedAmount,
                            unit = suggestion.emissionFactor.unit,
                            emissionFactorKgCo2e = suggestion.emissionFactor.kgCo2ePerUnit,
                            emissionFactorSource = suggestion.emissionFactor.source,
                            kgCo2e = kgCo2e,
                            carbonIntensityRatio = carbonIntensityRatio,
                            transactionLabel = transaction.label,
                            supplierName = transaction.supplier,
                            note = "Importé automatiquement",
                            isAutoMapped = true,
                            mappingConfidence = transaction.confidence,
                            suggestedBy = "advanced_mapping_engine"
                        )

                        financialEmissionDao.insert(entry)
                        savedCount++
                    }
                } catch (e: Exception) {
                    // Continue même en cas d'erreur
                }
            }
        }

        return savedCount
    }

    suspend fun learnFromCorrection(
        companyId: String,
        entryId: String,
        correctedCategory: String
    ) {
        val entry = financialEmissionDao.getAllByCompanyFlow(companyId).toString()
        // Implémenter la logique d'apprentissage
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GÉNÉRATION DE RAPPORTS
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun generateReport(
        companyId: String,
        periodStart: Long,
        periodEnd: Long
    ): EnhancedCarbonReport {
        val company = enhancedCompanyProfileDao.getById(companyId)
            ?: throw IllegalStateException("Entreprise non trouvée")

        val entries = financialEmissionDao.getByPeriod(companyId, periodStart, periodEnd)

        // Calculs des totaux
        val totalKgCo2e = entries.sumOf { it.kgCo2e }
        val totalSpending = entries.sumOf { it.amountEuro }
        val scope1Kg = entries.filter { it.scope == ScopeType.SCOPE1 }.sumOf { it.kgCo2e }
        val scope2Kg = entries.filter { it.scope == ScopeType.SCOPE2 }.sumOf { it.kgCo2e }
        val scope3Kg = entries.filter { it.scope == ScopeType.SCOPE3 }.sumOf { it.kgCo2e }

        // Intensité carbone globale
        val carbonIntensity = company.calculateCarbonIntensity(totalKgCo2e)

        // Ratio moyen par scope
        val averageRatioByScope = mapOf(
            "SCOPE1" to (if (scope1Kg > 0) scope1Kg / totalSpending else 0.0),
            "SCOPE2" to (if (scope2Kg > 0) scope2Kg / totalSpending else 0.0),
            "SCOPE3" to (if (scope3Kg > 0) scope3Kg / totalSpending else 0.0)
        )

        // Top catégories
        val categoryBreakdown = entries
            .groupBy { it.categoryName }
            .map { (category, entries) ->
                val kgCo2e = entries.sumOf { it.kgCo2e }
                CategoryBreakdown(
                    category = category,
                    kgCo2e = kgCo2e,
                    percentage = (kgCo2e / totalKgCo2e) * 100
                )
            }
            .sortedByDescending { it.kgCo2e }

        // Top fournisseurs
        val topSuppliers = entries
            .filter { it.supplierName.isNotBlank() }
            .groupBy { it.supplierName }
            .map { (supplier, entries) ->
                val spending = entries.sumOf { it.amountEuro }
                val emissions = entries.sumOf { it.kgCo2e }
                SupplierBreakdown(
                    supplierName = supplier,
                    totalSpending = spending,
                    totalKgCo2e = emissions,
                    carbonIntensity = emissions / spending,
                    transactionCount = entries.size
                )
            }
            .sortedByDescending { it.totalKgCo2e }
            .take(10)

        // Plan de réduction
        val reductionPlan = generateReductionPlan(categoryBreakdown, company)

        val report = EnhancedCarbonReport(
            id = UUID.randomUUID().toString(),
            companyId = companyId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            totalKgCo2e = totalKgCo2e,
            scope1Kg = scope1Kg,
            scope2Kg = scope2Kg,
            scope3Kg = scope3Kg,
            totalSpending = totalSpending,
            carbonIntensity = carbonIntensity,
            averageRatioByScope = averageRatioByScope,
            topEmissionCategories = categoryBreakdown,
            topSuppliers = topSuppliers,
            reductionPlan = reductionPlan,
            verificationStatus = VerificationStatus.DRAFT
        )

        enhancedCarbonReportDao.insert(report)
        return report
    }

    private fun generateReductionPlan(
        categories: List<CategoryBreakdown>,
        company: EnhancedCompanyProfile
    ): List<ReductionAction> {
        val actions = mutableListOf<ReductionAction>()

        categories.take(5).forEach { category ->
            when {
                category.category.contains("VEHICULE") -> {
                    actions.add(ReductionAction(
                        title = "Électrifier la flotte de véhicules",
                        description = "Remplacer progressivement les véhicules essence/diesel par des véhicules électriques. Réduction d'émissions jusqu'à 90%.",
                        potentialSavingKgCo2e = category.kgCo2e * 0.9,
                        potentialSavingEuro = category.kgCo2e * 0.9 * 0.05,
                        difficulty = "Moyen"
                    ))
                }
                category.category.contains("ELECTRICITE") -> {
                    actions.add(ReductionAction(
                        title = "Passer à l'électricité verte",
                        description = "Souscrire à un contrat d'électricité 100% renouvelable. Réduction immédiate de 80% des émissions.",
                        potentialSavingKgCo2e = category.kgCo2e * 0.8,
                        potentialSavingEuro = 0.0,
                        difficulty = "Facile"
                    ))
                }
                category.category.contains("AVION") -> {
                    actions.add(ReductionAction(
                        title = "Privilégier le train pour les trajets courts",
                        description = "Remplacer les vols < 800 km par le train. Réduction de 95% des émissions.",
                        potentialSavingKgCo2e = category.kgCo2e * 0.5,
                        potentialSavingEuro = category.kgCo2e * 0.5 * 0.08,
                        difficulty = "Facile"
                    ))
                }
                category.category.contains("CLOUD") -> {
                    actions.add(ReductionAction(
                        title = "Optimiser l'infrastructure cloud",
                        description = "Choisir des régions cloud bas-carbone, éteindre les ressources inutilisées.",
                        potentialSavingKgCo2e = category.kgCo2e * 0.3,
                        potentialSavingEuro = category.kgCo2e * 0.3 * 0.05,
                        difficulty = "Moyen"
                    ))
                }
            }
        }

        return actions
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SIGNATURE DE RAPPORT (MODE CONSULTANT)
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun signReport(
        reportId: String,
        consultantId: String,
        comments: String = ""
    ): ReportSignature {
        return reportSignatureService.signReport(reportId, consultantId, comments)
    }

    suspend fun markReportForReview(
        reportId: String,
        consultantId: String,
        revisionNotes: String
    ) {
        reportSignatureService.markForReview(reportId, consultantId, revisionNotes)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD CONSULTANT
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun getConsultantDashboard(consultantId: String): ConsultantDashboard {
        return consultantDashboardService.getConsultantDashboard(consultantId)
    }

    suspend fun addClient(
        consultantId: String,
        companyId: String,
        monthlyFee: Double
    ): ClientConsultantRelation {
        return consultantDashboardService.addClient(
            consultantId,
            companyId,
            "monthly",
            monthlyFee,
            AccessLevel.FULL_ACCESS
        )
    }

    fun getConsultantClients(consultantId: String): Flow<List<EnhancedCompanyProfile>> {
        return consultantDashboardService.getClientsFlow(consultantId)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun generatePdf(
        companyId: String,
        reportId: String
    ): java.io.File? {
        val company = enhancedCompanyProfileDao.getById(companyId) ?: return null
        val report = enhancedCarbonReportDao.getById(reportId) ?: return null
        
        // Convertir EnhancedCarbonReport en CarbonReport pour le générateur PDF
        val carbonReport = CarbonReport(
            id = report.id,
            companyId = report.companyId,
            periodStart = report.periodStart,
            periodEnd = report.periodEnd,
            totalKgCo2e = report.totalKgCo2e,
            scope1Kg = report.scope1Kg,
            scope2Kg = report.scope2Kg,
            scope3Kg = report.scope3Kg,
            carbonIntensity = report.carbonIntensity,
            topEmissionCategories = report.topEmissionCategories,
            reductionPlan = report.reductionPlan,
            generatedAt = report.generatedAt,
            pdfPath = report.pdfPath
        )
        
        // Convertir EnhancedCompanyProfile en CompanyProfile
        val companyProfile = CompanyProfile(
            id = company.id,
            userId = company.userId,
            companyName = company.companyName,
            sector = company.sector,
            employees = company.employees,
            annualRevenue = company.annualRevenue,
            currency = company.currency,
            country = company.country,
            createdAt = company.createdAt,
            updatedAt = company.updatedAt
        )
        
        return pdfReportGenerator.generateReport(companyProfile, carbonReport)
    }

    suspend fun exportCsv(
        companyId: String,
        outputFile: java.io.File
    ): Boolean {
        val entries = financialEmissionDao.getByPeriod(
            companyId,
            0,
            LocalDate.now().toEpochDay()
        )
        return csvExporter.exportEmissions(entries.map { 
            // Conversion vers B2BEmissionEntry pour l'export
            B2BEmissionEntry(
                id = it.id,
                companyId = it.companyId,
                date = it.date,
                categoryName = it.categoryName,
                scope = it.scope,
                valueInput = it.valueInput,
                unit = it.unit,
                emissionFactorKgCo2e = it.emissionFactorKgCo2e,
                emissionFactorSource = it.emissionFactorSource,
                kgCo2e = it.kgCo2e,
                transactionLabel = it.transactionLabel,
                invoiceReference = it.invoiceReference,
                supplierName = it.supplierName,
                note = it.note,
                isAutoMapped = it.isAutoMapped
            )
        }, outputFile)
    }
}
