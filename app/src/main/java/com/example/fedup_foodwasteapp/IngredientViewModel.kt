package com.example.fedup_foodwasteapp

import com.example.fedup_foodwasteapp.Ingredients
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// The IngredientViewModel class extends AndroidViewModel, providing the application context.
// It serves as a bridge between the UI and the repository, holding the app's data in a lifecycle-aware way.
class IngredientViewModel(application: Application) : AndroidViewModel(application) {

    // An instance of IngredientRepository to manage data operations.
    private val repository: IngredientRepository

    // A LiveData object holding all the ingredients, which is observed by the UI for changes.
    val allIngredients: LiveData<List<Ingredients>>

    // A MutableLiveData object that holds the filtered list of ingredients.
    // This is private and mutable to ensure that it can only be modified within the ViewModel.
    private val _filteredIngredients = MutableLiveData<List<Ingredients>>()

    // A public getter for the filtered ingredients, returning an immutable LiveData.
    val filteredIngredients: LiveData<List<Ingredients>> get() = _filteredIngredients

    // A MutableLiveData object to hold the result of an insert operation (success or failure).
    private val _insertResult = MutableLiveData<Boolean>()

    // A public getter for the insert result, returning an immutable LiveData.
    val insertResult: LiveData<Boolean> get() = _insertResult

    // The init block initializes the repository and retrieves all ingredients from the database.
    init {
        // The DAO is obtained from the AppDatabase singleton.
        val ingredientDao = AppDatabase.getDatabase(application).ingredientDao()

        // The repository is instantiated with the DAO.
        repository = IngredientRepository(ingredientDao)

        // All ingredients are fetched and assigned to the LiveData object.
        allIngredients = repository.allIngredients

        // The filtered ingredients LiveData is initialized with an empty list.
        _filteredIngredients.value = emptyList()
    }

    // This function inserts a new ingredient into the database.
    // It runs in a background thread (IO Dispatcher) to avoid blocking the main thread.
    fun insert(ingredient: Ingredients) = viewModelScope.launch(Dispatchers.IO) {
        try {
            // The repository's insert function is called.
            repository.insert(ingredient)

            // If successful, the result LiveData is updated to true.
            _insertResult.postValue(true)
        } catch (e: Exception) {
            // If an error occurs, the result LiveData is updated to false.
            _insertResult.postValue(false)
        }
    }

    // This function filters ingredients by category.
    // The results are observed forever, and the filtered ingredients LiveData is updated with the result.
    fun filterIngredientsByCategory(category: String) {
        repository.getIngredientsByCategory(category).observeForever { ingredientsByCategory ->
            _filteredIngredients.postValue(ingredientsByCategory)
        }
    }
}


