package com.ecotrace.app.business

import com.ecotrace.app.data.models.EmissionFactor
import com.ecotrace.app.data.models.MappingSuggestion
import com.ecotrace.app.data.repository.EmissionFactorDao

class IntelligentMappingEngine(
    private val emissionFactorDao: EmissionFactorDao
) {

    private val mappingRules = mapOf(
        "shell" to "VEHICULE_ENTREPRISE_ESSENCE",
        "total" to "VEHICULE_ENTREPRISE_DIESEL",
        "bp" to "VEHICULE_ENTREPRISE_ESSENCE",
        "esso" to "VEHICULE_ENTREPRISE_ESSENCE",
        "engie" to "GAZ_NATUREL_LOCAUX",
        "edf" to "ELECTRICITE_LOCAUX",
        "direct energie" to "ELECTRICITE_LOCAUX",
        "aws" to "SERVICES_CLOUD",
        "amazon web services" to "SERVICES_CLOUD",
        "azure" to "SERVICES_CLOUD",
        "microsoft azure" to "SERVICES_CLOUD",
        "google cloud" to "SERVICES_CLOUD",
        "gcp" to "SERVICES_CLOUD",
        "ovh" to "SERVICES_CLOUD",
        "dell" to "MATERIEL_INFORMATIQUE",
        "apple" to "MATERIEL_INFORMATIQUE",
        "lenovo" to "MATERIEL_INFORMATIQUE",
        "hp" to "MATERIEL_INFORMATIQUE",
        "air france" to "DEPLACEMENT_AVION_LONG",
        "lufthansa" to "DEPLACEMENT_AVION_LONG",
        "easyjet" to "DEPLACEMENT_AVION_COURT",
        "ryanair" to "DEPLACEMENT_AVION_COURT",
        "sncf" to "DEPLACEMENT_TRAIN",
        "uber" to "TAXI_VTC",
        "bolt" to "TAXI_VTC",
        "dhl" to "MESSAGERIE",
        "ups" to "MESSAGERIE",
        "fedex" to "FRET_AERIEN",
        "chronopost" to "MESSAGERIE",
        "colissimo" to "MESSAGERIE",
        "office depot" to "FOURNITURES_BUREAU",
        "staples" to "FOURNITURES_BUREAU",
        "ikea" to "MOBILIER",
        "amazon" to "FOURNITURES_BUREAU",
        "cdiscount" to "FOURNITURES_BUREAU"
    )

    private val semanticKeywords = mapOf(
        "carburant" to "VEHICULE_ENTREPRISE_ESSENCE",
        "essence" to "VEHICULE_ENTREPRISE_ESSENCE",
        "diesel" to "VEHICULE_ENTREPRISE_DIESEL",
        "gazole" to "VEHICULE_ENTREPRISE_DIESEL",
        "électricité" to "ELECTRICITE_LOCAUX",
        "electricite" to "ELECTRICITE_LOCAUX",
        "facture edf" to "ELECTRICITE_LOCAUX",
        "gaz" to "GAZ_NATUREL_LOCAUX",
        "chauffage" to "GAZ_NATUREL_LOCAUX",
        "cloud" to "SERVICES_CLOUD",
        "serveur" to "SERVICES_CLOUD",
        "hébergement" to "SERVICES_CLOUD",
        "ordinateur" to "MATERIEL_INFORMATIQUE",
        "laptop" to "MATERIEL_INFORMATIQUE",
        "pc" to "MATERIEL_INFORMATIQUE",
        "vol" to "DEPLACEMENT_AVION_LONG",
        "avion" to "DEPLACEMENT_AVION_LONG",
        "billet" to "DEPLACEMENT_AVION_LONG",
        "train" to "DEPLACEMENT_TRAIN",
        "tgv" to "DEPLACEMENT_TRAIN",
        "taxi" to "TAXI_VTC",
        "vtc" to "TAXI_VTC",
        "colis" to "MESSAGERIE",
        "livraison" to "MESSAGERIE",
        "fret" to "FRET_ROUTIER",
        "transport" to "FRET_ROUTIER",
        "fourniture" to "FOURNITURES_BUREAU",
        "papier" to "FOURNITURES_BUREAU",
        "bureau" to "FOURNITURES_BUREAU",
        "meuble" to "MOBILIER",
        "mobilier" to "MOBILIER"
    )

    suspend fun suggestMapping(
        transactionLabel: String,
        amount: Double? = null
    ): MappingSuggestion? {
        val normalizedLabel = transactionLabel.lowercase().trim()

        // 1. Recherche exacte par nom de fournisseur
        val exactMatch = mappingRules.entries
            .firstOrNull { (keyword, _) -> normalizedLabel.contains(keyword) }

        if (exactMatch != null) {
            val (keyword, category) = exactMatch
            val factor = emissionFactorDao.getByCategory(category)
            return MappingSuggestion(
                category = category,
                emissionFactor = factor,
                confidence = 0.90,
                reasoning = "Correspondance exacte : '$keyword' → $category"
            )
        }

        // 2. Recherche sémantique par mots-clés
        val semanticMatch = semanticKeywords.entries
            .firstOrNull { (keyword, _) -> normalizedLabel.contains(keyword) }

        if (semanticMatch != null) {
            val (keyword, category) = semanticMatch
            val factor = emissionFactorDao.getByCategory(category)
            return MappingSuggestion(
                category = category,
                emissionFactor = factor,
                confidence = 0.75,
                reasoning = "Détection sémantique : '$keyword' → $category"
            )
        }

        // 3. Analyse contextuelle basée sur le montant
        if (amount != null) {
            return analyzeByAmount(normalizedLabel, amount)
        }

        return null
    }

    private suspend fun analyzeByAmount(label: String, amount: Double): MappingSuggestion? {
        return when {
            // Montants élevés (> 1000€) souvent = IT, cloud, mobilier
            amount > 1000 && (label.contains("facture") || label.contains("invoice")) -> {
                val category = when {
                    label.contains("cloud") || label.contains("aws") || label.contains("azure") -> "SERVICES_CLOUD"
                    label.contains("ordinateur") || label.contains("serveur") -> "MATERIEL_INFORMATIQUE"
                    else -> "PRESTATIONS_EXTERNES"
                }
                MappingSuggestion(
                    category = category,
                    emissionFactor = emissionFactorDao.getByCategory(category),
                    confidence = 0.60,
                    reasoning = "Analyse montant élevé (${amount}€) + contexte"
                )
            }

            // Montants moyens (100-1000€) = fournitures, déplacements
            amount in 100.0..1000.0 -> {
                val category = when {
                    label.contains("billet") || label.contains("vol") -> "DEPLACEMENT_AVION_COURT"
                    label.contains("train") -> "DEPLACEMENT_TRAIN"
                    label.contains("hotel") -> "DEPLACEMENT_TRAIN"
                    else -> "FOURNITURES_BUREAU"
                }
                MappingSuggestion(
                    category = category,
                    emissionFactor = emissionFactorDao.getByCategory(category),
                    confidence = 0.55,
                    reasoning = "Analyse montant moyen (${amount}€) + contexte"
                )
            }

            // Petits montants (< 100€) = carburant, taxi, fournitures
            amount < 100 -> {
                val category = when {
                    label.contains("station") || label.contains("carburant") -> "VEHICULE_ENTREPRISE_ESSENCE"
                    label.contains("taxi") || label.contains("uber") -> "TAXI_VTC"
                    else -> "FOURNITURES_BUREAU"
                }
                MappingSuggestion(
                    category = category,
                    emissionFactor = emissionFactorDao.getByCategory(category),
                    confidence = 0.50,
                    reasoning = "Analyse petit montant (${amount}€) + contexte"
                )
            }

            else -> null
        }
    }

    suspend fun batchSuggestMapping(
        transactions: List<Pair<String, Double?>>
    ): List<MappingSuggestion?> {
        return transactions.map { (label, amount) ->
            suggestMapping(label, amount)
        }
    }

    fun getConfidenceLevel(confidence: Double): String {
        return when {
            confidence >= 0.85 -> "Très élevée"
            confidence >= 0.70 -> "Élevée"
            confidence >= 0.55 -> "Moyenne"
            confidence >= 0.40 -> "Faible"
            else -> "Très faible"
        }
    }

    fun shouldAutoApply(confidence: Double): Boolean {
        return confidence >= 0.85
    }
}
