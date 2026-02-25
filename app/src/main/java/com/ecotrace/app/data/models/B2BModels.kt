package com.ecotrace.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

// ── Profil Entreprise ───────────────────────────────────────────────────────
@Entity(tableName = "company_profiles")
data class CompanyProfile(
    @PrimaryKey val id: String,
    val userId: String,
    val companyName: String,
    val sector: BusinessSector,
    val employees: Int,
    val annualRevenue: Double,
    val currency: String = "EUR",
    val country: String = "FR",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun calculateCarbonIntensity(totalKgCo2e: Double): Double {
        return if (annualRevenue > 0) totalKgCo2e / annualRevenue else 0.0
    }
}

enum class BusinessSector(val label: String, val benchmarkKgCo2ePerEuro: Double) {
    SERVICES("Services", 0.05),
    COMMERCE("Commerce", 0.08),
    INDUSTRIE("Industrie", 0.15),
    CONSTRUCTION("Construction", 0.12),
    TRANSPORT("Transport & Logistique", 0.18),
    RESTAURATION("Restauration", 0.10),
    AGRICULTURE("Agriculture", 0.20),
    TECH("Tech & Digital", 0.04),
    SANTE("Santé", 0.06),
    EDUCATION("Éducation", 0.03),
    AUTRE("Autre", 0.10)
}

// ── Catégories B2B ──────────────────────────────────────────────────────────
enum class B2BCategory(
    val label: String,
    val icon: String,
    val scope: ScopeType,
    val unit: String,
    val factorKgCo2PerUnit: Double,
    val hint: String
) {
    // SCOPE 1 — Émissions directes
    VEHICULE_ENTREPRISE_ESSENCE(
        "Véhicule entreprise essence", "🚗", ScopeType.SCOPE1, "km",
        0.218, "km parcourus par la flotte"
    ),
    VEHICULE_ENTREPRISE_DIESEL(
        "Véhicule entreprise diesel", "🚙", ScopeType.SCOPE1, "km",
        0.171, "km parcourus par la flotte"
    ),
    VEHICULE_ENTREPRISE_ELECTRIQUE(
        "Véhicule entreprise électrique", "⚡", ScopeType.SCOPE1, "km",
        0.020, "km parcourus par la flotte"
    ),
    GAZ_NATUREL_LOCAUX(
        "Gaz naturel locaux", "🔥", ScopeType.SCOPE1, "m³",
        2.04, "m³ consommés (chauffage bureaux)"
    ),
    FIOUL_LOCAUX(
        "Fioul locaux", "🛢️", ScopeType.SCOPE1, "L",
        3.17, "litres consommés (chauffage)"
    ),
    CLIMATISATION(
        "Climatisation (fuites)", "❄️", ScopeType.SCOPE1, "kWh",
        0.5, "kWh consommés + fuites frigorigènes"
    ),

    // SCOPE 2 — Énergie indirecte
    ELECTRICITE_LOCAUX(
        "Électricité locaux", "💡", ScopeType.SCOPE2, "kWh",
        0.052, "kWh consommés (bureaux/ateliers)"
    ),

    // SCOPE 3 — Chaîne de valeur
    DEPLACEMENT_AVION_COURT(
        "Déplacement avion court-courrier", "✈️", ScopeType.SCOPE3, "km",
        0.255, "km de vol professionnel"
    ),
    DEPLACEMENT_AVION_LONG(
        "Déplacement avion long-courrier", "🌍", ScopeType.SCOPE3, "km",
        0.195, "km de vol professionnel"
    ),
    DEPLACEMENT_TRAIN(
        "Déplacement train", "🚂", ScopeType.SCOPE3, "km",
        0.004, "km trajets clients/fournisseurs"
    ),
    TAXI_VTC(
        "Taxi / VTC", "🚕", ScopeType.SCOPE3, "km",
        0.218, "km déplacements urbains"
    ),
    FOURNITURES_BUREAU(
        "Fournitures bureau", "📎", ScopeType.SCOPE3, "€",
        0.15, "euros dépensés (papier, stylos, etc.)"
    ),
    MATERIEL_INFORMATIQUE(
        "Matériel informatique", "💻", ScopeType.SCOPE3, "€",
        0.085, "euros dépensés (ordinateurs, serveurs)"
    ),
    MOBILIER(
        "Mobilier", "🪑", ScopeType.SCOPE3, "€",
        0.12, "euros dépensés (bureaux, chaises)"
    ),
    SERVICES_CLOUD(
        "Services cloud", "☁️", ScopeType.SCOPE3, "€",
        0.05, "euros dépensés (AWS, Azure, GCP)"
    ),
    PRESTATIONS_EXTERNES(
        "Prestations externes", "🤝", ScopeType.SCOPE3, "€",
        0.08, "euros dépensés (consultants, sous-traitants)"
    ),
    MATIERES_PREMIERES(
        "Matières premières", "📦", ScopeType.SCOPE3, "kg",
        0.5, "kg achetés (selon secteur)"
    ),
    FRET_ROUTIER(
        "Fret routier", "🚚", ScopeType.SCOPE3, "t.km",
        0.062, "tonnes × kilomètres"
    ),
    FRET_MARITIME(
        "Fret maritime", "🚢", ScopeType.SCOPE3, "t.km",
        0.011, "tonnes × kilomètres"
    ),
    FRET_AERIEN(
        "Fret aérien", "✈️", ScopeType.SCOPE3, "t.km",
        1.1, "tonnes × kilomètres"
    ),
    MESSAGERIE(
        "Messagerie / Colis", "📮", ScopeType.SCOPE3, "colis",
        0.5, "nombre de colis envoyés"
    ),
    DECHETS_RECYCLABLES(
        "Déchets recyclables", "♻️", ScopeType.SCOPE3, "kg",
        0.02, "kg de déchets (papier, carton, plastique)"
    ),
    DECHETS_NON_RECYCLABLES(
        "Déchets non recyclables", "🗑️", ScopeType.SCOPE3, "kg",
        0.5, "kg de déchets (ordures ménagères)"
    ),
    DECHETS_DANGEREUX(
        "Déchets dangereux", "☢️", ScopeType.SCOPE3, "kg",
        1.2, "kg de déchets (chimiques, électroniques)"
    )
}

// ── Type de Scope GHG ───────────────────────────────────────────────────────
enum class ScopeType(val label: String, val color: Long, val description: String) {
    SCOPE1(
        "Scope 1 · Émissions directes",
        0xFF4ADE80,
        "Émissions directes de sources détenues ou contrôlées par l'entreprise"
    ),
    SCOPE2(
        "Scope 2 · Énergie indirecte",
        0xFF60A5FA,
        "Émissions indirectes liées à l'énergie achetée (électricité, vapeur)"
    ),
    SCOPE3(
        "Scope 3 · Chaîne de valeur",
        0xFFF59E0B,
        "Autres émissions indirectes (fournisseurs, déplacements, déchets)"
    )
}

// ── Facteur d'Émission ──────────────────────────────────────────────────────
@Entity(tableName = "emission_factors")
data class EmissionFactor(
    @PrimaryKey val id: String,
    val category: String,
    val scope: ScopeType,
    val unit: String,
    val kgCo2ePerUnit: Double,
    val country: String = "FR",
    val source: String,
    val description: String,
    @TypeConverters(StringListConverter::class)
    val keywords: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

// ── Entrée d'Émission B2B ───────────────────────────────────────────────────
@Entity(tableName = "b2b_emission_entries")
data class B2BEmissionEntry(
    @PrimaryKey val id: String,
    val companyId: String,
    val date: Long,
    val categoryName: String,
    val scope: ScopeType,
    val valueInput: Double,
    val unit: String,
    val emissionFactorKgCo2e: Double,
    val emissionFactorSource: String,
    val kgCo2e: Double,
    val transactionLabel: String = "",
    val invoiceReference: String = "",
    val supplierName: String = "",
    val note: String = "",
    val isAutoMapped: Boolean = false
) {
    val localDate: LocalDate get() = LocalDate.ofEpochDay(date)
}

// ── Rapport Carbone ─────────────────────────────────────────────────────────
@Entity(tableName = "carbon_reports")
@TypeConverters(CategoryBreakdownListConverter::class, ReductionActionListConverter::class)
data class CarbonReport(
    @PrimaryKey val id: String,
    val companyId: String,
    val periodStart: Long,
    val periodEnd: Long,
    val totalKgCo2e: Double,
    val scope1Kg: Double,
    val scope2Kg: Double,
    val scope3Kg: Double,
    val carbonIntensity: Double,
    val topEmissionCategories: List<CategoryBreakdown>,
    val reductionPlan: List<ReductionAction>,
    val generatedAt: Long = System.currentTimeMillis(),
    val pdfPath: String = ""
)

data class CategoryBreakdown(
    val category: String,
    val kgCo2e: Double,
    val percentage: Double
)

data class ReductionAction(
    val title: String,
    val description: String,
    val potentialSavingKgCo2e: Double,
    val potentialSavingEuro: Double,
    val difficulty: String
)

// ── Suggestion de Mapping ───────────────────────────────────────────────────
data class MappingSuggestion(
    val category: String,
    val emissionFactor: EmissionFactor?,
    val confidence: Double,
    val reasoning: String
)

// ── Transaction Importée ────────────────────────────────────────────────────
data class ImportedTransaction(
    val date: Long,
    val label: String,
    val amount: Double,
    val supplier: String,
    val suggestedCategory: String?,
    val confidence: Double
)

data class ImportResult(
    val success: List<ImportedTransaction>,
    val errors: List<String>,
    val totalProcessed: Int
)

// ── Type Converters pour Room ───────────────────────────────────────────────
class StringListConverter {
    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toString(list: List<String>): String {
        return Gson().toJson(list)
    }
}

class CategoryBreakdownListConverter {
    @TypeConverter
    fun fromString(value: String): List<CategoryBreakdown> {
        val listType = object : TypeToken<List<CategoryBreakdown>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toString(list: List<CategoryBreakdown>): String {
        return Gson().toJson(list)
    }
}

class ReductionActionListConverter {
    @TypeConverter
    fun fromString(value: String): List<ReductionAction> {
        val listType = object : TypeToken<List<ReductionAction>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toString(list: List<ReductionAction>): String {
        return Gson().toJson(list)
    }
}
