package com.example.fedup_foodwasteapp

import androidx.lifecycle.LiveData
class IngredientRepository(private val ingredientDao: IngredientDao) {
    val allIngredients: LiveData<List<Ingredients>> = ingredientDao.getAllIngredients()

    suspend fun insert(ingredient: Ingredients) {
        ingredientDao.insert(ingredient)
    }
}
