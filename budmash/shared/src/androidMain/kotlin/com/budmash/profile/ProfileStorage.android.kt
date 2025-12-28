package com.budmash.profile

import android.content.Context
import android.content.SharedPreferences

actual class ProfileStorage {
    private val prefs: SharedPreferences by lazy {
        val context = ProfileStorageContext.applicationContext
            ?: throw IllegalStateException("ProfileStorage not initialized. Call ProfileStorageContext.init() first.")
        context.getSharedPreferences("budmash_profile", Context.MODE_PRIVATE)
    }

    actual fun getLikedStrains(): Set<String> {
        return prefs.getStringSet("liked_strains", emptySet()) ?: emptySet()
    }

    actual fun setLikedStrains(strains: Set<String>) {
        prefs.edit().putStringSet("liked_strains", strains).apply()
    }

    actual fun addLikedStrain(name: String) {
        val current = getLikedStrains().toMutableSet()
        current.add(name)
        setLikedStrains(current)
        // Remove from disliked if present
        removeDislikedStrain(name)
    }

    actual fun removeLikedStrain(name: String) {
        val current = getLikedStrains().toMutableSet()
        current.remove(name)
        setLikedStrains(current)
    }

    actual fun getDislikedStrains(): Set<String> {
        return prefs.getStringSet("disliked_strains", emptySet()) ?: emptySet()
    }

    actual fun setDislikedStrains(strains: Set<String>) {
        prefs.edit().putStringSet("disliked_strains", strains).apply()
    }

    actual fun addDislikedStrain(name: String) {
        val current = getDislikedStrains().toMutableSet()
        current.add(name)
        setDislikedStrains(current)
        // Remove from liked if present
        removeLikedStrain(name)
    }

    actual fun removeDislikedStrain(name: String) {
        val current = getDislikedStrains().toMutableSet()
        current.remove(name)
        setDislikedStrains(current)
    }
}

object ProfileStorageContext {
    var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
