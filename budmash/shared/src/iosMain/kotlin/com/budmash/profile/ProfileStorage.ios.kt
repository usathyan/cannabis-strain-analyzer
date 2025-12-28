package com.budmash.profile

import platform.Foundation.NSUserDefaults

actual class ProfileStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getLikedStrains(): Set<String> {
        val array = defaults.stringArrayForKey("liked_strains") ?: return emptySet()
        return array.map { it as String }.toSet()
    }

    actual fun setLikedStrains(strains: Set<String>) {
        defaults.setObject(strains.toList(), forKey = "liked_strains")
    }

    actual fun addLikedStrain(name: String) {
        val current = getLikedStrains().toMutableSet()
        current.add(name)
        setLikedStrains(current)
        removeDislikedStrain(name)
    }

    actual fun removeLikedStrain(name: String) {
        val current = getLikedStrains().toMutableSet()
        current.remove(name)
        setLikedStrains(current)
    }

    actual fun getDislikedStrains(): Set<String> {
        val array = defaults.stringArrayForKey("disliked_strains") ?: return emptySet()
        return array.map { it as String }.toSet()
    }

    actual fun setDislikedStrains(strains: Set<String>) {
        defaults.setObject(strains.toList(), forKey = "disliked_strains")
    }

    actual fun addDislikedStrain(name: String) {
        val current = getDislikedStrains().toMutableSet()
        current.add(name)
        setDislikedStrains(current)
        removeLikedStrain(name)
    }

    actual fun removeDislikedStrain(name: String) {
        val current = getDislikedStrains().toMutableSet()
        current.remove(name)
        setDislikedStrains(current)
    }
}
