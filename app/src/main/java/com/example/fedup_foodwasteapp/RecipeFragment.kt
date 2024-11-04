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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
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

        authManager.getIdToken { token, error ->
            if (token != null) {

                // Build a standardized Retrofit instance once
                val recipeRetrofit = Retrofit.Builder()
                    .baseUrl("https://spoonacular-recipe-food-nutrition-v1.p.rapidapi.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(
                        OkHttpClient.Builder().apply {
                            addInterceptor { chain ->
                                val request = chain.request()
                                    .newBuilder()
                                    .addHeader("X-RapidAPI-Key", "649a3d770bmsh6d6d3423d8e5a25p139e64jsne6fc42e17ee2")
                                    .addHeader("X-RapidAPI-Host", "spoonacular-recipe-food-nutrition-v1.p.rapidapi.com")
                                    .build()
                                chain.proceed(request)
                            }
                        }.build()
                    )
                    .build()

                val recipeApiService = recipeRetrofit.create(RecipeApiService::class.java)

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val ingredientsResponse = RetrofitClient.apiService.getIngredients()

                        if (ingredientsResponse.isSuccessful) {
                            val ingredients = ingredientsResponse.body() ?: emptyList()

                            if (ingredients.isNotEmpty()) {
                                val ingredientsQuery = ingredients.joinToString(",") { it.productName }

                                try {
                                    val response = recipeApiService.getRecipesByIngredients(ingredientsQuery)

                                    withContext(Dispatchers.Main) {
                                        if (response.isNotEmpty()) {
                                            recipeAdapter.updateData(response)
                                        } else {
                                            showError("No recipes found.")
                                        }
                                    }
                                } catch (e: HttpException) {
                                    val errorBody = e.response()?.errorBody()?.string()
                                    withContext(Dispatchers.Main) {
                                        showError("Failed to load recipes. Please check your connection.")
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    showError("No ingredients found.")
                                }
                            }
                        } else {
                            val errorBody = ingredientsResponse.errorBody()?.string()
                            withContext(Dispatchers.Main) {
                                showError("Failed to fetch ingredients.")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showError("Error occurred while fetching ingredients.")
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            } else {
                showError("Authentication error. Please try again.")
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
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

}