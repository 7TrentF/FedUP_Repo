package com.example.fedup_foodwasteapp

import com.example.fedup_foodwasteapp.Ingredients
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch



// The IngredientViewModel class extends AndroidViewModel, providing the application context.
// It serves as a bridge between the UI and the repository, holding the app's data in a lifecycle-aware way.
class IngredientViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: IngredientRepository
    val allIngredients: LiveData<List<Ingredients>>

    private val apiService = RetrofitClient.apiService
    private val authManager = AuthManager()

    init {
        val ingredientDao = AppDatabase.getDatabase(application).ingredientDao()
        repository = IngredientRepository(ingredientDao)
        allIngredients = repository.allIngredients
        fetchIngredientsFromFirebase()
    }


    // API call to fetch ingredients
    fun fetchIngredients(authToken: String, onResult: (List<Ingredient>?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getIngredients("Bearer $authToken")
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    // Load ingredients using the token
    fun loadIngredients() {
        authManager.getIdToken { token ->
            token?.let {
                // Call the fetchIngredients method directly
                fetchIngredients(it) { ingredients ->
                    if (ingredients != null) {
                        // Update LiveData or UI
                    } else {
                        // Handle error
                    }
                }
            } ?: run {
                // Handle unauthenticated state
            }
        }
    }



    private fun fetchIngredientsFromFirebase() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance().getReference("ingredients").child(user.uid)
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ingredientsList = mutableListOf<Ingredients>()
                    for (ingredientSnapshot in snapshot.children) {
                        val ingredient = ingredientSnapshot.getValue(Ingredients::class.java)
                        ingredient?.let { ingredientsList.add(it) }
                    }
                    // Update Room Database using viewModelScope
                    syncIngredients(ingredientsList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("IngredientViewModel", "Failed to read ingredients.", error.toException())
                }
            })
        }
    }



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
    // This function is called when the insert is successful.
    fun onInsertSuccess() {

        // For example, you might want to trigger additional actions or update UI
    }

    private fun syncIngredients(ingredients: List<Ingredients>) = viewModelScope.launch(Dispatchers.IO) {
        repository.syncIngredients(ingredients)
    }
}


