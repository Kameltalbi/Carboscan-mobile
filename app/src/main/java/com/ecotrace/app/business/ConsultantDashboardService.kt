package com.ecotrace.app.business

import com.ecotrace.app.data.models.*
import com.ecotrace.app.data.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ConsultantDashboardService(
    private val consultantProfileDao: ConsultantProfileDao,
    private val clientConsultantRelationDao: ClientConsultantRelationDao,
    private val enhancedCompanyProfileDao: EnhancedCompanyProfileDao,
    private val financialEmissionDao: FinancialEmissionDao,
    private val enhancedCarbonReportDao: EnhancedCarbonReportDao,
    private val reportSignatureDao: ReportSignatureDao
) {

    /**
     * Obtient le dashboard complet d'un consultant
     */
    suspend fun getConsultantDashboard(consultantId: String): ConsultantDashboard {
        val consultant = consultantProfileDao.getById(consultantId)
            ?: throw IllegalArgumentException("Consultant non trouvé")

        val activeClients = clientConsultantRelationDao.getActiveClientsByConsultant(consultantId)
        val companies = enhancedCompanyProfileDao.getByConsultantId(consultantId)

        val clientStats = companies.map { company ->
            getClientStats(company)
        }

        val totalRevenue = activeClients.sumOf { it.monthlyFee }
        val signatureStats = reportSignatureDao.getByConsultantId(consultantId)

        return ConsultantDashboard(
            consultant = consultant,
            totalClients = activeClients.size,
            activeClients = activeClients.count { it.status == RelationStatus.ACTIVE },
            monthlyRevenue = totalRevenue,
            clientStats = clientStats,
            reportsUnderReview = signatureStats.count { it.verificationStatus == VerificationStatus.UNDER_REVIEW },
            reportsSigned = signatureStats.count { it.verificationStatus == VerificationStatus.SIGNED },
            reportsRejected = signatureStats.count { it.verificationStatus == VerificationStatus.REJECTED }
        )
    }

    /**
     * Obtient les statistiques d'un client
     */
    private suspend fun getClientStats(company: EnhancedCompanyProfile): ClientStats {
        val currentMonth = LocalDate.now()
        val startOfMonth = currentMonth.withDayOfMonth(1).toEpochDay()
        val endOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth()).toEpochDay()

        val totalEmissions = financialEmissionDao.getTotalEmissions(
            company.id,
            startOfMonth,
            endOfMonth
        ) ?: 0.0

        val totalSpending = financialEmissionDao.getTotalSpending(
            company.id,
            startOfMonth,
            endOfMonth
        ) ?: 0.0

        val carbonIntensity = if (totalSpending > 0) totalEmissions / totalSpending else 0.0

        val latestReport = enhancedCarbonReportDao.getLatestByCompany(company.id)

        val lowConfidenceEntries = financialEmissionDao.getLowConfidenceMappings(company.id)

        return ClientStats(
            companyId = company.id,
            companyName = company.companyName,
            sector = company.sector,
            currentMonthEmissions = totalEmissions,
            currentMonthSpending = totalSpending,
            carbonIntensity = carbonIntensity,
            lastReportDate = latestReport?.generatedAt,
            lastReportStatus = latestReport?.verificationStatus,
            needsReview = lowConfidenceEntries.isNotEmpty(),
            lowConfidenceCount = lowConfidenceEntries.size,
            progressToTarget = company.calculateReductionProgress(totalEmissions)
        )
    }

    /**
     * Obtient la liste des clients avec alertes
     */
    suspend fun getClientsNeedingAttention(consultantId: String): List<ClientAlert> {
        val companies = enhancedCompanyProfileDao.getByConsultantId(consultantId)
        val alerts = mutableListOf<ClientAlert>()

        companies.forEach { company ->
            // Vérifier les entrées à faible confiance
            val lowConfidence = financialEmissionDao.getLowConfidenceMappings(company.id)
            if (lowConfidence.isNotEmpty()) {
                alerts.add(
                    ClientAlert(
                        companyId = company.id,
                        companyName = company.companyName,
                        alertType = AlertType.LOW_CONFIDENCE_MAPPINGS,
                        severity = AlertSeverity.MEDIUM,
                        message = "${lowConfidence.size} transactions nécessitent une vérification",
                        count = lowConfidence.size
                    )
                )
            }

            // Vérifier les rapports en attente
            val latestReport = enhancedCarbonReportDao.getLatestByCompany(company.id)
            if (latestReport?.verificationStatus == VerificationStatus.DRAFT) {
                val daysSinceCreation = (System.currentTimeMillis() - latestReport.generatedAt) / (1000 * 60 * 60 * 24)
                if (daysSinceCreation > 7) {
                    alerts.add(
                        ClientAlert(
                            companyId = company.id,
                            companyName = company.companyName,
                            alertType = AlertType.REPORT_PENDING,
                            severity = AlertSeverity.HIGH,
                            message = "Rapport en attente depuis $daysSinceCreation jours",
                            count = 1
                        )
                    )
                }
            }

            // Vérifier l'absence de rapport récent
            if (latestReport == null) {
                alerts.add(
                    ClientAlert(
                        companyId = company.id,
                        companyName = company.companyName,
                        alertType = AlertType.NO_RECENT_REPORT,
                        severity = AlertSeverity.LOW,
                        message = "Aucun rapport généré",
                        count = 0
                    )
                )
            }
        }

        return alerts.sortedByDescending { it.severity }
    }

    /**
     * Ajoute un nouveau client
     */
    suspend fun addClient(
        consultantId: String,
        companyId: String,
        contractType: String = "monthly",
        monthlyFee: Double,
        accessLevel: AccessLevel = AccessLevel.FULL_ACCESS
    ): ClientConsultantRelation {
        val relation = ClientConsultantRelation(
            id = java.util.UUID.randomUUID().toString(),
            clientCompanyId = companyId,
            consultantId = consultantId,
            status = RelationStatus.ACTIVE,
            contractType = contractType,
            monthlyFee = monthlyFee,
            accessLevel = accessLevel
        )

        clientConsultantRelationDao.insert(relation)

        // Mettre à jour le profil de l'entreprise
        val company = enhancedCompanyProfileDao.getById(companyId)
        company?.let {
            enhancedCompanyProfileDao.update(it.copy(consultantId = consultantId))
        }

        return relation
    }

    /**
     * Suspend un client
     */
    suspend fun suspendClient(relationId: String) {
        val relation = clientConsultantRelationDao.getActiveClientsByConsultant("")
            .find { it.id == relationId }
            ?: throw IllegalArgumentException("Relation non trouvée")

        clientConsultantRelationDao.update(
            relation.copy(status = RelationStatus.SUSPENDED)
        )
    }

    /**
     * Termine une relation client
     */
    suspend fun terminateClient(relationId: String) {
        val relation = clientConsultantRelationDao.getActiveClientsByConsultant("")
            .find { it.id == relationId }
            ?: throw IllegalArgumentException("Relation non trouvée")

        clientConsultantRelationDao.update(
            relation.copy(
                status = RelationStatus.TERMINATED,
                endDate = System.currentTimeMillis()
            )
        )

        // Retirer le consultant du profil entreprise
        val company = enhancedCompanyProfileDao.getById(relation.clientCompanyId)
        company?.let {
            enhancedCompanyProfileDao.update(it.copy(consultantId = null))
        }
    }

    /**
     * Flow des clients pour un consultant
     */
    fun getClientsFlow(consultantId: String): Flow<List<EnhancedCompanyProfile>> {
        return enhancedCompanyProfileDao.getByConsultantIdFlow(consultantId)
    }

    /**
     * Calcule le revenu mensuel total
     */
    suspend fun calculateMonthlyRevenue(consultantId: String): Double {
        val activeClients = clientConsultantRelationDao.getActiveClientsByConsultant(consultantId)
        return activeClients.sumOf { it.monthlyFee }
    }

    /**
     * Obtient les métriques de performance
     */
    suspend fun getPerformanceMetrics(consultantId: String): PerformanceMetrics {
        val allSignatures = reportSignatureDao.getByConsultantId(consultantId)
        val last30Days = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        
        val recentSignatures = allSignatures.filter { it.signedAt >= last30Days }
        
        return PerformanceMetrics(
            totalReportsSigned = allSignatures.count { it.verificationStatus == VerificationStatus.SIGNED },
            reportsSignedLast30Days = recentSignatures.count { it.verificationStatus == VerificationStatus.SIGNED },
            averageReviewTime = calculateAverageReviewTime(allSignatures),
            clientSatisfactionScore = 0.0, // À implémenter avec feedback client
            activeClients = clientConsultantRelationDao.getActiveClientsByConsultant(consultantId).size
        )
    }

    private fun calculateAverageReviewTime(signatures: List<ReportSignature>): Double {
        // Simplification : on estime 2-3 jours en moyenne
        return 2.5
    }
}

data class ConsultantDashboard(
    val consultant: ConsultantProfile,
    val totalClients: Int,
    val activeClients: Int,
    val monthlyRevenue: Double,
    val clientStats: List<ClientStats>,
    val reportsUnderReview: Int,
    val reportsSigned: Int,
    val reportsRejected: Int
)

data class ClientStats(
    val companyId: String,
    val companyName: String,
    val sector: BusinessSector,
    val currentMonthEmissions: Double,
    val currentMonthSpending: Double,
    val carbonIntensity: Double,
    val lastReportDate: Long?,
    val lastReportStatus: VerificationStatus?,
    val needsReview: Boolean,
    val lowConfidenceCount: Int,
    val progressToTarget: Double
)

data class ClientAlert(
    val companyId: String,
    val companyName: String,
    val alertType: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val count: Int
)

enum class AlertType {
    LOW_CONFIDENCE_MAPPINGS,
    REPORT_PENDING,
    NO_RECENT_REPORT,
    HIGH_EMISSIONS,
    MISSING_DATA
}

enum class AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class PerformanceMetrics(
    val totalReportsSigned: Int,
    val reportsSignedLast30Days: Int,
    val averageReviewTime: Double,
    val clientSatisfactionScore: Double,
    val activeClients: Int
)
