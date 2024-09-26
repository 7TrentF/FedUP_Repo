package com.example.fedup_foodwasteapp
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("api/Ingredients")
    suspend fun getIngredients(): Response<List<Ingredient>>

    @POST("api/Ingredients")
    suspend fun addIngredient(

        @Body ingredient: Ingredient
    ): Response<Ingredient>

    @PUT("api/Ingredients/{id}")
    suspend fun updateIngredient(

        @Path("id") id: Int,
        @Body ingredient: Ingredient
    ): Response<Void>

    @DELETE("api/Ingredients/{id}")
    suspend fun deleteIngredient(

        @Path("id") id: String
    ): Response<Void>
}

