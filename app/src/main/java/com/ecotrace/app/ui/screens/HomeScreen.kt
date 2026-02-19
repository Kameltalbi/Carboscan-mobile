package com.ecotrace.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecotrace.app.ui.components.*
import com.ecotrace.app.ui.theme.*
import com.ecotrace.app.viewmodel.EmissionViewModel
import com.ecotrace.app.viewmodel.ProductViewModel
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(viewModel: EmissionViewModel, productViewModel: ProductViewModel, onNavigateToAdd: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val productState by productViewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        viewModel.previousMonth()
                        productViewModel.previousMonth()
                    }) {
                        Icon(Icons.Default.ChevronLeft, null, tint = TextDim)
                    }
                    Text(
                        state.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
                                + " ${state.currentMonth.year}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = {
                        viewModel.nextMonth()
                        productViewModel.nextMonth()
                    }) {
                        Icon(Icons.Default.ChevronRight, null, tint = TextDim)
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EcoGreen)
                    }
                }
            } else {
                val summary = state.summary
                if (summary != null || productState.scannedProducts.isNotEmpty()) {
                    item {
                        // Score combiné (émissions + produits)
                        val totalEmissions = (summary?.totalKgCo2e ?: 0.0) + productState.totalKgCo2e
                        CombinedScoreCard(
                            emissionsSummary = summary,
                            productsKgCo2e = productState.totalKgCo2e,
                            totalKgCo2e = totalEmissions
                        )
                    }

                    // Produits scannés ce mois
                    if (productState.scannedProducts.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Text("PRODUITS SCANNÉS", style = MaterialTheme.typography.labelSmall,
                                color = TextDim, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.height(8.dp))
                            ProductsSummaryCard(productState.scannedProducts, productState.totalKgCo2e)
                        }
                    }

                    // Score principal (si données d'émissions)
                    if (summary != null) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Text("RÉPARTITION PAR SCOPE", style = MaterialTheme.typography.labelSmall,
                                color = TextDim, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.height(8.dp))
                            ScopePills(summary = summary)
                        }

                        // Historique
                        if (state.history.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(20.dp))
                                Text("HISTORIQUE 6 MOIS", style = MaterialTheme.typography.labelSmall,
                                    color = TextDim, fontFamily = FontFamily.Monospace)
                                Spacer(Modifier.height(8.dp))
                                HistoryBarChart(history = state.history)
                            }
                        }

                        // Conseils
                        if (state.advices.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(20.dp))
                                Text("CONSEILS PERSONNALISÉS", style = MaterialTheme.typography.labelSmall,
                                    color = TextDim, fontFamily = FontFamily.Monospace)
                            }
                            items(state.advices) { advice ->
                                Spacer(Modifier.height(8.dp))
                                AdviceCard(advice = advice)
                            }
                        }

                        item {
                            Spacer(Modifier.height(8.dp))
                            NationalComparisonCard(summary = summary)
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                } else {
                    // État vide avec call-to-action
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = EcoGreen.copy(alpha = 0.1f)),
                            border = BorderStroke(2.dp, EcoGreen)
                        ) {
                            Column(
                                Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌱", fontSize = 64.sp)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Commencez votre suivi carbone !",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Ajoutez vos premières émissions pour calculer votre empreinte carbone mensuelle",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextDim,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = onNavigateToAdd,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EcoGreen)
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Ajouter une émission",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "ou utilisez l'onglet Scanner pour vos achats",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextDim,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // Afficher les erreurs
            state.error?.let { error ->
                item {
                    Spacer(Modifier.height(12.dp))
                    ErrorCard(error) { viewModel.clearError() }
                }
            }
            productState.error?.let { error ->
                item {
                    Spacer(Modifier.height(12.dp))
                    ErrorCard(error) { productViewModel.clearError() }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = EcoGreen,
            contentColor = BgDark
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Ajouter une émission",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun HistoryBarChart(history: List<Pair<String, Double>>) {
    val maxVal = history.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            history.forEachIndexed { idx, (month, kg) ->
                val isLast = idx == history.lastIndex
                val fraction = (kg / maxVal).toFloat().coerceIn(0.05f, 1f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (kg >= 1000) "%.1ft".format(kg / 1000) else "${kg.toInt()}kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLast) EcoGreen else TextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(32.dp)
                            .height((fraction * 80).dp)
                            .background(
                                if (isLast) EcoGreen else EcoGreen.copy(alpha = 0.25f),
                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(month, style = MaterialTheme.typography.labelSmall,
                        color = if (isLast) EcoGreen else TextDim, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
fun NationalComparisonCard(summary: com.ecotrace.app.data.models.MonthlySummary) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2Dark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("COMPARAISON NATIONALE", style = MaterialTheme.typography.labelSmall,
                color = TextDim, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(12.dp))
            ComparisonRow("Vous", summary.totalKgCo2e, EcoGreen)
            Spacer(Modifier.height(8.dp))
            ComparisonRow("Moyenne française", 750.0, EcoAmber)
            Spacer(Modifier.height(8.dp))
            ComparisonRow("Objectif 2050", 167.0, EcoBlue)
        }
    }
}

@Composable
fun ComparisonRow(label: String, kg: Double, color: MaterialColor = EcoGreen) {
    val max = 1500.0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(130.dp), style = MaterialTheme.typography.bodyMedium, color = TextDim, fontSize = 12.sp)
        Box(Modifier.weight(1f).height(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).background(BorderDark)) {
            Box(Modifier.fillMaxWidth((kg / max).toFloat().coerceIn(0f, 1f)).fillMaxHeight()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).background(color))
        }
        Spacer(Modifier.width(8.dp))
        Text("${kg.toInt()} kg", style = MaterialTheme.typography.labelSmall,
            color = color, fontFamily = FontFamily.Monospace, modifier = Modifier.width(60.dp))
    }
}

typealias MaterialColor = androidx.compose.ui.graphics.Color

@Composable
fun EmptyStateCard() {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            Modifier.padding(40.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌱", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("Aucune donnée ce mois", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Ajoutez vos premières émissions\nen appuyant sur +",
                style = MaterialTheme.typography.bodyMedium, color = TextDim,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
