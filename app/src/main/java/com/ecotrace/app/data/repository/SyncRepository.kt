package com.ecotrace.app.data.repository

import com.ecotrace.app.data.models.EmissionEntry
import com.ecotrace.app.data.models.ScannedProduct
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class UserData(
    val emissions: List<EmissionEntry>,
    val products: List<ScannedProduct>,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

@Singleton
class SyncRepository @Inject constructor(
    private val driveRepository: DriveRepository,
    private val emissionRepository: EmissionRepository,
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()
    private val dataFileName = "ecotrace_user_data.json"

    /**
     * Sauvegarde toutes les données de l'utilisateur sur Google Drive
     */
    suspend fun backupToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!authRepository.isUserSignedIn()) {
                return@withContext Result.failure(Exception("User not signed in"))
            }

            // Récupérer toutes les données locales
            val emissions = emissionRepository.allEntries.first()
            val products = productRepository.allScannedProducts.first()

            val userData = UserData(
                emissions = emissions,
                products = products,
                lastSyncTimestamp = System.currentTimeMillis()
            )

            // Convertir en JSON
            val jsonData = gson.toJson(userData)

            // Sauvegarder sur Drive
            driveRepository.saveDataToDrive(dataFileName, jsonData)
                .onSuccess {
                    return@withContext Result.success(Unit)
                }
                .onFailure { e ->
                    return@withContext Result.failure(e)
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restaure les données depuis Google Drive
     */
    suspend fun restoreFromCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!authRepository.isUserSignedIn()) {
                return@withContext Result.failure(Exception("User not signed in"))
            }

            // Charger les données depuis Drive
            driveRepository.loadDataFromDrive(dataFileName)
                .onSuccess { jsonData ->
                    // Parser le JSON
                    val userData = gson.fromJson<UserData>(
                        jsonData,
                        object : TypeToken<UserData>() {}.type
                    )

                    // Restaurer les émissions
                    userData.emissions.forEach { emission ->
                        emissionRepository.insert(emission)
                    }

                    // Restaurer les produits
                    userData.products.forEach { product ->
                        productRepository.insertProduct(product)
                    }

                    return@withContext Result.success(Unit)
                }
                .onFailure { e ->
                    return@withContext Result.failure(e)
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Vérifie si des données de sauvegarde existent sur Drive
     */
    suspend fun hasCloudBackup(): Boolean {
        return driveRepository.hasBackupData(dataFileName)
    }

    /**
     * Synchronisation automatique (appelée périodiquement)
     */
    suspend fun autoSync(): Result<Unit> {
        return backupToCloud()
    }

    /**
     * Supprime toutes les données cloud de l'utilisateur
     */
    suspend fun deleteCloudData(): Result<Unit> {
        return driveRepository.deleteAllData()
    }
}
