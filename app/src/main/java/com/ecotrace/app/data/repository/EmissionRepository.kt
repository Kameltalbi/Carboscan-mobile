package com.ecotrace.app.data.repository

import com.ecotrace.app.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmissionRepository @Inject constructor(
    private val dao: EmissionDao
) {
    fun getAllFlow(): Flow<List<EmissionEntry>> = dao.getAllFlow()

    fun getMonthFlow(year: Int, month: Int): Flow<List<EmissionEntry>> {
        val ym = YearMonth.of(year, month)
        val from = ym.atDay(1).toEpochDay()
        val to = ym.atEndOfMonth().toEpochDay()
        return dao.getByPeriodFlow(from, to)
    }

    fun getMonthlySummaryFlow(year: Int, month: Int): Flow<MonthlySummary> =
        getMonthFlow(year, month).map { entries ->
            MonthlySummary(
                year = year,
                month = month,
                totalKgCo2e = entries.sumOf { it.kgCo2e },
                scope1Kg = entries.filter { it.scope == Scope.SCOPE1 }.sumOf { it.kgCo2e },
                scope2Kg = entries.filter { it.scope == Scope.SCOPE2 }.sumOf { it.kgCo2e },
                scope3Kg = entries.filter { it.scope == Scope.SCOPE3 }.sumOf { it.kgCo2e },
                entries = entries
            )
        }

    // Historique des 6 derniers mois
    fun getMonthlyHistoryFlow(): Flow<List<Pair<String, Double>>> {
        val today = LocalDate.now()
        val months = (5 downTo 0).map { offset ->
            val ym = YearMonth.from(today).minusMonths(offset.toLong())
            ym
        }
        val from = months.first().atDay(1).toEpochDay()
        val to = months.last().atEndOfMonth().toEpochDay()
        return dao.getByPeriodFlow(from, to).map { entries ->
            months.map { ym ->
                val label = "${ym.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
                val kg = entries
                    .filter { LocalDate.ofEpochDay(it.date).let { d -> d.year == ym.year && d.monthValue == ym.monthValue } }
                    .sumOf { it.kgCo2e }
                label to kg
            }
        }
    }

    suspend fun addEntry(
        category: Category,
        valueInput: Double,
        date: LocalDate = LocalDate.now(),
        note: String = ""
    ) {
        val entry = EmissionEntry(
            id = java.util.UUID.randomUUID().toString(),
            date = date.toEpochDay(),
            categoryName = category.name,
            valueInput = valueInput,
            kgCo2e = valueInput * category.factorKgCo2PerUnit,
            note = note
        )
        dao.insert(entry)
    }

    suspend fun deleteEntry(id: String) = dao.deleteById(id)

    // Génère des conseils personnalisés selon les émissions
    fun generateAdvices(summary: MonthlySummary): List<Advice> {
        val advices = mutableListOf<Advice>()
        val byCat = summary.entries.groupBy { it.category }

        byCat[Category.CAR_ESSENCE]?.let { entries ->
            val totalKm = entries.sumOf { it.valueInput }
            if (totalKm > 500) advices.add(
                Advice("🚲", "Réduire les trajets voiture",
                    "Vous avez parcouru ${totalKm.toInt()} km en voiture essence. Passer au vélo ou covoiturage pour les courts trajets pourrait économiser ${(totalKm * 0.1 * 0.218).toInt()} kg CO₂.",
                    totalKm * 0.1 * 0.218, Category.CAR_ESSENCE)
            )
        }
        byCat[Category.AVION_COURT]?.let { entries ->
            val totalKm = entries.sumOf { it.valueInput }
            advices.add(
                Advice("🚆", "Préférer le train",
                    "Remplacer un vol court-courrier par le train réduit les émissions de 95%. Économie potentielle : ${(totalKm * (0.255 - 0.004)).toInt()} kg CO₂.",
                    totalKm * (0.255 - 0.004), Category.AVION_COURT)
            )
        }
        byCat[Category.BOEUF]?.let { entries ->
            val kg = entries.sumOf { it.valueInput }
            if (kg > 2) advices.add(
                Advice("🥗", "Réduire la viande rouge",
                    "Vous avez consommé ${kg.toInt()} kg de bœuf. Réduire de moitié économiserait ${(kg * 0.5 * 27.0).toInt()} kg CO₂ par mois.",
                    kg * 0.5 * 27.0, Category.BOEUF)
            )
        }
        byCat[Category.STREAMING]?.let { entries ->
            val h = entries.sumOf { it.valueInput }
            if (h > 50) advices.add(
                Advice("📉", "Réduire la qualité vidéo",
                    "Passer en HD au lieu du 4K réduit la consommation de données de 4x. Économie estimée : ${(h * 0.027).toInt()} kg CO₂.",
                    h * 0.027, Category.STREAMING)
            )
        }
        if (advices.isEmpty()) advices.add(
            Advice("🌱", "Continuez comme ça !",
                "Votre empreinte est dans la bonne direction. Suivez vos progrès mois après mois.",
                0.0, Category.VEGETARIEN)
        )
        return advices.sortedByDescending { it.savingKg }
    }
}
