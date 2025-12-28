package com.budmash.profile

expect class ProfileStorage() {
    fun getLikedStrains(): Set<String>
    fun setLikedStrains(strains: Set<String>)
    fun addLikedStrain(name: String)
    fun removeLikedStrain(name: String)
    fun getDislikedStrains(): Set<String>
    fun setDislikedStrains(strains: Set<String>)
    fun addDislikedStrain(name: String)
    fun removeDislikedStrain(name: String)
}
