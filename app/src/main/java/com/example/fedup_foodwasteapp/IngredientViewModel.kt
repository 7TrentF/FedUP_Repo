package com.example.fedup_foodwasteapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class IngredientViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: IngredientRepository
    val allIngredients: LiveData<List<Ingredients>>

    private val _filteredIngredients = MutableLiveData<List<Ingredients>>()
    val filteredIngredients: LiveData<List<Ingredients>> get() = _filteredIngredients

    // LiveData to hold the result of the insertion operation
    private val _insertResult = MutableLiveData<Boolean>()
    val insertResult: LiveData<Boolean> get() = _insertResult

    init {
        val ingredientDao = AppDatabase.getDatabase(application).ingredientDao()
        repository = IngredientRepository(ingredientDao)
        allIngredients = repository.allIngredients
        _filteredIngredients.value = emptyList()
    }

    fun insert(ingredient: Ingredients) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.insert(ingredient)
            _insertResult.postValue(true) // Insertion successful
        } catch (e: Exception) {
            _insertResult.postValue(false) // Insertion failed
        }
    }

    fun filterIngredientsByCategory(category: String) {
        repository.getIngredientsByCategory(category).observeForever { ingredientsByCategory ->
            _filteredIngredients.postValue(ingredientsByCategory)
        }
    }




}

