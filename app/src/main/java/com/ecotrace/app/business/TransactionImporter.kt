package com.ecotrace.app.business

import com.ecotrace.app.data.models.B2BEmissionEntry
import com.ecotrace.app.data.models.ImportResult
import com.ecotrace.app.data.models.ImportedTransaction
import com.ecotrace.app.data.repository.B2BEmissionDao
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

class TransactionImporter(
    private val mappingEngine: IntelligentMappingEngine,
    private val b2bEmissionDao: B2BEmissionDao
) {

    private val dateFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    )

    suspend fun importFromCsv(
        csvFile: File,
        companyId: String,
        delimiter: String = ",",
        skipHeader: Boolean = true
    ): ImportResult {
        val results = mutableListOf<ImportedTransaction>()
        val errors = mutableListOf<String>()

        try {
            csvFile.bufferedReader().useLines { lines ->
                val linesList = lines.toList()
                val startIndex = if (skipHeader) 1 else 0

                linesList.drop(startIndex).forEachIndexed { index, line ->
                    try {
                        val parts = parseCsvLine(line, delimiter)
                        if (parts.size >= 3) {
                            val date = parseDate(parts[0])
                            val label = parts[1].trim()
                            val amount = parts[2].replace(",", ".").toDoubleOrNull() ?: 0.0
                            val supplier = parts.getOrNull(3)?.trim() ?: ""
                            val manualCategory = parts.getOrNull(4)?.trim()

                            val suggestion = if (manualCategory.isNullOrBlank()) {
                                mappingEngine.suggestMapping(label, amount)
                            } else {
                                null
                            }

                            results.add(
                                ImportedTransaction(
                                    date = date,
                                    label = label,
                                    amount = amount,
                                    supplier = supplier,
                                    suggestedCategory = suggestion?.category ?: manualCategory,
                                    confidence = suggestion?.confidence ?: 1.0
                                )
                            )
                        } else {
                            errors.add("Ligne ${index + startIndex + 1}: Format invalide (colonnes insuffisantes)")
                        }
                    } catch (e: Exception) {
                        errors.add("Ligne ${index + startIndex + 1}: ${e.message}")
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

    suspend fun confirmAndSaveTransactions(
        transactions: List<ImportedTransaction>,
        companyId: String
    ): Int {
        var savedCount = 0

        transactions.forEach { transaction ->
            if (transaction.suggestedCategory != null) {
                try {
                    val emissionFactor = mappingEngine.suggestMapping(
                        transaction.label,
                        transaction.amount
                    )?.emissionFactor

                    if (emissionFactor != null) {
                        val entry = B2BEmissionEntry(
                            id = UUID.randomUUID().toString(),
                            companyId = companyId,
                            date = transaction.date,
                            categoryName = transaction.suggestedCategory,
                            scope = emissionFactor.scope,
                            valueInput = transaction.amount,
                            unit = emissionFactor.unit,
                            emissionFactorKgCo2e = emissionFactor.kgCo2ePerUnit,
                            emissionFactorSource = emissionFactor.source,
                            kgCo2e = transaction.amount * emissionFactor.kgCo2ePerUnit,
                            transactionLabel = transaction.label,
                            invoiceReference = "",
                            supplierName = transaction.supplier,
                            note = "Importé automatiquement",
                            isAutoMapped = transaction.confidence < 1.0
                        )

                        b2bEmissionDao.insert(entry)
                        savedCount++
                    }
                } catch (e: Exception) {
                    // Log error but continue
                }
            }
        }

        return savedCount
    }

    private fun parseCsvLine(line: String, delimiter: String): List<String> {
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char.toString() == delimiter && !inQuotes -> {
                    parts.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        parts.add(current.toString())

        return parts.map { it.trim('"', ' ') }
    }

    private fun parseDate(dateStr: String): Long {
        val trimmed = dateStr.trim()

        for (formatter in dateFormatters) {
            try {
                return LocalDate.parse(trimmed, formatter).toEpochDay()
            } catch (e: DateTimeParseException) {
                continue
            }
        }

        throw IllegalArgumentException("Format de date non reconnu: $dateStr")
    }

    fun generateCsvTemplate(): String {
        return """Date,Libellé,Montant,Fournisseur,Catégorie (optionnel)
2024-01-15,Shell Station Paris,85.50,Shell,
2024-01-20,Facture AWS Janvier,450.00,Amazon Web Services,SERVICES_CLOUD
2024-02-01,Billet Air France CDG-JFK,680.00,Air France,
2024-02-05,Fournitures Office Depot,125.30,Office Depot,
2024-02-10,Facture EDF Février,320.00,EDF,ELECTRICITE_LOCAUX"""
    }

    suspend fun importFromExcel(
        excelFile: File,
        companyId: String
    ): ImportResult {
        // TODO: Implémenter avec Apache POI ou similaire
        // Pour l'instant, demander à l'utilisateur de convertir en CSV
        return ImportResult(
            success = emptyList(),
            errors = listOf("Import Excel non encore implémenté. Veuillez convertir en CSV."),
            totalProcessed = 0
        )
    }
}
