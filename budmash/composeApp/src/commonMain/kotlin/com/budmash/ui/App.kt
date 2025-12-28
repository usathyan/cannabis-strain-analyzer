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
import com.budmash.analysis.AnalysisEngine
import com.budmash.profile.ProfileStorage
import com.budmash.ui.screens.DashboardScreen
import com.budmash.ui.screens.HomeScreen
import com.budmash.ui.screens.ScanScreen
import com.budmash.ui.screens.SettingsScreen
import com.budmash.ui.screens.StrainDetailScreen
import com.budmash.ui.theme.BudMashTheme
import kotlinx.coroutines.flow.collect

sealed class Screen {
    data object Home : Screen()
    data object Settings : Screen()
    data class Scanning(val imageBase64: String) : Screen()
    data class Dashboard(val menu: DispensaryMenu) : Screen()
    data class StrainDetail(val strain: StrainData) : Screen()
}

@Composable
fun App() {
    println("[BudMash] App composable initializing")
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var parseStatus by remember { mutableStateOf<ParseStatus>(ParseStatus.Fetching) }

    // Profile storage for liked/disliked strains
    val profileStorage = remember { ProfileStorage() }
    var likedStrains by remember { mutableStateOf(profileStorage.getLikedStrains()) }
    var dislikedStrains by remember { mutableStateOf(profileStorage.getDislikedStrains()) }

    // Analysis engine for similarity scoring
    val analysisEngine = remember { AnalysisEngine() }

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
                    onPhotoCapture = { imageBase64 ->
                        println("[BudMash] Photo captured, base64 length: ${imageBase64.length}")
                        parseStatus = ParseStatus.Fetching // Reset status
                        currentScreen = Screen.Scanning(imageBase64)
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
                println("[BudMash] Rendering ScanScreen for image")

                // Trigger parsing when entering scan screen
                LaunchedEffect(screen.imageBase64) {
                    println("[BudMash] LaunchedEffect: Starting parse for image")
                    parser.parseFromImage(screen.imageBase64).collect { status ->
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

                // Build ideal profile from liked strains in this menu
                val likedStrainsInMenu = screen.menu.strains.filter { it.name in likedStrains }
                val idealProfile = analysisEngine.buildIdealProfile(likedStrainsInMenu)
                val hasValidProfile = likedStrainsInMenu.isNotEmpty()

                println("[BudMash] Profile built from ${likedStrainsInMenu.size} liked strains")

                // Calculate similarity for each strain, sorted by score
                val results = screen.menu.strains.map { strain ->
                    if (hasValidProfile) {
                        analysisEngine.calculateMatch(idealProfile, strain)
                    } else {
                        SimilarityResult(
                            strain = strain,
                            overallScore = 0.0,
                            cosineScore = 0.0,
                            euclideanScore = 0.0,
                            pearsonScore = 0.0
                        )
                    }
                }.sortedByDescending { it.overallScore }

                DashboardScreen(
                    strains = results,
                    likedStrains = likedStrains,
                    dislikedStrains = dislikedStrains,
                    hasProfile = likedStrains.isNotEmpty(),
                    onStrainClick = { strain ->
                        currentScreen = Screen.StrainDetail(strain)
                    },
                    onLikeClick = { strain ->
                        profileStorage.addLikedStrain(strain.name)
                        likedStrains = profileStorage.getLikedStrains()
                        dislikedStrains = profileStorage.getDislikedStrains()
                        println("[BudMash] Liked: ${strain.name}, total liked: ${likedStrains.size}")
                    },
                    onDislikeClick = { strain ->
                        profileStorage.addDislikedStrain(strain.name)
                        likedStrains = profileStorage.getLikedStrains()
                        dislikedStrains = profileStorage.getDislikedStrains()
                        println("[BudMash] Disliked: ${strain.name}, total disliked: ${dislikedStrains.size}")
                    }
                )
            }

            is Screen.StrainDetail -> {
                println("[BudMash] Rendering StrainDetail for: ${screen.strain.name}")
                StrainDetailScreen(
                    strain = screen.strain,
                    isLiked = screen.strain.name in likedStrains,
                    isDisliked = screen.strain.name in dislikedStrains,
                    onBack = {
                        currentScreen = Screen.Dashboard(
                            DispensaryMenu(
                                url = "Photo capture",
                                fetchedAt = 0,
                                strains = (parseStatus as? ParseStatus.Complete)?.menu?.strains ?: emptyList()
                            )
                        )
                    },
                    onLike = {
                        profileStorage.addLikedStrain(screen.strain.name)
                        likedStrains = profileStorage.getLikedStrains()
                        dislikedStrains = profileStorage.getDislikedStrains()
                        println("[BudMash] Liked from detail: ${screen.strain.name}")
                    },
                    onDislike = {
                        profileStorage.addDislikedStrain(screen.strain.name)
                        likedStrains = profileStorage.getLikedStrains()
                        dislikedStrains = profileStorage.getDislikedStrains()
                        println("[BudMash] Disliked from detail: ${screen.strain.name}")
                    }
                )
            }
        }
    }
}
