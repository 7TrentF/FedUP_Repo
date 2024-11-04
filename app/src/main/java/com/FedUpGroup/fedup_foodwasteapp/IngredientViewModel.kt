package com.FedUpGroup.fedup_foodwasteapp

import android.app.Application
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope

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

    private val _searchQuery = MutableStateFlow("")

    private val _ingredientCounts = MutableStateFlow<UiState<IngredientCounts>>(UiState.Loading)

    private val _filteredIngredients = MediatorLiveData<List<Ingredient>?>()
    val filteredIngredients: MediatorLiveData<List<Ingredient>?> get() = _filteredIngredients

    private val _filteredSearchIngredients = MutableLiveData<List<Ingredient>>()

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
    }

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

            try {
                _dataState.value = DataResult.Loading

                // Get Firebase token and fetch ingredients
                authManager.getIdToken { token, error ->
                    if (error != null) {
                        _dataState.postValue(DataResult.Error(Exception("Failed to get Firebase token: ${error}")))
                        loadFromRoom() // Fallback to local data
                        return@getIdToken
                    }

                    if (token != null) {

                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                // Fetch ingredients from Firebase
                                val firebaseIngredients = repository.fetchIngredientsFromApi(token)

                                if (firebaseIngredients != null) {

                                    // Update Room database
                                    withContext(Dispatchers.IO) {
                                        firebaseIngredients.forEach { firebaseIngredient ->
                                            // Try to find existing ingredient in Room
                                            val existingIngredient = ingredientDao.getIngredientById(firebaseIngredient.id)

                                            if (existingIngredient != null) {
                                                // Update existing ingredient
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
                                            } else {
                                                // If ingredient doesn't exist in Room, insert it
                                                val newIngredient = firebaseIngredient.copy(
                                                    isSynced = true
                                                )
                                                ingredientDao.insert(newIngredient)
                                            }
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
                        _dataState.postValue(DataResult.Error(Exception("Unknown error occurred: No token received")))
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
        authManager.getIdToken { token, error ->
            if (token != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val ingredients = repository.fetchIngredientsFromApi(token)
                        if (ingredients != null) {
                            // Update LiveData with fetched ingredients
                            _filteredIngredients.postValue(ingredients)
                        } else {
                            _filteredIngredients.postValue(emptyList())
                        }
                    } catch (e: Exception) {
                    }
                }
            } else {
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

                }
            } catch (e: Exception) {
            }
        }
    }

    // Delete ingredient by firebase_id
    fun deleteIngredientByFirebaseId(firebaseId: String) {
        viewModelScope.launch {
            val ingredient = repository.getIngredientByFirebaseId(firebaseId)
            if (ingredient != null) {
                repository.deleteIngredientByFirebaseId(firebaseId)
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
            repository.updateIngredientDetails(ingredient)
        }
    }

    fun loadFromRoomOffline() {
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
                return@launch
            }

            try {
                _isSyncing.value = true

                val unsyncedIngredients = withContext(Dispatchers.IO) {
                    repository.getUnsyncedIngredients()
                }

                // Process each ingredient sequentially to avoid race conditions
                unsyncedIngredients.forEach { ingredient ->
                    try {
                        // Double-check sync status before processing
                        val currentIngredient = repository.getIngredientById(ingredient.id)
                        if (currentIngredient == null || currentIngredient.isSynced) {
                            return@forEach
                        }

                        when {
                            ingredient.isDeleted -> handleDeletedIngredient(ingredient)
                            ingredient.firebaseId.isEmpty() -> handleNewIngredient(ingredient)
                            else -> handleExistingIngredient(ingredient)
                        }
                    } catch (e: Exception) {
                    }
                }

            } finally {
                _isSyncing.value = false
                syncMutex.unlock()
            }
        }
    }

    private suspend fun handleDeletedIngredient(ingredient: Ingredient) {
        try {
            // If the ingredient has no Firebase ID, it was created offline and then deleted
            // We can just remove it from Room without syncing to Firebase
            if (ingredient.firebaseId.isEmpty()) {
                repository.hardDeleteIngredient(ingredient)
                return
            }

            // Otherwise, try to delete from Firebase
            val deleteResponse = repository.deleteIngredientFromFirebase(ingredient)
            if (deleteResponse.isSuccessful) {
                // Remove from local database after successful Firebase deletion
                repository.hardDeleteIngredient(ingredient)
            } else {
                // If deletion from Firebase fails, keep the soft delete mark but update sync status
                ingredient.apply {
                    isSynced = false  // Mark for retry
                    lastModified = System.currentTimeMillis()
                }
                repository.updateIngredient(ingredient)
            }
        } catch (e: Exception) {
            // In case of error, keep the soft delete mark but update sync status
            ingredient.apply {
                isSynced = false  // Mark for retry
                lastModified = System.currentTimeMillis()
            }
            repository.updateIngredient(ingredient)
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
            }
        }
    }


    private suspend fun handleNewIngredient(ingredient: Ingredient) {
        try {
            // First, check if this ingredient has already been synced
            if (ingredient.isSynced) {
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
                } ?: run {
                    // If response body is null, mark as unsynced to retry later
                    ingredient.isSynced = false
                    repository.updateIngredient(ingredient)
                }
            } else {
                // If sync failed, mark as unsynced to retry later
                ingredient.isSynced = false
                repository.updateIngredient(ingredient)
            }
        } catch (e: Exception) {
            // In case of any exception, mark as unsynced to retry later
            ingredient.isSynced = false
            repository.updateIngredient(ingredient)
        }
    }

    private suspend fun handleExistingIngredient(ingredient: Ingredient) {
        val firebaseIngredient = getIngredientFromFirebase(ingredient)
        if (firebaseIngredient == null || ingredient.version > firebaseIngredient.version) {
            val updateResponse = repository.updateIngredientOnFirebase(ingredient)
            if (updateResponse.isSuccessful) {
                ingredient.apply {
                    isSynced = true
                    lastModified = System.currentTimeMillis()
                }
                repository.updateIngredient(ingredient)
            }
        }
    }

    private suspend fun getIngredientFromFirebase(ingredient: Ingredient): Ingredient? {
        return try {
            // Retrieve the token using getAuthToken()
            val token = getAuthToken()?.let { "Bearer $it" }

            if (token == null) {
                return null
            }

            val response = apiService.getIngredientById(ingredient.firebaseId)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    body
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
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

    fun fetchAndDisplayIngredientCounts(
        freshTextView: TextView,
        expiringSoonTextView: TextView,
        expiredTextView: TextView
    ) {
        viewModelScope.launch {
            try {
                val counts = repository.getIngredientCounts()
                withContext(Dispatchers.Main) {
                    freshTextView.text = counts.freshCount.toString()
                    expiringSoonTextView.text = counts.expiringSoonCount.toString()
                    expiredTextView.text = counts.expiredCount.toString()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

}


