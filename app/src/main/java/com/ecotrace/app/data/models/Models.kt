package com.ecotrace.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

// ── User Model ──────────────────────────────────────────────────────────────
@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

// ── Scope GHG Protocol ──────────────────────────────────────────────────────
enum class Scope(val label: String, val color: Long) {
    SCOPE1("Scope 1 · Direct", 0xFF4ADE80),
    SCOPE2("Scope 2 · Énergie", 0xFF60A5FA),
    SCOPE3("Scope 3 · Indirect", 0xFFF59E0B)
}

// ── Catégories d'émissions ──────────────────────────────────────────────────
enum class Category(
    val label: String,
    val icon: String,
    val scope: Scope,
    val unit: String,
    val factorKgCo2PerUnit: Double, // kg CO2e par unité
    val hint: String
) {
    // SCOPE 1 — Émissions directes
    CAR_ESSENCE(
        "Voiture essence", "🚗", Scope.SCOPE1, "km",
        0.218, "km parcourus ce mois"
    ),
    CAR_DIESEL(
        "Voiture diesel", "🚙", Scope.SCOPE1, "km",
        0.171, "km parcourus ce mois"
    ),
    CAR_ELECTRIQUE(
        "Voiture électrique", "⚡", Scope.SCOPE1, "km",
        0.020, "km parcourus ce mois"
    ),
    CHAUFFAGE_GAZ(
        "Chauffage gaz", "🔥", Scope.SCOPE1, "m³",
        2.04, "m³ consommés ce mois"
    ),
    CHAUFFAGE_FIOUL(
        "Chauffage fioul", "🛢️", Scope.SCOPE1, "L",
        3.17, "litres consommés ce mois"
    ),

    // SCOPE 2 — Énergie indirecte
    ELECTRICITE(
        "Électricité", "💡", Scope.SCOPE2, "kWh",
        0.052, "kWh consommés ce mois (facture)"
    ),

    // SCOPE 3 — Autres émissions indirectes
    AVION_COURT(
        "Avion court-courrier", "✈️", Scope.SCOPE3, "km",
        0.255, "km de vol (aller simple)"
    ),
    AVION_LONG(
        "Avion long-courrier", "🌍", Scope.SCOPE3, "km",
        0.195, "km de vol (aller simple)"
    ),
    TRAIN(
        "Train", "🚂", Scope.SCOPE3, "km",
        0.004, "km parcourus"
    ),
    BOEUF(
        "Bœuf / agneau", "🥩", Scope.SCOPE3, "kg",
        27.0, "kg consommés ce mois"
    ),
    PORC_VOLAILLE(
        "Porc / volaille", "🍗", Scope.SCOPE3, "kg",
        6.0, "kg consommés ce mois"
    ),
    POISSON(
        "Poisson", "🐟", Scope.SCOPE3, "kg",
        3.0, "kg consommés ce mois"
    ),
    VEGETARIEN(
        "Repas végétariens", "🥗", Scope.SCOPE3, "repas",
        0.5, "repas végétariens ce mois"
    ),
    ACHATS_VETEMENTS(
        "Vêtements", "👕", Scope.SCOPE3, "€",
        0.025, "euros dépensés"
    ),
    ACHATS_ELECTRONIQUE(
        "Électronique", "📱", Scope.SCOPE3, "€",
        0.085, "euros dépensés"
    ),
    STREAMING(
        "Streaming vidéo", "📺", Scope.SCOPE3, "h",
        0.036, "heures de streaming ce mois"
    ),
}

// ── Entrée d'émission (Room Entity) ─────────────────────────────────────────
@Entity(tableName = "emission_entries")
data class EmissionEntry(
    @PrimaryKey val id: String,
    val date: Long, // epoch days
    val categoryName: String,
    val valueInput: Double,   // valeur saisie par l'utilisateur
    val kgCo2e: Double,       // kg CO2e calculés
    val note: String = ""
) {
    val category: Category get() = Category.valueOf(categoryName)
    val localDate: LocalDate get() = LocalDate.ofEpochDay(date)
    val scope: Scope get() = category.scope
}

// ── Résumé mensuel ───────────────────────────────────────────────────────────
data class MonthlySummary(
    val year: Int,
    val month: Int,
    val totalKgCo2e: Double,
    val scope1Kg: Double,
    val scope2Kg: Double,
    val scope3Kg: Double,
    val entries: List<EmissionEntry>
) {
    val tCo2e: Double get() = totalKgCo2e / 1000.0
    // Moyenne française ~9 tCO2e/an = 750 kg/mois
    val vsFranceMoyenne: Double get() = (totalKgCo2e / 750.0) * 100
    // Objectif 2050 : 2 tCO2e/an = ~167 kg/mois
    val vsObjectif2050: Double get() = (totalKgCo2e / 167.0) * 100
}

// ── Conseil personnalisé ─────────────────────────────────────────────────────
data class Advice(
    val icon: String,
    val title: String,
    val description: String,
    val savingKg: Double,
    val category: Category
)

// ── Produit scanné ──────────────────────────────────────────────────────────
@Entity(tableName = "scanned_products")
data class ScannedProduct(
    @PrimaryKey val id: String,
    val barcode: String,
    val name: String,
    val brand: String = "",
    val category: String = "",
    val kgCo2ePer100g: Double,
    val weight: Double,
    val date: Long,
    val imageUrl: String = ""
) {
    val totalKgCo2e: Double get() = (weight / 100.0) * kgCo2ePer100g
    val localDate: LocalDate get() = LocalDate.ofEpochDay(date)
}

// ── Base de données produits (cache local) ──────────────────────────────────
@Entity(tableName = "product_database")
data class ProductInfo(
    @PrimaryKey val barcode: String,
    val name: String,
    val brand: String = "",
    val category: String = "",
    val kgCo2ePer100g: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)
