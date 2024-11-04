package com.FedUpGroup.fedup_foodwasteapp
import retrofit2.Response
import retrofit2.http.*
interface ApiService {
    // GET request to retrieve a list of all ingredients.
    @GET("api/Ingredients")
    suspend fun getIngredients(): Response<List<Ingredient>> // Returns a Response containing a list of Ingredient objects

    // GET request to retrieve a specific ingredient by its ID.
    // In your ApiService interface
    @GET("api/Ingredients/{ingredientId}")
    suspend fun getIngredientById(
        @Path("ingredientId") ingredientId: String,
    ): Response<Ingredient>

    // POST request to add a new ingredient.
    @POST("api/Ingredients")
    suspend fun addIngredient(
        @Body ingredient: Ingredient // The Ingredient object to be added
    ): Response<Ingredient> // Returns a Response containing the added Ingredient object

    // PUT request to update an existing ingredient by its Firebase ID.
    @PUT("api/Ingredients/{firebaseId}")
    suspend fun updateIngredient(
        @Path("firebaseId") firebaseId: String, // The Firebase ID of the ingredient to update
        @Body ingredient: Ingredient // The updated Ingredient object
    ): Response<Void> // Returns a Response indicating the result of the update operation

    // DELETE request to remove an ingredient by its Firebase ID.
    @DELETE("api/Ingredients/{firebaseId}")
    suspend fun deleteIngredient(
        @Path("firebaseId") firebaseId: String // The Firebase ID of the ingredient to delete
    ): Response<Void> // Returns a Response indicating the result of the delete operation

    // GET request to retrieve ingredients by their category.
    @GET("api/Ingredients/category/{category}")
    suspend fun getIngredientsByCategory(
        @Path("category") category: String // The category of ingredients to retrieve
    ): Response<List<Ingredient>> // Returns a Response containing a list of Ingredient objects in the specified category

    @FormUrlEncoded
    @POST("expiration-data") // Replace with your actual endpoint
    suspend fun sendExpirationData(
        @Header("Authorization") token: String,
        @Field("fcmToken") fcmToken: String,
        @Field("notificationData") notificationData: Map<String, String>
    ): Response<Unit>




}
