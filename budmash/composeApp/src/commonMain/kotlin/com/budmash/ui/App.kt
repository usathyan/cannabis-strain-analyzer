package com.budmash.ui

import androidx.compose.runtime.*
import com.budmash.data.DispensaryMenu
import com.budmash.data.SimilarityResult
import com.budmash.data.StrainData
import com.budmash.llm.KtorLlmProvider
import com.budmash.llm.LlmConfig
import com.budmash.llm.LlmConfigStorage
import com.budmash.llm.LlmProviderType
import com.budmash.parser.DefaultMenuParser
import com.budmash.parser.ParseStatus
import com.budmash.ui.screens.DashboardScreen
import com.budmash.ui.screens.HomeScreen
import com.budmash.ui.screens.ScanScreen
import com.budmash.ui.screens.SettingsScreen
import com.budmash.ui.theme.BudMashTheme
import kotlinx.coroutines.flow.collect

sealed class Screen {
    data object Home : Screen()
    data object Settings : Screen()
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

    // LLM configuration storage
    val configStorage = remember { LlmConfigStorage() }

    // API key and model state - load from storage
    var apiKey by remember { mutableStateOf(configStorage.getApiKey() ?: "") }
    var model by remember { mutableStateOf(configStorage.getModel()) }

    // LLM-based menu parser - recreate when apiKey or model changes
    val llmProvider = remember { KtorLlmProvider() }
    val config by remember(apiKey, model) {
        derivedStateOf {
            LlmConfig(
                provider = LlmProviderType.OPENROUTER,
                apiKey = apiKey,
                model = model
            )
        }
    }
    val parser = remember(config) { DefaultMenuParser(llmProvider, config) }

    println("[BudMash] Current screen: $currentScreen")

    BudMashTheme {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                println("[BudMash] Rendering HomeScreen")
                HomeScreen(
                    hasApiKey = apiKey.isNotBlank(),
                    onScanClick = { url ->
                        println("[BudMash] Scan clicked with URL: $url")
                        parseStatus = ParseStatus.Fetching // Reset status
                        currentScreen = Screen.Scanning(url)
                    },
                    onSettingsClick = {
                        println("[BudMash] Settings clicked")
                        currentScreen = Screen.Settings
                    }
                )
            }

            is Screen.Settings -> {
                println("[BudMash] Rendering SettingsScreen")
                SettingsScreen(
                    currentApiKey = apiKey,
                    currentModel = model,
                    onSave = { newApiKey, newModel ->
                        println("[BudMash] Saving settings - Model: $newModel")
                        // Update state
                        apiKey = newApiKey
                        model = newModel
                        // Persist to storage
                        configStorage.setApiKey(newApiKey)
                        configStorage.setModel(newModel)
                        // Navigate back
                        currentScreen = Screen.Home
                    },
                    onBack = {
                        println("[BudMash] Settings cancelled")
                        currentScreen = Screen.Home
                    }
                )
            }

            is Screen.Scanning -> {
                println("[BudMash] Rendering ScanScreen for URL: ${screen.url}")

                // Trigger parsing when entering scan screen
                LaunchedEffect(screen.url) {
                    println("[BudMash] LaunchedEffect: Starting parse for ${screen.url}")
                    parser.parseMenu(screen.url).collect { status ->
                        println("[BudMash] Parse status: $status")
                        parseStatus = status
                    }
                }

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
