package com.example.fedup_foodwasteapp

import android.util.Log
import androidx.lifecycle.LiveData

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class IngredientRepository(
    private val ingredientDao: IngredientDao,
    val apiService: ApiService // Made apiService public for ViewModel access
) {
    val allIngredients: LiveData<List<Ingredient>> = ingredientDao.getAllIngredients()

    suspend fun insert(ingredient: Ingredient) {
        ingredientDao.insert(ingredient)
    }

    suspend fun update(ingredient: Ingredient) {
        ingredientDao.update(ingredient)
    }

    suspend fun delete(ingredient: Ingredient) {
        ingredientDao.delete(ingredient)
    }

    // Synchronize ingredients from Firebase to Room
    fun syncIngredients(coroutineScope: CoroutineScope, ingredients: List<Ingredient>) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                ingredientDao.deleteAll()
                ingredientDao.insertAll(ingredients)
            } catch (e: Exception) {
                Log.e("IngredientRepository", "Sync failed: ${e.message}")
            }
        }
    }

    fun getIngredientsByCategory(category: String): LiveData<List<Ingredient>> {
        return ingredientDao.getIngredientByCategory(category)
    }

    // Fetch ingredients from REST API
    fun fetchIngredients(coroutineScope: CoroutineScope, authToken: String, onResult: (List<Ingredient>?, String?) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getIngredients("Bearer $authToken")
                if (response.isSuccessful) {
                    val ingredients = response.body()
                    onResult(ingredients, null)
                } else {
                    onResult(null, "Error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                onResult(null, e.localizedMessage)
            }
        }
    }
}
