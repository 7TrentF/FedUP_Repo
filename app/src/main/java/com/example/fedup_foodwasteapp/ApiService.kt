package com.example.fedup_foodwasteapp
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("api/Ingredients")
    suspend fun getIngredients(@Header("Authorization") authToken: String): Response<List<Ingredient>>

    @POST("api/Ingredients")
    suspend fun addIngredient(
        @Header("Authorization") authToken: String,
        @Body ingredient: Ingredient
    ): Response<Ingredient>

    @PUT("api/Ingredients/{id}")
    suspend fun updateIngredient(
        @Header("Authorization") authToken: String,
        @Path("id") id: Int,
        @Body ingredient: Ingredient
    ): Response<Void>

    @DELETE("api/Ingredients/{id}")
    suspend fun deleteIngredient(
        @Header("Authorization") authToken: String,
        @Path("id") id: Int
    ): Response<Void>
}