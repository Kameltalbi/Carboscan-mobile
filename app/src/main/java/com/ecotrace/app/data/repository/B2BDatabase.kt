package com.ecotrace.app.data.repository

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ecotrace.app.data.models.*
import kotlinx.coroutines.flow.Flow

// ── DAO Profil Entreprise ───────────────────────────────────────────────────
@Dao
interface CompanyProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: CompanyProfile)

    @Update
    suspend fun update(profile: CompanyProfile)

    @Query("SELECT * FROM company_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): CompanyProfile?

    @Query("SELECT * FROM company_profiles WHERE userId = :userId LIMIT 1")
    fun getByUserIdFlow(userId: String): Flow<CompanyProfile?>

    @Query("SELECT * FROM company_profiles WHERE id = :id")
    suspend fun getById(id: String): CompanyProfile?
}

// ── DAO Facteurs d'Émission ─────────────────────────────────────────────────
@Dao
interface EmissionFactorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(factor: EmissionFactor)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(factors: List<EmissionFactor>)

    @Query("SELECT * FROM emission_factors WHERE category = :category AND country = :country LIMIT 1")
    suspend fun getByCountryAndCategory(country: String, category: String): EmissionFactor?

    @Query("SELECT * FROM emission_factors WHERE country = :country")
    suspend fun getAllByCountry(country: String): List<EmissionFactor>

    @Query("SELECT * FROM emission_factors WHERE country = :country")
    fun getAllByCountryFlow(country: String): Flow<List<EmissionFactor>>

    @Query("SELECT * FROM emission_factors WHERE category = :category LIMIT 1")
    suspend fun getByCategory(category: String): EmissionFactor?

    @Query("SELECT COUNT(*) FROM emission_factors")
    suspend fun getCount(): Int
}

// ── DAO Entrées d'Émission B2B ──────────────────────────────────────────────
@Dao
interface B2BEmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: B2BEmissionEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<B2BEmissionEntry>)

    @Delete
    suspend fun delete(entry: B2BEmissionEntry)

    @Query("SELECT * FROM b2b_emission_entries WHERE companyId = :companyId ORDER BY date DESC")
    fun getAllByCompanyFlow(companyId: String): Flow<List<B2BEmissionEntry>>

    @Query("SELECT * FROM b2b_emission_entries WHERE companyId = :companyId AND date >= :fromDay AND date <= :toDay ORDER BY date DESC")
    fun getByPeriodFlow(companyId: String, fromDay: Long, toDay: Long): Flow<List<B2BEmissionEntry>>

    @Query("SELECT * FROM b2b_emission_entries WHERE companyId = :companyId AND date >= :fromDay AND date <= :toDay")
    suspend fun getByPeriod(companyId: String, fromDay: Long, toDay: Long): List<B2BEmissionEntry>

    @Query("DELETE FROM b2b_emission_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT SUM(kgCo2e) FROM b2b_emission_entries WHERE companyId = :companyId AND date >= :fromDay AND date <= :toDay")
    suspend fun getTotalEmissions(companyId: String, fromDay: Long, toDay: Long): Double?

    @Query("SELECT SUM(kgCo2e) FROM b2b_emission_entries WHERE companyId = :companyId AND scope = :scope AND date >= :fromDay AND date <= :toDay")
    suspend fun getEmissionsByScope(companyId: String, scope: ScopeType, fromDay: Long, toDay: Long): Double?
}

// ── DAO Rapports Carbone ────────────────────────────────────────────────────
@Dao
interface CarbonReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: CarbonReport)

    @Query("SELECT * FROM carbon_reports WHERE companyId = :companyId ORDER BY generatedAt DESC")
    fun getAllByCompanyFlow(companyId: String): Flow<List<CarbonReport>>

    @Query("SELECT * FROM carbon_reports WHERE companyId = :companyId ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatestByCompany(companyId: String): CarbonReport?

    @Query("SELECT * FROM carbon_reports WHERE id = :id")
    suspend fun getById(id: String): CarbonReport?

    @Query("DELETE FROM carbon_reports WHERE id = :id")
    suspend fun deleteById(id: String)
}

// ── Database Extension ──────────────────────────────────────────────────────
@Database(
    entities = [
        EmissionEntry::class,
        ScannedProduct::class,
        ProductInfo::class,
        CompanyProfile::class,
        EmissionFactor::class,
        B2BEmissionEntry::class,
        CarbonReport::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(
    StringListConverter::class,
    CategoryBreakdownListConverter::class,
    ReductionActionListConverter::class
)
abstract class EcoTraceB2BDatabase : RoomDatabase() {
    abstract fun emissionDao(): EmissionDao
    abstract fun scannedProductDao(): ScannedProductDao
    abstract fun productInfoDao(): ProductInfoDao
    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun emissionFactorDao(): EmissionFactorDao
    abstract fun b2bEmissionDao(): B2BEmissionDao
    abstract fun carbonReportDao(): CarbonReportDao
}

// ── Migration 2 → 3 (B2B) ───────────────────────────────────────────────────
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Table CompanyProfile
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS company_profiles (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                companyName TEXT NOT NULL,
                sector TEXT NOT NULL,
                employees INTEGER NOT NULL,
                annualRevenue REAL NOT NULL,
                currency TEXT NOT NULL,
                country TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )"""
        )

        // Table EmissionFactor
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS emission_factors (
                id TEXT PRIMARY KEY NOT NULL,
                category TEXT NOT NULL,
                scope TEXT NOT NULL,
                unit TEXT NOT NULL,
                kgCo2ePerUnit REAL NOT NULL,
                country TEXT NOT NULL,
                source TEXT NOT NULL,
                description TEXT NOT NULL,
                keywords TEXT NOT NULL,
                lastUpdated INTEGER NOT NULL
            )"""
        )

        // Table B2BEmissionEntry
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS b2b_emission_entries (
                id TEXT PRIMARY KEY NOT NULL,
                companyId TEXT NOT NULL,
                date INTEGER NOT NULL,
                categoryName TEXT NOT NULL,
                scope TEXT NOT NULL,
                valueInput REAL NOT NULL,
                unit TEXT NOT NULL,
                emissionFactorKgCo2e REAL NOT NULL,
                emissionFactorSource TEXT NOT NULL,
                kgCo2e REAL NOT NULL,
                transactionLabel TEXT NOT NULL,
                invoiceReference TEXT NOT NULL,
                supplierName TEXT NOT NULL,
                note TEXT NOT NULL,
                isAutoMapped INTEGER NOT NULL
            )"""
        )

        // Table CarbonReport
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS carbon_reports (
                id TEXT PRIMARY KEY NOT NULL,
                companyId TEXT NOT NULL,
                periodStart INTEGER NOT NULL,
                periodEnd INTEGER NOT NULL,
                totalKgCo2e REAL NOT NULL,
                scope1Kg REAL NOT NULL,
                scope2Kg REAL NOT NULL,
                scope3Kg REAL NOT NULL,
                carbonIntensity REAL NOT NULL,
                topEmissionCategories TEXT NOT NULL,
                reductionPlan TEXT NOT NULL,
                generatedAt INTEGER NOT NULL,
                pdfPath TEXT NOT NULL
            )"""
        )

        // Index pour performances
        database.execSQL("CREATE INDEX IF NOT EXISTS index_company_profiles_userId ON company_profiles(userId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_emission_factors_country ON emission_factors(country)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_b2b_emission_entries_companyId ON b2b_emission_entries(companyId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_b2b_emission_entries_date ON b2b_emission_entries(date)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_carbon_reports_companyId ON carbon_reports(companyId)")
    }
}
