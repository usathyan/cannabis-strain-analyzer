package com.strainanalyzer.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.strainanalyzer.app.analysis.LocalAnalysisEngine
import com.strainanalyzer.app.data.StrainDataService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
    object Idle : UiState()
}

/**
 * Local user profile - stored in SharedPreferences
 */
data class LocalUserProfile(
    val favoriteStrains: List<String> = emptyList()
)

class StrainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val analysisEngine = LocalAnalysisEngine.getInstance(context)
    private val strainDataService = StrainDataService.getInstance(context)
    private val prefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<LocalUserProfile> = _userProfile.asStateFlow()

    private val _availableStrains = MutableStateFlow<List<String>>(emptyList())
    val availableStrains: StateFlow<List<String>> = _availableStrains.asStateFlow()

    private val _selectedStrains = MutableStateFlow<Set<String>>(emptySet())
    val selectedStrains: StateFlow<Set<String>> = _selectedStrains.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadAvailableStrains()
        _selectedStrains.value = _userProfile.value.favoriteStrains.toSet()
    }

    private fun loadProfile(): LocalUserProfile {
        val favorites = prefs.getStringSet("favorites", emptySet())?.toList() ?: emptyList()
        return LocalUserProfile(favoriteStrains = favorites)
    }

    private fun saveProfile(profile: LocalUserProfile) {
        prefs.edit()
            .putStringSet("favorites", profile.favoriteStrains.toSet())
            .apply()
        _userProfile.value = profile
    }

    fun loadAvailableStrains() {
        _availableStrains.value = analysisEngine.getAvailableStrains()
    }

    fun toggleStrainSelection(strain: String) {
        val current = _selectedStrains.value.toMutableSet()
        if (current.contains(strain)) {
            current.remove(strain)
        } else {
            current.add(strain)
        }
        _selectedStrains.value = current
    }

    /**
     * Add a strain to favorites
     * If strain is not in database, fetch via LLM first
     */
    fun addStrainToProfile(strainName: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // Check if strain exists or needs to be fetched
            val result = strainDataService.getStrainData(strainName)

            when (result.source) {
                StrainDataService.StrainSource.NOT_FOUND -> {
                    _uiState.value = UiState.Error(result.error ?: "Could not find strain")
                    _message.value = result.error
                }
                else -> {
                    // Strain found or generated - add to favorites
                    val currentFavorites = _userProfile.value.favoriteStrains.toMutableList()
                    val normalizedName = strainName.lowercase().trim()

                    if (!currentFavorites.contains(normalizedName)) {
                        currentFavorites.add(normalizedName)
                        saveProfile(LocalUserProfile(favoriteStrains = currentFavorites))
                        _selectedStrains.value = currentFavorites.toSet()

                        val sourceMsg = when (result.source) {
                            StrainDataService.StrainSource.LOCAL_DATABASE -> "from database"
                            StrainDataService.StrainSource.LOCAL_CACHE -> "from cache"
                            StrainDataService.StrainSource.LLM_GENERATED -> "generated via API"
                            else -> ""
                        }
                        _message.value = "Added ${result.strain?.name ?: strainName} ($sourceMsg)"

                        // Refresh available strains in case new one was added
                        loadAvailableStrains()
                    } else {
                        _message.value = "${strainName} is already in your favorites"
                    }
                    _uiState.value = UiState.Success
                }
            }
        }
    }

    fun removeStrainFromProfile(strainName: String) {
        val currentFavorites = _userProfile.value.favoriteStrains.toMutableList()
        val normalizedName = strainName.lowercase().trim()

        if (currentFavorites.remove(normalizedName)) {
            saveProfile(LocalUserProfile(favoriteStrains = currentFavorites))
            _selectedStrains.value = currentFavorites.toSet()
            _message.value = "Removed $strainName from favorites"
        }
    }

    /**
     * Update profile with selected strains
     */
    fun updateProfile() {
        val selected = _selectedStrains.value.toList()
        saveProfile(LocalUserProfile(favoriteStrains = selected))
        _message.value = "Profile updated with ${selected.size} strains"
    }

    /**
     * Select a strain from the available list (toggle for favorites)
     */
    fun selectStrain(strain: String) {
        toggleStrainSelection(strain)
    }

    fun clearMessage() {
        _message.value = null
    }

    fun setMessage(msg: String) {
        _message.value = msg
    }
}
