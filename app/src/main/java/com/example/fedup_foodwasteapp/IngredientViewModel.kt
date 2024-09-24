package com.example.fedup_foodwasteapp

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// The IngredientViewModel class extends AndroidViewModel, providing the application context.
// It serves as a bridge between the UI and the repository, holding the app's data in a lifecycle-aware way.
class IngredientViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IngredientRepository
    val allIngredients: LiveData<List<Ingredient>>
    private val _filteredIngredients = MutableLiveData<List<Ingredient>>()
    val filteredIngredients: LiveData<List<Ingredient>> get() = _filteredIngredients
    private val _insertResult = MutableLiveData<Boolean>()
    val insertResult: LiveData<Boolean> get() = _insertResult
    private val apiService = RetrofitClient.apiService
    private val authManager = AuthManager.getInstance()

    // LiveData for synchronization status
    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> get() = _syncStatus


    init {
        val ingredientDao = AppDatabase.getDatabase(application).ingredientDao()
        repository = IngredientRepository(ingredientDao, apiService)
        allIngredients = repository.allIngredients
        _filteredIngredients.value = emptyList()
        fetchIngredientsFromFirebase()
        syncData()
    }

    private fun syncData() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            Log.d("IngredientViewModel", "No network available. Sync postponed.")
            _syncStatus.postValue("No network. Sync postponed.")
            return
        }

        // Push local data to Firebase
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localIngredients = repository.getAllIngredientsLocalSync()
                localIngredients.forEach { ingredient ->
                    repository.addIngredientToFirebaseSync(ingredient)
                }
                _syncStatus.postValue("Local data synced to Firebase.")
            } catch (e: Exception) {
                Log.e("IngredientViewModel", "Failed to push local ingredients to Firebase.", e)
                _syncStatus.postValue("Failed to sync local data.")
            }
        }

        // Listen for Firebase changes and update Room
        repository.listenToFirebaseChanges { ingredients ->
            syncIngredients(ingredients)
        }
    }

    private fun syncIngredients(ingredients: List<Ingredient>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.syncIngredients(viewModelScope, ingredients)
                _syncStatus.postValue("Firebase data synced to local database.")
            } catch (e: Exception) {
                Log.e("IngredientViewModel", "Failed to sync ingredients to Room.", e)
                _syncStatus.postValue("Failed to sync Firebase data.")
            }
        }
    }



    // Insert Ingredient
    fun insert(ingredient: Ingredient) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.insert(ingredient)
            _insertResult.postValue(true)
            _syncStatus.postValue("Ingredient inserted locally.")
            // Optionally push to Firebase immediately
            repository.addIngredientToFirebaseSync(ingredient)
        } catch (e: Exception) {
            _insertResult.postValue(false)
            _syncStatus.postValue("Failed to insert ingredient.")
        }
    }

    private fun fetchIngredientsFromFirebase() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance().getReference("ingredients").child(user.uid)
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ingredientsList = mutableListOf<Ingredient>()
                    for (ingredientSnapshot in snapshot.children) {
                        val ingredient = ingredientSnapshot.getValue(Ingredient::class.java)
                        ingredient?.let { ingredientsList.add(it) }
                    }
                    // Synchronize with Room Database
                    repository.syncIngredients(viewModelScope, ingredientsList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("IngredientViewModel", "Failed to read ingredients.", error.toException())
                }
            })
        }
    }




    fun loadIngredients() {
        AuthManager.getInstance().getIdToken { token, error ->
            if (token != null) {
                // Fetch from REST API
                repository.fetchIngredients(viewModelScope, token) { ingredients, fetchError ->
                    if (ingredients != null) {
                        // Update local Room database with fetched data
                        viewModelScope.launch(Dispatchers.IO) {
                            repository.syncIngredients(viewModelScope, ingredients)
                        }
                    } else {
                        // Handle error
                        Log.e("IngredientViewModel", "Fetch failed: $fetchError")
                    }
                }
            } else {
                // Handle unauthenticated state or fetch error
                Log.e("IngredientViewModel", "Token retrieval failed: $error")
            }
        }
    }

    private fun syncIngredientToApi(ingredient: Ingredient) {
        authManager.getIdToken { token, error ->
            if (token != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = repository.apiService.addIngredient("Bearer $token", ingredient)
                        if (response.isSuccessful) {
                            Log.d("IngredientViewModel", "Ingredient synced to API successfully.")
                        } else {
                            Log.e("IngredientViewModel", "API sync failed: ${response.code()} ${response.message()}")
                        }
                    } catch (e: Exception) {
                        Log.e("IngredientViewModel", "API sync exception: ${e.message}")
                    }
                }
            } else {
                Log.e("IngredientViewModel", "API sync token retrieval failed: $error")
            }
        }
    }


// Extension function to map Ingredients to Ingredient API model
fun Ingredient.toApiModel(): Ingredient {
    return Ingredient(
        id = this.id,
        productName = this.productName,
        quantity = this.quantity,
        expirationDate = this.expirationDate,
        category = this.category,
        userId = this.userId
    )
}


    fun filterIngredientsByCategory(category: String) {
        repository.getIngredientsByCategory(category).observeForever { ingredientsByCategory ->
            _filteredIngredients.postValue(ingredientsByCategory)
        }
    }

    fun onInsertSuccess() {
        _insertResult.postValue(true)
    }




}
