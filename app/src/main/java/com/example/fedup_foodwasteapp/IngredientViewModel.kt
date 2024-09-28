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

    // Define the LiveData with the correct type
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
        //fetchIngredientsFromFirebase()
        // syncApiToFirebase()  // Sync from API to Firebase
        // syncData()   // Sync from Firebase to RoomDB
    }

    // Your new method to sync RoomDB with API
    fun syncRoomWithApi() {
        if (NetworkUtils.isNetworkAvailable(getApplication())) {
            viewModelScope.launch(Dispatchers.IO) {
                val unsyncedIngredients = repository.getUnsyncedIngredients() // Fetch unsynced ingredients

                if (unsyncedIngredients.isNotEmpty()) {
                    authManager.getIdToken { token, error ->
                        if (token != null) {
                            viewModelScope.launch {
                                unsyncedIngredients.forEach { ingredient ->
                                    repository.addIngredientToApi(ingredient) // Sync RoomDB to API
                                }
                                repository.markIngredientsAsSynced(unsyncedIngredients) // Mark ingredients as synced locally
                            }
                        }
                    }
                }

                fetchIngredientsFromApi() // Fetch updated data from API and sync RoomDB
            }
        }
    }


    fun fetchIngredientsFromApi() {
        authManager.getIdToken { token, error ->
            if (token != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val ingredients = repository.fetchIngredientsFromApi(token)
                    if (ingredients != null) {
                        // Pass viewModelScope to syncIngredientsWithRoom
                        repository.syncIngredientsWithRoom(viewModelScope, ingredients)
                    }
                }
            }
        }
    }

    fun fetchIngredientById(id: String) {
        authManager.getIdToken { token, error ->
            if (token != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = repository.apiService.getIngredients()
                    if (response.isSuccessful) {
                        val ingredient = response.body()
                        // Handle the ingredient object (e.g., update LiveData, etc.)
                    } else {
                        Log.e("IngredientViewModel", "Failed to fetch ingredient: ${response.code()}")
                    }
                }
            } else {
                Log.e("IngredientViewModel", "Failed to get token: $error")
            }
        }
    }

    // Call this method in your fragment to set up real-time updates
    fun observeIngredientChanges() {
        observeIngredientChangesInFirebase() // Set up real-time listener
    }

    // Fetch ingredients from the API and post to LiveData
    fun fetchIngredientsFromFirebase() {
        authManager.getIdToken { token, error ->
            if (token != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val ingredients = repository.fetchIngredientsFromApi(token)
                    if (ingredients != null) {
                        _filteredIngredients.postValue(ingredients ?: emptyList())
                    }
                }
            } else {
                Log.e("IngredientViewModel", "Failed to get token: $error")
            }
        }
    }

    fun observeIngredientChangesInFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val ingredientsRef = FirebaseDatabase.getInstance().getReference("ingredients/$userId")

            // Listen for any changes in the ingredients
            ingredientsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Re-fetch ingredients from API when data changes in Firebase
                    fetchIngredientsFromFirebase()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("IngredientViewModel", "Failed to listen for real-time updates: ${error.message}")
                }
            })
        }
    }

    // Fetch ingredients filtered by category from the API
    fun filterIngredientsByCategory(category: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getIngredientsByCategory(category)
                if (response.isSuccessful) {
                    _filteredIngredients.value = response.body() ?: emptyList()
                } else {
                    Log.e("IngredientViewModel", "Error filtering ingredients by category: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("IngredientViewModel", "Exception: ${e.message}")
            }
        }
    }



    // Method to fetch ingredients in real-time from Firebase
    fun fetchIngredientsFromFirebaseRealTime() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val ingredientsRef = FirebaseDatabase.getInstance().getReference("ingredients/$userId")

            // Set up a ValueEventListener to listen for changes in real-time
            ingredientsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ingredientList = mutableListOf<Ingredient>()

                    // Loop through the data snapshot to retrieve each ingredient
                    for (ingredientSnapshot in snapshot.children) {
                        val ingredient = ingredientSnapshot.getValue(Ingredient::class.java)
                        ingredient?.let { ingredientList.add(it) }
                    }

                    // Post the new list to LiveData, so the UI is updated
                    _filteredIngredients.postValue(ingredientList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("IngredientViewModel", "Failed to fetch real-time updates: ${error.message}")
                }
            })
        } else {
            Log.e("IngredientViewModel", "User is not authenticated")
        }
    }




    // Sync Data Between Firebase and RoomDB
    private fun syncData() {
        // Listen to Firebase and sync to Room
        repository.listenToFirebaseChanges(viewModelScope) { ingredients ->
            repository.syncIngredients(viewModelScope, ingredients)  // Sync Firebase -> Room
        }
    }


    fun filterIngredientsByCategoryLocal(category: String) {
        repository.getIngredientsByCategory(category).observeForever { ingredientsByCategory ->
            _filteredIngredients.postValue(ingredientsByCategory)
        }
    }

    fun onInsertSuccess() {
        _insertResult.postValue(true)
    }

    fun updateIngredient(firebaseId: String, ingredient: Ingredient) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.update(ingredient)
            authManager.getIdToken { token, error ->
                if (token != null) {
                    viewModelScope.launch {
                        repository.updateIngredientInApi(firebaseId, ingredient)
                    }

                }
            }
        } catch (e: Exception) {
            Log.e("ViewModel", "Failed to update ingredient.")
        }
    }




}