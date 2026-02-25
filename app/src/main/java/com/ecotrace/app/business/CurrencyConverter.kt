package com.ecotrace.app.business

import com.ecotrace.app.data.models.ExchangeRate
import com.ecotrace.app.data.repository.ExchangeRateDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.UUID

class CurrencyConverter(
    private val exchangeRateDao: ExchangeRateDao
) {

    // API gratuite : https://api.exchangerate-api.com/v4/latest/EUR
    private val apiBaseUrl = "https://api.exchangerate-api.com/v4/latest"
    
    // Cache des taux en mémoire
    private val rateCache = mutableMapOf<Pair<String, String>, ExchangeRate>()

    /**
     * Convertit un montant d'une devise vers EUR
     * Utilise le cache local puis l'API si nécessaire
     */
    suspend fun convertToEur(
        amount: Double,
        fromCurrency: String
    ): ConversionResult {
        if (fromCurrency == "EUR") {
            return ConversionResult(
                originalAmount = amount,
                convertedAmount = amount,
                fromCurrency = "EUR",
                toCurrency = "EUR",
                rate = 1.0,
                date = LocalDate.now().toEpochDay(),
                source = "direct"
            )
        }

        // 1. Chercher dans le cache mémoire
        val cacheKey = Pair(fromCurrency, "EUR")
        val cachedRate = rateCache[cacheKey]
        
        if (cachedRate != null && isRateRecent(cachedRate.date)) {
            return ConversionResult(
                originalAmount = amount,
                convertedAmount = amount * cachedRate.rate,
                fromCurrency = fromCurrency,
                toCurrency = "EUR",
                rate = cachedRate.rate,
                date = cachedRate.date,
                source = "cache"
            )
        }

        // 2. Chercher dans la base de données
        val dbRate = exchangeRateDao.getLatestRate(fromCurrency, "EUR")
        
        if (dbRate != null && isRateRecent(dbRate.date)) {
            rateCache[cacheKey] = dbRate
            return ConversionResult(
                originalAmount = amount,
                convertedAmount = amount * dbRate.rate,
                fromCurrency = fromCurrency,
                toCurrency = "EUR",
                rate = dbRate.rate,
                date = dbRate.date,
                source = "database"
            )
        }

        // 3. Récupérer depuis l'API
        return try {
            val rate = fetchRateFromApi(fromCurrency, "EUR")
            val convertedAmount = amount * rate
            
            // Sauvegarder en base
            val exchangeRate = ExchangeRate(
                id = UUID.randomUUID().toString(),
                fromCurrency = fromCurrency,
                toCurrency = "EUR",
                rate = rate,
                date = LocalDate.now().toEpochDay(),
                source = "ExchangeRate-API"
            )
            exchangeRateDao.insert(exchangeRate)
            rateCache[cacheKey] = exchangeRate
            
            ConversionResult(
                originalAmount = amount,
                convertedAmount = convertedAmount,
                fromCurrency = fromCurrency,
                toCurrency = "EUR",
                rate = rate,
                date = LocalDate.now().toEpochDay(),
                source = "api"
            )
        } catch (e: Exception) {
            // Fallback : utiliser le dernier taux connu même s'il est ancien
            if (dbRate != null) {
                ConversionResult(
                    originalAmount = amount,
                    convertedAmount = amount * dbRate.rate,
                    fromCurrency = fromCurrency,
                    toCurrency = "EUR",
                    rate = dbRate.rate,
                    date = dbRate.date,
                    source = "fallback",
                    error = "Taux non à jour (${formatDate(dbRate.date)})"
                )
            } else {
                ConversionResult(
                    originalAmount = amount,
                    convertedAmount = amount,
                    fromCurrency = fromCurrency,
                    toCurrency = "EUR",
                    rate = 1.0,
                    date = LocalDate.now().toEpochDay(),
                    source = "error",
                    error = "Impossible de récupérer le taux : ${e.message}"
                )
            }
        }
    }

    /**
     * Récupère le taux de change depuis l'API
     */
    private suspend fun fetchRateFromApi(from: String, to: String): Double = withContext(Dispatchers.IO) {
        val url = URL("$apiBaseUrl/$from")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                val json = JSONObject(response)
                val rates = json.getJSONObject("rates")
                
                if (to == "EUR") {
                    rates.getDouble("EUR")
                } else {
                    throw IllegalArgumentException("Conversion vers $to non supportée")
                }
            } else {
                throw Exception("API error: HTTP $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Vérifie si un taux est récent (< 24h)
     */
    private fun isRateRecent(rateDate: Long): Boolean {
        val today = LocalDate.now().toEpochDay()
        return (today - rateDate) <= 1 // Max 1 jour
    }

    /**
     * Synchronise tous les taux pour les devises courantes
     */
    suspend fun syncCommonCurrencies() {
        val currencies = listOf("USD", "GBP", "CHF", "CAD", "TND", "MAD", "DZD", "JPY", "CNY")
        
        currencies.forEach { currency ->
            try {
                convertToEur(1.0, currency)
            } catch (e: Exception) {
                // Continue même en cas d'erreur
            }
        }
    }

    /**
     * Nettoie les taux de change anciens (> 30 jours)
     */
    suspend fun cleanOldRates() {
        val thirtyDaysAgo = LocalDate.now().minusDays(30).toEpochDay()
        exchangeRateDao.deleteOldRates(thirtyDaysAgo)
    }

    /**
     * Obtient le taux actuel sans conversion
     */
    suspend fun getRate(from: String, to: String = "EUR"): Double {
        val result = convertToEur(1.0, from)
        return result.rate
    }

    /**
     * Liste des devises supportées avec leurs symboles
     */
    fun getSupportedCurrencies(): List<CurrencyInfo> {
        return listOf(
            CurrencyInfo("EUR", "Euro", "€"),
            CurrencyInfo("USD", "Dollar américain", "$"),
            CurrencyInfo("GBP", "Livre sterling", "£"),
            CurrencyInfo("CHF", "Franc suisse", "CHF"),
            CurrencyInfo("CAD", "Dollar canadien", "CA$"),
            CurrencyInfo("TND", "Dinar tunisien", "TND"),
            CurrencyInfo("MAD", "Dirham marocain", "MAD"),
            CurrencyInfo("DZD", "Dinar algérien", "DZD"),
            CurrencyInfo("JPY", "Yen japonais", "¥"),
            CurrencyInfo("CNY", "Yuan chinois", "¥"),
            CurrencyInfo("AUD", "Dollar australien", "A$"),
            CurrencyInfo("BRL", "Real brésilien", "R$"),
            CurrencyInfo("INR", "Roupie indienne", "₹"),
            CurrencyInfo("ZAR", "Rand sud-africain", "R")
        )
    }

    private fun formatDate(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).toString()
    }
}

data class ConversionResult(
    val originalAmount: Double,
    val convertedAmount: Double,
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val date: Long,
    val source: String, // "cache", "database", "api", "fallback", "error"
    val error: String? = null
)

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String
)
