package com.example.fedup_foodwasteapp


import RecipeApiService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RecipeViewModel(private val recipeApiService: RecipeApiService) : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> get() = _recipes

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun fetchRecipes(query: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = recipeApiService.getRecipes(query)
                _recipes.value = response.results
            } catch (e: Exception) {
                // Handle error here
            } finally {
                _loading.value = false
            }
        }
    }
}