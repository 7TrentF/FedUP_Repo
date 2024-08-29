package com.example.fedup_foodwasteapp

import androidx.lifecycle.LiveData
// The IngredientRepository class acts as a mediator between the ViewModel and the DAO.
// It abstracts the data operations, making it easier to manage the data in the application.
class IngredientRepository(private val ingredientDao: IngredientDao) {

    // A LiveData object that holds all the ingredients from the database.
    // The DAO's getAllIngredients function is called to retrieve this data.
    val allIngredients: LiveData<List<Ingredients>> = ingredientDao.getAllIngredients()

    // This function inserts a new ingredient into the database.
    // It calls the DAO's insert function, which is a suspend function,
    // meaning it must be called within a coroutine.
    suspend fun insert(ingredient: Ingredients) {
        ingredientDao.insert(ingredient)
    }

    // This function deletes an ingredient from the database.
    // It calls the DAO's delete function, which is a suspend function.
    suspend fun delete(ingredient: Ingredients) {
        ingredientDao.delete(ingredient)
    }

    // Add the update method in the repository
    suspend fun update(ingredient: Ingredients) {
        ingredientDao.update(ingredient)
    }

    // This function retrieves ingredients by category from the database.
    // It calls the DAO's getIngredientByCategory function and returns the result as LiveData.
    fun getIngredientsByCategory(category: String): LiveData<List<Ingredients>> {
        return ingredientDao.getIngredientByCategory(category)
    }
}
