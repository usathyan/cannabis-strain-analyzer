package com.budmash.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.budmash.database.StrainDatabase
import com.budmash.profile.ProfileStorage
import com.budmash.ui.screens.*
import com.budmash.ui.theme.BudMashTheme
import kotlinx.coroutines.flow.collect

// Bottom navigation tabs
enum class BottomTab {
    HOME, SEARCH, SETTINGS
}

// Full-screen destinations (hide bottom nav)
sealed class SubScreen {
    data class Scanning(val imageBase64: String) : SubScreen()
    data class Results(val menu: DispensaryMenu) : SubScreen()
    data class StrainDetail(
        val strain: StrainData,
        val similarity: SimilarityResult?,
        val returnTo: SubScreen? = null  // Where to go back to
    ) : SubScreen()
    data class ProfileStrainPicker(val searchQuery: String = "") : SubScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    println("[BudMash] App composable initializing")

    // Navigation state
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }
    var subScreen by remember { mutableStateOf<SubScreen?>(null) }
    var parseStatus by remember { mutableStateOf<ParseStatus>(ParseStatus.Fetching) }

    // Persist last scanned menu so it doesn't disappear on back
    var lastScannedMenu by remember { mutableStateOf<DispensaryMenu?>(null) }

    // Profile storage for liked/disliked strains
    val profileStorage = remember { ProfileStorage() }
    var likedStrains by remember { mutableStateOf(profileStorage.getLikedStrains()) }
    var dislikedStrains by remember { mutableStateOf(profileStorage.getDislikedStrains()) }

    // Analysis engine for similarity scoring
    val analysisEngine = remember { AnalysisEngine() }

    // Build ideal profile from liked strains in database
    val idealProfile by remember(likedStrains) {
        derivedStateOf {
            val likedDbStrains = likedStrains.mapNotNull { name ->
                StrainDatabase.getStrainByName(name)
            }
            analysisEngine.buildIdealProfile(likedDbStrains)
        }
    }
    val hasProfile = likedStrains.isNotEmpty()

    // LLM configuration storage
    val configStorage = remember { LlmConfigStorage() }

    // API key and model state - load from storage
    var apiKey by remember { mutableStateOf(configStorage.getApiKey() ?: "") }
    var model by remember { mutableStateOf(configStorage.getModel()) }
    var visionModel by remember { mutableStateOf(configStorage.getVisionModel()) }

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
    val parser = remember(config, visionModel) { DefaultMenuParser(llmProvider, config, visionModel) }

    // Helper to add/remove strains from profile
    fun toggleLike(strainName: String) {
        if (strainName in likedStrains) {
            profileStorage.removeLikedStrain(strainName)
        } else {
            profileStorage.addLikedStrain(strainName)
        }
        likedStrains = profileStorage.getLikedStrains()
        dislikedStrains = profileStorage.getDislikedStrains()
    }

    fun toggleDislike(strainName: String) {
        if (strainName in dislikedStrains) {
            profileStorage.removeDislikedStrain(strainName)
        } else {
            profileStorage.addDislikedStrain(strainName)
        }
        likedStrains = profileStorage.getLikedStrains()
        dislikedStrains = profileStorage.getDislikedStrains()
    }

    BudMashTheme {
        // If we're in a sub-screen, show it full screen
        val currentSubScreen = subScreen
        if (currentSubScreen != null) {
            when (currentSubScreen) {
                is SubScreen.Scanning -> {
                    // Trigger parsing when entering scan screen
                    LaunchedEffect(currentSubScreen.imageBase64) {
                        println("[BudMash] LaunchedEffect: Starting parse for image")
                        parser.parseFromImage(currentSubScreen.imageBase64).collect { status ->
                            println("[BudMash] Parse status: $status")
                            parseStatus = status
                        }
                    }

                    ScanScreen(
                        status = parseStatus,
                        onComplete = {
                            val status = parseStatus
                            if (status is ParseStatus.Complete) {
                                lastScannedMenu = status.menu  // Store for persistence
                                subScreen = SubScreen.Results(status.menu)
                            }
                        },
                        onError = {
                            subScreen = null
                        }
                    )
                }

                is SubScreen.Results -> {
                    val menu = currentSubScreen.menu

                    // Calculate similarity for each strain
                    val results = menu.strains.map { strain ->
                        if (hasProfile) {
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
                        hasProfile = hasProfile,
                        onStrainClick = { strain ->
                            val similarity = results.find { it.strain.name == strain.name }
                            // Pass current screen as returnTo so back works correctly
                            subScreen = SubScreen.StrainDetail(strain, similarity, currentSubScreen)
                        },
                        onLikeClick = { strain -> toggleLike(strain.name) },
                        onDislikeClick = { strain -> toggleDislike(strain.name) },
                        onBack = {
                            // Go back to Search tab but keep results accessible
                            subScreen = null
                            currentTab = BottomTab.SEARCH
                        }
                    )
                }

                is SubScreen.StrainDetail -> {
                    StrainDetailScreen(
                        strain = currentSubScreen.strain,
                        similarity = currentSubScreen.similarity,
                        idealProfile = idealProfile,
                        isLiked = currentSubScreen.strain.name in likedStrains,
                        isDisliked = currentSubScreen.strain.name in dislikedStrains,
                        onBack = {
                            // Return to where we came from, or null if nowhere
                            subScreen = currentSubScreen.returnTo
                        },
                        onLike = { toggleLike(currentSubScreen.strain.name) },
                        onDislike = { toggleDislike(currentSubScreen.strain.name) }
                    )
                }

                is SubScreen.ProfileStrainPicker -> {
                    ProfileStrainPickerScreen(
                        initialQuery = currentSubScreen.searchQuery,
                        likedStrains = likedStrains,
                        onStrainSelect = { strain ->
                            toggleLike(strain.name)
                        },
                        onStrainClick = { strain ->
                            subScreen = SubScreen.StrainDetail(strain, null, currentSubScreen)
                        },
                        onBack = { subScreen = null }
                    )
                }
            }
        } else {
            // Main app with bottom navigation
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Text("H") },
                            label = { Text("Home") },
                            selected = currentTab == BottomTab.HOME,
                            onClick = { currentTab = BottomTab.HOME }
                        )
                        NavigationBarItem(
                            icon = { Text("S") },
                            label = { Text("Search") },
                            selected = currentTab == BottomTab.SEARCH,
                            onClick = { currentTab = BottomTab.SEARCH }
                        )
                        NavigationBarItem(
                            icon = { Text("G") },
                            label = { Text("Settings") },
                            selected = currentTab == BottomTab.SETTINGS,
                            onClick = { currentTab = BottomTab.SETTINGS }
                        )
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (currentTab) {
                        BottomTab.HOME -> {
                            ProfileHomeScreen(
                                likedStrains = likedStrains,
                                analysisEngine = analysisEngine,
                                onAddStrain = {
                                    subScreen = SubScreen.ProfileStrainPicker()
                                },
                                onStrainClick = { strain ->
                                    subScreen = SubScreen.StrainDetail(strain, null)
                                },
                                onRemoveStrain = { strainName ->
                                    toggleLike(strainName)
                                }
                            )
                        }

                        BottomTab.SEARCH -> {
                            SearchScreen(
                                hasApiKey = apiKey.isNotBlank(),
                                hasProfile = hasProfile,
                                idealProfile = idealProfile,
                                analysisEngine = analysisEngine,
                                likedStrains = likedStrains,
                                dislikedStrains = dislikedStrains,
                                lastScannedMenu = lastScannedMenu,
                                onPhotoCapture = { imageBase64 ->
                                    parseStatus = ParseStatus.Fetching
                                    subScreen = SubScreen.Scanning(imageBase64)
                                },
                                onViewLastScan = {
                                    lastScannedMenu?.let { menu ->
                                        subScreen = SubScreen.Results(menu)
                                    }
                                },
                                onStrainClick = { strain, similarity ->
                                    subScreen = SubScreen.StrainDetail(strain, similarity)
                                },
                                onLikeClick = { strain -> toggleLike(strain.name) },
                                onDislikeClick = { strain -> toggleDislike(strain.name) }
                            )
                        }

                        BottomTab.SETTINGS -> {
                            SettingsScreen(
                                currentApiKey = apiKey,
                                currentModel = model,
                                currentVisionModel = visionModel,
                                onSave = { newApiKey, newModel, newVisionModel ->
                                    apiKey = newApiKey
                                    model = newModel
                                    visionModel = newVisionModel
                                    configStorage.setApiKey(newApiKey)
                                    configStorage.setModel(newModel)
                                    configStorage.setVisionModel(newVisionModel)
                                    currentTab = BottomTab.HOME
                                },
                                onBack = {
                                    currentTab = BottomTab.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
