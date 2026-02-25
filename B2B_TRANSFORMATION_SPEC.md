# 🏢 Carboscan B2B - Spécification de Transformation

## 📋 Vue d'ensemble

Transformation de **Carboscan** d'une application B2C (empreinte carbone personnelle) vers une **solution B2B pour TPE/Freelances** conforme à la méthodologie Bilan Carbone et au protocole GHG.

**Expertise IFC** : Comptabilité carbone professionnelle pour les Très Petites Entreprises à l'international.

---

## 🎯 Objectifs de la Transformation

### 1. Terminologie Business
- ❌ **Supprimer** : Catégories "Vie Quotidienne" (Alimentation personnelle, Transport perso)
- ✅ **Ajouter** : Catégories "Business" (Énergie locaux, Déplacements pro, Achats fournisseurs, Fret, Déchets)

### 2. Structure Scopes GHG Professionnelle
- **Scope 1** : Émissions directes (véhicules entreprise, chauffage locaux)
- **Scope 2** : Énergie indirecte (électricité, vapeur)
- **Scope 3** : Chaîne de valeur (fournisseurs, déplacements pro, déchets, fret)

### 3. Métriques Business
- **Intensité carbone** : kgCO₂e / € CA (Chiffre d'Affaires)
- **Profil Entreprise** : Secteur, Effectif, CA annuel
- **Rapports professionnels** : PDF audit-ready avec plan de réduction

---

## 📊 Mapping B2C → B2B

### Catégories à Transformer

| **B2C (Actuel)** | **B2B (Nouveau)** | **Scope** | **Justification** |
|------------------|-------------------|-----------|-------------------|
| Voiture essence/diesel | Véhicules entreprise | Scope 1 | Flotte professionnelle |
| Chauffage gaz/fioul | Chauffage locaux | Scope 1 | Locaux commerciaux |
| Électricité | Électricité locaux | Scope 2 | Bureaux/ateliers |
| Avion | Déplacements professionnels | Scope 3 | Voyages d'affaires |
| Train | Déplacements professionnels | Scope 3 | Trajets clients |
| Bœuf/Porc/Poisson | ❌ **SUPPRIMER** | - | Non pertinent B2B |
| Repas végétariens | ❌ **SUPPRIMER** | - | Non pertinent B2B |
| Vêtements | Achats fournisseurs | Scope 3 | Uniformes/EPI |
| Électronique | Achats fournisseurs | Scope 3 | Matériel IT |
| Streaming | ❌ **SUPPRIMER** | - | Non pertinent B2B |

### Nouvelles Catégories B2B

| **Catégorie** | **Scope** | **Unité** | **FE (kgCO₂e/unité)** | **Exemples** |
|---------------|-----------|-----------|----------------------|--------------|
| **Énergie & Locaux** | | | | |
| Électricité locaux | Scope 2 | kWh | 0.052 (FR) | Bureaux, ateliers |
| Gaz naturel locaux | Scope 1 | m³ | 2.04 | Chauffage |
| Fioul locaux | Scope 1 | L | 3.17 | Chauffage |
| Climatisation | Scope 1 | kWh | 0.5 | Fuites frigorigènes |
| **Mobilité Professionnelle** | | | | |
| Véhicule entreprise essence | Scope 1 | km | 0.218 | Flotte |
| Véhicule entreprise diesel | Scope 1 | km | 0.171 | Flotte |
| Véhicule entreprise électrique | Scope 1 | km | 0.020 | Flotte |
| Déplacements avion | Scope 3 | km | 0.255 / 0.195 | Voyages d'affaires |
| Déplacements train | Scope 3 | km | 0.004 | Trajets clients |
| Taxi/VTC | Scope 3 | km | 0.218 | Déplacements urbains |
| **Achats & Fournisseurs** | | | | |
| Fournitures bureau | Scope 3 | € | 0.15 | Papier, stylos |
| Matériel informatique | Scope 3 | € | 0.085 | Ordinateurs, serveurs |
| Mobilier | Scope 3 | € | 0.12 | Bureaux, chaises |
| Services cloud | Scope 3 | € | 0.05 | AWS, Azure, Google Cloud |
| Prestations externes | Scope 3 | € | 0.08 | Consultants, sous-traitants |
| Matières premières | Scope 3 | kg | Variable | Selon secteur |
| **Fret & Logistique** | | | | |
| Fret routier | Scope 3 | t.km | 0.062 | Camions |
| Fret maritime | Scope 3 | t.km | 0.011 | Conteneurs |
| Fret aérien | Scope 3 | t.km | 1.1 | Colis express |
| Messagerie | Scope 3 | colis | 0.5 | DHL, UPS, Colissimo |
| **Déchets** | | | | |
| Déchets recyclables | Scope 3 | kg | 0.02 | Papier, carton, plastique |
| Déchets non recyclables | Scope 3 | kg | 0.5 | Ordures ménagères |
| Déchets dangereux | Scope 3 | kg | 1.2 | Chimiques, électroniques |

---

## 🗄️ Architecture de Données

### 1. Nouveau Modèle : `CompanyProfile`

```kotlin
@Entity(tableName = "company_profiles")
data class CompanyProfile(
    @PrimaryKey val id: String,
    val userId: String,
    val companyName: String,
    val sector: BusinessSector,
    val employees: Int,
    val annualRevenue: Double, // € CA annuel
    val currency: String = "EUR",
    val country: String = "FR",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Intensité carbone : kgCO₂e / € CA
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
```

### 2. Modèle Enrichi : `EmissionEntry`

```kotlin
@Entity(tableName = "emission_entries")
data class EmissionEntry(
    @PrimaryKey val id: String,
    val companyId: String, // Lien vers CompanyProfile
    val date: Long,
    val categoryName: String,
    val scope: ScopeType, // SCOPE1, SCOPE2, SCOPE3
    val valueInput: Double,
    val unit: String,
    val emissionFactorKgCo2e: Double, // FE utilisé
    val emissionFactorSource: String, // "ADEME", "EPA", "DEFRA", etc.
    val kgCo2e: Double,
    val transactionLabel: String = "", // "Shell Station", "AWS Invoice", etc.
    val invoiceReference: String = "",
    val supplierName: String = "",
    val note: String = "",
    val isAutoMapped: Boolean = false // Mapping intelligent ou manuel
)

enum class ScopeType(val label: String, val color: Long) {
    SCOPE1("Scope 1 · Émissions directes", 0xFF4ADE80),
    SCOPE2("Scope 2 · Énergie indirecte", 0xFF60A5FA),
    SCOPE3("Scope 3 · Chaîne de valeur", 0xFFF59E0B)
}
```

### 3. Nouveau Modèle : `EmissionFactor`

```kotlin
@Entity(tableName = "emission_factors")
data class EmissionFactor(
    @PrimaryKey val id: String,
    val category: String,
    val scope: ScopeType,
    val unit: String,
    val kgCo2ePerUnit: Double,
    val country: String = "FR", // "FR", "US", "UK", "DE", etc.
    val source: String, // "ADEME 2024", "EPA 2023", "DEFRA 2024"
    val description: String,
    val keywords: List<String> = emptyList(), // Pour mapping intelligent
    val lastUpdated: Long = System.currentTimeMillis()
)
```

### 4. Nouveau Modèle : `CarbonReport`

```kotlin
@Entity(tableName = "carbon_reports")
data class CarbonReport(
    @PrimaryKey val id: String,
    val companyId: String,
    val periodStart: Long,
    val periodEnd: Long,
    val totalKgCo2e: Double,
    val scope1Kg: Double,
    val scope2Kg: Double,
    val scope3Kg: Double,
    val carbonIntensity: Double, // kgCO₂e / € CA
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
    val difficulty: String // "Facile", "Moyen", "Difficile"
)
```

---

## 🧠 Moteur de Mapping Intelligent

### Principe
Analyser le libellé d'une transaction (ex: "Shell Station", "AWS Invoice") et suggérer automatiquement :
1. La **catégorie** d'émission
2. Le **Facteur d'Émission** approprié
3. Le **Scope** GHG

### Implémentation Kotlin

```kotlin
class IntelligentMappingEngine(
    private val emissionFactorDao: EmissionFactorDao
) {
    
    private val mappingRules = mapOf(
        // Énergie
        "shell" to "VEHICULE_ENTREPRISE_ESSENCE",
        "total" to "VEHICULE_ENTREPRISE_DIESEL",
        "bp" to "VEHICULE_ENTREPRISE_ESSENCE",
        "engie" to "GAZ_NATUREL_LOCAUX",
        "edf" to "ELECTRICITE_LOCAUX",
        
        // Cloud & IT
        "aws" to "SERVICES_CLOUD",
        "azure" to "SERVICES_CLOUD",
        "google cloud" to "SERVICES_CLOUD",
        "ovh" to "SERVICES_CLOUD",
        "dell" to "MATERIEL_INFORMATIQUE",
        "apple" to "MATERIEL_INFORMATIQUE",
        
        // Transport
        "air france" to "DEPLACEMENT_AVION",
        "lufthansa" to "DEPLACEMENT_AVION",
        "sncf" to "DEPLACEMENT_TRAIN",
        "uber" to "TAXI_VTC",
        "dhl" to "MESSAGERIE",
        "ups" to "MESSAGERIE",
        "fedex" to "FRET_AERIEN",
        
        // Fournitures
        "office depot" to "FOURNITURES_BUREAU",
        "staples" to "FOURNITURES_BUREAU",
        "ikea" to "MOBILIER"
    )
    
    suspend fun suggestMapping(
        transactionLabel: String,
        amount: Double? = null
    ): MappingSuggestion? {
        val normalizedLabel = transactionLabel.lowercase()
        
        // 1. Recherche par mots-clés
        val matchedCategory = mappingRules.entries
            .firstOrNull { (keyword, _) -> normalizedLabel.contains(keyword) }
            ?.value
        
        if (matchedCategory != null) {
            val factor = emissionFactorDao.getByCategory(matchedCategory)
            return MappingSuggestion(
                category = matchedCategory,
                emissionFactor = factor,
                confidence = 0.85,
                reasoning = "Correspondance mot-clé : '${matchedCategory}'"
            )
        }
        
        // 2. Recherche sémantique (à améliorer avec ML)
        val semanticMatch = findSemanticMatch(normalizedLabel)
        if (semanticMatch != null) {
            return semanticMatch
        }
        
        return null
    }
    
    private suspend fun findSemanticMatch(label: String): MappingSuggestion? {
        // Logique de matching sémantique
        // Peut être amélioré avec TensorFlow Lite ou ML Kit
        return when {
            label.contains("carburant") || label.contains("essence") || label.contains("diesel") 
                -> MappingSuggestion(
                    category = "VEHICULE_ENTREPRISE_ESSENCE",
                    emissionFactor = emissionFactorDao.getByCategory("VEHICULE_ENTREPRISE_ESSENCE"),
                    confidence = 0.70,
                    reasoning = "Détection sémantique : carburant"
                )
            label.contains("électricité") || label.contains("facture edf")
                -> MappingSuggestion(
                    category = "ELECTRICITE_LOCAUX",
                    emissionFactor = emissionFactorDao.getByCategory("ELECTRICITE_LOCAUX"),
                    confidence = 0.75,
                    reasoning = "Détection sémantique : électricité"
                )
            else -> null
        }
    }
}

data class MappingSuggestion(
    val category: String,
    val emissionFactor: EmissionFactor?,
    val confidence: Double, // 0.0 à 1.0
    val reasoning: String
)
```

---

## 📥 Module Import/Export

### 1. Import CSV/Excel

#### Format attendu
```csv
Date,Libellé,Montant,Fournisseur,Catégorie (optionnel)
2024-01-15,Shell Station Paris,85.50,Shell,
2024-01-20,Facture AWS Janvier,450.00,Amazon Web Services,Services Cloud
2024-02-01,Billet Air France CDG-JFK,680.00,Air France,
```

#### Implémentation

```kotlin
class TransactionImporter(
    private val mappingEngine: IntelligentMappingEngine,
    private val emissionRepository: EmissionRepository
) {
    
    suspend fun importFromCsv(
        csvFile: File,
        companyId: String
    ): ImportResult {
        val results = mutableListOf<ImportedTransaction>()
        val errors = mutableListOf<String>()
        
        try {
            csvFile.bufferedReader().useLines { lines ->
                lines.drop(1).forEachIndexed { index, line ->
                    try {
                        val parts = line.split(",")
                        if (parts.size >= 4) {
                            val date = parseDate(parts[0])
                            val label = parts[1]
                            val amount = parts[2].toDoubleOrNull() ?: 0.0
                            val supplier = parts[3]
                            val manualCategory = parts.getOrNull(4)
                            
                            // Mapping intelligent
                            val suggestion = if (manualCategory.isNullOrBlank()) {
                                mappingEngine.suggestMapping(label, amount)
                            } else {
                                null
                            }
                            
                            results.add(ImportedTransaction(
                                date = date,
                                label = label,
                                amount = amount,
                                supplier = supplier,
                                suggestedCategory = suggestion?.category,
                                confidence = suggestion?.confidence ?: 0.0
                            ))
                        }
                    } catch (e: Exception) {
                        errors.add("Ligne ${index + 2}: ${e.message}")
                    }
                }
            }
            
            return ImportResult(
                success = results,
                errors = errors,
                totalProcessed = results.size + errors.size
            )
            
        } catch (e: Exception) {
            return ImportResult(
                success = emptyList(),
                errors = listOf("Erreur lecture fichier: ${e.message}"),
                totalProcessed = 0
            )
        }
    }
    
    private fun parseDate(dateStr: String): Long {
        // Format: YYYY-MM-DD
        return LocalDate.parse(dateStr).toEpochDay()
    }
}

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
```

### 2. Export PDF Professionnel

#### Structure du Rapport

```kotlin
class PdfReportGenerator(
    private val context: Context
) {
    
    fun generateReport(
        company: CompanyProfile,
        report: CarbonReport,
        entries: List<EmissionEntry>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        
        // Page 1 : Synthèse Executive
        val page1 = pdfDocument.startPage(pageInfo)
        drawExecutiveSummary(page1.canvas, company, report)
        pdfDocument.finishPage(page1)
        
        // Page 2 : Répartition par Scope
        val page2 = pdfDocument.startPage(pageInfo)
        drawScopeBreakdown(page2.canvas, report)
        pdfDocument.finishPage(page2)
        
        // Page 3 : Top Catégories
        val page3 = pdfDocument.startPage(pageInfo)
        drawTopCategories(page3.canvas, report.topEmissionCategories)
        pdfDocument.finishPage(page3)
        
        // Page 4 : Plan de Réduction
        val page4 = pdfDocument.startPage(pageInfo)
        drawReductionPlan(page4.canvas, report.reductionPlan)
        pdfDocument.finishPage(page4)
        
        // Sauvegarde
        val file = File(context.filesDir, "rapport_carbone_${company.companyName}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        
        return file
    }
    
    private fun drawExecutiveSummary(canvas: Canvas, company: CompanyProfile, report: CarbonReport) {
        val paint = Paint().apply {
            textSize = 24f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        // Titre
        canvas.drawText("Bilan Carbone ${company.companyName}", 50f, 100f, paint)
        
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        // Période
        val period = "${formatDate(report.periodStart)} - ${formatDate(report.periodEnd)}"
        canvas.drawText("Période : $period", 50f, 140f, paint)
        
        // Métriques clés
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Émissions Totales", 50f, 200f, paint)
        
        paint.textSize = 36f
        paint.color = Color.parseColor("#F59E0B")
        canvas.drawText("${String.format("%.2f", report.totalKgCo2e / 1000)} tCO₂e", 50f, 250f, paint)
        
        // Intensité carbone
        paint.textSize = 18f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Intensité Carbone", 50f, 320f, paint)
        
        paint.textSize = 24f
        paint.color = Color.parseColor("#60A5FA")
        canvas.drawText(
            "${String.format("%.3f", report.carbonIntensity)} kgCO₂e/€ CA",
            50f, 360f, paint
        )
        
        // Benchmark secteur
        val benchmark = company.sector.benchmarkKgCo2ePerEuro
        val vsAverage = ((report.carbonIntensity / benchmark) - 1) * 100
        
        paint.textSize = 14f
        paint.color = if (vsAverage < 0) Color.parseColor("#4ADE80") else Color.parseColor("#EF4444")
        canvas.drawText(
            "${if (vsAverage > 0) "+" else ""}${String.format("%.1f", vsAverage)}% vs moyenne secteur",
            50f, 400f, paint
        )
    }
    
    private fun drawScopeBreakdown(canvas: Canvas, report: CarbonReport) {
        val paint = Paint().apply {
            textSize = 20f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        canvas.drawText("Répartition par Scope GHG", 50f, 100f, paint)
        
        // Graphique en barres
        val maxWidth = 400f
        val barHeight = 60f
        var y = 180f
        
        val scopes = listOf(
            Triple("Scope 1 · Émissions directes", report.scope1Kg, Color.parseColor("#4ADE80")),
            Triple("Scope 2 · Énergie indirecte", report.scope2Kg, Color.parseColor("#60A5FA")),
            Triple("Scope 3 · Chaîne de valeur", report.scope3Kg, Color.parseColor("#F59E0B"))
        )
        
        scopes.forEach { (label, kg, color) ->
            val percentage = (kg / report.totalKgCo2e) * 100
            val barWidth = (kg / report.totalKgCo2e) * maxWidth
            
            // Barre
            paint.color = color
            paint.style = Paint.Style.FILL
            canvas.drawRect(150f, y, 150f + barWidth.toFloat(), y + barHeight, paint)
            
            // Label
            paint.color = Color.BLACK
            paint.textSize = 14f
            canvas.drawText(label, 50f, y + 35f, paint)
            
            // Valeur
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(
                "${String.format("%.1f", kg)} kg (${String.format("%.1f", percentage)}%)",
                160f, y + 35f, paint
            )
            
            y += barHeight + 40f
        }
    }
    
    private fun drawTopCategories(canvas: Canvas, categories: List<CategoryBreakdown>) {
        val paint = Paint().apply {
            textSize = 20f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        canvas.drawText("Top 10 Catégories d'Émissions", 50f, 100f, paint)
        
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        var y = 160f
        categories.take(10).forEachIndexed { index, breakdown ->
            canvas.drawText(
                "${index + 1}. ${breakdown.category}",
                50f, y, paint
            )
            canvas.drawText(
                "${String.format("%.1f", breakdown.kgCo2e)} kg (${String.format("%.1f", breakdown.percentage)}%)",
                350f, y, paint
            )
            y += 30f
        }
    }
    
    private fun drawReductionPlan(canvas: Canvas, actions: List<ReductionAction>) {
        val paint = Paint().apply {
            textSize = 20f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        canvas.drawText("Plan de Réduction Recommandé", 50f, 100f, paint)
        
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        var y = 160f
        actions.take(5).forEach { action ->
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("• ${action.title}", 50f, y, paint)
            
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(action.description, 70f, y + 20f, paint)
            
            paint.color = Color.parseColor("#4ADE80")
            canvas.drawText(
                "Économie potentielle : ${String.format("%.1f", action.potentialSavingKgCo2e)} kg CO₂e",
                70f, y + 40f, paint
            )
            
            paint.color = Color.BLACK
            y += 80f
        }
    }
    
    private fun formatDate(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
}
```

---

## 🌍 Internationalisation des Facteurs d'Émission

### Structure Firebase Remote Config

```json
{
  "emission_factors_FR": {
    "electricity": {
      "value": 0.052,
      "unit": "kgCO2e/kWh",
      "source": "ADEME 2024",
      "description": "Mix électrique français (nucléaire 70%)"
    },
    "natural_gas": {
      "value": 2.04,
      "unit": "kgCO2e/m³",
      "source": "ADEME 2024"
    }
  },
  "emission_factors_US": {
    "electricity": {
      "value": 0.385,
      "unit": "kgCO2e/kWh",
      "source": "EPA 2023",
      "description": "US average grid mix"
    },
    "natural_gas": {
      "value": 2.15,
      "unit": "kgCO2e/m³",
      "source": "EPA 2023"
    }
  },
  "emission_factors_DE": {
    "electricity": {
      "value": 0.485,
      "unit": "kgCO2e/kWh",
      "source": "UBA 2024",
      "description": "German grid mix (coal 30%)"
    }
  },
  "emission_factors_UK": {
    "electricity": {
      "value": 0.233,
      "unit": "kgCO2e/kWh",
      "source": "DEFRA 2024",
      "description": "UK grid mix"
    }
  }
}
```

### Implémentation Kotlin

```kotlin
class EmissionFactorService(
    private val remoteConfig: FirebaseRemoteConfig,
    private val localDao: EmissionFactorDao
) {
    
    suspend fun syncFactorsForCountry(countryCode: String) {
        remoteConfig.fetchAndActivate().await()
        
        val factorsJson = remoteConfig.getString("emission_factors_$countryCode")
        if (factorsJson.isNotEmpty()) {
            val factors = parseFactors(factorsJson, countryCode)
            factors.forEach { localDao.insert(it) }
        }
    }
    
    private fun parseFactors(json: String, country: String): List<EmissionFactor> {
        // Parse JSON et créer objets EmissionFactor
        // Implémentation avec Gson ou kotlinx.serialization
        return emptyList() // Placeholder
    }
    
    suspend fun getFactorForCategory(
        category: String,
        country: String = "FR"
    ): EmissionFactor? {
        return localDao.getByCountryAndCategory(country, category)
            ?: localDao.getByCountryAndCategory("FR", category) // Fallback France
    }
}
```

---

## 📱 Nouvelles Interfaces UI

### 1. Écran Profil Entreprise

```kotlin
@Composable
fun CompanyProfileScreen(
    viewModel: CompanyViewModel,
    onSave: () -> Unit
) {
    var companyName by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf(BusinessSector.SERVICES) }
    var employees by remember { mutableStateOf("") }
    var revenue by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Profil Entreprise",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Nom entreprise
        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("Nom de l'entreprise") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Secteur d'activité
        ExposedDropdownMenuBox(
            expanded = false,
            onExpandedChange = { }
        ) {
            OutlinedTextField(
                value = sector.label,
                onValueChange = { },
                readOnly = true,
                label = { Text("Secteur d'activité") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Effectif
        OutlinedTextField(
            value = employees,
            onValueChange = { employees = it },
            label = { Text("Nombre d'employés") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Chiffre d'affaires
        OutlinedTextField(
            value = revenue,
            onValueChange = { revenue = it },
            label = { Text("Chiffre d'affaires annuel (€)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                viewModel.saveCompanyProfile(
                    CompanyProfile(
                        id = UUID.randomUUID().toString(),
                        userId = "", // From auth
                        companyName = companyName,
                        sector = sector,
                        employees = employees.toIntOrNull() ?: 0,
                        annualRevenue = revenue.toDoubleOrNull() ?: 0.0
                    )
                )
                onSave()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enregistrer")
        }
    }
}
```

### 2. Dashboard B2B

```kotlin
@Composable
fun B2BDashboardScreen(
    company: CompanyProfile,
    currentReport: CarbonReport,
    onGeneratePdf: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // En-tête entreprise
        Text(
            company.companyName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            company.sector.label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Carte Intensité Carbone
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF60A5FA))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Intensité Carbone",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    "${String.format("%.3f", currentReport.carbonIntensity)} kgCO₂e/€ CA",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                val benchmark = company.sector.benchmarkKgCo2ePerEuro
                val vsAverage = ((currentReport.carbonIntensity / benchmark) - 1) * 100
                
                Text(
                    "${if (vsAverage > 0) "+" else ""}${String.format("%.1f", vsAverage)}% vs moyenne secteur",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Répartition Scopes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScopeCard(
                "Scope 1",
                currentReport.scope1Kg,
                currentReport.totalKgCo2e,
                Color(0xFF4ADE80),
                Modifier.weight(1f)
            )
            ScopeCard(
                "Scope 2",
                currentReport.scope2Kg,
                currentReport.totalKgCo2e,
                Color(0xFF60A5FA),
                Modifier.weight(1f)
            )
            ScopeCard(
                "Scope 3",
                currentReport.scope3Kg,
                currentReport.totalKgCo2e,
                Color(0xFFF59E0B),
                Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Bouton génération PDF
        Button(
            onClick = onGeneratePdf,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Générer Rapport PDF")
        }
    }
}

@Composable
fun ScopeCard(
    label: String,
    kgCo2e: Double,
    total: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                "${String.format("%.1f", kgCo2e)} kg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${String.format("%.0f", (kgCo2e / total) * 100)}%",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
```

---

## 🚀 Roadmap de Développement

### Phase 1 : Fondations (2 semaines)
- [ ] Créer nouveaux modèles de données (CompanyProfile, EmissionFactor, CarbonReport)
- [ ] Migration Room Database (v2 → v3)
- [ ] Refactoriser catégories B2C → B2B
- [ ] Créer écran Profil Entreprise
- [ ] Adapter Dashboard pour métriques B2B

### Phase 2 : Mapping Intelligent (1 semaine)
- [ ] Implémenter IntelligentMappingEngine
- [ ] Créer base de données mots-clés
- [ ] Tests unitaires mapping
- [ ] UI pour validation suggestions

### Phase 3 : Import/Export (2 semaines)
- [ ] Parser CSV/Excel
- [ ] Validation et nettoyage données
- [ ] Générateur PDF professionnel
- [ ] Templates de rapports personnalisables
- [ ] Export CSV des données

### Phase 4 : Internationalisation (1 semaine)
- [ ] Intégration Firebase Remote Config
- [ ] Base de données facteurs multi-pays
- [ ] Détection automatique pays
- [ ] Sélecteur manuel pays
- [ ] Tests avec différents pays

### Phase 5 : Rapports Avancés (1 semaine)
- [ ] Moteur de recommandations
- [ ] Calcul plan de réduction
- [ ] Benchmarking sectoriel
- [ ] Graphiques avancés
- [ ] Export multi-formats

### Phase 6 : Tests & Déploiement (1 semaine)
- [ ] Tests unitaires complets
- [ ] Tests d'intégration
- [ ] Tests utilisateurs (beta)
- [ ] Documentation API
- [ ] Déploiement production

---

## 📊 Métriques de Succès

### KPIs Techniques
- ✅ 100% des catégories B2C migrées vers B2B
- ✅ Taux de mapping automatique > 70%
- ✅ Génération PDF < 3 secondes
- ✅ Support 5+ pays (FR, US, UK, DE, ES)

### KPIs Business
- 🎯 Intensité carbone calculée pour 100% des entreprises
- 🎯 Rapports PDF générés mensuellement
- 🎯 Taux d'adoption fonctionnalités B2B > 80%
- 🎯 Satisfaction utilisateurs > 4.5/5

---

## 🔐 Sécurité & Conformité

### RGPD
- ✅ Données entreprise chiffrées localement
- ✅ Export données personnelles
- ✅ Droit à l'oubli
- ✅ Politique de confidentialité B2B

### Audit
- ✅ Traçabilité complète des calculs
- ✅ Sources des facteurs d'émission documentées
- ✅ Historique des modifications
- ✅ Logs d'audit exportables

---

## 📚 Ressources & Références

### Méthodologie
- **GHG Protocol** : https://ghgprotocol.org/
- **ISO 14064** : Norme internationale comptabilité carbone
- **Bilan Carbone® ADEME** : Méthodologie française

### Facteurs d'Émission
- **ADEME Base Carbone** (FR) : https://base-empreinte.ademe.fr/
- **EPA GHG Factors** (US) : https://www.epa.gov/climateleadership
- **DEFRA** (UK) : https://www.gov.uk/government/collections/government-conversion-factors-for-company-reporting
- **UBA** (DE) : https://www.umweltbundesamt.de/

### APIs Externes
- **Carbon Interface** : https://www.carboninterface.com/
- **Climatiq** : https://www.climatiq.io/
- **Open Food Facts** : https://world.openfoodfacts.org/

---

**Version** : 1.0  
**Date** : Février 2026  
**Auteur** : Consultant IFC - Comptabilité Carbone B2B
