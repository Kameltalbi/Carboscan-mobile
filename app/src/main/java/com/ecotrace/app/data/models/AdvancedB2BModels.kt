package com.ecotrace.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

// ── Entrée d'Émission Enrichie avec Ratio Monétaire ────────────────────────
@Entity(tableName = "financial_emission_entries")
data class FinancialEmissionEntry(
    @PrimaryKey val id: String,
    val companyId: String,
    val date: Long,
    val categoryName: String,
    val scope: ScopeType,
    
    // Données financières
    val amountEuro: Double,
    val originalAmount: Double,
    val originalCurrency: String = "EUR",
    val exchangeRate: Double = 1.0,
    
    // Données carbone
    val valueInput: Double,
    val unit: String,
    val emissionFactorKgCo2e: Double,
    val emissionFactorSource: String,
    val kgCo2e: Double,
    
    // Ratio monétaire (KPI clé IFC)
    val carbonIntensityRatio: Double, // kgCO₂e / €
    
    // Métadonnées transaction
    val transactionLabel: String = "",
    val invoiceReference: String = "",
    val supplierName: String = "",
    val supplierCategory: String = "",
    val paymentMethod: String = "",
    val note: String = "",
    
    // Auto-mapping
    val isAutoMapped: Boolean = false,
    val mappingConfidence: Double = 0.0,
    val suggestedBy: String = "system"
) {
    val localDate: java.time.LocalDate get() = java.time.LocalDate.ofEpochDay(date)
    
    // Calcul automatique du ratio si non fourni
    fun calculateRatio(): Double {
        return if (amountEuro > 0) kgCo2e / amountEuro else 0.0
    }
}

// ── Profil Consultant IFC ───────────────────────────────────────────────────
@Entity(tableName = "consultant_profiles")
data class ConsultantProfile(
    @PrimaryKey val id: String,
    val userId: String,
    val fullName: String,
    val certification: String, // "IFC", "Bilan Carbone®", "ISO 14064"
    val certificationNumber: String = "",
    val company: String = "",
    val siret: String = "",
    val email: String,
    val phone: String = "",
    val logoUrl: String = "",
    val signature: String = "", // Base64 de la signature
    val hourlyRate: Double = 0.0,
    val currency: String = "EUR",
    @TypeConverters(StringListConverter::class)
    val clientIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

// ── Signature de Rapport ────────────────────────────────────────────────────
@Entity(tableName = "report_signatures")
data class ReportSignature(
    @PrimaryKey val id: String,
    val reportId: String,
    val consultantId: String,
    val consultantName: String,
    val certification: String,
    val signedAt: Long = System.currentTimeMillis(),
    val verificationStatus: VerificationStatus,
    val comments: String = "",
    val revisionNotes: String = "",
    val digitalSignature: String, // Hash SHA-256
    val qrCodeData: String = "" // Pour vérification
)

enum class VerificationStatus(val label: String, val color: Long) {
    DRAFT("Brouillon", 0xFF9CA3AF),
    UNDER_REVIEW("En révision", 0xFFF59E0B),
    VERIFIED("Vérifié", 0xFF4ADE80),
    REJECTED("Rejeté", 0xFFEF4444),
    SIGNED("Signé", 0xFF3B82F6)
}

// ── Relation Client-Consultant ──────────────────────────────────────────────
@Entity(tableName = "client_consultant_relations")
data class ClientConsultantRelation(
    @PrimaryKey val id: String,
    val clientCompanyId: String,
    val consultantId: String,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val status: RelationStatus,
    val contractType: String = "monthly", // "monthly", "annual", "per_report"
    val monthlyFee: Double = 0.0,
    val currency: String = "EUR",
    val accessLevel: AccessLevel
)

enum class RelationStatus {
    ACTIVE,
    SUSPENDED,
    TERMINATED
}

enum class AccessLevel(val label: String) {
    READ_ONLY("Lecture seule"),
    REVIEW("Révision"),
    FULL_ACCESS("Accès complet")
}

// ── Taux de Change ──────────────────────────────────────────────────────────
@Entity(tableName = "exchange_rates")
data class ExchangeRate(
    @PrimaryKey val id: String,
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val date: Long,
    val source: String = "ExchangeRate-API"
)

// ── Dictionnaire de Mapping Intelligent ─────────────────────────────────────
@Entity(tableName = "mapping_dictionary")
data class MappingRule(
    @PrimaryKey val id: String,
    val keyword: String,
    val category: String,
    val scope: ScopeType,
    val confidence: Double, // 0.0 à 1.0
    val supplierName: String = "",
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val country: String = "ALL",
    val isLearned: Boolean = false, // Appris des corrections utilisateur
    val usageCount: Int = 0,
    val lastUsed: Long = 0
)

// ── Historique d'Apprentissage ──────────────────────────────────────────────
@Entity(tableName = "mapping_learning_history")
data class MappingLearning(
    @PrimaryKey val id: String,
    val companyId: String,
    val transactionLabel: String,
    val suggestedCategory: String,
    val correctedCategory: String,
    val amount: Double,
    val learnedAt: Long = System.currentTimeMillis()
)

// ── Profil Entreprise Enrichi ───────────────────────────────────────────────
@Entity(tableName = "enhanced_company_profiles")
data class EnhancedCompanyProfile(
    @PrimaryKey val id: String,
    val userId: String,
    val companyName: String,
    val sector: BusinessSector,
    val employees: Int,
    
    // Données financières
    val annualRevenue: Double,
    val fiscalYearStart: Int, // Mois (1-12)
    val fiscalYearEnd: Int,
    val currency: String = "EUR",
    val country: String = "FR",
    
    // Objectifs carbone
    val baselineYear: Int = 2024,
    val baselineEmissions: Double = 0.0,
    val reductionTargetPercent: Double = 0.0, // Ex: -10% = 10.0
    val targetYear: Int = 2030,
    
    // Consultant assigné
    val consultantId: String? = null,
    
    // Métadonnées
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun calculateCarbonIntensity(totalKgCo2e: Double): Double {
        return if (annualRevenue > 0) totalKgCo2e / annualRevenue else 0.0
    }
    
    fun calculateReductionProgress(currentEmissions: Double): Double {
        if (baselineEmissions == 0.0) return 0.0
        val actualReduction = ((baselineEmissions - currentEmissions) / baselineEmissions) * 100
        return (actualReduction / reductionTargetPercent) * 100 // % de l'objectif atteint
    }
}

// ── Rapport Carbone Enrichi ─────────────────────────────────────────────────
@Entity(tableName = "enhanced_carbon_reports")
@TypeConverters(
    CategoryBreakdownListConverter::class,
    ReductionActionListConverter::class,
    FinancialMetricsConverter::class
)
data class EnhancedCarbonReport(
    @PrimaryKey val id: String,
    val companyId: String,
    val periodStart: Long,
    val periodEnd: Long,
    
    // Métriques carbone
    val totalKgCo2e: Double,
    val scope1Kg: Double,
    val scope2Kg: Double,
    val scope3Kg: Double,
    
    // Métriques financières
    val totalSpending: Double,
    val carbonIntensity: Double, // kgCO₂e / €
    val averageRatioByScope: Map<String, Double>,
    
    // Analyse
    val topEmissionCategories: List<CategoryBreakdown>,
    val topSuppliers: List<SupplierBreakdown>,
    val reductionPlan: List<ReductionAction>,
    
    // Signature
    val signatureId: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.DRAFT,
    
    // Métadonnées
    val generatedAt: Long = System.currentTimeMillis(),
    val pdfPath: String = "",
    val version: Int = 1
)

data class SupplierBreakdown(
    val supplierName: String,
    val totalSpending: Double,
    val totalKgCo2e: Double,
    val carbonIntensity: Double,
    val transactionCount: Int
)

data class FinancialMetrics(
    val totalSpending: Double,
    val spendingByScope: Map<String, Double>,
    val spendingByCategory: Map<String, Double>,
    val averageTransactionAmount: Double
)

// ── Type Converter pour FinancialMetrics ────────────────────────────────────
class FinancialMetricsConverter {
    private val gson = com.google.gson.Gson()
    
    @androidx.room.TypeConverter
    fun fromFinancialMetrics(value: FinancialMetrics?): String {
        return gson.toJson(value)
    }
    
    @androidx.room.TypeConverter
    fun toFinancialMetrics(value: String): FinancialMetrics? {
        return try {
            gson.fromJson(value, FinancialMetrics::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
