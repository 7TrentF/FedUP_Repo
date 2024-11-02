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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

// The IngredientViewModel class extends AndroidViewModel, providing the application context.
// It serves as a bridge between the UI and the repository, holding the app's data in a lifecycle-aware way.
class IngredientViewModel(application: Application) : AndroidViewModel(application) {

    private val _dataState = MutableLiveData<DataResult<List<Ingredient>>>()
    val dataState: LiveData<DataResult<List<Ingredient>>> = _dataState
    private val ingredientDao: IngredientDao
    private val networkMonitor = NetworkMonitor(application)
    private val coroutineScope = viewModelScope
    private val repository: IngredientRepository
    val allIngredients: Flow<List<Ingredient>>

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
    private var syncJob: Job? = null

    // Sync-related state
    private val syncMutex = Mutex()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

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
        //loadFromRoomOffline()
        //fetchIngredientsFromFirebase()
        // syncApiToFirebase()  // Sync from API to Firebase
        // syncData()   // Sync from Firebase to RoomDB
    }


    ////////////////////////////////////////////////////////////NEW///////////////////////////////////////////////////////////////////
    private fun setupNetworkMonitoring() {
        networkMonitor.startMonitoring()

        viewModelScope.launch {
            networkMonitor.isNetworkAvailable
                .debounce(300) // Wait 300ms for network state to stabilize
                .distinctUntilChanged() // Only react to actual state changes
                .collect { isAvailable ->
                    if (isAvailable) {
                        syncUnsyncedIngredients()
                    } else {
                        loadFromRoomOffline()
                    }
                }
        }
    }
    private suspend fun syncDataWithFirebase() {
        syncJob?.cancel() // Cancel any existing sync
        syncJob = coroutineScope.launch {
            Log.d("ViewModel", "Starting sync data with Firebase")

            try {
                _dataState.value = DataResult.Loading
                Log.d("ViewModel", "Data state set to Loading")

                // Get Firebase token and fetch ingredients
                authManager.getIdToken { token, error ->
                    if (error != null) {
                        Log.e("ViewModel", "Failed to get Firebase token: ${error}")
                        _dataState.postValue(DataResult.Error(Exception("Failed to get Firebase token: ${error}")))
                        loadFromRoom() // Fallback to local data
                        return@getIdToken
                    }

                    if (token != null) {
                        Log.d("ViewModel", "Received Firebase token")

                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                // Fetch ingredients from Firebase
                                Log.d("ViewModel", "Fetching ingredients from Firebase")
                                val firebaseIngredients = repository.fetchIngredientsFromApi(token)

                                if (firebaseIngredients != null) {
                                    Log.d("ViewModel", "Fetched ${firebaseIngredients.size} ingredients from Firebase")

                                    // Update Room database
                                    withContext(Dispatchers.IO) {
                                        firebaseIngredients.forEach { firebaseIngredient ->
                                            // Try to find existing ingredient in Room
                                            val existingIngredient = ingredientDao.getIngredientById(firebaseIngredient.id)

                                            if (existingIngredient != null) {
                                                // Update existing ingredient
                                                Log.d("ViewModel", "Updating existing ingredient: ${firebaseIngredient.productName}")
                                                val updatedIngredient = existingIngredient.copy(
                                                    productName = firebaseIngredient.productName,
                                                    quantity = firebaseIngredient.quantity,
                                                    expirationDate = firebaseIngredient.expirationDate,
                                                    category = firebaseIngredient.category,
                                                    firebaseId = existingIngredient.firebaseId.ifEmpty { firebaseIngredient.firebaseId },
                                                    version = existingIngredient.version + 1, // Increment only on update
                                                    lastModified = System.currentTimeMillis(),
                                                    isSynced = true
                                                )
                                                ingredientDao.update(updatedIngredient)
                                                Log.d("ViewModel", "Updated ingredient in Room: ${updatedIngredient.productName}")
                                            } else {
                                                // If ingredient doesn't exist in Room, insert it
                                                Log.d("ViewModel", "Inserting new ingredient: ${firebaseIngredient.productName}")
                                                val newIngredient = firebaseIngredient.copy(
                                                    isSynced = true
                                                )
                                                ingredientDao.insert(newIngredient)
                                                Log.d("ViewModel", "Inserted ingredient into Room: ${newIngredient.productName}")
                                            }
                                        }
                                    }

                                    // Update UI with room data
                                    loadFromRoom()
                                    Log.d("ViewModel", "UI updated with Room data")
                                } else {
                                    Log.e("ViewModel", "No data received from Firebase")
                                    _dataState.postValue(DataResult.Error(Exception("No data received from Firebase")))
                                    loadFromRoom() // Fallback to local data
                                }
                            } catch (e: Exception) {
                                Log.e("ViewModel", "Error while syncing data with Firebase: ${e.message}")
                                _dataState.postValue(DataResult.Error(e))
                                loadFromRoom() // Fallback to local data
                            }
                        }
                    } else {
                        Log.e("ViewModel", "Unknown error occurred: No token received")
                        _dataState.postValue(DataResult.Error(Exception("Unknown error occurred: No token received")))
                        loadFromRoom() // Fallback to local data
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error in syncDataWithFirebase: ${e.message}")
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
      //  syncJob?.cancel()
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

    fun deleteIngredient(firebaseId: String) {
        viewModelScope.launch {
            repository.deleteIngredientByFirebaseId(firebaseId)

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


    fun loadFromRoomOffline() {
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

    fun observeRoomIngredients() {
        _filteredIngredients.addSource(ingredientDao.getAllIngredients()) { ingredients ->
            _filteredIngredients.value = ingredients
        }
    }

    suspend fun insertOffline(ingredient: Ingredient): Long {
        return withContext(Dispatchers.IO) {
            repository.insertIngredient(ingredient)
        }
    }

    fun syncUnsyncedIngredients() {
        viewModelScope.launch {
            if (!syncMutex.tryLock()) {
                Log.d("IngredientSync", "Sync already in progress, skipping")
                return@launch
            }

            try {
                _isSyncing.value = true
                Log.d("IngredientSync", "Starting sync of unsynced ingredients")

                // Get unsynced ingredients inside a transaction to prevent modifications during sync
                val unsyncedIngredients = withContext(Dispatchers.IO) {
                    repository.getUnsyncedIngredients()
                }

                Log.d("IngredientSync", "Fetched ${unsyncedIngredients.size} unsynced ingredients")

                // Process each ingredient sequentially to avoid race conditions
                unsyncedIngredients.forEach { ingredient ->
                    try {
                        // Double-check sync status before processing
                        val currentIngredient = repository.getIngredientById(ingredient.id)
                        if (currentIngredient == null || currentIngredient.isSynced) {
                            Log.d("IngredientSync", "Ingredient ${ingredient.productName} no longer needs sync, skipping")
                            return@forEach
                        }

                        when {
                            ingredient.isDeleted -> handleDeletedIngredient(ingredient)
                            ingredient.firebaseId.isEmpty() -> handleNewIngredient(ingredient)
                            else -> handleExistingIngredient(ingredient)
                        }
                    } catch (e: Exception) {
                        Log.e("IngredientSync", "Failed to sync ingredient ${ingredient.productName}: ${e.message}")
                    }
                }

                Log.d("IngredientSync", "Sync process completed for all unsynced ingredients")
            } finally {
                _isSyncing.value = false
                syncMutex.unlock()
            }
        }
    }

    private suspend fun handleDeletedIngredient(ingredient: Ingredient) {
        try {
            Log.d("IngredientSync", "Processing deletion for ingredient: ${ingredient.productName}")

            // If the ingredient has no Firebase ID, it was created offline and then deleted
            // We can just remove it from Room without syncing to Firebase
            if (ingredient.firebaseId.isEmpty()) {
                Log.d("IngredientSync", "Ingredient was never synced to Firebase, removing locally: ${ingredient.productName}")
                repository.hardDeleteIngredient(ingredient)
                return
            }

            // Otherwise, try to delete from Firebase
            val deleteResponse = repository.deleteIngredientFromFirebase(ingredient)
            if (deleteResponse.isSuccessful) {
                Log.d("IngredientSync", "Successfully deleted ingredient from Firebase: ${ingredient.productName}")
                // Remove from local database after successful Firebase deletion
                repository.hardDeleteIngredient(ingredient)
                Log.d("IngredientSync", "Removed ingredient from RoomDB: ${ingredient.productName}")
            } else {
                // If deletion from Firebase fails, keep the soft delete mark but update sync status
                ingredient.apply {
                    isSynced = false  // Mark for retry
                    lastModified = System.currentTimeMillis()
                }
                repository.updateIngredient(ingredient)
                Log.e("IngredientSync", "Failed to delete ingredient from Firebase: ${ingredient.productName}, Error: ${deleteResponse.errorBody()}")
            }
        } catch (e: Exception) {
            // In case of error, keep the soft delete mark but update sync status
            ingredient.apply {
                isSynced = false  // Mark for retry
                lastModified = System.currentTimeMillis()
            }
            repository.updateIngredient(ingredient)
            Log.e("IngredientSync", "Exception while deleting ingredient ${ingredient.productName}: ${e.message}")
        }
    }


    fun SoftDeleteIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            try {
                repository.softDeleteIngredient(ingredient)
                // If we're online, trigger sync immediately
                if (networkMonitor.isNetworkAvailable.value) {
                    syncUnsyncedIngredients()
                }
            } catch (e: Exception) {
                Log.e("IngredientSync", "Error deleting ingredient: ${e.message}")
            }
        }
    }




    private suspend fun handleNewIngredient(ingredient: Ingredient) {
        try {
            Log.d("IngredientSync", "Adding new ingredient to Firebase: ${ingredient.productName}")

            // First, check if this ingredient has already been synced
            if (ingredient.isSynced) {
                Log.d("IngredientSync", "Ingredient ${ingredient.productName} is already synced, skipping")
                return
            }

            // Mark as being processed to prevent duplicate processing
            ingredient.apply {
                isSynced = true
                lastModified = System.currentTimeMillis()
            }
            repository.updateIngredient(ingredient)

            val addResponse = repository.addIngredientToFirebase(ingredient)
            if (addResponse.isSuccessful) {
                addResponse.body()?.let { createdIngredient ->
                    // Update with Firebase ID and confirm sync
                    ingredient.apply {
                        firebaseId = createdIngredient.firebaseId
                    }
                    repository.updateIngredient(ingredient)
                    Log.d("IngredientSync", "New ingredient added and updated in RoomDB: ${ingredient.productName}")
                } ?: run {
                    // If response body is null, mark as unsynced to retry later
                    ingredient.isSynced = false
                    repository.updateIngredient(ingredient)
                    Log.e("IngredientSync", "Failed to parse created ingredient response for: ${ingredient.productName}")
                }
            } else {
                // If sync failed, mark as unsynced to retry later
                ingredient.isSynced = false
                repository.updateIngredient(ingredient)
                Log.e("IngredientSync", "Failed to add ingredient to Firebase: ${ingredient.productName}, Error: ${addResponse.errorBody()}")
            }
        } catch (e: Exception) {
            // In case of any exception, mark as unsynced to retry later
            ingredient.isSynced = false
            repository.updateIngredient(ingredient)
            Log.e("IngredientSync", "Exception while syncing ingredient ${ingredient.productName}: ${e.message}")
        }
    }

    private suspend fun handleExistingIngredient(ingredient: Ingredient) {
        Log.d("IngredientSync", "Checking if ingredient update is needed for: ${ingredient.firebaseId}")

        val firebaseIngredient = getIngredientFromFirebase(ingredient)

        Log.d("IngredientSync", "Local version: ${ingredient.version}")
        Log.d("IngredientSync", "Firebase version: ${firebaseIngredient?.version}")

        if (firebaseIngredient == null || ingredient.version > firebaseIngredient.version) {
            Log.d("IngredientSync", "Updating ingredient on Firebase: ${ingredient.productName}")
            val updateResponse = repository.updateIngredientOnFirebase(ingredient)
            if (updateResponse.isSuccessful) {
                ingredient.apply {
                    isSynced = true
                    lastModified = System.currentTimeMillis()
                }
                repository.updateIngredient(ingredient)
                Log.d("IngredientSync", "Ingredient updated on Firebase and synced in RoomDB: ${ingredient.productName}")
            } else {
                Log.e("IngredientSync", "Failed to update ingredient on Firebase: ${ingredient.productName}, Error: ${updateResponse.errorBody()}")
            }
        } else {
            Log.d("IngredientSync", "No update needed for ingredient: ${ingredient.productName}")
        }
    }

    private suspend fun getIngredientFromFirebase(ingredient: Ingredient): Ingredient? {
        return try {
            // Retrieve the token using getAuthToken()
            val token = getAuthToken()?.let { "Bearer $it" }

            if (token == null) {
                Log.e("IngredientSync", "Failed to fetch token for Firebase request.")
                return null
            }

            Log.d("IngredientSync", "Fetching ingredient from Firebase with ID: ${ingredient.firebaseId}")
            val response = apiService.getIngredientById(ingredient.firebaseId)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d("IngredientSync", "Successfully retrieved ingredient from Firebase with version: ${body.version}")
                    body
                } else {
                    Log.e("IngredientSync", "Response successful but body is null for ingredient ID: ${ingredient.firebaseId}")
                    null
                }
            } else {
                Log.e("IngredientSync", "Failed to get ingredient from Firebase. Status code: ${response.code()}, Error: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("IngredientSync", "Exception while fetching ingredient from Firebase: ${e.message}", e)
            null
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


}