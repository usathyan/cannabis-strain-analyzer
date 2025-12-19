package com.strainanalyzer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strainanalyzer.app.data.*
import com.strainanalyzer.app.network.ApiClient
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

class StrainViewModel : ViewModel() {
    
    private val apiService = ApiClient.apiService
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    
    private val _availableStrains = MutableStateFlow<List<String>>(emptyList())
    val availableStrains: StateFlow<List<String>> = _availableStrains.asStateFlow()
    
    private val _selectedStrains = MutableStateFlow<Set<String>>(emptySet())
    val selectedStrains: StateFlow<Set<String>> = _selectedStrains.asStateFlow()
    
    private val _comparisonResult = MutableStateFlow<ComparisonResult?>(null)
    val comparisonResult: StateFlow<ComparisonResult?> = _comparisonResult.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    init {
        loadUserProfile()
        loadAvailableStrains()
    }
    
    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val profile = apiService.getUserProfile()
                _userProfile.value = profile
                _selectedStrains.value = profile.favoriteStrains.toSet()
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }
    
    fun loadAvailableStrains() {
        viewModelScope.launch {
            try {
                val response = apiService.getAvailableStrains()
                _availableStrains.value = response.strains
            } catch (e: Exception) {
                _message.value = "Failed to load strains: ${e.message}"
            }
        }
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
    
    fun addStrainToProfile(strainName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val response = apiService.addStrainToProfile(AddStrainRequest(strainName))
                if (response.success) {
                    loadUserProfile()
                    _message.value = response.message
                } else {
                    _message.value = response.message
                }
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to add strain")
            }
        }
    }
    
    fun removeStrainFromProfile(strainName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val response = apiService.removeStrainFromProfile(RemoveStrainRequest(strainName))
                if (response.success) {
                    loadUserProfile()
                    _message.value = response.message
                } else {
                    _message.value = response.message
                }
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to remove strain")
            }
        }
    }
    
    fun createIdealProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val strains = _selectedStrains.value.toList()
                val response = apiService.createIdealProfile(strains)
                loadUserProfile()
                _message.value = response.message
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to create profile")
            }
        }
    }
    
    fun compareStrain(strainName: String, useZScore: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val result = apiService.compareStrain(
                    CompareStrainRequest(strainName, useZScore)
                )
                _comparisonResult.value = result
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to compare strain")
            }
        }
    }
    
    fun clearMessage() {
        _message.value = null
    }
    
    fun clearComparisonResult() {
        _comparisonResult.value = null
    }
}
