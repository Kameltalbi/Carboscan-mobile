package com.ecotrace.app.data.repository

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ecotrace.app.data.models.*
import kotlinx.coroutines.flow.Flow

// ── DAO Mapping Dictionary ──────────────────────────────────────────────────
@Dao
interface MappingDictionaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: MappingRule)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(rule: MappingRule)

    @Update
    suspend fun update(rule: MappingRule)

    @Query("SELECT * FROM mapping_dictionary WHERE keyword = :keyword LIMIT 1")
    suspend fun findByKeyword(keyword: String): MappingRule?

    @Query("SELECT * FROM mapping_dictionary WHERE category = :category")
    suspend fun findByCategory(category: String): List<MappingRule>

    @Query("SELECT * FROM mapping_dictionary ORDER BY usageCount DESC LIMIT 100")
    suspend fun getMostUsed(): List<MappingRule>

    @Query("SELECT COUNT(*) FROM mapping_dictionary")
    suspend fun getCount(): Int
}

// ── DAO Mapping Learning ────────────────────────────────────────────────────
@Dao
interface MappingLearningDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(learning: MappingLearning)

    @Query("SELECT * FROM mapping_learning_history WHERE companyId = :companyId AND transactionLabel LIKE '%' || :label || '%' ORDER BY learnedAt DESC LIMIT 1")
    suspend fun findSimilarTransaction(companyId: String, label: String): MappingLearning?

    @Query("SELECT * FROM mapping_learning_history WHERE companyId = :companyId ORDER BY learnedAt DESC LIMIT 50")
    suspend fun getRecentLearnings(companyId: String): List<MappingLearning>

    @Query("SELECT COUNT(*) FROM mapping_learning_history WHERE companyId = :companyId")
    suspend fun getLearningCount(companyId: String): Int
}

// ── DAO Consultant Profile ──────────────────────────────────────────────────
@Dao
interface ConsultantProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ConsultantProfile)

    @Update
    suspend fun update(profile: ConsultantProfile)

    @Query("SELECT * FROM consultant_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): ConsultantProfile?

    @Query("SELECT * FROM consultant_profiles WHERE id = :id")
    suspend fun getById(id: String): ConsultantProfile?

    @Query("SELECT * FROM consultant_profiles WHERE userId = :userId")
    fun getByUserIdFlow(userId: String): Flow<ConsultantProfile?>
}

// ── DAO Client-Consultant Relations ─────────────────────────────────────────
@Dao
interface ClientConsultantRelationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relation: ClientConsultantRelation)

    @Update
    suspend fun update(relation: ClientConsultantRelation)

    @Query("SELECT * FROM client_consultant_relations WHERE consultantId = :consultantId AND status = 'ACTIVE'")
    suspend fun getActiveClientsByConsultant(consultantId: String): List<ClientConsultantRelation>

    @Query("SELECT * FROM client_consultant_relations WHERE clientCompanyId = :companyId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveConsultantByClient(companyId: String): ClientConsultantRelation?

    @Query("SELECT * FROM client_consultant_relations WHERE consultantId = :consultantId")
    fun getAllClientsByConsultantFlow(consultantId: String): Flow<List<ClientConsultantRelation>>
}

// ── DAO Report Signature ────────────────────────────────────────────────────
@Dao
interface ReportSignatureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(signature: ReportSignature)

    @Update
    suspend fun update(signature: ReportSignature)

    @Query("SELECT * FROM report_signatures WHERE reportId = :reportId LIMIT 1")
    suspend fun getByReportId(reportId: String): ReportSignature?

    @Query("SELECT * FROM report_signatures WHERE consultantId = :consultantId ORDER BY signedAt DESC")
    suspend fun getByConsultantId(consultantId: String): List<ReportSignature>

    @Query("SELECT COUNT(*) FROM report_signatures WHERE consultantId = :consultantId AND verificationStatus = 'SIGNED'")
    suspend fun getSignedReportsCount(consultantId: String): Int
}

// ── DAO Exchange Rates ──────────────────────────────────────────────────────
@Dao
interface ExchangeRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: ExchangeRate)

    @Query("SELECT * FROM exchange_rates WHERE fromCurrency = :from AND toCurrency = :to ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRate(from: String, to: String): ExchangeRate?

    @Query("SELECT * FROM exchange_rates WHERE date >= :minDate")
    suspend fun getRecentRates(minDate: Long): List<ExchangeRate>

    @Query("DELETE FROM exchange_rates WHERE date < :beforeDate")
    suspend fun deleteOldRates(beforeDate: Long)
}

// ── DAO Financial Emission Entries ──────────────────────────────────────────
@Dao
interface FinancialEmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FinancialEmissionEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FinancialEmissionEntry>)

    @Delete
    suspend fun delete(entry: FinancialEmissionEntry)

    @Query("SELECT * FROM financial_emission_entries WHERE companyId = :companyId ORDER BY date DESC")
    fun getAllByCompanyFlow(companyId: String): Flow<List<FinancialEmissionEntry>>

    @Query("SELECT * FROM financial_emission_entries WHERE companyId = :companyId AND date >= :fromDay AND date <= :toDay ORDER BY date DESC")
    fun getByPeriodFlow(companyId: String, fromDay: Long, toDay: Long): Flow<List<FinancialEmissionEntry>>

    @Query("SELECT * FROM financial_emission_entries WHERE companyId = :companyId AND date >= :fromDay AND date <= :toDay")
    suspend fun getByPeriod(companyId: String, fromDay: Long, toDay: Long): List<FinancialEmissionEntry>

    @Query("DELETE FROM financial_emission_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT SUM(kgCo2e) FROM financial_emission_entries WHERE companyId = :companyId AND date >= :fromDay AND date <= :toDay")
    suspend fun getTotalEmissions(companyId: String, fromDay: Long, toDay: Long): Double?

    @Query("SELECT SUM(amountEuro) FROM financial_emission_entries WHERE companyId = :companyId AND date >= :fromDay AND date <= :toDay")
    suspend fun getTotalSpending(companyId: String, fromDay: Long, toDay: Long): Double?

    @Query("SELECT AVG(carbonIntensityRatio) FROM financial_emission_entries WHERE companyId = :companyId AND date >= :fromDay AND date <= :toDay")
    suspend fun getAverageCarbonIntensity(companyId: String, fromDay: Long, toDay: Long): Double?

    @Query("SELECT * FROM financial_emission_entries WHERE companyId = :companyId AND isAutoMapped = 1 AND mappingConfidence < 0.8 ORDER BY date DESC LIMIT 20")
    suspend fun getLowConfidenceMappings(companyId: String): List<FinancialEmissionEntry>
}

// ── DAO Enhanced Company Profile ───────────────────────────────────────────
@Dao
interface EnhancedCompanyProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: EnhancedCompanyProfile)

    @Update
    suspend fun update(profile: EnhancedCompanyProfile)

    @Query("SELECT * FROM enhanced_company_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): EnhancedCompanyProfile?

    @Query("SELECT * FROM enhanced_company_profiles WHERE userId = :userId LIMIT 1")
    fun getByUserIdFlow(userId: String): Flow<EnhancedCompanyProfile?>

    @Query("SELECT * FROM enhanced_company_profiles WHERE id = :id")
    suspend fun getById(id: String): EnhancedCompanyProfile?

    @Query("SELECT * FROM enhanced_company_profiles WHERE consultantId = :consultantId")
    suspend fun getByConsultantId(consultantId: String): List<EnhancedCompanyProfile>

    @Query("SELECT * FROM enhanced_company_profiles WHERE consultantId = :consultantId")
    fun getByConsultantIdFlow(consultantId: String): Flow<List<EnhancedCompanyProfile>>
}

// ── DAO Enhanced Carbon Report ──────────────────────────────────────────────
@Dao
interface EnhancedCarbonReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: EnhancedCarbonReport)

    @Update
    suspend fun update(report: EnhancedCarbonReport)

    @Query("SELECT * FROM enhanced_carbon_reports WHERE companyId = :companyId ORDER BY generatedAt DESC")
    fun getAllByCompanyFlow(companyId: String): Flow<List<EnhancedCarbonReport>>

    @Query("SELECT * FROM enhanced_carbon_reports WHERE companyId = :companyId ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatestByCompany(companyId: String): EnhancedCarbonReport?

    @Query("SELECT * FROM enhanced_carbon_reports WHERE id = :id")
    suspend fun getById(id: String): EnhancedCarbonReport?

    @Query("SELECT * FROM enhanced_carbon_reports WHERE verificationStatus = :status")
    suspend fun getByStatus(status: VerificationStatus): List<EnhancedCarbonReport>

    @Query("DELETE FROM enhanced_carbon_reports WHERE id = :id")
    suspend fun deleteById(id: String)
}

// ── Database Extension (Version 4) ──────────────────────────────────────────
@Database(
    entities = [
        // Anciennes entités
        EmissionEntry::class,
        ScannedProduct::class,
        ProductInfo::class,
        CompanyProfile::class,
        EmissionFactor::class,
        B2BEmissionEntry::class,
        CarbonReport::class,
        // Nouvelles entités avancées
        FinancialEmissionEntry::class,
        ConsultantProfile::class,
        ReportSignature::class,
        ClientConsultantRelation::class,
        ExchangeRate::class,
        MappingRule::class,
        MappingLearning::class,
        EnhancedCompanyProfile::class,
        EnhancedCarbonReport::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(
    StringListConverter::class,
    CategoryBreakdownListConverter::class,
    ReductionActionListConverter::class,
    FinancialMetricsConverter::class
)
abstract class AdvancedB2BDatabase : RoomDatabase() {
    // DAOs existants
    abstract fun emissionDao(): EmissionDao
    abstract fun scannedProductDao(): ScannedProductDao
    abstract fun productInfoDao(): ProductInfoDao
    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun emissionFactorDao(): EmissionFactorDao
    abstract fun b2bEmissionDao(): B2BEmissionDao
    abstract fun carbonReportDao(): CarbonReportDao
    
    // Nouveaux DAOs avancés
    abstract fun financialEmissionDao(): FinancialEmissionDao
    abstract fun consultantProfileDao(): ConsultantProfileDao
    abstract fun reportSignatureDao(): ReportSignatureDao
    abstract fun clientConsultantRelationDao(): ClientConsultantRelationDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun mappingDictionaryDao(): MappingDictionaryDao
    abstract fun mappingLearningDao(): MappingLearningDao
    abstract fun enhancedCompanyProfileDao(): EnhancedCompanyProfileDao
    abstract fun enhancedCarbonReportDao(): EnhancedCarbonReportDao
}

// ── Migration 3 → 4 (Fonctionnalités Avancées) ─────────────────────────────
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Table FinancialEmissionEntry
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS financial_emission_entries (
                id TEXT PRIMARY KEY NOT NULL,
                companyId TEXT NOT NULL,
                date INTEGER NOT NULL,
                categoryName TEXT NOT NULL,
                scope TEXT NOT NULL,
                amountEuro REAL NOT NULL,
                originalAmount REAL NOT NULL,
                originalCurrency TEXT NOT NULL,
                exchangeRate REAL NOT NULL,
                valueInput REAL NOT NULL,
                unit TEXT NOT NULL,
                emissionFactorKgCo2e REAL NOT NULL,
                emissionFactorSource TEXT NOT NULL,
                kgCo2e REAL NOT NULL,
                carbonIntensityRatio REAL NOT NULL,
                transactionLabel TEXT NOT NULL,
                invoiceReference TEXT NOT NULL,
                supplierName TEXT NOT NULL,
                supplierCategory TEXT NOT NULL,
                paymentMethod TEXT NOT NULL,
                note TEXT NOT NULL,
                isAutoMapped INTEGER NOT NULL,
                mappingConfidence REAL NOT NULL,
                suggestedBy TEXT NOT NULL
            )"""
        )

        // Table ConsultantProfile
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS consultant_profiles (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                fullName TEXT NOT NULL,
                certification TEXT NOT NULL,
                certificationNumber TEXT NOT NULL,
                company TEXT NOT NULL,
                siret TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                logoUrl TEXT NOT NULL,
                signature TEXT NOT NULL,
                hourlyRate REAL NOT NULL,
                currency TEXT NOT NULL,
                clientIds TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )"""
        )

        // Table ReportSignature
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS report_signatures (
                id TEXT PRIMARY KEY NOT NULL,
                reportId TEXT NOT NULL,
                consultantId TEXT NOT NULL,
                consultantName TEXT NOT NULL,
                certification TEXT NOT NULL,
                signedAt INTEGER NOT NULL,
                verificationStatus TEXT NOT NULL,
                comments TEXT NOT NULL,
                revisionNotes TEXT NOT NULL,
                digitalSignature TEXT NOT NULL,
                qrCodeData TEXT NOT NULL
            )"""
        )

        // Table ClientConsultantRelation
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS client_consultant_relations (
                id TEXT PRIMARY KEY NOT NULL,
                clientCompanyId TEXT NOT NULL,
                consultantId TEXT NOT NULL,
                startDate INTEGER NOT NULL,
                endDate INTEGER,
                status TEXT NOT NULL,
                contractType TEXT NOT NULL,
                monthlyFee REAL NOT NULL,
                currency TEXT NOT NULL,
                accessLevel TEXT NOT NULL
            )"""
        )

        // Table ExchangeRate
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS exchange_rates (
                id TEXT PRIMARY KEY NOT NULL,
                fromCurrency TEXT NOT NULL,
                toCurrency TEXT NOT NULL,
                rate REAL NOT NULL,
                date INTEGER NOT NULL,
                source TEXT NOT NULL
            )"""
        )

        // Table MappingRule
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS mapping_dictionary (
                id TEXT PRIMARY KEY NOT NULL,
                keyword TEXT NOT NULL,
                category TEXT NOT NULL,
                scope TEXT NOT NULL,
                confidence REAL NOT NULL,
                supplierName TEXT NOT NULL,
                minAmount REAL,
                maxAmount REAL,
                country TEXT NOT NULL,
                isLearned INTEGER NOT NULL,
                usageCount INTEGER NOT NULL,
                lastUsed INTEGER NOT NULL
            )"""
        )

        // Table MappingLearning
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS mapping_learning_history (
                id TEXT PRIMARY KEY NOT NULL,
                companyId TEXT NOT NULL,
                transactionLabel TEXT NOT NULL,
                suggestedCategory TEXT NOT NULL,
                correctedCategory TEXT NOT NULL,
                amount REAL NOT NULL,
                learnedAt INTEGER NOT NULL
            )"""
        )

        // Table EnhancedCompanyProfile
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS enhanced_company_profiles (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                companyName TEXT NOT NULL,
                sector TEXT NOT NULL,
                employees INTEGER NOT NULL,
                annualRevenue REAL NOT NULL,
                fiscalYearStart INTEGER NOT NULL,
                fiscalYearEnd INTEGER NOT NULL,
                currency TEXT NOT NULL,
                country TEXT NOT NULL,
                baselineYear INTEGER NOT NULL,
                baselineEmissions REAL NOT NULL,
                reductionTargetPercent REAL NOT NULL,
                targetYear INTEGER NOT NULL,
                consultantId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )"""
        )

        // Table EnhancedCarbonReport
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS enhanced_carbon_reports (
                id TEXT PRIMARY KEY NOT NULL,
                companyId TEXT NOT NULL,
                periodStart INTEGER NOT NULL,
                periodEnd INTEGER NOT NULL,
                totalKgCo2e REAL NOT NULL,
                scope1Kg REAL NOT NULL,
                scope2Kg REAL NOT NULL,
                scope3Kg REAL NOT NULL,
                totalSpending REAL NOT NULL,
                carbonIntensity REAL NOT NULL,
                averageRatioByScope TEXT NOT NULL,
                topEmissionCategories TEXT NOT NULL,
                topSuppliers TEXT NOT NULL,
                reductionPlan TEXT NOT NULL,
                signatureId TEXT,
                verificationStatus TEXT NOT NULL,
                generatedAt INTEGER NOT NULL,
                pdfPath TEXT NOT NULL,
                version INTEGER NOT NULL
            )"""
        )

        // Index pour optimisation
        database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_emissions_companyId ON financial_emission_entries(companyId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_emissions_date ON financial_emission_entries(date)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_consultant_profiles_userId ON consultant_profiles(userId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_report_signatures_reportId ON report_signatures(reportId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_client_consultant_relations_consultantId ON client_consultant_relations(consultantId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_mapping_dictionary_keyword ON mapping_dictionary(keyword)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_mapping_learning_companyId ON mapping_learning_history(companyId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_enhanced_company_profiles_userId ON enhanced_company_profiles(userId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_enhanced_company_profiles_consultantId ON enhanced_company_profiles(consultantId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_enhanced_carbon_reports_companyId ON enhanced_carbon_reports(companyId)")
    }
}
