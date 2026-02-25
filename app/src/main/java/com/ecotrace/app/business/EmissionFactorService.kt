package com.ecotrace.app.business

import com.ecotrace.app.data.models.B2BCategory
import com.ecotrace.app.data.models.EmissionFactor
import com.ecotrace.app.data.models.ScopeType
import com.ecotrace.app.data.repository.EmissionFactorDao
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EmissionFactorService(
    private val remoteConfig: FirebaseRemoteConfig,
    private val localDao: EmissionFactorDao
) {

    private val gson = Gson()

    suspend fun initializeDefaultFactors() {
        val count = localDao.getCount()
        if (count == 0) {
            val defaultFactors = createDefaultFactorsFrance()
            localDao.insertAll(defaultFactors)
        }
    }

    suspend fun syncFactorsForCountry(countryCode: String) {
        try {
            remoteConfig.fetchAndActivate().await()
            
            val factorsJson = remoteConfig.getString("emission_factors_$countryCode")
            if (factorsJson.isNotEmpty()) {
                val factors = parseFactorsFromJson(factorsJson, countryCode)
                if (factors.isNotEmpty()) {
                    localDao.insertAll(factors)
                }
            }
        } catch (e: Exception) {
            // Fallback sur facteurs locaux
        }
    }

    private fun parseFactorsFromJson(json: String, country: String): List<EmissionFactor> {
        try {
            val type = object : TypeToken<Map<String, FactorData>>() {}.type
            val factorsMap: Map<String, FactorData> = gson.fromJson(json, type)
            
            return factorsMap.map { (category, data) ->
                EmissionFactor(
                    id = UUID.randomUUID().toString(),
                    category = category,
                    scope = parseScopeFromCategory(category),
                    unit = data.unit,
                    kgCo2ePerUnit = data.value,
                    country = country,
                    source = data.source,
                    description = data.description ?: "",
                    keywords = data.keywords ?: emptyList()
                )
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    suspend fun getFactorForCategory(
        category: String,
        country: String = "FR"
    ): EmissionFactor? {
        return localDao.getByCountryAndCategory(country, category)
            ?: localDao.getByCountryAndCategory("FR", category)
    }

    suspend fun getAllFactorsByCountry(country: String): List<EmissionFactor> {
        return localDao.getAllByCountry(country)
    }

    private fun createDefaultFactorsFrance(): List<EmissionFactor> {
        return B2BCategory.values().map { category ->
            EmissionFactor(
                id = UUID.randomUUID().toString(),
                category = category.name,
                scope = category.scope,
                unit = category.unit,
                kgCo2ePerUnit = category.factorKgCo2PerUnit,
                country = "FR",
                source = "ADEME Base Carbone 2024",
                description = category.label,
                keywords = generateKeywords(category.label)
            )
        }
    }

    fun createFactorsForCountry(country: String): List<EmissionFactor> {
        return when (country) {
            "US" -> createFactorsUSA()
            "UK" -> createFactorsUK()
            "DE" -> createFactorsGermany()
            "ES" -> createFactorsSpain()
            else -> createDefaultFactorsFrance()
        }
    }

    private fun createFactorsUSA(): List<EmissionFactor> {
        return listOf(
            EmissionFactor(
                id = UUID.randomUUID().toString(),
                category = "ELECTRICITE_LOCAUX",
                scope = ScopeType.SCOPE2,
                unit = "kWh",
                kgCo2ePerUnit = 0.385,
                country = "US",
                source = "EPA eGRID 2023",
                description = "US average grid mix",
                keywords = listOf("electricity", "power", "energy")
            ),
            EmissionFactor(
                id = UUID.randomUUID().toString(),
                category = "GAZ_NATUREL_LOCAUX",
                scope = ScopeType.SCOPE1,
                unit = "m³",
                kgCo2ePerUnit = 2.15,
                country = "US",
                source = "EPA 2023",
                description = "Natural gas combustion",
                keywords = listOf("natural gas", "gas", "heating")
            ),
            EmissionFactor(
                id = UUID.randomUUID().toString(),
                category = "VEHICULE_ENTREPRISE_ESSENCE",
                scope = ScopeType.SCOPE1,
                unit = "mile",
                kgCo2ePerUnit = 0.351,
                country = "US",
                source = "EPA 2023",
                description = "Gasoline vehicle (converted to miles)",
                keywords = listOf("car", "vehicle", "gasoline", "petrol")
            )
        )
    }

    private fun createFactorsUK(): List<EmissionFactor> {
        return listOf(
            EmissionFactor(
                id = UUID.randomUUID().toString(),
                category = "ELECTRICITE_LOCAUX",
                scope = ScopeType.SCOPE2,
                unit = "kWh",
                kgCo2ePerUnit = 0.233,
                country = "UK",
                source = "DEFRA 2024",
                description = "UK grid average",
                keywords = listOf("electricity", "power")
            ),
            EmissionFactor(
                id = UUID.randomUUID().toString(),
                category = "GAZ_NATUREL_LOCAUX",
                scope = ScopeType.SCOPE1,
                unit = "m³",
                kgCo2ePerUnit = 2.04,
                country = "UK",
                source = "DEFRA 2024",
                description = "Natural gas",
                keywords = listOf("natural gas", "gas")
            )
        )
    }

    private fun createFactorsGermany(): List<EmissionFactor> {
        return listOf(
            EmissionFactor(
                id = UUID.randomUUID().toString(),
                category = "ELECTRICITE_LOCAUX",
                scope = ScopeType.SCOPE2,
                unit = "kWh",
                kgCo2ePerUnit = 0.485,
                country = "DE",
                source = "UBA 2024",
                description = "German grid mix (coal intensive)",
                keywords = listOf("strom", "elektrizität", "electricity")
            ),
            EmissionFactor(
                id = UUID.randomUUID().toString(),
                category = "GAZ_NATUREL_LOCAUX",
                scope = ScopeType.SCOPE1,
                unit = "m³",
                kgCo2ePerUnit = 2.04,
                country = "DE",
                source = "UBA 2024",
                description = "Erdgas",
                keywords = listOf("erdgas", "gas", "natural gas")
            )
        )
    }

    private fun createFactorsSpain(): List<EmissionFactor> {
        return listOf(
            EmissionFactor(
                id = UUID.randomUUID().toString(),
                category = "ELECTRICITE_LOCAUX",
                scope = ScopeType.SCOPE2,
                unit = "kWh",
                kgCo2ePerUnit = 0.210,
                country = "ES",
                source = "MITECO 2024",
                description = "Spanish grid mix",
                keywords = listOf("electricidad", "electricity")
            ),
            EmissionFactor(
                id = UUID.randomUUID().toString(),
                category = "GAZ_NATUREL_LOCAUX",
                scope = ScopeType.SCOPE1,
                unit = "m³",
                kgCo2ePerUnit = 2.04,
                country = "ES",
                source = "MITECO 2024",
                description = "Gas natural",
                keywords = listOf("gas natural", "gas")
            )
        )
    }

    private fun parseScopeFromCategory(category: String): ScopeType {
        return when {
            category.contains("VEHICULE") || category.contains("GAZ") || category.contains("FIOUL") -> ScopeType.SCOPE1
            category.contains("ELECTRICITE") -> ScopeType.SCOPE2
            else -> ScopeType.SCOPE3
        }
    }

    private fun generateKeywords(label: String): List<String> {
        return label.lowercase()
            .split(" ", "/", "-")
            .filter { it.length > 2 }
    }

    data class FactorData(
        val value: Double,
        val unit: String,
        val source: String,
        val description: String? = null,
        val keywords: List<String>? = null
    )
}
