package com.ecotrace.app.ui.screens

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ecotrace.app.data.models.ProductInfo
import com.ecotrace.app.ui.theme.*
import com.ecotrace.app.viewmodel.ProductViewModel
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(viewModel: ProductViewModel, onSuccess: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    var productInfo by remember { mutableStateOf<ProductInfo?>(null) }
    var weight by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(scannedBarcode) {
        scannedBarcode?.let { barcode ->
            isScanning = false
            val info = viewModel.getProductInfo(barcode)
            productInfo = info
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        if (cameraPermissionState.status.isGranted) {
            if (isScanning && !showManualInput) {
                Box(Modifier.fillMaxSize()) {
                    CameraPreview(
                        onBarcodeDetected = { barcode ->
                            if (scannedBarcode == null) {
                                scannedBarcode = barcode
                            }
                        }
                    )
                    
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.9f))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "📱 Scanner un produit",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = EcoGreen
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Placez le code-barres dans le cadre",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextDim
                                )
                            }
                        }

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .border(3.dp, EcoGreen, RoundedCornerShape(16.dp))
                        )

                        Button(
                            onClick = { showManualInput = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceDark.copy(alpha = 0.9f),
                                contentColor = EcoGreen
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Saisie manuelle")
                        }
                    }
                }
            } else {
                ProductInputScreen(
                    productInfo = productInfo,
                    barcode = scannedBarcode,
                    weight = weight,
                    onWeightChange = { weight = it },
                    onSave = { w ->
                        productInfo?.let { info ->
                            viewModel.addScannedProduct(
                                barcode = info.barcode,
                                name = info.name,
                                brand = info.brand,
                                category = info.category,
                                kgCo2ePer100g = info.kgCo2ePer100g,
                                weight = w
                            )
                            scannedBarcode = null
                            productInfo = null
                            weight = ""
                            isScanning = true
                            onSuccess()
                        }
                    },
                    onCancel = {
                        scannedBarcode = null
                        productInfo = null
                        weight = ""
                        isScanning = true
                        showManualInput = false
                    },
                    onManualEntry = { name, brand, cat, co2, w ->
                        viewModel.addScannedProduct(
                            barcode = scannedBarcode ?: "MANUAL_${System.currentTimeMillis()}",
                            name = name,
                            brand = brand,
                            category = cat,
                            kgCo2ePer100g = co2,
                            weight = w
                        )
                        scannedBarcode = null
                        weight = ""
                        isScanning = true
                        showManualInput = false
                        onSuccess()
                    },
                    isManualMode = showManualInput
                )
            }
        } else {
            PermissionRequestScreen(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }
    }
}

@Composable
fun CameraPreview(onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(executor) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy, onBarcodeDetected)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.camera.core.ExperimentalGetImage
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        if (barcode.valueType == Barcode.TYPE_PRODUCT) {
                            onBarcodeDetected(value)
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Composable
fun ProductInputScreen(
    productInfo: ProductInfo?,
    barcode: String?,
    weight: String,
    onWeightChange: (String) -> Unit,
    onSave: (Double) -> Unit,
    onCancel: () -> Unit,
    onManualEntry: (String, String, String, Double, Double) -> Unit,
    isManualMode: Boolean
) {
    var manualName by remember { mutableStateOf("") }
    var manualBrand by remember { mutableStateOf("") }
    var manualCategory by remember { mutableStateOf("") }
    var manualCo2 by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        if (isManualMode) {
            Text("Saisie manuelle", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text("Entrez les informations du produit", style = MaterialTheme.typography.bodyMedium, color = TextDim)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = manualName,
                onValueChange = { manualName = it },
                label = { Text("Nom du produit") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EcoGreen,
                    unfocusedBorderColor = BorderDark,
                    focusedLabelColor = EcoGreen
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = manualBrand,
                onValueChange = { manualBrand = it },
                label = { Text("Marque") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EcoGreen,
                    unfocusedBorderColor = BorderDark,
                    focusedLabelColor = EcoGreen
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = manualCategory,
                onValueChange = { manualCategory = it },
                label = { Text("Catégorie") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EcoGreen,
                    unfocusedBorderColor = BorderDark,
                    focusedLabelColor = EcoGreen
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = manualCo2,
                onValueChange = { manualCo2 = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("kg CO₂e / 100g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EcoGreen,
                    unfocusedBorderColor = BorderDark,
                    focusedLabelColor = EcoGreen
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Poids (grammes)") },
                suffix = { Text("g", color = TextDim, fontFamily = FontFamily.Monospace) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EcoGreen,
                    unfocusedBorderColor = BorderDark,
                    focusedLabelColor = EcoGreen
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val w = weight.toDoubleOrNull() ?: return@Button
                    val co2 = manualCo2.toDoubleOrNull() ?: return@Button
                    onManualEntry(manualName, manualBrand, manualCategory, co2, w)
                },
                enabled = manualName.isNotBlank() && weight.toDoubleOrNull() != null && 
                         manualCo2.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreen, contentColor = BgDark)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Ajouter le produit", style = MaterialTheme.typography.titleMedium)
            }
        } else if (productInfo != null) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, EcoGreen)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("✅ Produit trouvé", style = MaterialTheme.typography.labelSmall, 
                        color = EcoGreen, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(12.dp))
                    Text(productInfo.name, style = MaterialTheme.typography.headlineMedium)
                    if (productInfo.brand.isNotBlank()) {
                        Text(productInfo.brand, style = MaterialTheme.typography.bodyLarge, color = TextDim)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Code-barres: ${productInfo.barcode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EcoGreenDim
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Empreinte carbone", style = MaterialTheme.typography.bodyMedium, color = TextDim)
                            Text(
                                "${productInfo.kgCo2ePer100g} kg CO₂e/100g",
                                style = MaterialTheme.typography.titleMedium,
                                color = EcoGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Poids du produit acheté") },
                suffix = { Text("g", color = TextDim, fontFamily = FontFamily.Monospace) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EcoGreen,
                    unfocusedBorderColor = BorderDark,
                    focusedLabelColor = EcoGreen
                ),
                shape = RoundedCornerShape(12.dp)
            )

            val weightVal = weight.toDoubleOrNull()
            AnimatedVisibility(visible = weightVal != null && weightVal > 0) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    val totalCo2 = (weightVal ?: 0.0) / 100.0 * productInfo.kgCo2ePer100g
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
                            Text("Émission totale", style = MaterialTheme.typography.bodyMedium, color = TextDim)
                            Text(
                                "%.2f kg CO₂e".format(totalCo2),
                                style = MaterialTheme.typography.titleMedium,
                                color = EcoGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val w = weight.toDoubleOrNull() ?: return@Button
                    onSave(w)
                },
                enabled = weight.toDoubleOrNull() != null && weight.toDoubleOrNull()!! > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreen, contentColor = BgDark)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Ajouter à mon empreinte", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface2Dark),
                border = BorderStroke(1.dp, EcoAmber)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Produit non reconnu", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Code-barres: ${barcode ?: "Inconnu"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDim,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Ce produit n'est pas dans notre base de données. Vous pouvez l'ajouter manuellement.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDim,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim),
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Icon(Icons.Default.Close, null)
            Spacer(Modifier.width(8.dp))
            Text("Annuler")
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Text("📷", fontSize = 64.sp)
            Spacer(Modifier.height(24.dp))
            Text(
                "Permission caméra requise",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Pour scanner les codes-barres des produits, nous avons besoin d'accéder à votre caméra.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreen, contentColor = BgDark)
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("Autoriser la caméra")
            }
        }
    }
}
