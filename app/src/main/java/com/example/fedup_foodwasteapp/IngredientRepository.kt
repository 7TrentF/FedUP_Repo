package com.example.fedup_foodwasteapp

import androidx.lifecycle.LiveData
class IngredientRepository(private val ingredientDao: IngredientDao) {
    val allIngredients: LiveData<List<Ingredients>> = ingredientDao.getAllIngredients()

    suspend fun insert(ingredient: Ingredients) {
        ingredientDao.insert(ingredient)
    }

    suspend fun delete(ingredient: Ingredients) {
        ingredientDao.delete(ingredient)
    }
    // Method to get ingredients by category
    fun getIngredientsByCategory(category: String): LiveData<List<Ingredients>> {
        return ingredientDao.getIngredientByCategory(category)
    }
}
