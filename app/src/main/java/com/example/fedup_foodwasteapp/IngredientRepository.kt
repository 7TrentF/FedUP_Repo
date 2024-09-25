package com.example.fedup_foodwasteapp

import android.util.Log
import androidx.lifecycle.LiveData

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class IngredientRepository(
    private val ingredientDao: IngredientDao,
    val apiService: ApiService // Made apiService public for ViewModel access
) {
    val allIngredients: LiveData<List<Ingredient>> = ingredientDao.getAllIngredients()

    suspend fun insert(ingredient: Ingredient) {
        ingredientDao.insert(ingredient)
    }

    suspend fun update(ingredient: Ingredient) {
        ingredientDao.update(ingredient)
    }

    suspend fun delete(ingredient: Ingredient) {
        ingredientDao.delete(ingredient)
    }


    // Listen for Firebase Changes
    fun listenToFirebaseChanges(callback: (List<Ingredient>) -> Unit) {
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
                    callback(ingredientsList)  // Callback to update RoomDB and UI
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("IngredientRepository", "Failed to read from Firebase.", error.toException())
                }
            })
        }
    }

    // Fetch from REST API
    suspend fun fetchIngredientsFromApi(token: String): List<Ingredient>? {
        try {
            val response = apiService.getIngredients("Bearer $token")
            return if (response.isSuccessful) {
                response.body()  // Return the ingredients list
            } else {
                Log.e("Repository", "Failed to fetch ingredients from API: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching ingredients from API.", e)
            return null
        }
    }


    // Push Local Ingredients to Firebase
     fun syncLocalToFirebase(ingredients: List<Ingredient>) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance().getReference("ingredients").child(user.uid)
            ingredients.forEach { ingredient ->
                val key = database.push().key ?: return@forEach
                database.child(key).setValue(ingredient)
                    .addOnSuccessListener { Log.d("IngredientRepository", "Ingredient synced to Firebase.") }
                    .addOnFailureListener { e -> Log.e("IngredientRepository", "Sync failed", e) }
            }
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

    // Sync from API to Firebase
    suspend fun syncApiToFirebase(authToken: String) {
        val apiIngredients = fetchIngredientsFromApi(authToken)
        apiIngredients?.let { syncLocalToFirebase(it) }
    }

    // Sync Firebase with RoomDB (For Offline Access)
    fun syncIngredients(coroutineScope: CoroutineScope, ingredients: List<Ingredient>) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                ingredientDao.deleteAll()  // Clear local DB
                ingredientDao.insertAll(ingredients)  // Insert latest data
            } catch (e: Exception) {
                Log.e("IngredientRepository", "Failed to sync Firebase data with RoomDB", e)
            }
        }
    }


    // Fetch ingredients from REST API
    fun fetchIngredients(coroutineScope: CoroutineScope, authToken: String, onResult: (List<Ingredient>?, String?) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getIngredients("Bearer $authToken")
                if (response.isSuccessful) {
                    val ingredients = response.body()
                    onResult(ingredients, null)
                } else {
                    onResult(null, "Error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                onResult(null, e.localizedMessage)
            }
        }
    }

    fun getIngredientsByCategory(category: String): LiveData<List<Ingredient>> {
        return ingredientDao.getIngredientByCategory(category)
    }

    // Add Ingredient to REST API
    suspend fun addIngredientToApi(token: String, ingredient: Ingredient) {
        try {
            val response = apiService.addIngredient("Bearer $token", ingredient)
            if (response.isSuccessful) {
                Log.d("Repository", "Ingredient added to API.")
            } else {
                Log.e("Repository", "Failed to add ingredient to API: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("Repository", "Exception in adding ingredient to API.", e)
        }
    }


    suspend fun updateIngredientInApi(token: String, id: Int, ingredient: Ingredient) {
        try {
            val response = apiService.updateIngredient("Bearer $token", id, ingredient)
            if (response.isSuccessful) {
                Log.d("Repository", "Ingredient updated in API.")
            } else {
                Log.e("Repository", "Failed to update ingredient in API: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("Repository", "Exception in updating ingredient in API.", e)
        }
    }

    suspend fun deleteIngredientFromFirebase(ingredient: Ingredient) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance().getReference("ingredients").child(user.uid)
            database.child(ingredient.id.toString()).removeValue()
                .addOnSuccessListener { Log.d("IngredientRepository", "Ingredient deleted from Firebase.") }
                .addOnFailureListener { e -> Log.e("IngredientRepository", "Failed to delete ingredient from Firebase.", e) }
        }
    }



    /*
    // Synchronize ingredients from Firebase to Room
    fun syncIngredients(coroutineScope: CoroutineScope, ingredients: List<Ingredient>) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                ingredientDao.deleteAll()
                ingredientDao.insertAll(ingredients)
            } catch (e: Exception) {
                Log.e("IngredientRepository", "Sync failed: ${e.message}")
            }
        }
    }



    // Fetch ingredients from REST API
    fun fetchIngredients(coroutineScope: CoroutineScope, authToken: String, onResult: (List<Ingredient>?, String?) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getIngredients("Bearer $authToken")
                if (response.isSuccessful) {
                    val ingredients = response.body()
                    onResult(ingredients, null)
                } else {
                    onResult(null, "Error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                onResult(null, e.localizedMessage)
            }
        }
    }

    fun getAllIngredientsLocal(coroutineScope: CoroutineScope, callback: (List<Ingredient>) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val ingredients = ingredientDao.getAllIngredientsNonLive() // Create a DAO method to get list directly
            withContext(Dispatchers.Main) {
                callback(ingredients)
            }
        }
    }
    fun addIngredientToFirebase(ingredient: Ingredient) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance().getReference("ingredients").child(user.uid)
            val key = database.push().key
            if (key != null) {
                database.child(key).setValue(ingredient)
                    .addOnSuccessListener {
                        Log.d("IngredientRepository", "Ingredient added to Firebase successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("IngredientRepository", "Failed to add ingredient to Firebase.", e)
                    }
            }
        } else {
            Log.e("IngredientRepository", "User not authenticated. Cannot add ingredient to Firebase.")
        }
    }



    fun listenToFirebaseChanges(callback: (List<Ingredient>) -> Unit) {
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
                    callback(ingredientsList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("IngredientRepository", "Failed to read ingredients from Firebase.", error.toException())
                }
            })
        } else {
            Log.e("IngredientRepository", "User not authenticated. Cannot listen to Firebase changes.")
        }
    }

    fun syncIngredientsLocal(coroutineScope: CoroutineScope, ingredients: List<Ingredient>) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                ingredientDao.deleteAll()
                ingredientDao.insertAll(ingredients)
            } catch (e: Exception) {
                Log.e("IngredientRepository", "syncIngredientsLocal failed: ${e.message}")
            }
        }
    }

    suspend fun getAllIngredientsLocalSync(): List<Ingredient> {
        return ingredientDao.getAllIngredientsNonLive()
    }

    suspend fun addIngredientToFirebaseSync(ingredient: Ingredient) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance().getReference("ingredients").child(user.uid)
            val key = database.push().key
            if (key != null) {
                database.child(key).setValue(ingredient).await() // Use Kotlin coroutines with Tasks
            }
        }
    }


    // REST API Operations
    suspend fun fetchIngredientsFromApi(authToken: String): List<Ingredient>? {
        val response = RetrofitClient.apiService.getIngredients("Bearer $authToken")
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun addIngredientToApi(authToken: String, ingredient: Ingredient): Ingredient? {
        val response = RetrofitClient.apiService.addIngredient("Bearer $authToken", ingredient)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun updateIngredientInApi(authToken: String, id: Int, ingredient: Ingredient): Boolean {
        val response = RetrofitClient.apiService.updateIngredient("Bearer $authToken", id, ingredient)
        return response.isSuccessful
    }

    suspend fun deleteIngredientInApi(authToken: String, id: Int): Boolean {
        val response = RetrofitClient.apiService.deleteIngredient("Bearer $authToken", id)
        return response.isSuccessful
    }

*/



}
