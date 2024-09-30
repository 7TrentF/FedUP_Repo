package com.example.fedup_foodwasteapp

data class RecipeDetails(
    val id: Int,
    val title: String,
    val image: String,
    val summary: String,
    val instructions: String?,  // Make instructions nullable
    val extendedIngredients: List<RecipeIngredient> = emptyList()
)

data class RecipeIngredient(
    val id: Int,
    val aisle: String,
    val image: String,
    val name: String,
    val amount: Double,
    val unit: String
)