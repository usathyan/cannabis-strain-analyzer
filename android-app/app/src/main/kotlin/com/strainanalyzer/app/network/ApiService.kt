package com.strainanalyzer.app.network

import com.strainanalyzer.app.data.*
import retrofit2.http.*

interface ApiService {
    
    @POST("api/set-user")
    suspend fun setUser(@Body userId: String): Map<String, String>
    
    @GET("api/available-strains")
    suspend fun getAvailableStrains(): AvailableStrainsResponse
    
    @POST("api/create-ideal-profile")
    suspend fun createIdealProfile(@Body strains: List<String>): CreateProfileResponse
    
    @POST("api/add-strain-to-profile")
    suspend fun addStrainToProfile(@Body request: AddStrainRequest): StrainOperationResponse
    
    @POST("api/remove-strain-from-profile")
    suspend fun removeStrainFromProfile(@Body request: RemoveStrainRequest): StrainOperationResponse
    
    @POST("api/compare-strain")
    suspend fun compareStrain(@Body request: CompareStrainRequest): ComparisonResult
    
    @GET("api/user-profile")
    suspend fun getUserProfile(): UserProfile
    
    @GET("api/ideal-profile")
    suspend fun getIdealProfile(): IdealProfile
    
    @POST("api/save-ranked-favorites")
    suspend fun saveRankedFavorites(): Map<String, Any>
}
