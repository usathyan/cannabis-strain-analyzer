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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var parseStatus by remember { mutableStateOf<ParseStatus>(ParseStatus.Fetching) }
    var likedStrains by remember { mutableStateOf<Set<String>>(emptySet()) }

    BudMashTheme {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    onScanClick = { url ->
                        currentScreen = Screen.Scanning(url)
                        // TODO: Trigger actual parsing
                    }
                )
            }

            is Screen.Scanning -> {
                ScanScreen(
                    status = parseStatus,
                    onComplete = {
                        // Navigate to dashboard when complete
                        val status = parseStatus
                        if (status is ParseStatus.Complete) {
                            currentScreen = Screen.Dashboard(status.menu)
                        }
                    },
                    onError = {
                        currentScreen = Screen.Home
                    }
                )
            }

            is Screen.Dashboard -> {
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
                // TODO: Implement detail screen
            }
        }
    }
}
