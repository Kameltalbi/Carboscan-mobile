package com.ecotrace.app.data.repository

import android.content.Context
import androidx.room.Room
import com.ecotrace.app.business.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AdvancedB2BDatabase =
        Room.databaseBuilder(ctx, AdvancedB2BDatabase::class.java, "carboscan_b2b.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    // DAOs existants
    @Provides
    fun provideEmissionDao(db: AdvancedB2BDatabase): EmissionDao = db.emissionDao()

    @Provides
    fun provideScannedProductDao(db: AdvancedB2BDatabase): ScannedProductDao = db.scannedProductDao()

    @Provides
    fun provideProductInfoDao(db: AdvancedB2BDatabase): ProductInfoDao = db.productInfoDao()

    @Provides
    fun provideCompanyProfileDao(db: AdvancedB2BDatabase): CompanyProfileDao = db.companyProfileDao()

    @Provides
    fun provideEmissionFactorDao(db: AdvancedB2BDatabase): EmissionFactorDao = db.emissionFactorDao()

    @Provides
    fun provideB2BEmissionDao(db: AdvancedB2BDatabase): B2BEmissionDao = db.b2bEmissionDao()

    @Provides
    fun provideCarbonReportDao(db: AdvancedB2BDatabase): CarbonReportDao = db.carbonReportDao()

    // Nouveaux DAOs avancés
    @Provides
    fun provideFinancialEmissionDao(db: AdvancedB2BDatabase): FinancialEmissionDao = db.financialEmissionDao()

    @Provides
    fun provideConsultantProfileDao(db: AdvancedB2BDatabase): ConsultantProfileDao = db.consultantProfileDao()

    @Provides
    fun provideReportSignatureDao(db: AdvancedB2BDatabase): ReportSignatureDao = db.reportSignatureDao()

    @Provides
    fun provideClientConsultantRelationDao(db: AdvancedB2BDatabase): ClientConsultantRelationDao = db.clientConsultantRelationDao()

    @Provides
    fun provideExchangeRateDao(db: AdvancedB2BDatabase): ExchangeRateDao = db.exchangeRateDao()

    @Provides
    fun provideMappingDictionaryDao(db: AdvancedB2BDatabase): MappingDictionaryDao = db.mappingDictionaryDao()

    @Provides
    fun provideMappingLearningDao(db: AdvancedB2BDatabase): MappingLearningDao = db.mappingLearningDao()

    @Provides
    fun provideEnhancedCompanyProfileDao(db: AdvancedB2BDatabase): EnhancedCompanyProfileDao = db.enhancedCompanyProfileDao()

    @Provides
    fun provideEnhancedCarbonReportDao(db: AdvancedB2BDatabase): EnhancedCarbonReportDao = db.enhancedCarbonReportDao()

    // Firebase Remote Config
    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1 heure
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        return remoteConfig
    }

    // Services métier
    @Provides
    @Singleton
    fun provideAdvancedMappingEngine(
        emissionFactorDao: EmissionFactorDao,
        mappingDictionaryDao: MappingDictionaryDao,
        mappingLearningDao: MappingLearningDao
    ): AdvancedMappingEngine = AdvancedMappingEngine(
        emissionFactorDao,
        mappingDictionaryDao,
        mappingLearningDao
    )

    @Provides
    @Singleton
    fun provideCurrencyConverter(
        exchangeRateDao: ExchangeRateDao
    ): CurrencyConverter = CurrencyConverter(exchangeRateDao)

    @Provides
    @Singleton
    fun provideEmissionFactorService(
        remoteConfig: FirebaseRemoteConfig,
        emissionFactorDao: EmissionFactorDao
    ): EmissionFactorService = EmissionFactorService(remoteConfig, emissionFactorDao)

    @Provides
    @Singleton
    fun provideReportSignatureService(
        consultantProfileDao: ConsultantProfileDao,
        reportSignatureDao: ReportSignatureDao,
        enhancedCarbonReportDao: EnhancedCarbonReportDao
    ): ReportSignatureService = ReportSignatureService(
        consultantProfileDao,
        reportSignatureDao,
        enhancedCarbonReportDao
    )

    @Provides
    @Singleton
    fun provideConsultantDashboardService(
        consultantProfileDao: ConsultantProfileDao,
        clientConsultantRelationDao: ClientConsultantRelationDao,
        enhancedCompanyProfileDao: EnhancedCompanyProfileDao,
        financialEmissionDao: FinancialEmissionDao,
        enhancedCarbonReportDao: EnhancedCarbonReportDao,
        reportSignatureDao: ReportSignatureDao
    ): ConsultantDashboardService = ConsultantDashboardService(
        consultantProfileDao,
        clientConsultantRelationDao,
        enhancedCompanyProfileDao,
        financialEmissionDao,
        enhancedCarbonReportDao,
        reportSignatureDao
    )

    @Provides
    @Singleton
    fun provideTransactionImporter(
        mappingEngine: AdvancedMappingEngine,
        financialEmissionDao: FinancialEmissionDao
    ): TransactionImporter = TransactionImporter(mappingEngine, financialEmissionDao)

    @Provides
    @Singleton
    fun providePdfReportGenerator(
        @ApplicationContext context: Context
    ): PdfReportGenerator = PdfReportGenerator(context)

    @Provides
    @Singleton
    fun provideCsvExporter(): CsvExporter = CsvExporter()
}
