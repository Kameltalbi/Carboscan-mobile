package com.ecotrace.app.business

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.ecotrace.app.data.models.CarbonReport
import com.ecotrace.app.data.models.CategoryBreakdown
import com.ecotrace.app.data.models.CompanyProfile
import com.ecotrace.app.data.models.ReductionAction
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PdfReportGenerator(
    private val context: Context
) {

    private val pageWidth = 595f
    private val pageHeight = 842f
    private val margin = 50f

    fun generateReport(
        company: CompanyProfile,
        report: CarbonReport
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(
            pageWidth.toInt(),
            pageHeight.toInt(),
            1
        ).create()

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
        val fileName = "Rapport_Carbone_${company.companyName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        return file
    }

    private fun drawExecutiveSummary(
        canvas: Canvas,
        company: CompanyProfile,
        report: CarbonReport
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // En-tête avec logo/titre
        paint.apply {
            textSize = 28f
            color = Color.parseColor("#1F2937")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Bilan Carbone", margin, 80f, paint)

        paint.apply {
            textSize = 22f
            color = Color.parseColor("#4B5563")
        }
        canvas.drawText(company.companyName, margin, 115f, paint)

        // Ligne de séparation
        paint.apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 2f
        }
        canvas.drawLine(margin, 135f, pageWidth - margin, 135f, paint)

        // Période
        paint.apply {
            textSize = 14f
            color = Color.parseColor("#6B7280")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val period = "${formatDate(report.periodStart)} - ${formatDate(report.periodEnd)}"
        canvas.drawText("Période : $period", margin, 165f, paint)
        canvas.drawText("Secteur : ${company.sector.label}", margin, 185f, paint)

        // Carte Émissions Totales
        drawCard(canvas, margin, 220f, pageWidth - 2 * margin, 140f, Color.parseColor("#FEF3C7"))

        paint.apply {
            textSize = 16f
            color = Color.parseColor("#92400E")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Émissions Totales", margin + 20f, 255f, paint)

        paint.apply {
            textSize = 42f
            color = Color.parseColor("#F59E0B")
        }
        val totalTonnes = report.totalKgCo2e / 1000
        canvas.drawText(String.format("%.2f tCO₂e", totalTonnes), margin + 20f, 310f, paint)

        paint.apply {
            textSize = 12f
            color = Color.parseColor("#92400E")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(
            String.format("soit %.0f kg CO₂e", report.totalKgCo2e),
            margin + 20f,
            335f,
            paint
        )

        // Carte Intensité Carbone
        drawCard(canvas, margin, 380f, pageWidth - 2 * margin, 140f, Color.parseColor("#DBEAFE"))

        paint.apply {
            textSize = 16f
            color = Color.parseColor("#1E3A8A")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Intensité Carbone", margin + 20f, 415f, paint)

        paint.apply {
            textSize = 32f
            color = Color.parseColor("#3B82F6")
        }
        canvas.drawText(
            String.format("%.3f", report.carbonIntensity),
            margin + 20f,
            460f,
            paint
        )

        paint.apply {
            textSize = 14f
            color = Color.parseColor("#1E3A8A")
        }
        canvas.drawText("kgCO₂e / € CA", margin + 20f, 485f, paint)

        // Benchmark secteur
        val benchmark = company.sector.benchmarkKgCo2ePerEuro
        val vsAverage = ((report.carbonIntensity / benchmark) - 1) * 100

        paint.apply {
            textSize = 12f
            color = if (vsAverage < 0) Color.parseColor("#059669") else Color.parseColor("#DC2626")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val icon = if (vsAverage < 0) "↓" else "↑"
        canvas.drawText(
            "$icon ${String.format("%.1f%%", Math.abs(vsAverage))} vs moyenne secteur",
            margin + 20f,
            505f,
            paint
        )

        // Footer page 1
        drawPageFooter(canvas, 1, company.companyName)
    }

    private fun drawScopeBreakdown(canvas: Canvas, report: CarbonReport) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Titre
        paint.apply {
            textSize = 24f
            color = Color.parseColor("#1F2937")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Répartition par Scope GHG", margin, 80f, paint)

        paint.apply {
            textSize = 12f
            color = Color.parseColor("#6B7280")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Protocole GHG - Greenhouse Gas Protocol", margin, 105f, paint)

        // Graphique en barres horizontales
        val maxBarWidth = pageWidth - 2 * margin - 200f
        val barHeight = 70f
        var y = 160f

        val scopes = listOf(
            Triple("Scope 1", report.scope1Kg, Color.parseColor("#4ADE80")),
            Triple("Scope 2", report.scope2Kg, Color.parseColor("#60A5FA")),
            Triple("Scope 3", report.scope3Kg, Color.parseColor("#F59E0B"))
        )

        val scopeDescriptions = listOf(
            "Émissions directes (véhicules, chauffage)",
            "Énergie indirecte (électricité)",
            "Chaîne de valeur (fournisseurs, déplacements, déchets)"
        )

        scopes.forEachIndexed { index, (label, kg, color) ->
            val percentage = if (report.totalKgCo2e > 0) (kg / report.totalKgCo2e) * 100 else 0.0
            val barWidth = if (report.totalKgCo2e > 0) (kg / report.totalKgCo2e) * maxBarWidth else 0f

            // Label du scope
            paint.apply {
                textSize = 16f
                this.color = Color.parseColor("#1F2937")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(label, margin, y + 25f, paint)

            // Description
            paint.apply {
                textSize = 10f
                this.color = Color.parseColor("#6B7280")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText(scopeDescriptions[index], margin, y + 42f, paint)

            // Barre de fond
            paint.apply {
                this.color = Color.parseColor("#F3F4F6")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                margin + 200f,
                y,
                margin + 200f + maxBarWidth,
                y + barHeight,
                8f, 8f,
                paint
            )

            // Barre colorée
            paint.color = color
            canvas.drawRoundRect(
                margin + 200f,
                y,
                margin + 200f + barWidth.toFloat(),
                y + barHeight,
                8f, 8f,
                paint
            )

            // Valeur
            paint.apply {
                textSize = 14f
                this.color = Color.parseColor("#1F2937")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                String.format("%.1f kg", kg),
                margin + 210f,
                y + 30f,
                paint
            )

            paint.apply {
                textSize = 12f
                this.color = Color.parseColor("#6B7280")
            }
            canvas.drawText(
                String.format("(%.1f%%)", percentage),
                margin + 210f,
                y + 50f,
                paint
            )

            y += barHeight + 40f
        }

        // Note méthodologique
        paint.apply {
            textSize = 10f
            color = Color.parseColor("#9CA3AF")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText(
            "Méthodologie : GHG Protocol Corporate Standard",
            margin,
            pageHeight - 80f,
            paint
        )

        drawPageFooter(canvas, 2, "")
    }

    private fun drawTopCategories(canvas: Canvas, categories: List<CategoryBreakdown>) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        paint.apply {
            textSize = 24f
            color = Color.parseColor("#1F2937")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Top 10 Catégories d'Émissions", margin, 80f, paint)

        paint.apply {
            textSize = 12f
            color = Color.parseColor("#6B7280")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Postes d'émissions les plus importants", margin, 105f, paint)

        var y = 150f
        val top10 = categories.take(10)

        top10.forEachIndexed { index, breakdown ->
            val bgColor = if (index % 2 == 0) Color.parseColor("#F9FAFB") else Color.WHITE

            paint.apply {
                color = bgColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(margin, y, pageWidth - margin, y + 45f, paint)

            paint.apply {
                textSize = 18f
                color = Color.parseColor("#F59E0B")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("${index + 1}", margin + 15f, y + 28f, paint)

            paint.apply {
                textSize = 14f
                color = Color.parseColor("#1F2937")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val categoryName = breakdown.category.replace("_", " ")
            canvas.drawText(categoryName, margin + 50f, y + 28f, paint)

            paint.apply {
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                String.format("%.1f kg", breakdown.kgCo2e),
                pageWidth - margin - 150f,
                y + 28f,
                paint
            )

            paint.apply {
                textSize = 12f
                color = Color.parseColor("#6B7280")
            }
            canvas.drawText(
                String.format("(%.1f%%)", breakdown.percentage),
                pageWidth - margin - 60f,
                y + 28f,
                paint
            )

            y += 50f
        }

        drawPageFooter(canvas, 3, "")
    }

    private fun drawReductionPlan(canvas: Canvas, actions: List<ReductionAction>) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        paint.apply {
            textSize = 24f
            color = Color.parseColor("#1F2937")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Plan de Réduction Recommandé", margin, 80f, paint)

        paint.apply {
            textSize = 12f
            color = Color.parseColor("#6B7280")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Actions prioritaires pour réduire votre empreinte carbone", margin, 105f, paint)

        var y = 150f
        val top5 = actions.take(5)

        top5.forEachIndexed { index, action ->
            drawCard(canvas, margin, y, pageWidth - 2 * margin, 110f, Color.parseColor("#F0FDF4"))

            paint.apply {
                textSize = 14f
                color = Color.parseColor("#065F46")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("${index + 1}. ${action.title}", margin + 15f, y + 25f, paint)

            paint.apply {
                textSize = 11f
                color = Color.parseColor("#374151")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            drawMultilineText(canvas, action.description, margin + 15f, y + 45f, pageWidth - 2 * margin - 30f, paint)

            paint.apply {
                textSize = 12f
                color = Color.parseColor("#059669")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                "💰 Économie : ${String.format("%.1f kg CO₂e", action.potentialSavingKgCo2e)}",
                margin + 15f,
                y + 85f,
                paint
            )

            paint.apply {
                textSize = 10f
                color = Color.parseColor("#6B7280")
            }
            canvas.drawText(
                "Difficulté : ${action.difficulty}",
                margin + 15f,
                y + 102f,
                paint
            )

            y += 125f
        }

        drawPageFooter(canvas, 4, "")
    }

    private fun drawCard(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, color: Int) {
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(x, y, x + width, y + height, 12f, 12f, paint)

        paint.apply {
            this.color = Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(x, y, x + width, y + height, 12f, 12f, paint)
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, companyName: String) {
        val paint = Paint().apply {
            textSize = 10f
            color = Color.parseColor("#9CA3AF")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        canvas.drawText(
            "Page $pageNumber",
            pageWidth / 2 - 20f,
            pageHeight - 30f,
            paint
        )

        canvas.drawText(
            "Généré le ${formatDate(LocalDate.now().toEpochDay())}",
            margin,
            pageHeight - 30f,
            paint
        )
    }

    private fun drawMultilineText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        val words = text.split(" ")
        var currentLine = ""
        var currentY = y

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentLine = word
                currentY += paint.textSize + 4f
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
    }

    private fun formatDate(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
}
