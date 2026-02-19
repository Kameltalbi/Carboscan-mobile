package com.ecotrace.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecotrace.app.ui.screens.*
import com.ecotrace.app.ui.theme.*
import com.ecotrace.app.viewmodel.EmissionViewModel
import com.ecotrace.app.viewmodel.ProductViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EcoTraceApp : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcoTraceTheme {
                EcoTraceNavigation()
            }
        }
    }
}

sealed class Screen(val route: String, val labelResId: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Dashboard)
    object Add : Screen("add", R.string.nav_add, Icons.Default.AddCircle)
    object Scan : Screen("scan", R.string.nav_scan, Icons.Default.QrCodeScanner)
    object History : Screen("history", R.string.nav_history, Icons.Default.History)
}

@Composable
fun EcoTraceNavigation() {
    val emissionViewModel: EmissionViewModel = hiltViewModel()
    val productViewModel: ProductViewModel = hiltViewModel()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val screens = listOf(Screen.Home, Screen.Add, Screen.Scan, Screen.History)

    Scaffold(
        containerColor = BgDark,
        topBar = { EcoTopBar() },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 0.dp
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                screen.icon,
                                null,
                                modifier = Modifier.size(
                                    when (screen) {
                                        Screen.Add -> 28.dp
                                        Screen.Scan -> 26.dp
                                        else -> 22.dp
                                    }
                                )
                            )
                        },
                        label = { Text(androidx.compose.ui.res.stringResource(screen.labelResId), fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EcoGreen,
                            selectedTextColor = EcoGreen,
                            unselectedIconColor = TextDim,
                            unselectedTextColor = TextDim,
                            indicatorColor = EcoGreenDim
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    viewModel = emissionViewModel, 
                    productViewModel = productViewModel,
                    onNavigateToAdd = { currentScreen = Screen.Add }
                )
                Screen.Add -> AddEntryScreen(emissionViewModel, onSuccess = { currentScreen = Screen.Home })
                Screen.Scan -> ScanScreen(productViewModel, onSuccess = { currentScreen = Screen.Home })
                Screen.History -> HistoryScreen(emissionViewModel, productViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌿", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "EcoTrace",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EcoGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
    )
}
