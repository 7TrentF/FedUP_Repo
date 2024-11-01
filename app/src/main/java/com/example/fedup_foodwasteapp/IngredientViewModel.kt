package com.example.fedup_foodwasteapp

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

// The IngredientViewModel class extends AndroidViewModel, providing the application context.
// It serves as a bridge between the UI and the repository, holding the app's data in a lifecycle-aware way.
class IngredientViewModel(application: Application) : AndroidViewModel(application) {

    private val _dataState = MutableLiveData<DataResult<List<Ingredient>>>()
    val dataState: LiveData<DataResult<List<Ingredient>>> = _dataState



    private val ingredientDao: IngredientDao
    private val networkMonitor = NetworkMonitor(application)
    private val coroutineScope = viewModelScope


    private var syncJob: Job? = null

    private val repository: IngredientRepository
    val allIngredients: LiveData<List<Ingredient>>


    // Define the LiveData with the correct type
    //private val _filteredIngredients = MutableLiveData<List<Ingredient>>()
    private val _filteredIngredients = MediatorLiveData<List<Ingredient>?>()
    val filteredIngredients: MediatorLiveData<List<Ingredient>?> get() = _filteredIngredients

    private val _insertResult = MutableLiveData<Boolean>()
    val insertResult: LiveData<Boolean> get() = _insertResult
    private val apiService = RetrofitClient.apiService
    private val authManager = AuthManager.getInstance()

    // LiveData for synchronization status
    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> get() = _syncStatus

    val data = MutableLiveData<Ingredient?>()


    fun updateFilteredIngredients(newList: List<Ingredient>) {
        _filteredIngredients.value = newList
    }

    init {
        //  val ingredientDao = AppDatabase.getDatabase(application).ingredientDao()
        val database = (application as FedUpFoodWaste).database
        ingredientDao = database.ingredientDao()

        repository = IngredientRepository(ingredientDao, apiService)
        allIngredients = repository.allIngredients
        _filteredIngredients.value = emptyList()


        setupNetworkMonitoring()

        //fetchIngredientsFromFirebase()
        // syncApiToFirebase()  // Sync from API to Firebase
        // syncData()   // Sync from Firebase to RoomDB
    }


    ////////////////////////////////////////////////////////////NEW///////////////////////////////////////////////////////////////////
    private fun setupNetworkMonitoring() {
        networkMonitor.startMonitoring()

        coroutineScope.launch {
            networkMonitor.isNetworkAvailable.collect { isAvailable ->
                if (isAvailable) {
                    syncDataWithFirebase()
                } else {
                    loadFromRoom()
                }
            }
        }
    }

    private suspend fun syncDataWithFirebase() {
        syncJob?.cancel() // Cancel any existing sync
        syncJob = coroutineScope.launch {
            try {
                _dataState.value = DataResult.Loading

                // Get Firebase token and fetch ingredients
                authManager.getIdToken { token, error ->
                    if (token != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                // Fetch ingredients from Firebase
                                val ingredients = repository.fetchIngredientsFromApi(token)

                                if (ingredients != null) {
                                    // Save to Room and mark as synced
                                    withContext(Dispatchers.IO) {
                                        ingredients.forEach { ingredient ->
                                            // Create a copy with sync status
                                            val syncedIngredient = ingredient.copy(
                                                id = 0, // Let Room auto-generate ID
                                                isSynced = true
                                            )
                                            ingredientDao.insert(syncedIngredient)
                                        }
                                    }

                                    // Update UI with room data
                                    loadFromRoom()
                                } else {
                                    _dataState.postValue(DataResult.Error(Exception("No data received from Firebase")))
                                    loadFromRoom() // Fallback to local data
                                }
                            } catch (e: Exception) {
                                _dataState.postValue(DataResult.Error(e))
                                loadFromRoom() // Fallback to local data
                            }
                        }
                    } else {
                        _dataState.postValue(DataResult.Error(Exception("Unknown error occurred")))
                        loadFromRoom() // Fallback to local data
                    }
                }
            } catch (e: Exception) {
                _dataState.postValue(DataResult.Error(e))
                loadFromRoom() // Fallback to local data
            }
        }
    }

    fun loadFromRoom() {

        coroutineScope.launch {
            try {
                _dataState.value = DataResult.Loading
                val roomIngredients = ingredientDao.getAllIngredients().value
                _dataState.value = DataResult.Success(roomIngredients ?: emptyList())
            } catch (e: Exception) {
                _dataState.value = DataResult.Error(e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.stopMonitoring()
        syncJob?.cancel()
    }

    // Your new method to sync RoomDB with API
    fun syncRoomWithApi() {
        if (NetworkUtils.isNetworkAvailable(getApplication())) {
            viewModelScope.launch(Dispatchers.IO) {
                val unsyncedIngredients =
                    repository.getUnsyncedIngredients() // Fetch unsynced ingredients

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


    // Call this method in your fragment to set up real-time updates
    fun observeIngredientChanges() {
        observeIngredientChangesInFirebase() // Set up real-time listener
    }

    fun fetchIngredientsFromFirebase() {
        Log.d("IngredientViewModel", "Attempting to fetch ingredients from Firebase.")
        authManager.getIdToken { token, error ->
            if (token != null) {
                Log.d("IngredientViewModel", "Received Firebase token successfully.")
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val ingredients = repository.fetchIngredientsFromApi(token)
                        if (ingredients != null) {
                            Log.d("IngredientViewModel", "Fetched ${ingredients.size} ingredients from API.")
                            // Update LiveData with fetched ingredients
                            _filteredIngredients.postValue(ingredients)
                        } else {
                            Log.w("IngredientViewModel", "No ingredients fetched from the API (null response).")
                            _filteredIngredients.postValue(emptyList())
                        }
                    } catch (e: Exception) {
                        Log.e("IngredientViewModel", "Error fetching ingredients from Firebase API", e)
                    }
                }
            } else {
                Log.e("IngredientViewModel", "Failed to retrieve Firebase token: $error")
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
                    Log.e(
                        "IngredientViewModel",
                        "Failed to listen for real-time updates: ${error.message}"
                    )
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
                    Log.e(
                        "IngredientViewModel",
                        "Error filtering ingredients by category: ${response.message()}"
                    )
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
                    Log.e(
                        "IngredientViewModel",
                        "Failed to fetch real-time updates: ${error.message}"
                    )
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




    /////////////////////////////////////////////      ROOM DB     /////////////////////////////////////////////////////////////////////////////////////////////
// Function to insert an ingredient using viewModelScope
    fun insertIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repository.insert(ingredient)
        }
    }

    fun deleteIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repository.delete(ingredient)
        }
    }

    // Retrieve ingredient by firebase_id
    suspend fun getIngredientByFirebaseId(firebaseId: String): Ingredient? {
        return repository.getIngredientByFirebaseId(firebaseId)
    }


    // Delete ingredient by firebase_id
    fun deleteIngredientByFirebaseId(firebaseId: String) {
        viewModelScope.launch {
            val ingredient = repository.getIngredientByFirebaseId(firebaseId)
            if (ingredient != null) {
                repository.deleteIngredientByFirebaseId(firebaseId)
            } else {
                Log.e("DeleteIngredientError", "Ingredient with Firebase ID $firebaseId not found in local database")
            }
        }
    }



    // Updates only the firebaseId in RoomDB without incrementing version
    fun updateFirebaseIdOnly(id: Long, firebaseId: String) {
        viewModelScope.launch {
            repository.updateFirebaseIdOnly(id, firebaseId)
        }
    }

    // Function to update an ingredient
    fun updateIngredientDetails(ingredient: Ingredient) {
        viewModelScope.launch {
            try {
                    val success = repository.updateIngredientDetails(ingredient)
                    if (success) {
                    Log.d("ViewModel", "Ingredient updated successfully")
                } else {
                    Log.e("ViewModel", "Failed to update ingredient - not found in database")
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error updating ingredient", e)
            }
        }
    }

    // Method to fetch ingredients from RoomDB for offline use
    fun getIngredientsFromRoomDB(): LiveData<List<Ingredient>> {
        return repository.allIngredientsFromRoomDB()  // Implement this in repository
    }

    fun loadIngredients() {
        val context = getApplication<Application>()
        if (NetworkUtils.isNetworkAvailable(context)) {
            Log.d("IngredientViewModel", "Network is available. Fetching ingredients from Firebase.")
            fetchIngredientsFromFirebase()
            observeIngredientChanges()
        } else {
            Log.d("IngredientViewModel", "Network is unavailable. Loading ingredients from Room database.")
            loadFromRoom()
        }
    }


    fun loadFromRoomz() {
        Log.d("IngredientViewModel", "Setting up observer to load ingredients from Room database.")
        _filteredIngredients.addSource(ingredientDao.getAllIngredients()) { ingredients ->
            _filteredIngredients.value = ingredients
            if (ingredients.isNullOrEmpty()) {
                Log.w("IngredientViewModel", "Room database returned an empty or null ingredient list.")
            } else {
                Log.d("IngredientViewModel", "Loaded ${ingredients.size} ingredients from Room database.")
            }
        }
    }


    suspend fun insertOffline(ingredient: Ingredient): Long {
        return withContext(Dispatchers.IO) {
            repository.insertIngredient(ingredient)
        }
    }


    fun syncUnsyncedIngredients() {
        viewModelScope.launch {
            // Fetch all unsynced ingredients (new, updated, or marked for deletion)
            val unsyncedIngredients = repository.getUnsyncedIngredients()

            for (ingredient in unsyncedIngredients) {
                try {
                    if (ingredient.isDeleted) {
                        // Handle deletions for ingredients marked as deleted
                        val deleteResponse = repository.deleteIngredientFromFirebase(ingredient)
                        if (deleteResponse.isSuccessful) {
                            repository.removeIngredient(ingredient) // Remove from RoomDB after deletion
                        }
                    } else {
                        if (ingredient.firebaseId.isEmpty()) {
                            // Handle new ingredient addition
                            val addResponse = repository.addIngredientToFirebase(ingredient)
                            if (addResponse.isSuccessful) {
                                val createdIngredient = addResponse.body()
                                if (createdIngredient != null) {
                                    ingredient.firebaseId = createdIngredient.firebaseId
                                    ingredient.isSynced = true
                                    ingredient.version += 1 // Update version as synced
                                    ingredient.lastModified = System.currentTimeMillis()
                                    repository.updateIngredient(ingredient)
                                }
                            }
                        } else {
                            // Check if local version is higher than Firebase version (requires a fetch from Firebase)
                            val firebaseIngredient = repository.getIngredientByFirebaseId(ingredient.firebaseId)
                            if (firebaseIngredient == null || ingredient.version > firebaseIngredient.version) {
                                // Update Firebase since local version is higher or Firebase record not found
                                val updateResponse = repository.updateIngredientOnFirebase(ingredient)
                                if (updateResponse.isSuccessful) {
                                    ingredient.isSynced = true
                                    ingredient.lastModified = System.currentTimeMillis()
                                    repository.updateIngredient(ingredient)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Handle exceptions (network error, etc.)
                    Log.e("IngredientSync", "Failed to sync ingredient: ${e.message}")
                }
            }
        }
    }


    private suspend fun getAuthToken(): String? = suspendCancellableCoroutine { continuation ->
        AuthManager.getInstance().getIdToken { token, error ->
            if (error != null) {
                continuation.resume(null) { }
            } else {
                continuation.resume(token) { }
            }
        }
    }

    private suspend fun getIngredientFromFirebase(ingredient: Ingredient, token: String): Ingredient? {
        return try {
            val response = apiService.getIngredientById(ingredient.firebaseId)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }



}