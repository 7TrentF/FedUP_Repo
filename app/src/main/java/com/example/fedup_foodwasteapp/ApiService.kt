package com.example.fedup_foodwasteapp
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("api/Ingredients")
    suspend fun getIngredients(): Response<List<Ingredient>>

    @GET("api/Ingredients/{ingredientId}")
    suspend fun getIngredientById(@Path("ingredientId") ingredientId: String): Response<Ingredient>

    @POST("api/Ingredients")
    suspend fun addIngredient(
        @Body ingredient: Ingredient
    ): Response<Ingredient>

    @PUT("api/Ingredients/{firebaseId}")
    suspend fun updateIngredient(
        @Path("firebaseId") firebaseId: String,
        @Body ingredient: Ingredient
    ): Response<Void>

    @DELETE("api/Ingredients/{firebaseId}")
    suspend fun deleteIngredient(
        @Path("firebaseId") firebaseId: String
    ): Response<Void>

    @GET("api/Ingredients/category/{category}")
    suspend fun getIngredientsByCategory(
        @Path("category") category: String
    ): Response<List<Ingredient>>
}

