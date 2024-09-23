package com.example.fedup_foodwasteapp

import com.example.fedup_foodwasteapp.Ingredients
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope


// The IngredientRepository class acts as a mediator between the ViewModel and the DAO.
// It abstracts the data operations, making it easier to manage the data in the application.
class IngredientRepository(private val ingredientDao: IngredientDao) {

    val allIngredients: LiveData<List<Ingredients>> = ingredientDao.getAllIngredients()

    suspend fun insert(ingredient: Ingredients) {
        ingredientDao.insert(ingredient)
    }

    suspend fun update(ingredient: Ingredients) {
        ingredientDao.update(ingredient)
    }

    suspend fun delete(ingredient: Ingredients) {
        ingredientDao.delete(ingredient)
    }

    suspend fun syncIngredients(ingredients: List<Ingredients>) {
        ingredientDao.deleteAll()
        ingredientDao.insertAll(ingredients)
    }

    // This function retrieves ingredients by category from the database.
    // It calls the DAO's getIngredientByCategory function and returns the result as LiveData.
    fun getIngredientsByCategory(category: String): LiveData<List<Ingredients>> {
        return ingredientDao.getIngredientByCategory(category)
    }
}
