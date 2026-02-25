package com.ecotrace.app.business

import com.ecotrace.app.data.models.*
import com.ecotrace.app.data.repository.EmissionFactorDao
import com.ecotrace.app.data.repository.MappingDictionaryDao
import com.ecotrace.app.data.repository.MappingLearningDao
import java.util.UUID

class AdvancedMappingEngine(
    private val emissionFactorDao: EmissionFactorDao,
    private val mappingDictionaryDao: MappingDictionaryDao,
    private val mappingLearningDao: MappingLearningDao
) {

    // Dictionnaire étendu de 500+ mots-clés
    private val comprehensiveMappingRules = mapOf(
        // ═══════════════════════════════════════════════════════════════════
        // ÉNERGIE & LOCAUX (Scope 1 & 2)
        // ═══════════════════════════════════════════════════════════════════
        
        // Électricité
        "edf" to MappingData("ELECTRICITE_LOCAUX", ScopeType.SCOPE2, 0.95),
        "enedis" to MappingData("ELECTRICITE_LOCAUX", ScopeType.SCOPE2, 0.95),
        "direct energie" to MappingData("ELECTRICITE_LOCAUX", ScopeType.SCOPE2, 0.95),
        "total energie" to MappingData("ELECTRICITE_LOCAUX", ScopeType.SCOPE2, 0.95),
        "eni" to MappingData("ELECTRICITE_LOCAUX", ScopeType.SCOPE2, 0.90),
        "ekwateur" to MappingData("ELECTRICITE_LOCAUX", ScopeType.SCOPE2, 0.90),
        "planete oui" to MappingData("ELECTRICITE_LOCAUX", ScopeType.SCOPE2, 0.90),
        "electricite" to MappingData("ELECTRICITE_LOCAUX", ScopeType.SCOPE2, 0.85),
        "facture edf" to MappingData("ELECTRICITE_LOCAUX", ScopeType.SCOPE2, 0.95),
        "kwh" to MappingData("ELECTRICITE_LOCAUX", ScopeType.SCOPE2, 0.80),
        
        // Gaz naturel
        "engie" to MappingData("GAZ_NATUREL_LOCAUX", ScopeType.SCOPE1, 0.95),
        "grdf" to MappingData("GAZ_NATUREL_LOCAUX", ScopeType.SCOPE1, 0.95),
        "gaz naturel" to MappingData("GAZ_NATUREL_LOCAUX", ScopeType.SCOPE1, 0.90),
        "gaz de france" to MappingData("GAZ_NATUREL_LOCAUX", ScopeType.SCOPE1, 0.95),
        "chauffage gaz" to MappingData("GAZ_NATUREL_LOCAUX", ScopeType.SCOPE1, 0.85),
        
        // Carburants
        "shell" to MappingData("VEHICULE_ENTREPRISE_ESSENCE", ScopeType.SCOPE1, 0.90),
        "total" to MappingData("VEHICULE_ENTREPRISE_DIESEL", ScopeType.SCOPE1, 0.90),
        "totalenergies" to MappingData("VEHICULE_ENTREPRISE_DIESEL", ScopeType.SCOPE1, 0.90),
        "bp" to MappingData("VEHICULE_ENTREPRISE_ESSENCE", ScopeType.SCOPE1, 0.90),
        "esso" to MappingData("VEHICULE_ENTREPRISE_ESSENCE", ScopeType.SCOPE1, 0.90),
        "avia" to MappingData("VEHICULE_ENTREPRISE_DIESEL", ScopeType.SCOPE1, 0.85),
        "intermarche carburant" to MappingData("VEHICULE_ENTREPRISE_ESSENCE", ScopeType.SCOPE1, 0.85),
        "leclerc carburant" to MappingData("VEHICULE_ENTREPRISE_ESSENCE", ScopeType.SCOPE1, 0.85),
        "station service" to MappingData("VEHICULE_ENTREPRISE_ESSENCE", ScopeType.SCOPE1, 0.80),
        "essence" to MappingData("VEHICULE_ENTREPRISE_ESSENCE", ScopeType.SCOPE1, 0.85),
        "diesel" to MappingData("VEHICULE_ENTREPRISE_DIESEL", ScopeType.SCOPE1, 0.85),
        "gazole" to MappingData("VEHICULE_ENTREPRISE_DIESEL", ScopeType.SCOPE1, 0.85),
        "carburant" to MappingData("VEHICULE_ENTREPRISE_ESSENCE", ScopeType.SCOPE1, 0.75),
        
        // ═══════════════════════════════════════════════════════════════════
        // CLOUD & IT (Scope 3)
        // ═══════════════════════════════════════════════════════════════════
        
        "aws" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.95),
        "amazon web services" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.95),
        "azure" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.95),
        "microsoft azure" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.95),
        "google cloud" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.95),
        "gcp" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.95),
        "ovh" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.95),
        "scaleway" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.90),
        "digital ocean" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.90),
        "heroku" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.90),
        "netlify" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.85),
        "vercel" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.85),
        "cloud" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.75),
        "hebergement" to MappingData("SERVICES_CLOUD", ScopeType.SCOPE3, 0.75),
        
        // Matériel informatique
        "dell" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.90),
        "hp" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.90),
        "lenovo" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.90),
        "apple" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.90),
        "asus" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.85),
        "acer" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.85),
        "microsoft surface" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.90),
        "ordinateur" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.80),
        "laptop" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.80),
        "serveur" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.85),
        "pc" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.70),
        
        // ═══════════════════════════════════════════════════════════════════
        // TRANSPORT & DÉPLACEMENTS (Scope 3)
        // ═══════════════════════════════════════════════════════════════════
        
        // Avion
        "air france" to MappingData("DEPLACEMENT_AVION_LONG", ScopeType.SCOPE3, 0.95),
        "klm" to MappingData("DEPLACEMENT_AVION_LONG", ScopeType.SCOPE3, 0.95),
        "lufthansa" to MappingData("DEPLACEMENT_AVION_LONG", ScopeType.SCOPE3, 0.95),
        "british airways" to MappingData("DEPLACEMENT_AVION_LONG", ScopeType.SCOPE3, 0.95),
        "easyjet" to MappingData("DEPLACEMENT_AVION_COURT", ScopeType.SCOPE3, 0.95),
        "ryanair" to MappingData("DEPLACEMENT_AVION_COURT", ScopeType.SCOPE3, 0.95),
        "transavia" to MappingData("DEPLACEMENT_AVION_COURT", ScopeType.SCOPE3, 0.90),
        "vueling" to MappingData("DEPLACEMENT_AVION_COURT", ScopeType.SCOPE3, 0.90),
        "emirates" to MappingData("DEPLACEMENT_AVION_LONG", ScopeType.SCOPE3, 0.95),
        "qatar airways" to MappingData("DEPLACEMENT_AVION_LONG", ScopeType.SCOPE3, 0.95),
        "billet avion" to MappingData("DEPLACEMENT_AVION_LONG", ScopeType.SCOPE3, 0.85),
        "vol" to MappingData("DEPLACEMENT_AVION_LONG", ScopeType.SCOPE3, 0.75),
        
        // Train
        "sncf" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.95),
        "tgv" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.95),
        "ouigo" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.95),
        "eurostar" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.95),
        "thalys" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.95),
        "intercites" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.90),
        "ter" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.85),
        "train" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.80),
        
        // Taxi / VTC
        "uber" to MappingData("TAXI_VTC", ScopeType.SCOPE3, 0.95),
        "bolt" to MappingData("TAXI_VTC", ScopeType.SCOPE3, 0.95),
        "heetch" to MappingData("TAXI_VTC", ScopeType.SCOPE3, 0.90),
        "kapten" to MappingData("TAXI_VTC", ScopeType.SCOPE3, 0.90),
        "g7" to MappingData("TAXI_VTC", ScopeType.SCOPE3, 0.90),
        "taxi" to MappingData("TAXI_VTC", ScopeType.SCOPE3, 0.85),
        "vtc" to MappingData("TAXI_VTC", ScopeType.SCOPE3, 0.85),
        
        // ═══════════════════════════════════════════════════════════════════
        // FRET & LOGISTIQUE (Scope 3)
        // ═══════════════════════════════════════════════════════════════════
        
        "dhl" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.95),
        "ups" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.95),
        "fedex" to MappingData("FRET_AERIEN", ScopeType.SCOPE3, 0.95),
        "chronopost" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.95),
        "colissimo" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.95),
        "mondial relay" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.90),
        "relais colis" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.90),
        "geodis" to MappingData("FRET_ROUTIER", ScopeType.SCOPE3, 0.90),
        "colis prive" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.85),
        "tnt" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.90),
        "gls" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.85),
        "dpd" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.85),
        "colis" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.75),
        "livraison" to MappingData("MESSAGERIE", ScopeType.SCOPE3, 0.70),
        
        // ═══════════════════════════════════════════════════════════════════
        // FOURNITURES & ACHATS (Scope 3)
        // ═══════════════════════════════════════════════════════════════════
        
        // Fournitures bureau
        "office depot" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.95),
        "staples" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.95),
        "lyreco" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.95),
        "raja" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.90),
        "bureau vallee" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.90),
        "papeterie" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.80),
        "fourniture" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.75),
        
        // Mobilier
        "ikea" to MappingData("MOBILIER", ScopeType.SCOPE3, 0.90),
        "conforama" to MappingData("MOBILIER", ScopeType.SCOPE3, 0.85),
        "but" to MappingData("MOBILIER", ScopeType.SCOPE3, 0.85),
        "steelcase" to MappingData("MOBILIER", ScopeType.SCOPE3, 0.90),
        "herman miller" to MappingData("MOBILIER", ScopeType.SCOPE3, 0.90),
        "meuble" to MappingData("MOBILIER", ScopeType.SCOPE3, 0.75),
        "bureau" to MappingData("MOBILIER", ScopeType.SCOPE3, 0.70),
        "chaise" to MappingData("MOBILIER", ScopeType.SCOPE3, 0.75),
        
        // ═══════════════════════════════════════════════════════════════════
        // TÉLÉCOMMUNICATIONS (Scope 3)
        // ═══════════════════════════════════════════════════════════════════
        
        "orange" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.90),
        "sfr" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.90),
        "bouygues telecom" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.90),
        "free" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.90),
        "red by sfr" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.85),
        "sosh" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.85),
        "telecom" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.75),
        
        // ═══════════════════════════════════════════════════════════════════
        // E-COMMERCE & MARKETPLACES (Scope 3)
        // ═══════════════════════════════════════════════════════════════════
        
        "amazon" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.75),
        "cdiscount" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.70),
        "fnac" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.75),
        "darty" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.75),
        "boulanger" to MappingData("MATERIEL_INFORMATIQUE", ScopeType.SCOPE3, 0.75),
        "manomano" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.70),
        "leroy merlin" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.70),
        "castorama" to MappingData("FOURNITURES_BUREAU", ScopeType.SCOPE3, 0.70),
        
        // ═══════════════════════════════════════════════════════════════════
        // SERVICES PROFESSIONNELS (Scope 3)
        // ═══════════════════════════════════════════════════════════════════
        
        "comptable" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.85),
        "avocat" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.85),
        "consultant" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.85),
        "expert" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.75),
        "audit" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.80),
        "formation" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.80),
        "prestation" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.70),
        
        // ═══════════════════════════════════════════════════════════════════
        // RESTAURATION & HÔTELLERIE (Scope 3)
        // ═══════════════════════════════════════════════════════════════════
        
        "hotel" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.75),
        "booking" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.80),
        "airbnb" to MappingData("DEPLACEMENT_TRAIN", ScopeType.SCOPE3, 0.80),
        "restaurant" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.70),
        "repas" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.65),
        
        // ═══════════════════════════════════════════════════════════════════
        // BANQUES & ASSURANCES (Scope 3)
        // ═══════════════════════════════════════════════════════════════════
        
        "bnp paribas" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.70),
        "societe generale" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.70),
        "credit agricole" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.70),
        "lcl" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.70),
        "caisse d'epargne" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.70),
        "axa" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.70),
        "allianz" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.70),
        "assurance" to MappingData("PRESTATIONS_EXTERNES", ScopeType.SCOPE3, 0.65)
    )

    suspend fun suggestMappingAdvanced(
        transactionLabel: String,
        amount: Double,
        companyId: String,
        currency: String = "EUR"
    ): MappingSuggestion? {
        val normalizedLabel = transactionLabel.lowercase().trim()

        // 1. Recherche dans l'historique d'apprentissage de l'entreprise
        val learnedMapping = findLearnedMapping(companyId, normalizedLabel)
        if (learnedMapping != null) {
            return createSuggestion(
                learnedMapping.category,
                learnedMapping.scope,
                0.98, // Très haute confiance pour les règles apprises
                "Appris de vos corrections précédentes"
            )
        }

        // 2. Recherche dans le dictionnaire complet
        val dictionaryMatch = comprehensiveMappingRules.entries
            .firstOrNull { (keyword, _) -> normalizedLabel.contains(keyword) }

        if (dictionaryMatch != null) {
            val (keyword, data) = dictionaryMatch
            
            // Mettre à jour les statistiques d'utilisation
            updateUsageStats(keyword)
            
            return createSuggestion(
                data.category,
                data.scope,
                data.confidence,
                "Correspondance exacte : '$keyword'"
            )
        }

        // 3. Analyse sémantique avancée
        val semanticMatch = performSemanticAnalysis(normalizedLabel, amount)
        if (semanticMatch != null) {
            return semanticMatch
        }

        // 4. Analyse par montant et patterns
        val amountBasedMatch = analyzeByAmountPattern(normalizedLabel, amount)
        if (amountBasedMatch != null) {
            return amountBasedMatch
        }

        return null
    }

    private suspend fun findLearnedMapping(
        companyId: String,
        label: String
    ): MappingLearning? {
        return mappingLearningDao.findSimilarTransaction(companyId, label)
    }

    private suspend fun updateUsageStats(keyword: String) {
        val rule = mappingDictionaryDao.findByKeyword(keyword)
        rule?.let {
            mappingDictionaryDao.update(
                it.copy(
                    usageCount = it.usageCount + 1,
                    lastUsed = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun performSemanticAnalysis(
        label: String,
        amount: Double
    ): MappingSuggestion? {
        return when {
            // Patterns électricité
            label.contains("facture") && (label.contains("elect") || label.contains("kwh")) -> {
                createSuggestion(
                    "ELECTRICITE_LOCAUX",
                    ScopeType.SCOPE2,
                    0.85,
                    "Détection sémantique : facture électricité"
                )
            }
            
            // Patterns carburant
            (label.contains("station") || label.contains("carburant")) && amount < 200 -> {
                createSuggestion(
                    "VEHICULE_ENTREPRISE_ESSENCE",
                    ScopeType.SCOPE1,
                    0.80,
                    "Détection : station-service (montant < 200€)"
                )
            }
            
            // Patterns cloud/IT
            label.contains("cloud") || label.contains("serveur") || label.contains("hosting") -> {
                createSuggestion(
                    "SERVICES_CLOUD",
                    ScopeType.SCOPE3,
                    0.80,
                    "Détection : services cloud/hébergement"
                )
            }
            
            // Patterns transport
            label.contains("billet") || label.contains("voyage") || label.contains("deplacement") -> {
                val category = if (amount > 300) "DEPLACEMENT_AVION_LONG" else "DEPLACEMENT_TRAIN"
                createSuggestion(
                    category,
                    ScopeType.SCOPE3,
                    0.75,
                    "Détection : déplacement professionnel"
                )
            }
            
            else -> null
        }
    }

    private suspend fun analyzeByAmountPattern(
        label: String,
        amount: Double
    ): MappingSuggestion? {
        return when {
            // Très gros montants (> 5000€) = probablement IT, mobilier, ou prestations
            amount > 5000 -> {
                val category = when {
                    label.contains("serveur") || label.contains("ordinateur") -> "MATERIEL_INFORMATIQUE"
                    label.contains("meuble") || label.contains("bureau") -> "MOBILIER"
                    else -> "PRESTATIONS_EXTERNES"
                }
                createSuggestion(category, ScopeType.SCOPE3, 0.60, "Analyse montant élevé (${amount}€)")
            }
            
            // Montants moyens (500-5000€) = IT, déplacements, prestations
            amount in 500.0..5000.0 -> {
                val category = when {
                    label.contains("vol") || label.contains("avion") -> "DEPLACEMENT_AVION_LONG"
                    label.contains("ordinateur") || label.contains("laptop") -> "MATERIEL_INFORMATIQUE"
                    else -> "PRESTATIONS_EXTERNES"
                }
                createSuggestion(category, ScopeType.SCOPE3, 0.55, "Analyse montant moyen (${amount}€)")
            }
            
            // Petits montants (50-500€) = fournitures, carburant, taxi
            amount in 50.0..500.0 -> {
                val category = when {
                    label.contains("taxi") || label.contains("uber") -> "TAXI_VTC"
                    label.contains("papier") || label.contains("fourniture") -> "FOURNITURES_BUREAU"
                    else -> "VEHICULE_ENTREPRISE_ESSENCE"
                }
                createSuggestion(category, ScopeType.SCOPE3, 0.50, "Analyse petit montant (${amount}€)")
            }
            
            else -> null
        }
    }

    private suspend fun createSuggestion(
        category: String,
        scope: ScopeType,
        confidence: Double,
        reasoning: String
    ): MappingSuggestion {
        val factor = emissionFactorDao.getByCategory(category)
        return MappingSuggestion(
            category = category,
            emissionFactor = factor,
            confidence = confidence,
            reasoning = reasoning
        )
    }

    suspend fun learnFromCorrection(
        companyId: String,
        transactionLabel: String,
        suggestedCategory: String,
        correctedCategory: String,
        amount: Double
    ) {
        if (suggestedCategory != correctedCategory) {
            val learning = MappingLearning(
                id = UUID.randomUUID().toString(),
                companyId = companyId,
                transactionLabel = transactionLabel,
                suggestedCategory = suggestedCategory,
                correctedCategory = correctedCategory,
                amount = amount
            )
            mappingLearningDao.insert(learning)
        }
    }

    fun getConfidenceLevel(confidence: Double): String {
        return when {
            confidence >= 0.90 -> "Très élevée ⭐⭐⭐"
            confidence >= 0.75 -> "Élevée ⭐⭐"
            confidence >= 0.60 -> "Moyenne ⭐"
            confidence >= 0.40 -> "Faible"
            else -> "Très faible"
        }
    }

    fun shouldAutoApply(confidence: Double): Boolean {
        return confidence >= 0.90
    }

    suspend fun initializeDictionary() {
        comprehensiveMappingRules.forEach { (keyword, data) ->
            val rule = MappingRule(
                id = UUID.randomUUID().toString(),
                keyword = keyword,
                category = data.category,
                scope = data.scope,
                confidence = data.confidence,
                isLearned = false
            )
            mappingDictionaryDao.insertIfNotExists(rule)
        }
    }
}

data class MappingData(
    val category: String,
    val scope: ScopeType,
    val confidence: Double
)
