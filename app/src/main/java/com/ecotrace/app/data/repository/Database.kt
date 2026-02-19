package com.ecotrace.app.data.repository

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ecotrace.app.data.models.EmissionEntry
import com.ecotrace.app.data.models.ScannedProduct
import com.ecotrace.app.data.models.ProductInfo
import kotlinx.coroutines.flow.Flow

// ── DAO ──────────────────────────────────────────────────────────────────────
@Dao
interface EmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EmissionEntry)

    @Delete
    suspend fun delete(entry: EmissionEntry)

    @Query("SELECT * FROM emission_entries ORDER BY date DESC")
    fun getAllFlow(): Flow<List<EmissionEntry>>

    @Query("SELECT * FROM emission_entries WHERE date >= :fromDay AND date <= :toDay ORDER BY date DESC")
    fun getByPeriodFlow(fromDay: Long, toDay: Long): Flow<List<EmissionEntry>>

    @Query("SELECT * FROM emission_entries ORDER BY date DESC LIMIT 50")
    fun getRecentFlow(): Flow<List<EmissionEntry>>

    @Query("DELETE FROM emission_entries WHERE id = :id")
    suspend fun deleteById(id: String)
}

// ── DAO Produits Scannés ────────────────────────────────────────────────────
@Dao
interface ScannedProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ScannedProduct)

    @Delete
    suspend fun delete(product: ScannedProduct)

    @Query("SELECT * FROM scanned_products ORDER BY date DESC")
    fun getAllFlow(): Flow<List<ScannedProduct>>

    @Query("SELECT * FROM scanned_products WHERE date >= :fromDay AND date <= :toDay ORDER BY date DESC")
    fun getByPeriodFlow(fromDay: Long, toDay: Long): Flow<List<ScannedProduct>>

    @Query("DELETE FROM scanned_products WHERE id = :id")
    suspend fun deleteById(id: String)
}

// ── DAO Base de données produits ───────────────────────────────────────────
@Dao
interface ProductInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductInfo)

    @Query("SELECT * FROM product_database WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): ProductInfo?

    @Query("SELECT COUNT(*) FROM product_database")
    suspend fun getCount(): Int
}

// ── Database ─────────────────────────────────────────────────────────────────
@Database(
    entities = [EmissionEntry::class, ScannedProduct::class, ProductInfo::class],
    version = 2,
    exportSchema = false
)
abstract class EcoTraceDatabase : RoomDatabase() {
    abstract fun emissionDao(): EmissionDao
    abstract fun scannedProductDao(): ScannedProductDao
    abstract fun productInfoDao(): ProductInfoDao
}

// ── Migrations ───────────────────────────────────────────────────────────────
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS scanned_products (
                id TEXT PRIMARY KEY NOT NULL,
                barcode TEXT NOT NULL,
                name TEXT NOT NULL,
                brand TEXT NOT NULL,
                category TEXT NOT NULL,
                kgCo2ePer100g REAL NOT NULL,
                weight REAL NOT NULL,
                date INTEGER NOT NULL,
                imageUrl TEXT NOT NULL
            )"""
        )
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS product_database (
                barcode TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                brand TEXT NOT NULL,
                category TEXT NOT NULL,
                kgCo2ePer100g REAL NOT NULL,
                lastUpdated INTEGER NOT NULL
            )"""
        )
    }
}
