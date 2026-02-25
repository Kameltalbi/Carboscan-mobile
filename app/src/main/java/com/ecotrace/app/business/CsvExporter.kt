package com.ecotrace.app.business

import com.ecotrace.app.data.models.B2BEmissionEntry
import com.ecotrace.app.data.models.CarbonReport
import com.ecotrace.app.data.models.CompanyProfile
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CsvExporter {

    fun exportEmissions(
        entries: List<B2BEmissionEntry>,
        outputFile: File
    ): Boolean {
        return try {
            outputFile.bufferedWriter().use { writer ->
                writer.write("Date,Catégorie,Scope,Valeur,Unité,Facteur Émission,Source FE,kgCO₂e,Libellé Transaction,Fournisseur,Référence Facture,Note,Auto-mappé\n")
                
                entries.forEach { entry ->
                    val date = LocalDate.ofEpochDay(entry.date).format(DateTimeFormatter.ISO_DATE)
                    val autoMapped = if (entry.isAutoMapped) "Oui" else "Non"
                    
                    writer.write(
                        "${escapeCsv(date)}," +
                        "${escapeCsv(entry.categoryName)}," +
                        "${escapeCsv(entry.scope.label)}," +
                        "${entry.valueInput}," +
                        "${escapeCsv(entry.unit)}," +
                        "${entry.emissionFactorKgCo2e}," +
                        "${escapeCsv(entry.emissionFactorSource)}," +
                        "${entry.kgCo2e}," +
                        "${escapeCsv(entry.transactionLabel)}," +
                        "${escapeCsv(entry.supplierName)}," +
                        "${escapeCsv(entry.invoiceReference)}," +
                        "${escapeCsv(entry.note)}," +
                        "$autoMapped\n"
                    )
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun exportReport(
        company: CompanyProfile,
        report: CarbonReport,
        outputFile: File
    ): Boolean {
        return try {
            outputFile.bufferedWriter().use { writer ->
                writer.write("# Rapport Carbone - ${company.companyName}\n")
                writer.write("# Période : ${formatDate(report.periodStart)} - ${formatDate(report.periodEnd)}\n")
                writer.write("# Généré le : ${formatDate(LocalDate.now().toEpochDay())}\n\n")
                
                writer.write("## Synthèse Globale\n")
                writer.write("Entreprise,${company.companyName}\n")
                writer.write("Secteur,${company.sector.label}\n")
                writer.write("Effectif,${company.employees}\n")
                writer.write("CA Annuel (€),${company.annualRevenue}\n")
                writer.write("Émissions Totales (kgCO₂e),${report.totalKgCo2e}\n")
                writer.write("Émissions Totales (tCO₂e),${report.totalKgCo2e / 1000}\n")
                writer.write("Intensité Carbone (kgCO₂e/€ CA),${report.carbonIntensity}\n\n")
                
                writer.write("## Répartition par Scope\n")
                writer.write("Scope,kgCO₂e,Pourcentage\n")
                writer.write("Scope 1,${report.scope1Kg},${(report.scope1Kg / report.totalKgCo2e * 100)}\n")
                writer.write("Scope 2,${report.scope2Kg},${(report.scope2Kg / report.totalKgCo2e * 100)}\n")
                writer.write("Scope 3,${report.scope3Kg},${(report.scope3Kg / report.totalKgCo2e * 100)}\n\n")
                
                writer.write("## Top Catégories d'Émissions\n")
                writer.write("Rang,Catégorie,kgCO₂e,Pourcentage\n")
                report.topEmissionCategories.forEachIndexed { index, breakdown ->
                    writer.write("${index + 1},${breakdown.category},${breakdown.kgCo2e},${breakdown.percentage}\n")
                }
                
                writer.write("\n## Plan de Réduction\n")
                writer.write("Action,Description,Économie Potentielle (kgCO₂e),Économie Potentielle (€),Difficulté\n")
                report.reductionPlan.forEach { action ->
                    writer.write(
                        "${escapeCsv(action.title)}," +
                        "${escapeCsv(action.description)}," +
                        "${action.potentialSavingKgCo2e}," +
                        "${action.potentialSavingEuro}," +
                        "${action.difficulty}\n"
                    )
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun formatDate(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
}
