# 🚀 Guide d'Implémentation B2B - Carboscan

## 📋 Vue d'ensemble

Ce guide vous accompagne dans l'intégration des fonctionnalités B2B dans votre application Carboscan-mobile existante.

---

## 🗂️ Fichiers Créés

### 1. Modèles de Données
- **`B2BModels.kt`** : Tous les nouveaux modèles B2B
  - `CompanyProfile` : Profil entreprise
  - `B2BCategory` : Catégories professionnelles
  - `ScopeType` : Types de scopes GHG
  - `EmissionFactor` : Facteurs d'émission
  - `B2BEmissionEntry` : Entrées d'émission B2B
  - `CarbonReport` : Rapports carbone
  - Type converters pour Room

### 2. Base de Données
- **`B2BDatabase.kt`** : Extension de la base de données Room
  - DAOs pour toutes les nouvelles entités
  - Migration 2 → 3
  - Index pour optimisation

### 3. Logique Métier
- **`IntelligentMappingEngine.kt`** : Moteur de mapping automatique
- **`TransactionImporter.kt`** : Import CSV/Excel
- **`CsvExporter.kt`** : Export CSV
- **`PdfReportGenerator.kt`** : Génération PDF professionnels
- **`EmissionFactorService.kt`** : Gestion facteurs d'émission internationaux

### 4. Documentation
- **`B2B_TRANSFORMATION_SPEC.md`** : Spécification complète
- **`B2B_IMPLEMENTATION_GUIDE.md`** : Ce guide

---

## 🔧 Étapes d'Intégration

### Étape 1 : Mise à jour des dépendances

Ajoutez dans `app/build.gradle.kts` :

```kotlin
dependencies {
    // Gson pour parsing JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Firebase Remote Config (facteurs d'émission)
    implementation("com.google.firebase:firebase-config-ktx:21.6.0")
    
    // Apache POI pour Excel (optionnel)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
}
```

### Étape 2 : Mise à jour de la Database

Dans `AppModule.kt`, remplacez `EcoTraceDatabase` par `EcoTraceB2BDatabase` :

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EcoTraceB2BDatabase {
        return Room.databaseBuilder(
            context,
            EcoTraceB2BDatabase::class.java,
            "ecotrace_b2b.db"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }
    
    @Provides
    fun provideCompanyProfileDao(db: EcoTraceB2BDatabase) = db.companyProfileDao()
    
    @Provides
    fun provideEmissionFactorDao(db: EcoTraceB2BDatabase) = db.emissionFactorDao()
    
    @Provides
    fun provideB2BEmissionDao(db: EcoTraceB2BDatabase) = db.b2bEmissionDao()
    
    @Provides
    fun provideCarbonReportDao(db: EcoTraceB2BDatabase) = db.carbonReportDao()
}
```

### Étape 3 : Initialiser les Facteurs d'Émission

Créez un `B2BRepository.kt` :

```kotlin
@Singleton
class B2BRepository @Inject constructor(
    private val companyProfileDao: CompanyProfileDao,
    private val emissionFactorDao: EmissionFactorDao,
    private val b2bEmissionDao: B2BEmissionDao,
    private val carbonReportDao: CarbonReportDao,
    private val emissionFactorService: EmissionFactorService
) {
    
    suspend fun initializeApp() {
        // Initialiser les facteurs d'émission par défaut
        emissionFactorService.initializeDefaultFactors()
    }
    
    suspend fun createOrUpdateCompany(profile: CompanyProfile) {
        companyProfileDao.insert(profile)
    }
    
    fun getCompanyByUserId(userId: String) = companyProfileDao.getByUserIdFlow(userId)
    
    suspend fun addEmission(entry: B2BEmissionEntry) {
        b2bEmissionDao.insert(entry)
    }
    
    fun getEmissionsByCompany(companyId: String) = 
        b2bEmissionDao.getAllByCompanyFlow(companyId)
    
    suspend fun generateReport(
        companyId: String,
        periodStart: Long,
        periodEnd: Long
    ): CarbonReport {
        val company = companyProfileDao.getById(companyId) 
            ?: throw IllegalStateException("Company not found")
        
        val entries = b2bEmissionDao.getByPeriod(companyId, periodStart, periodEnd)
        
        val totalKgCo2e = entries.sumOf { it.kgCo2e }
        val scope1Kg = entries.filter { it.scope == ScopeType.SCOPE1 }.sumOf { it.kgCo2e }
        val scope2Kg = entries.filter { it.scope == ScopeType.SCOPE2 }.sumOf { it.kgCo2e }
        val scope3Kg = entries.filter { it.scope == ScopeType.SCOPE3 }.sumOf { it.kgCo2e }
        
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
        
        val reductionPlan = generateReductionPlan(categoryBreakdown, company)
        
        val report = CarbonReport(
            id = UUID.randomUUID().toString(),
            companyId = companyId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            totalKgCo2e = totalKgCo2e,
            scope1Kg = scope1Kg,
            scope2Kg = scope2Kg,
            scope3Kg = scope3Kg,
            carbonIntensity = company.calculateCarbonIntensity(totalKgCo2e),
            topEmissionCategories = categoryBreakdown,
            reductionPlan = reductionPlan
        )
        
        carbonReportDao.insert(report)
        return report
    }
    
    private fun generateReductionPlan(
        categories: List<CategoryBreakdown>,
        company: CompanyProfile
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
                        description = "Souscrire à un contrat d'électricité 100% renouvelable. Réduction immédiate de 80% des émissions liées à l'électricité.",
                        potentialSavingKgCo2e = category.kgCo2e * 0.8,
                        potentialSavingEuro = 0.0,
                        difficulty = "Facile"
                    ))
                }
                category.category.contains("AVION") -> {
                    actions.add(ReductionAction(
                        title = "Privilégier le train pour les trajets courts",
                        description = "Remplacer les vols < 800 km par le train. Réduction de 95% des émissions sur ces trajets.",
                        potentialSavingKgCo2e = category.kgCo2e * 0.5,
                        potentialSavingEuro = category.kgCo2e * 0.5 * 0.08,
                        difficulty = "Facile"
                    ))
                }
                category.category.contains("CLOUD") -> {
                    actions.add(ReductionAction(
                        title = "Optimiser l'infrastructure cloud",
                        description = "Choisir des régions cloud bas-carbone, éteindre les ressources inutilisées, optimiser le code.",
                        potentialSavingKgCo2e = category.kgCo2e * 0.3,
                        potentialSavingEuro = category.kgCo2e * 0.3 * 0.05,
                        difficulty = "Moyen"
                    ))
                }
            }
        }
        
        return actions
    }
}
```

### Étape 4 : Créer le ViewModel B2B

```kotlin
@HiltViewModel
class B2BViewModel @Inject constructor(
    private val repository: B2BRepository,
    private val authRepository: AuthRepository,
    private val mappingEngine: IntelligentMappingEngine,
    private val transactionImporter: TransactionImporter,
    private val pdfGenerator: PdfReportGenerator,
    private val csvExporter: CsvExporter
) : ViewModel() {
    
    private val _currentCompany = MutableStateFlow<CompanyProfile?>(null)
    val currentCompany: StateFlow<CompanyProfile?> = _currentCompany.asStateFlow()
    
    private val _emissions = MutableStateFlow<List<B2BEmissionEntry>>(emptyList())
    val emissions: StateFlow<List<B2BEmissionEntry>> = _emissions.asStateFlow()
    
    private val _currentReport = MutableStateFlow<CarbonReport?>(null)
    val currentReport: StateFlow<CarbonReport?> = _currentReport.asStateFlow()
    
    init {
        viewModelScope.launch {
            repository.initializeApp()
            loadCompany()
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
    
    fun saveCompanyProfile(profile: CompanyProfile) {
        viewModelScope.launch {
            repository.createOrUpdateCompany(profile)
        }
    }
    
    fun addEmission(entry: B2BEmissionEntry) {
        viewModelScope.launch {
            repository.addEmission(entry)
        }
    }
    
    fun importCsv(file: File) {
        viewModelScope.launch {
            val companyId = _currentCompany.value?.id ?: return@launch
            val result = transactionImporter.importFromCsv(file, companyId)
            
            // Afficher résultats à l'utilisateur
            // puis confirmer et sauvegarder
            if (result.success.isNotEmpty()) {
                transactionImporter.confirmAndSaveTransactions(result.success, companyId)
            }
        }
    }
    
    fun generateReport(periodStart: Long, periodEnd: Long) {
        viewModelScope.launch {
            val companyId = _currentCompany.value?.id ?: return@launch
            val report = repository.generateReport(companyId, periodStart, periodEnd)
            _currentReport.value = report
        }
    }
    
    fun generatePdf(): File? {
        val company = _currentCompany.value ?: return null
        val report = _currentReport.value ?: return null
        return pdfGenerator.generateReport(company, report)
    }
    
    fun exportCsv(file: File): Boolean {
        return csvExporter.exportEmissions(_emissions.value, file)
    }
}
```

### Étape 5 : Créer les Écrans UI

#### Écran Profil Entreprise

```kotlin
@Composable
fun CompanyProfileScreen(
    viewModel: B2BViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var companyName by remember { mutableStateOf("") }
    var selectedSector by remember { mutableStateOf(BusinessSector.SERVICES) }
    var employees by remember { mutableStateOf("") }
    var revenue by remember { mutableStateOf("") }
    var showSectorDialog by remember { mutableStateOf(false) }
    
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
        
        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("Nom de l'entreprise") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = selectedSector.label,
            onValueChange = { },
            label = { Text("Secteur d'activité") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showSectorDialog = true }) {
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        if (showSectorDialog) {
            AlertDialog(
                onDismissRequest = { showSectorDialog = false },
                title = { Text("Sélectionnez votre secteur") },
                text = {
                    LazyColumn {
                        items(BusinessSector.values()) { sector ->
                            TextButton(
                                onClick = {
                                    selectedSector = sector
                                    showSectorDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(sector.label, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSectorDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = employees,
            onValueChange = { employees = it },
            label = { Text("Nombre d'employés") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
                        sector = selectedSector,
                        employees = employees.toIntOrNull() ?: 0,
                        annualRevenue = revenue.toDoubleOrNull() ?: 0.0
                    )
                )
                onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = companyName.isNotBlank() && employees.isNotBlank() && revenue.isNotBlank()
        ) {
            Text("Enregistrer")
        }
    }
}
```

#### Écran Import CSV

```kotlin
@Composable
fun ImportCsvScreen(
    viewModel: B2BViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = File(context.cacheDir, "import.csv")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.importCsv(file)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Upload,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Importer vos transactions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Format CSV attendu : Date, Libellé, Montant, Fournisseur",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { launcher.launch("text/csv") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sélectionner un fichier CSV")
        }
    }
}
```

---

## 🧪 Tests

### Test du Mapping Intelligent

```kotlin
@Test
fun testIntelligentMapping() = runTest {
    val mappingEngine = IntelligentMappingEngine(emissionFactorDao)
    
    // Test fournisseur connu
    val suggestion1 = mappingEngine.suggestMapping("Shell Station Paris", 85.50)
    assertEquals("VEHICULE_ENTREPRISE_ESSENCE", suggestion1?.category)
    assertTrue(suggestion1?.confidence ?: 0.0 > 0.85)
    
    // Test sémantique
    val suggestion2 = mappingEngine.suggestMapping("Facture électricité", 320.0)
    assertEquals("ELECTRICITE_LOCAUX", suggestion2?.category)
    
    // Test montant
    val suggestion3 = mappingEngine.suggestMapping("Facture AWS", 450.0)
    assertEquals("SERVICES_CLOUD", suggestion3?.category)
}
```

### Test Import CSV

```kotlin
@Test
fun testCsvImport() = runTest {
    val csvContent = """
        Date,Libellé,Montant,Fournisseur
        2024-01-15,Shell Station,85.50,Shell
        2024-01-20,Facture AWS,450.00,Amazon
    """.trimIndent()
    
    val file = File.createTempFile("test", ".csv")
    file.writeText(csvContent)
    
    val importer = TransactionImporter(mappingEngine, b2bEmissionDao)
    val result = importer.importFromCsv(file, "company-123")
    
    assertEquals(2, result.success.size)
    assertEquals(0, result.errors.size)
}
```

---

## 📱 Configuration Firebase Remote Config

Dans la console Firebase, ajoutez ces paramètres :

```json
{
  "emission_factors_FR": {
    "ELECTRICITE_LOCAUX": {
      "value": 0.052,
      "unit": "kWh",
      "source": "ADEME 2024",
      "description": "Mix électrique français",
      "keywords": ["électricité", "edf", "énergie"]
    },
    "GAZ_NATUREL_LOCAUX": {
      "value": 2.04,
      "unit": "m³",
      "source": "ADEME 2024",
      "keywords": ["gaz", "chauffage", "engie"]
    }
  },
  "emission_factors_US": {
    "ELECTRICITE_LOCAUX": {
      "value": 0.385,
      "unit": "kWh",
      "source": "EPA 2023",
      "description": "US average grid"
    }
  }
}
```

---

## 🎯 Checklist de Déploiement

- [ ] Mettre à jour les dépendances Gradle
- [ ] Intégrer les nouveaux modèles dans AppModule
- [ ] Créer B2BRepository et B2BViewModel
- [ ] Implémenter les écrans UI (Profil, Dashboard, Import)
- [ ] Configurer Firebase Remote Config
- [ ] Tester la migration de base de données
- [ ] Tester le mapping intelligent
- [ ] Tester l'import CSV
- [ ] Tester la génération PDF
- [ ] Ajouter les permissions (WRITE_EXTERNAL_STORAGE pour PDF)
- [ ] Mettre à jour les strings.xml avec les nouveaux textes
- [ ] Tester sur différents appareils
- [ ] Créer la documentation utilisateur

---

## 🚨 Points d'Attention

### Permissions Android

Ajoutez dans `AndroidManifest.xml` :

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
```

### Gestion des Fichiers (Android 11+)

Pour Android 11+, utilisez le Storage Access Framework :

```kotlin
val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "application/pdf"
    putExtra(Intent.EXTRA_TITLE, "rapport_carbone.pdf")
}
startActivityForResult(intent, CREATE_PDF_REQUEST)
```

### Performance

- Utilisez `Flow` pour les requêtes Room
- Limitez les imports CSV à 1000 lignes max
- Générez les PDF en arrière-plan (WorkManager)

---

## 📚 Ressources Supplémentaires

- [GHG Protocol](https://ghgprotocol.org/)
- [ADEME Base Carbone](https://base-empreinte.ademe.fr/)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)

---

**Version** : 1.0  
**Dernière mise à jour** : Février 2026
