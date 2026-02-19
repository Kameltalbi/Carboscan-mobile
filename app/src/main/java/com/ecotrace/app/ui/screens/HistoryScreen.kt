package com.ecotrace.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecotrace.app.data.models.*
import com.ecotrace.app.ui.theme.*
import com.ecotrace.app.viewmodel.EmissionViewModel
import com.ecotrace.app.viewmodel.ProductViewModel
import java.time.format.DateTimeFormatter
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@Composable
fun HistoryScreen(emissionViewModel: EmissionViewModel, productViewModel: ProductViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val entries by emissionViewModel.allEntries.collectAsState()
    val products by productViewModel.allScannedProducts.collectAsState()
    val grouped = entries.groupBy { it.localDate }.entries.sortedByDescending { it.key }
    val groupedProducts = products.groupBy { it.localDate }.entries.sortedByDescending { it.key }
    val fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH)
    val scopeColor = mapOf(Scope.SCOPE1 to Scope1Color, Scope.SCOPE2 to Scope2Color, Scope.SCOPE3 to Scope3Color)

    if (entries.isEmpty() && products.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📋", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text("Aucun historique", style = MaterialTheme.typography.titleMedium)
                Text("Ajoutez des émissions ou scannez des produits", style = MaterialTheme.typography.bodyMedium, color = TextDim)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = EcoGreen
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Émissions (${entries.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Produits (${products.size})") }
            )
        }

        when (selectedTab) {
            0 -> EmissionsHistoryTab(grouped, entries, scopeColor, fmt, emissionViewModel)
            1 -> ProductsHistoryTab(groupedProducts, products, fmt, productViewModel)
        }
    }
}

@Composable
fun EmissionsHistoryTab(
    grouped: List<Map.Entry<java.time.LocalDate, List<EmissionEntry>>>,
    entries: List<EmissionEntry>,
    scopeColor: Map<Scope, androidx.compose.ui.graphics.Color>,
    fmt: DateTimeFormatter,
    viewModel: EmissionViewModel
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Historique des émissions", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text("${entries.size} entrées · ${(entries.sumOf { it.kgCo2e } / 1000).let { "%.2f t".format(it) }} CO₂e total",
                style = MaterialTheme.typography.bodyMedium, color = TextDim)
        }

        grouped.forEach { (date, dayEntries) ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        date.format(fmt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.width(8.dp))
                    HorizontalDivider(Modifier.weight(1f), color = BorderDark)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "%.0f kg".format(dayEntries.sumOf { it.kgCo2e }),
                        style = MaterialTheme.typography.labelSmall,
                        color = EcoGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            items(dayEntries, key = { it.id }) { entry ->
                EntryRow(
                    entry = entry,
                    color = scopeColor[entry.scope] ?: EcoGreen,
                    onDelete = { viewModel.deleteEntry(entry.id) }
                )
            }
        }
    }
}

@Composable
fun ProductsHistoryTab(
    grouped: List<Map.Entry<java.time.LocalDate, List<com.ecotrace.app.data.models.ScannedProduct>>>,
    products: List<com.ecotrace.app.data.models.ScannedProduct>,
    fmt: DateTimeFormatter,
    viewModel: ProductViewModel
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Historique des produits", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text("${products.size} produits · ${(products.sumOf { it.totalKgCo2e }).let { "%.2f kg".format(it) }} CO₂e total",
                style = MaterialTheme.typography.bodyMedium, color = TextDim)
        }

        grouped.forEach { (date, dayProducts) ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        date.format(fmt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.width(8.dp))
                    HorizontalDivider(Modifier.weight(1f), color = BorderDark)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "%.1f kg".format(dayProducts.sumOf { it.totalKgCo2e }),
                        style = MaterialTheme.typography.labelSmall,
                        color = EcoAmber,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            items(dayProducts, key = { it.id }) { product ->
                ProductRow(
                    product = product,
                    onDelete = { viewModel.deleteScannedProduct(product.id) }
                )
            }
        }
    }
}

@Composable
fun EntryRow(entry: EmissionEntry, color: androidx.compose.ui.graphics.Color, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val cat = entry.category

    Card(
        Modifier.fillMaxWidth().clickable { showDelete = !showDelete },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(cat.icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cat.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "%.1f %s · %s".format(entry.valueInput, cat.unit, cat.scope.name.replace("SCOPE", "Scope ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDim,
                    fontSize = 12.sp
                )
                if (entry.note.isNotBlank()) {
                    Text(entry.note, style = MaterialTheme.typography.bodyMedium, color = TextDim, fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.1f kg".format(entry.kgCo2e),
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontFamily = FontFamily.Monospace
                )
                Text("CO₂e", style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.5f))
            }
            if (showDelete) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = EcoRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ProductRow(product: com.ecotrace.app.data.models.ScannedProduct, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth().clickable { showDelete = !showDelete },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(EcoAmber.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🛒", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (product.brand.isNotBlank()) {
                        Text(
                            product.brand,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDim,
                            fontSize = 12.sp
                        )
                        Text(" · ", color = TextDim, fontSize = 12.sp)
                    }
                    Text(
                        "${product.weight.toInt()}g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDim,
                        fontSize = 12.sp
                    )
                }
                if (product.category.isNotBlank()) {
                    Text(
                        product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.2f kg".format(product.totalKgCo2e),
                    style = MaterialTheme.typography.titleMedium,
                    color = EcoAmber,
                    fontFamily = FontFamily.Monospace
                )
                Text("CO₂e", style = MaterialTheme.typography.labelSmall, color = EcoAmber.copy(alpha = 0.5f))
            }
            if (showDelete) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = EcoRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
