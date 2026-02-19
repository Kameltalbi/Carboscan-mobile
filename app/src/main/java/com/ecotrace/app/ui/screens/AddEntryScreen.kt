package com.ecotrace.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecotrace.app.data.models.*
import com.ecotrace.app.ui.theme.*
import com.ecotrace.app.viewmodel.EmissionViewModel

@Composable
fun AddEntryScreen(viewModel: EmissionViewModel, onSuccess: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val scopeGroups = listOf(
        Scope.SCOPE1 to Category.entries.filter { it.scope == Scope.SCOPE1 },
        Scope.SCOPE2 to Category.entries.filter { it.scope == Scope.SCOPE2 },
        Scope.SCOPE3 to Category.entries.filter { it.scope == Scope.SCOPE3 },
    )

    val scopeColor = mapOf(
        Scope.SCOPE1 to Scope1Color,
        Scope.SCOPE2 to Scope2Color,
        Scope.SCOPE3 to Scope3Color
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Ajouter une émission", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text("Sélectionnez une catégorie", style = MaterialTheme.typography.bodyMedium, color = TextDim)
        Spacer(Modifier.height(20.dp))

        // Scope groups
        scopeGroups.forEach { (scope, cats) ->
            val color = scopeColor[scope]!!
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(scope.label, style = MaterialTheme.typography.labelSmall,
                    color = color, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(8.dp))

            cats.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCategory = cat; value = "" },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) color.copy(alpha = 0.15f) else SurfaceDark
                            ),
                            border = BorderStroke(1.dp, if (isSelected) color else BorderDark)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat.icon, fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    cat.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }

        // Input section
        AnimatedVisibility(
            visible = selectedCategory != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selectedCategory?.let { cat ->
                Column {
                    HorizontalDivider(color = BorderDark)
                    Spacer(Modifier.height(20.dp))

                    // Category info
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = EcoGreenDim)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.icon, fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(cat.label, style = MaterialTheme.typography.titleMedium, color = EcoGreen)
                                Text(
                                    "Facteur : ${cat.factorKgCo2PerUnit} kg CO₂e / ${cat.unit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EcoGreen.copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Value input
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(cat.hint) },
                        suffix = { Text(cat.unit, color = TextDim, fontFamily = FontFamily.Monospace) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EcoGreen,
                            unfocusedBorderColor = BorderDark,
                            focusedLabelColor = EcoGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Preview
                    val inputVal = value.toDoubleOrNull()
                    AnimatedVisibility(visible = inputVal != null && inputVal > 0) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            val kgCo2 = (inputVal ?: 0.0) * cat.factorKgCo2PerUnit
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EcoGreenDim,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Émission estimée", style = MaterialTheme.typography.bodyMedium, color = TextDim)
                                    Text(
                                        "%.1f kg CO₂e".format(kgCo2),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EcoGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EcoGreen,
                            unfocusedBorderColor = BorderDark,
                            focusedLabelColor = EcoGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
                    )

                    Spacer(Modifier.height(20.dp))

                    state.error?.let { error ->
                        Spacer(Modifier.height(12.dp))
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = EcoRed.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, EcoRed)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EcoRed
                                )
                            }
                        }
                    }

                    validationError?.let { error ->
                        Spacer(Modifier.height(12.dp))
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = EcoAmber.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, EcoAmber)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ℹ️", fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EcoAmber
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val v = value.toDoubleOrNull()
                            if (v == null) {
                                validationError = "Veuillez entrer une valeur valide"
                                return@Button
                            }
                            validationError = null
                            val success = viewModel.addEntry(cat, v, note)
                            if (success) {
                                selectedCategory = null
                                value = ""
                                note = ""
                                onSuccess()
                            }
                        },
                        enabled = value.toDoubleOrNull() != null && value.toDoubleOrNull()!! > 0,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoGreen, contentColor = BgDark)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ajouter l'émission", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                    }
                    
                    LaunchedEffect(state.error) {
                        if (state.error != null) {
                            kotlinx.coroutines.delay(5000)
                            viewModel.clearError()
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
