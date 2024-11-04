package com.example.fedup_foodwasteapp

import RecipeApiService
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var loadingProgressBar: ProgressBar

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        // Initialize the ProgressBar
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Recipe Details" // Optional: You can set the title for the action bar

        // Retrieve the passed Recipe object from the intent
        val recipe: Recipe? =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("recipe", Recipe::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Recipe>("recipe")
            }

        recipe?.let {
            // Use the recipe id to fetch additional details from the API
            loadRecipeDetails(it.id)
        } ?: run {
            // Handle case where the recipe is null
            Toast.makeText(this, "Failed to load recipe details.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRecipeDetails(recipeId: Int) {
        // Show the progress bar while loading
        loadingProgressBar.visibility = View.VISIBLE

        // Hide the recipe title and summary while loading
        findViewById<TextView>(R.id.recipeTitleTextView).visibility = View.GONE
        findViewById<TextView>(R.id.recipeSummaryTextView).visibility = View.GONE

        // Make an API call to fetch the detailed recipe data
        lifecycleScope.launch {
            try {

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://spoonacular-recipe-food-nutrition-v1.p.rapidapi.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(RecipeApiService::class.java)

                // Fetch detailed recipe information using the recipeId
                val recipeDetails = apiService.getRecipeDetails(
                    recipeId,
                    "649a3d770bmsh6d6d3423d8e5a25p139e64jsne6fc42e17ee2",
                    "spoonacular-recipe-food-nutrition-v1.p.rapidapi.com"
                )
                // Now that we have the RecipeDetails object, pass it to displayRecipeDetails
                displayRecipeDetails(recipeDetails)

            } catch (e: Exception) {
                // Handle any errors
                Toast.makeText(
                    this@RecipeDetailActivity,
                    "Failed to load recipe details.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Hide the progress bar after loading is complete
                loadingProgressBar.visibility = View.GONE
            }
        }
    }

    private fun displayRecipeDetails(details: RecipeDetails) {
        // Set the recipe title
        val titleTextView = findViewById<TextView>(R.id.recipeTitleTextView)
        titleTextView.text = details.title
        titleTextView.visibility = View.VISIBLE // Make it visible after loading

        // Set the recipe summary and render HTML content
        val summaryTextView = findViewById<TextView>(R.id.recipeSummaryTextView)
        summaryTextView.text = Html.fromHtml(details.summary, Html.FROM_HTML_MODE_LEGACY)
        summaryTextView.visibility = View.VISIBLE // Make it visible after loading

        // Set the recipe instructions and render HTML (if available)
        if (!details.instructions.isNullOrEmpty()) {
            findViewById<TextView>(R.id.recipeInstructions)?.text =
                Html.fromHtml(details.instructions, Html.FROM_HTML_MODE_LEGACY)
        } else {
            findViewById<TextView>(R.id.recipeInstructions)?.text =
                getString(R.string.no_instructions_available)
        }

        // Load the recipe image using Picasso with detailed error handling
        val recipeImageView = findViewById<ImageView>(R.id.recipeImageView)
        recipeImageView?.let {
            Picasso.get()
                .load(details.image) // Check this URL in the logs
                .placeholder(R.drawable.placeholder_image) // Fallback image while loading
                .error(R.drawable.error_image) // Fallback image on error
                .into(it, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                    }

                    override fun onError(e: java.lang.Exception?) {
                    }
                })
        }

        // Set the extended ingredients, if available
        val ingredientsTextView = findViewById<TextView>(R.id.recipeIngredients)
        if (details.extendedIngredients.isNotEmpty()) {
            val ingredientsList = details.extendedIngredients.joinToString("\n") { ingredient ->
                "${ingredient.amount} ${ingredient.unit} - ${ingredient.name}"
            }
            ingredientsTextView?.text = ingredientsList
        } else {
            ingredientsTextView?.text = getString(R.string.no_ingredients_available)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Go back to the previous fragment/activity when back button is pressed
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}