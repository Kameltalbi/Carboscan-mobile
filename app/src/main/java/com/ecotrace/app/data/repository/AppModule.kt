package com.ecotrace.app.data.repository

import android.content.Context
import androidx.room.Room
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
    fun provideDatabase(@ApplicationContext ctx: Context): EcoTraceDatabase =
        Room.databaseBuilder(ctx, EcoTraceDatabase::class.java, "ecotrace.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideEmissionDao(db: EcoTraceDatabase): EmissionDao = db.emissionDao()

    @Provides
    fun provideScannedProductDao(db: EcoTraceDatabase): ScannedProductDao = db.scannedProductDao()

    @Provides
    fun provideProductInfoDao(db: EcoTraceDatabase): ProductInfoDao = db.productInfoDao()
}
