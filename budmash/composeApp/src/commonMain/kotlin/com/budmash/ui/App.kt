package com.budmash.ui

import androidx.compose.runtime.*
import com.budmash.data.DispensaryMenu
import com.budmash.data.SimilarityResult
import com.budmash.data.StrainData
import com.budmash.parser.ParseStatus
import com.budmash.ui.screens.DashboardScreen
import com.budmash.ui.screens.HomeScreen
import com.budmash.ui.screens.ScanScreen
import com.budmash.ui.theme.BudMashTheme

sealed class Screen {
    data object Home : Screen()
    data class Scanning(val url: String) : Screen()
    data class Dashboard(val menu: DispensaryMenu) : Screen()
    data class StrainDetail(val strain: StrainData) : Screen()
}

@Composable
fun App() {
    println("[BudMash] App composable initializing")
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var parseStatus by remember { mutableStateOf<ParseStatus>(ParseStatus.Fetching) }
    var likedStrains by remember { mutableStateOf<Set<String>>(emptySet()) }

    println("[BudMash] Current screen: $currentScreen")

    BudMashTheme {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                println("[BudMash] Rendering HomeScreen")
                HomeScreen(
                    onScanClick = { url ->
                        println("[BudMash] Scan clicked with URL: $url")
                        currentScreen = Screen.Scanning(url)
                        // TODO: Trigger actual parsing
                    }
                )
            }

            is Screen.Scanning -> {
                println("[BudMash] Rendering ScanScreen for URL: ${screen.url}")
                ScanScreen(
                    status = parseStatus,
                    onComplete = {
                        println("[BudMash] Scan complete, navigating to Dashboard")
                        val status = parseStatus
                        if (status is ParseStatus.Complete) {
                            currentScreen = Screen.Dashboard(status.menu)
                        }
                    },
                    onError = {
                        println("[BudMash] Scan error, returning to Home")
                        currentScreen = Screen.Home
                    }
                )
            }

            is Screen.Dashboard -> {
                println("[BudMash] Rendering Dashboard with ${screen.menu.strains.size} strains")
                val results = screen.menu.strains.map { strain ->
                    SimilarityResult(
                        strain = strain,
                        overallScore = 0.0, // TODO: Calculate with profile
                        cosineScore = 0.0,
                        euclideanScore = 0.0,
                        pearsonScore = 0.0
                    )
                }

                DashboardScreen(
                    strains = results,
                    hasProfile = likedStrains.isNotEmpty(),
                    onStrainClick = { strain ->
                        currentScreen = Screen.StrainDetail(strain)
                    },
                    onLikeClick = { strain ->
                        likedStrains = likedStrains + strain.name
                    },
                    onDislikeClick = { strain ->
                        likedStrains = likedStrains - strain.name
                    }
                )
            }

            is Screen.StrainDetail -> {
                println("[BudMash] Rendering StrainDetail for: ${screen.strain.name}")
                // TODO: Implement detail screen
            }
        }
    }
}
