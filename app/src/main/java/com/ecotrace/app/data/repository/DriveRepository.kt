package com.ecotrace.app.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var driveService: Drive? = null

    fun initializeDriveService() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccount = account.account

            driveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("EcoTrace")
                .build()
        }
    }

    /**
     * Sauvegarde les données JSON sur Google Drive
     */
    suspend fun saveDataToDrive(fileName: String, jsonData: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: throw Exception("Drive service not initialized")

            // Chercher si le fichier existe déjà
            val existingFile = findFileByName(fileName)

            val fileMetadata = File().apply {
                name = fileName
                mimeType = "application/json"
                parents = listOf("appDataFolder") // Dossier privé de l'app
            }

            val mediaContent = com.google.api.client.http.ByteArrayContent(
                "application/json",
                jsonData.toByteArray()
            )

            val file = if (existingFile != null) {
                // Mettre à jour le fichier existant
                service.files().update(existingFile.id, fileMetadata, mediaContent).execute()
            } else {
                // Créer un nouveau fichier
                service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
            }

            Result.success(file.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Récupère les données JSON depuis Google Drive
     */
    suspend fun loadDataFromDrive(fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: throw Exception("Drive service not initialized")

            val file = findFileByName(fileName)
                ?: return@withContext Result.failure(Exception("File not found"))

            val outputStream = ByteArrayOutputStream()
            service.files().get(file.id).executeMediaAndDownloadTo(outputStream)

            val jsonData = outputStream.toString("UTF-8")
            Result.success(jsonData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cherche un fichier par nom dans le dossier appDataFolder
     */
    private suspend fun findFileByName(fileName: String): File? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null

            val result: FileList = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName'")
                .setFields("files(id, name)")
                .execute()

            result.files.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Supprime toutes les données de l'utilisateur sur Drive
     */
    suspend fun deleteAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: throw Exception("Drive service not initialized")

            val result: FileList = service.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id)")
                .execute()

            result.files.forEach { file ->
                service.files().delete(file.id).execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Vérifie si des données existent sur Drive
     */
    suspend fun hasBackupData(fileName: String): Boolean = withContext(Dispatchers.IO) {
        findFileByName(fileName) != null
    }
}
