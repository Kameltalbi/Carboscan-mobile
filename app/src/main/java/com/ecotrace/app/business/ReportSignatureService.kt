package com.ecotrace.app.business

import android.graphics.Bitmap
import android.graphics.Color
import com.ecotrace.app.data.models.*
import com.ecotrace.app.data.repository.ConsultantProfileDao
import com.ecotrace.app.data.repository.ReportSignatureDao
import com.ecotrace.app.data.repository.EnhancedCarbonReportDao
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ReportSignatureService(
    private val consultantProfileDao: ConsultantProfileDao,
    private val reportSignatureDao: ReportSignatureDao,
    private val enhancedCarbonReportDao: EnhancedCarbonReportDao
) {

    /**
     * Crée une signature de rapport par un consultant IFC
     */
    suspend fun signReport(
        reportId: String,
        consultantId: String,
        comments: String = "",
        revisionNotes: String = ""
    ): ReportSignature {
        val consultant = consultantProfileDao.getById(consultantId)
            ?: throw IllegalArgumentException("Consultant non trouvé")

        val report = enhancedCarbonReportDao.getById(reportId)
            ?: throw IllegalArgumentException("Rapport non trouvé")

        // Générer la signature numérique (hash SHA-256)
        val digitalSignature = generateDigitalSignature(
            reportId = reportId,
            consultantId = consultantId,
            timestamp = System.currentTimeMillis()
        )

        // Générer les données du QR code pour vérification
        val qrCodeData = generateQRCodeData(reportId, digitalSignature)

        val signature = ReportSignature(
            id = UUID.randomUUID().toString(),
            reportId = reportId,
            consultantId = consultantId,
            consultantName = consultant.fullName,
            certification = consultant.certification,
            signedAt = System.currentTimeMillis(),
            verificationStatus = VerificationStatus.SIGNED,
            comments = comments,
            revisionNotes = revisionNotes,
            digitalSignature = digitalSignature,
            qrCodeData = qrCodeData
        )

        // Sauvegarder la signature
        reportSignatureDao.insert(signature)

        // Mettre à jour le statut du rapport
        enhancedCarbonReportDao.update(
            report.copy(
                signatureId = signature.id,
                verificationStatus = VerificationStatus.SIGNED
            )
        )

        return signature
    }

    /**
     * Marque un rapport comme "en révision"
     */
    suspend fun markForReview(
        reportId: String,
        consultantId: String,
        revisionNotes: String
    ) {
        val report = enhancedCarbonReportDao.getById(reportId)
            ?: throw IllegalArgumentException("Rapport non trouvé")

        enhancedCarbonReportDao.update(
            report.copy(verificationStatus = VerificationStatus.UNDER_REVIEW)
        )

        // Créer une signature temporaire
        val consultant = consultantProfileDao.getById(consultantId)
            ?: throw IllegalArgumentException("Consultant non trouvé")

        val signature = ReportSignature(
            id = UUID.randomUUID().toString(),
            reportId = reportId,
            consultantId = consultantId,
            consultantName = consultant.fullName,
            certification = consultant.certification,
            signedAt = System.currentTimeMillis(),
            verificationStatus = VerificationStatus.UNDER_REVIEW,
            comments = "",
            revisionNotes = revisionNotes,
            digitalSignature = "",
            qrCodeData = ""
        )

        reportSignatureDao.insert(signature)
    }

    /**
     * Rejette un rapport
     */
    suspend fun rejectReport(
        reportId: String,
        consultantId: String,
        reason: String
    ) {
        val report = enhancedCarbonReportDao.getById(reportId)
            ?: throw IllegalArgumentException("Rapport non trouvé")

        enhancedCarbonReportDao.update(
            report.copy(verificationStatus = VerificationStatus.REJECTED)
        )

        val consultant = consultantProfileDao.getById(consultantId)
            ?: throw IllegalArgumentException("Consultant non trouvé")

        val signature = ReportSignature(
            id = UUID.randomUUID().toString(),
            reportId = reportId,
            consultantId = consultantId,
            consultantName = consultant.fullName,
            certification = consultant.certification,
            signedAt = System.currentTimeMillis(),
            verificationStatus = VerificationStatus.REJECTED,
            comments = reason,
            revisionNotes = "",
            digitalSignature = "",
            qrCodeData = ""
        )

        reportSignatureDao.insert(signature)
    }

    /**
     * Vérifie l'authenticité d'une signature
     */
    suspend fun verifySignature(signatureId: String): SignatureVerification {
        val signature = reportSignatureDao.getByReportId(signatureId)
            ?: return SignatureVerification(
                isValid = false,
                message = "Signature non trouvée",
                details = null
            )

        val report = enhancedCarbonReportDao.getById(signature.reportId)
            ?: return SignatureVerification(
                isValid = false,
                message = "Rapport associé non trouvé",
                details = null
            )

        // Recalculer la signature et comparer
        val expectedSignature = generateDigitalSignature(
            reportId = signature.reportId,
            consultantId = signature.consultantId,
            timestamp = signature.signedAt
        )

        val isValid = expectedSignature == signature.digitalSignature

        return SignatureVerification(
            isValid = isValid,
            message = if (isValid) "Signature valide" else "Signature invalide",
            details = SignatureDetails(
                consultantName = signature.consultantName,
                certification = signature.certification,
                signedAt = signature.signedAt,
                reportPeriod = "${formatDate(report.periodStart)} - ${formatDate(report.periodEnd)}",
                companyId = report.companyId
            )
        )
    }

    /**
     * Génère une signature numérique SHA-256
     */
    private fun generateDigitalSignature(
        reportId: String,
        consultantId: String,
        timestamp: Long
    ): String {
        val data = "$reportId|$consultantId|$timestamp|CARBOSCAN_IFC_SIGNATURE"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Génère les données du QR code
     */
    private fun generateQRCodeData(reportId: String, signature: String): String {
        return "https://carboscan.app/verify?report=$reportId&sig=${signature.take(16)}"
    }

    /**
     * Génère un QR code bitmap
     */
    fun generateQRCodeBitmap(qrCodeData: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(qrCodeData, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }

    /**
     * Génère le texte du tampon professionnel
     */
    fun generateSignatureStamp(signature: ReportSignature): String {
        val date = formatDateTime(signature.signedAt)
        return """
            ╔══════════════════════════════════════════════╗
            ║   RAPPORT VÉRIFIÉ ET SIGNÉ                   ║
            ║                                              ║
            ║   ${signature.consultantName.padEnd(42)} ║
            ║   ${signature.certification.padEnd(42)} ║
            ║                                              ║
            ║   Signé le : ${date.padEnd(31)} ║
            ║                                              ║
            ║   Signature numérique :                      ║
            ║   ${signature.digitalSignature.take(42)} ║
            ╚══════════════════════════════════════════════╝
        """.trimIndent()
    }

    /**
     * Obtient les statistiques de signature d'un consultant
     */
    suspend fun getConsultantStats(consultantId: String): ConsultantStats {
        val allSignatures = reportSignatureDao.getByConsultantId(consultantId)
        val signedCount = reportSignatureDao.getSignedReportsCount(consultantId)
        
        return ConsultantStats(
            totalReviews = allSignatures.size,
            signedReports = signedCount,
            underReview = allSignatures.count { it.verificationStatus == VerificationStatus.UNDER_REVIEW },
            rejected = allSignatures.count { it.verificationStatus == VerificationStatus.REJECTED },
            lastSignature = allSignatures.maxByOrNull { it.signedAt }?.signedAt
        )
    }

    private fun formatDate(epochDay: Long): String {
        return java.time.LocalDate.ofEpochDay(epochDay)
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    private fun formatDateTime(timestamp: Long): String {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            java.time.ZoneId.systemDefault()
        ).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }
}

data class SignatureVerification(
    val isValid: Boolean,
    val message: String,
    val details: SignatureDetails?
)

data class SignatureDetails(
    val consultantName: String,
    val certification: String,
    val signedAt: Long,
    val reportPeriod: String,
    val companyId: String
)

data class ConsultantStats(
    val totalReviews: Int,
    val signedReports: Int,
    val underReview: Int,
    val rejected: Int,
    val lastSignature: Long?
)
