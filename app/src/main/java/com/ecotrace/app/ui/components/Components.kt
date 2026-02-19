package com.ecotrace.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ecotrace.app.data.models.*
import com.ecotrace.app.ui.theme.*
import kotlin.math.min
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

// ── Score Principal ──────────────────────────────────────────────────────────
@Composable
fun ScoreCard(summary: MonthlySummary, modifier: Modifier = Modifier) {
    val tCo2 = summary.tCo2e
    val scoreColor = when {
        tCo2 < 0.3 -> EcoGreen
        tCo2 < 0.75 -> EcoAmber
        else -> EcoRed
    }
    val label = when {
        tCo2 < 0.3 -> "Excellent 🌱"
        tCo2 < 0.75 -> "Moyen ⚠️"
        else -> "Élevé 🔥"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(scoreColor.copy(alpha = 0.08f), Color.Transparent),
                        radius = 600f
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "EMPREINTE DU MOIS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%.2f".format(tCo2),
                        style = MaterialTheme.typography.displayLarge,
                        color = scoreColor,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "t CO₂e",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDim,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                Text(label, color = scoreColor, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))

                // Barre vs France
                CompareBar(
                    label = "Vs. Française (750 kg/mois)",
                    percent = (summary.vsFranceMoyenne / 100.0).toFloat().coerceAtMost(1.5f),
                    color = EcoAmber
                )
                Spacer(Modifier.height(8.dp))
                CompareBar(
                    label = "Vs. Objectif 2050 (167 kg/mois)",
                    percent = (summary.vsObjectif2050 / 100.0).toFloat().coerceAtMost(2f),
                    color = EcoBlue
                )
            }
        }
    }
}

@Composable
fun CompareBar(label: String, percent: Float, color: Color) {
    val animatedWidth by animateFloatAsState(
        targetValue = min(percent, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "bar"
    )
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextDim, fontSize = 11.sp)
            Text(
                "${(percent * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(BorderDark)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

// ── Scope Pills ──────────────────────────────────────────────────────────────
@Composable
fun ScopePills(summary: MonthlySummary, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ScopePill(
            scope = Scope.SCOPE1,
            kg = summary.scope1Kg,
            color = Scope1Color,
            modifier = Modifier.weight(1f)
        )
        ScopePill(
            scope = Scope.SCOPE2,
            kg = summary.scope2Kg,
            color = Scope2Color,
            modifier = Modifier.weight(1f)
        )
        ScopePill(
            scope = Scope.SCOPE3,
            kg = summary.scope3Kg,
            color = Scope3Color,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ScopePill(scope: Scope, kg: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.06f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    scope.name.replace("SCOPE", "S"),
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (kg >= 1000) "%.1f t".format(kg / 1000) else "${kg.toInt()} kg",
                    style = MaterialTheme.typography.titleLarge,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Text("CO₂e", style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.5f))
            }
        }
    }
}

// ── Advice Card ──────────────────────────────────────────────────────────────
@Composable
fun AdviceCard(advice: Advice, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2Dark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EcoGreenDim),
                contentAlignment = Alignment.Center
            ) {
                Text(advice.icon, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(advice.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(advice.description, style = MaterialTheme.typography.bodyMedium, color = TextDim)
                if (advice.savingKg > 0) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EcoGreenDim
                    ) {
                        Text(
                            "−${advice.savingKg.toInt()} kg CO₂e potentiel",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = EcoGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ── Score Combiné (Émissions + Produits) ────────────────────────────────────
@Composable
fun CombinedScoreCard(emissionsSummary: MonthlySummary?, productsKgCo2e: Double, totalKgCo2e: Double, modifier: Modifier = Modifier) {
    val tCo2 = totalKgCo2e / 1000.0
    val scoreColor = when {
        tCo2 < 0.3 -> EcoGreen
        tCo2 < 0.75 -> EcoAmber
        else -> EcoRed
    }
    val label = when {
        tCo2 < 0.3 -> "Excellent 🌱"
        tCo2 < 0.75 -> "Moyen ⚠️"
        else -> "Élevé 🔥"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(scoreColor.copy(alpha = 0.08f), Color.Transparent),
                        radius = 600f
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "EMPREINTE TOTALE DU MOIS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%.2f".format(tCo2),
                        style = MaterialTheme.typography.displayLarge,
                        color = scoreColor,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "t CO₂e",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDim,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                Text(label, color = scoreColor, fontWeight = FontWeight.SemiBold)
                
                if (emissionsSummary != null && productsKgCo2e > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Activités", style = MaterialTheme.typography.labelSmall, color = TextDim)
                            Text(
                                "%.1f kg".format(emissionsSummary.totalKgCo2e),
                                style = MaterialTheme.typography.titleMedium,
                                color = EcoBlue,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Produits", style = MaterialTheme.typography.labelSmall, color = TextDim)
                            Text(
                                "%.1f kg".format(productsKgCo2e),
                                style = MaterialTheme.typography.titleMedium,
                                color = EcoAmber,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                val vsFrance = (totalKgCo2e / 750.0).toFloat()
                val vsObjectif = (totalKgCo2e / 167.0).toFloat()
                CompareBar(
                    label = "Vs. Française (750 kg/mois)",
                    percent = vsFrance.coerceAtMost(1.5f),
                    color = EcoAmber
                )
                Spacer(Modifier.height(8.dp))
                CompareBar(
                    label = "Vs. Objectif 2050 (167 kg/mois)",
                    percent = vsObjectif.coerceAtMost(2f),
                    color = EcoBlue
                )
            }
        }
    }
}

// ── Carte Résumé Produits ───────────────────────────────────────────────────
@Composable
fun ProductsSummaryCard(products: List<com.ecotrace.app.data.models.ScannedProduct>, totalKgCo2e: Double) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2Dark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${products.size} produit${if (products.size > 1) "s" else ""} scanné${if (products.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Impact carbone des achats",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDim,
                        fontSize = 12.sp
                    )
                }
                Text(
                    "%.1f kg".format(totalKgCo2e),
                    style = MaterialTheme.typography.headlineMedium,
                    color = EcoAmber,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            if (products.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = BorderDark)
                Spacer(Modifier.height(12.dp))
                
                val topProducts = products.sortedByDescending { it.totalKgCo2e }.take(3)
                topProducts.forEach { product ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                product.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                            if (product.brand.isNotBlank()) {
                                Text(
                                    product.brand,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextDim,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            "%.1f kg".format(product.totalKgCo2e),
                            style = MaterialTheme.typography.bodyMedium,
                            color = EcoAmber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Carte d'Erreur ───────────────────────────────────────────────────────────
@Composable
fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EcoRed.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, EcoRed)
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoRed
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = EcoRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}
