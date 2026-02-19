package com.ecotrace.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ecotrace.app.R

/**
 * Centralized string resources for easy access throughout the app
 */
object Strings {
    @Composable
    fun homeTitle() = stringResource(R.string.home_title)
    
    @Composable
    fun homeEmptyTitle() = stringResource(R.string.home_empty_title)
    
    @Composable
    fun homeEmptyMessage() = stringResource(R.string.home_empty_message)
    
    @Composable
    fun homeEmptyButton() = stringResource(R.string.home_empty_button)
    
    @Composable
    fun homeEmptyHint() = stringResource(R.string.home_empty_hint)
    
    @Composable
    fun homeTotalLabel() = stringResource(R.string.home_total_label)
    
    @Composable
    fun homeProductsScanned() = stringResource(R.string.home_products_scanned)
    
    @Composable
    fun homeScopeDistribution() = stringResource(R.string.home_scope_distribution)
    
    @Composable
    fun homeHistory6Months() = stringResource(R.string.home_history_6months)
    
    @Composable
    fun homePersonalizedAdvice() = stringResource(R.string.home_personalized_advice)
    
    @Composable
    fun homeNationalComparison() = stringResource(R.string.home_national_comparison)
    
    @Composable
    fun comparisonVsFrance(kg: Int) = stringResource(R.string.comparison_vs_france, kg)
    
    @Composable
    fun comparisonVsObjective(kg: Int) = stringResource(R.string.comparison_vs_objective, kg)
    
    @Composable
    fun comparisonYou() = stringResource(R.string.comparison_you)
    
    @Composable
    fun comparisonFrenchAverage() = stringResource(R.string.comparison_french_average)
    
    @Composable
    fun comparisonObjective2050() = stringResource(R.string.comparison_objective_2050)
    
    @Composable
    fun fabAddEmission() = stringResource(R.string.fab_add_emission)
    
    @Composable
    fun productTop3Title() = stringResource(R.string.product_top3_title)
    
    @Composable
    fun productTotal() = stringResource(R.string.product_total)
    
    @Composable
    fun productCount(count: Int) = stringResource(R.string.product_count, count)
}
