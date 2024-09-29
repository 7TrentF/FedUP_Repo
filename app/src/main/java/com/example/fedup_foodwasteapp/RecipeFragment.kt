package com.example.fedup_foodwasteapp

import RecipeApiService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fedup_foodwasteapp.databinding.FragmentRecipeBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeFragment : Fragment() {
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var binding: FragmentRecipeBinding
    private val authManager = AuthManager.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecipeBinding.inflate(inflater, container, false)
        progressBar = binding.progressBar
        recyclerView = binding.recipeRecyclerView

        recipeAdapter = RecipeAdapter(listOf()) { recipe ->
            openRecipeDetailActivity(recipe)
        }

        recyclerView.adapter = recipeAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        loadRecipes()

        return binding.root
    }

    private fun loadRecipes() {
        progressBar.visibility = View.VISIBLE

        // Get the Firebase ID token using AuthManager
        authManager.getIdToken { token, error ->
            if (token != null) {
                // Set up Retrofit for API calls
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://spoonacular-recipe-food-nutrition-v1.p.rapidapi.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(RecipeApiService::class.java)

                // Firebase reference to ingredients
                val db = FirebaseDatabase.getInstance().getReference("ingredients")

                // Add logging to check the Firebase reference
                Log.d("LoadRecipes", "Fetching ingredients from Firebase...")

                // Listen for Firebase ingredients data
                db.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val ingredientNames = mutableListOf<String>()

                        Log.d("LoadRecipes", "Firebase onDataChange called. DataSnapshot size: ${dataSnapshot.childrenCount}")

                        // Loop through the Firebase data to extract ingredient names
                        for (snapshot in dataSnapshot.children) {
                            val ingredient = snapshot.getValue(FirebaseIngredient::class.java)
                            Log.d("LoadRecipes", "Ingredient fetched from Firebase: ${ingredient?.ingredient_name}")

                            ingredient?.let {
                                ingredientNames.add(it.ingredient_name)
                            }
                        }

                        // If ingredient names are retrieved, log the query and make API call
                        if (ingredientNames.isNotEmpty()) {
                            val ingredientsQuery = ingredientNames.joinToString(",") // Join ingredients as comma-separated string
                            Log.d("LoadRecipes", "Ingredients Query: $ingredientsQuery")

                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    // Call the API with the ingredients query and token
                                    Log.d("LoadRecipes", "Making API call to Spoonacular with query: $ingredientsQuery")
                                    val response = apiService.getRecipes("Bearer $token", ingredientsQuery)

                                    // Log response status
                                    Log.d("LoadRecipes", "API call successful. Response: ${response.results.size} recipes found.")

                                    if (response.results.isNotEmpty()) {
                                        // Update the RecyclerView with the recipes
                                        recipeAdapter.updateData(response.results)
                                    } else {
                                        Log.w("LoadRecipes", "No recipes found for the provided ingredients.")
                                        showError("No recipes found.")
                                    }
                                } catch (e: Exception) {
                                    // Log API call failure
                                    Log.e("LoadRecipes", "Error occurred during API call", e)
                                    showError("Failed to load recipes. Please check your internet connection.")
                                } finally {
                                    progressBar.visibility = View.GONE
                                }
                            }
                        } else {
                            Log.w("LoadRecipes", "No ingredients found in Firebase.")
                            showError("No ingredients found in Firebase.")
                            progressBar.visibility = View.GONE
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Log the error with Firebase
                        Log.e("LoadRecipes", "Firebase error occurred: ${databaseError.message}", databaseError.toException())
                        showError("Failed to load ingredients from Firebase.")
                        progressBar.visibility = View.GONE
                    }
                })
            } else {
                Log.e("RecipeFragment", "Failed to get token: $error")
                showError("Authentication failed: $error")
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun openRecipeDetailActivity(recipe: Recipe) {
        // Create an intent to navigate to RecipeDetailsActivity
        val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
            // Pass the Recipe object to the activity
            putExtra("recipe", recipe)
        }

        // Start the activity
        startActivity(intent)
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}