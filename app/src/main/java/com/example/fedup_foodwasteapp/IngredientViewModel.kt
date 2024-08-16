package com.example.fedup_foodwasteapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class IngredientViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: IngredientRepository
    val allIngredients: LiveData<List<Ingredients>>

    init {
        val ingredientDao = AppDatabase.getDatabase(application).ingredientDao()
        repository = IngredientRepository(ingredientDao)
        allIngredients = repository.allIngredients
    }

    fun insert(ingredient: Ingredients) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(ingredient)
    }
}
