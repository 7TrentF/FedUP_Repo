package com.FedUpGroup.fedup_foodwasteapp

import android.util.Log
import androidx.lifecycle.LiveData

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Response

class IngredientRepository(
    private val ingredientDao: IngredientDao,
    val apiService: ApiService, // Made apiService public for ViewModel access
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastSyncTime: Long = 0 // This variable will hold the last sync time

    // In your Repository
    val allIngredients: Flow<List<Ingredient>> = ingredientDao.getAllIngredientsWhereDeleted()

    suspend fun insertIngredient(ingredient: Ingredient): Long {
        return ingredientDao.insert(ingredient)
    }

    suspend fun markAsSynced(ingredient: Ingredient) {
        ingredientDao.update(ingredient.copy(isSynced = true))
    }

    suspend fun getIngredientCounts(): IngredientCounts {
        return ingredientDao.getIngredientCounts()
    }

    suspend fun getIngredientById(id: Long): Ingredient? = withContext(Dispatchers.IO) {
        ingredientDao.getIngredientById(id)
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    suspend fun getUnsyncedIngredients(): List<Ingredient> {
        return ingredientDao.getUnsyncedIngredients()
    }
    suspend fun addIngredientToFirebase(ingredient: Ingredient): Response<Ingredient> {
        Log.d("IngredientSync", "addIngredientToFirebase: $ingredient")

        return apiService.addIngredient(ingredient)

    }
    suspend fun updateIngredientInFirebase(ingredient: Ingredient): Response<Void> {
        return apiService.updateIngredient(ingredient.firebaseId, ingredient)
    }

    suspend fun deleteIngredientFromFirebase(ingredient: Ingredient): Response<Void> {
        return apiService.deleteIngredient(ingredient.firebaseId)
    }
    suspend fun updateIngredient(ingredient: Ingredient) {
        ingredientDao.update(ingredient) // Update ingredient in RoomDB
    }
    suspend fun removeIngredient(ingredient: Ingredient) {
        ingredientDao.delete(ingredient) // Remove ingredient from RoomDB
    }

    // Function to update an existing ingredient on Firebase
    suspend fun updateIngredientOnFirebase(ingredient: Ingredient): Response<Void> {
        // Ensure that the ingredient has a Firebase ID before trying to update
        if (ingredient.firebaseId.isEmpty()) {
            throw IllegalArgumentException("Firebase ID is missing for this ingredient.")
        }

        // Make the API call to update the ingredient
        return apiService.updateIngredient(
            firebaseId = ingredient.firebaseId,
            ingredient = ingredient
        )
    }

    fun getAllRoomIngredients(): LiveData<List<Ingredient>> {
        return ingredientDao.getAllIngredients()
    }


    // Get the last sync time
    fun getLastSyncTime(): Long {
        return lastSyncTime
    }

    // Set the last sync time
    fun setLastSyncTime(time: Long) {
        lastSyncTime = time
    }

    // Call this function after a successful sync to update the last sync time
    suspend fun updateLastSyncTime() {
        lastSyncTime = System.currentTimeMillis()
    }

    suspend fun sendExpirationData(
        token: String,
        fcmToken: String,
        notificationData: Map<String, String>
    ) {
        try {
            apiService.sendExpirationData(token, fcmToken, notificationData)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteIngredient(ingredient: Ingredient) {
        ingredient.apply {
            isDeleted = true
            isSynced = false
            version++
            lastModified = System.currentTimeMillis()
        }

        ingredientDao.updateIng(ingredient)
    }


    fun getIngredients(): Flow<List<Ingredient>> {
        return ingredientDao.getActiveIngredients()
    }
    suspend fun insert(ingredient: Ingredient) {
        ingredientDao.insert(ingredient)
        val newId = ingredientDao.insert(ingredient) // insert should return the generated ID

    }

    // Update only Firebase ID in RoomDB without incrementing version
    suspend fun updateFirebaseIdOnly(id: Long, firebaseId: String) {
        val existingIngredient = ingredientDao.getIngredientById(id)
        if (existingIngredient != null) {
            val ingredientWithFirebaseId = existingIngredient.copy(firebaseId = firebaseId)
            ingredientDao.updateIng(ingredientWithFirebaseId)
        }
    }

    // Full update for ingredient details, including version increment

    suspend fun updateIngredientDetails(updatedIngredient: Ingredient): Boolean {
        // Look up the existing ingredient by Room `id` instead of `firebaseId`
        val existingIngredient = ingredientDao.getIngredientById(updatedIngredient.id)

        return if (existingIngredient != null) {
            // Ensure we're updating the existing record and incrementing its version
            val ingredientToUpdate = existingIngredient.copy(
                productName = updatedIngredient.productName,
                quantity = updatedIngredient.quantity,
                expirationDate = updatedIngredient.expirationDate,
                category = updatedIngredient.category,
                firebaseId = existingIngredient.firebaseId.ifEmpty { updatedIngredient.firebaseId },
                version = existingIngredient.version + 1, // Increment only on update
                lastModified = System.currentTimeMillis(),
                isSynced = false
            )

            // Update the ingredient in the Room database
            ingredientDao.updateIng(ingredientToUpdate) > 0
        } else {
            false // No ingredient found; nothing to update
        }
    }


    suspend fun deleteIngredientByFirebaseId(firebaseId: String) {
        ingredientDao.deleteByFirebaseId(firebaseId)
    }

    suspend fun getIngredientByFirebaseId(firebaseId: String): Ingredient? {
        return ingredientDao.getIngredientByFirebaseId(firebaseId)
    }

    // Listen for Firebase Changes
    fun listenToFirebaseChanges(scope: CoroutineScope, callback: (List<Ingredient>) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance().getReference("ingredients").child(user.uid)
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ingredientsListFromFirebase = mutableListOf<Ingredient>()
                    for (ingredientSnapshot in snapshot.children) {
                        val ingredient = ingredientSnapshot.getValue(Ingredient::class.java)
                        ingredient?.let { ingredientsListFromFirebase.add(it) }
                    }

                    // Use the passed scope
                    scope.launch {
                        val ingredientsInRoom = ingredientDao.getAllIngredientsNonLive() // Fetch all ingredients from RoomDB as a list

                        val ingredientsToInsert = ingredientsListFromFirebase.filter { firebaseIngredient ->
                            // Check if the ingredient already exists in RoomDB based on a unique property (e.g., id or name)
                            !ingredientsInRoom.any { roomIngredient ->
                                roomIngredient.id == firebaseIngredient.id // Compare by ID (or use name if no ID)
                            }
                        }

                        val ingredientsToUpdate = ingredientsListFromFirebase.filter { firebaseIngredient ->
                            ingredientsInRoom.any { roomIngredient ->
                                roomIngredient.id == firebaseIngredient.id // Compare by ID (or use name)
                            }
                        }

                        // Insert new ingredients and update existing ones
                        ingredientsToInsert.forEach { ingredientDao.insert(it) }
                        ingredientsToUpdate.forEach { ingredientDao.update(it) }

                        callback(ingredientsListFromFirebase)  // Callback to update UI after syncing RoomDB
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    suspend fun markIngredientsAsSynced(ingredients: List<Ingredient>) {
        ingredients.forEach { it.isSynced = true }
        ingredientDao.updateIngredients(ingredients)
    }

    fun syncIngredientsWithRoom(coroutineScope: CoroutineScope, ingredients: List<Ingredient>) {
        coroutineScope.launch(Dispatchers.IO) {
            ingredients.forEach { ingredient ->
                // Use the suspend function to get the local ingredient
                val localIngredient = ingredientDao.getIngredientByIdSuspend(ingredient.id)

                if (localIngredient == null) {
                    ingredientDao.insert(ingredient) // Insert new ingredient if not present in RoomDB
                } else if (localIngredient != ingredient) {
                    ingredientDao.update(ingredient) // Update if ingredient details differ
                }
            }
        }
    }

    // Fetch from REST API
    suspend fun fetchIngredientsFromApi(token: String): List<Ingredient>? {
        try {
            val response = apiService.getIngredients()
            return if (response.isSuccessful) {
                response.body()  // Return the ingredients list
            } else {
                null
            }
        } catch (e: Exception) {
            return null
        }
    }

    // Add Ingredient to Firebase
    suspend fun addIngredientToFirebaseSync(ingredient: Ingredient) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance().getReference("ingredients").child(user.uid)
            val key = database.push().key ?: return
            database.child(key).setValue(ingredient).await() // Use coroutines to sync
        }
    }

    // Sync Firebase with RoomDB (For Offline Access)
    fun syncIngredients(coroutineScope: CoroutineScope, ingredients: List<Ingredient>) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                ingredientDao.deleteAll()  // Clear local DB
                ingredientDao.insertAll(ingredients)  // Insert latest data
            } catch (e: Exception) {
            }
        }
    }

    fun getIngredientsByCategory(category: String): LiveData<List<Ingredient>> {
        return ingredientDao.getIngredientByCategory(category)
    }
    // Add Ingredient to REST API
    suspend fun addIngredientToApi(ingredient: Ingredient) {
        try {
            val response = apiService.addIngredient(ingredient)
            if (response.isSuccessful) {
            } else {
            }
        } catch (e: Exception) {
        }
    }

    suspend fun updateIngredientInApi(firebaseId: String, ingredient: Ingredient) {
        try {
            val response = apiService.updateIngredient( firebaseId, ingredient)
            if (response.isSuccessful) {
                Log.d("Repository", "Ingredient updated in API.")
            } else {
                Log.e("Repository", "Failed to update ingredient in API: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("Repository", "Exception in updating ingredient in API.", e)
        }
    }


    // Fetch all ingredients from RoomDB for offline usage
    fun allIngredientsFromRoomDB(): LiveData<List<Ingredient>> {
        return ingredientDao.getAllIngredients() // Uses LiveData to observe RoomDB changes
    }

    suspend fun softDeleteIngredient(ingredient: Ingredient) {
        ingredient.apply {
            isDeleted = true     // Mark for deletion
            isSynced = false     // Mark as unsynced so it will be processed when online
            lastModified = System.currentTimeMillis()
            version += 1         // Increment version
        }
        ingredientDao.update(ingredient)  // Update in Room
    }

    // This will be called after successful Firebase deletion
    suspend fun hardDeleteIngredient(ingredient: Ingredient) {
        ingredientDao.delete(ingredient)  // Actually remove from Room
    }

    // Add this function
    suspend fun searchIngredients(query: String): List<Ingredient> {
        return ingredientDao.searchIngredients(query)
    }

}