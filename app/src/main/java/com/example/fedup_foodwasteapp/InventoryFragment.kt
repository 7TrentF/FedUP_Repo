package com.example.fedup_foodwasteapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

// InventoryFragment class represents the fragment that displays the inventory of ingredients.
class InventoryFragment : Fragment() {

    // Nullable parameters that can be passed to the fragment as arguments.
    private var param1: String? = null
    private var param2: String? = null

    // ViewModel for managing UI-related data in a lifecycle-conscious way.
    private lateinit var ingredientViewModel: IngredientViewModel

    // Adapter for the RecyclerView that displays the list of ingredients.
    private lateinit var ingredientAdapter: IngredientAdapter

    // ImageButton for selecting a category.
    private lateinit var categoryButton: ImageButton

    // ImageButton for selecting a category (likely the same as above).
    private lateinit var imgCategory: ImageButton

    private lateinit var imgSort: ImageButton

    // Called when the fragment is being created.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the arguments passed to the fragment (if any).
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    // Called to create the view hierarchy associated with the fragment.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment.
        val view = inflater.inflate(R.layout.fragment_inventory, container, false)

        // Initialize the category selection ImageButton.
        imgCategory = view.findViewById(R.id.img_category)
        imgSort =  view.findViewById(R.id.img_sort)
        imgCategory.setOnClickListener {
            // Show a dialog to select a category when the button is clicked.
            showCategorySelectionDialog()
        }

        imgSort.setOnClickListener {
            showSortSelectionDialog()
        }



        // Initialize the RecyclerView and its layout manager.
        val recyclerView: RecyclerView = view.findViewById(R.id.Ingredient_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize ingredientDao using the correct application cast
        val ingredientDao = (requireActivity().application as FedUpFoodWaste).database.ingredientDao()

        // Set up the adapter for the RecyclerView.
        ingredientAdapter = IngredientAdapter(requireContext(), ingredientDao)
        recyclerView.adapter = ingredientAdapter

        // Initialize the ViewModel associated with this fragment.
        ingredientViewModel = ViewModelProvider(requireActivity()).get(IngredientViewModel::class.java)

        return view
    }


    private fun filterByCategory(category: String) {
        lifecycleScope.launch {
            try {
                Log.d("FilterCategory", "Filtering ingredients by category: $category")

                // Call the API to get ingredients by category
                val response = RetrofitClient.apiService.getIngredientsByCategory(category)

                if (response.isSuccessful) {
                    val filteredIngredients = response.body() ?: emptyList()
                    Log.d("FilterCategory", "Successfully fetched ${filteredIngredients.size} ingredients")

                    // Update the adapter with the filtered ingredients
                    ingredientAdapter.setIngredients(filteredIngredients)
                } else {
                    // Handle different HTTP error codes
                    when (response.code()) {
                        400 -> {
                            Log.e("FilterCategory", "Bad Request: Invalid category")
                            Toast.makeText(context, "Bad Request: Invalid category", Toast.LENGTH_SHORT).show()
                        }
                        404 -> {
                            Log.e("FilterCategory", "Not Found: No ingredients found for this category")
                            Toast.makeText(context, "Not Found: No ingredients found for this category", Toast.LENGTH_SHORT).show()
                        }
                        500 -> {
                            Log.e("FilterCategory", "Server Error: Please try again later")
                            Toast.makeText(context, "Server Error: Please try again later", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Log.e("FilterCategory", "Failed to load ingredients: ${response.message()}")
                            Toast.makeText(context, "Failed to load ingredients: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: IOException) {
                // Handle network errors
                Log.e("FilterCategory", "Network Error: ${e.message}", e)
                Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Handle any other errors
                Log.e("FilterCategory", "Error: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }




    // Displays a dialog for category selection.
    private fun showCategorySelectionDialog() {
        // Get the display names of the categories.
        val categories = Category.values().map { it.displayName }.toTypedArray()
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Category")

        // Set up the dialog with the category names and handle the selection.
        builder.setItems(categories) { dialog, which ->
            val selectedCategory = Category.values()[which].name

            // Log the selected category
            Log.d("CategorySelection", "Selected Category: $selectedCategory")

            // Call the filter method with the selected category
            filterByCategory(selectedCategory)
        }
        builder.show()
    }


    private fun  showSortSelectionDialog(){
        val options = arrayOf("About to Expire", "Alphabetical")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Sort By")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> { // About to Expire
                    sortIngredientsByExpirationDate()
                }
                1 -> { // Alphabetical
                    sortIngredientsAlphabetically()
                }
            }
        }
        builder.show()

    }

    private fun sortIngredientsByExpirationDate() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getIngredients() // Call the API to fetch ingredients
                if (response.isSuccessful) {
                    val ingredients = response.body()
                    ingredients?.let {
                        val currentDate = LocalDate.now()

                        // Sort ingredients based on expiration date
                        val sortedByExpiry = ingredients.sortedBy { ingredient ->
                            val expirationDate = LocalDate.parse(ingredient.expirationDate) // Assuming your expirationDate is in ISO format
                            ChronoUnit.DAYS.between(currentDate, expirationDate)
                        }
                        // Update RecyclerView with sorted ingredients
                        ingredientAdapter.updateIngredients(sortedByExpiry)

                    }
                }
            } catch (e: Exception) {
                Log.e("SortError", "Error sorting ingredients: ${e.message}")
            }
        }
    }


    private fun sortIngredientsAlphabetically() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getIngredients() // Call the API to fetch ingredients
                if (response.isSuccessful) {
                    val ingredients = response.body()
                    ingredients?.let {
                        // Sort ingredients by name alphabetically
                        val sortedByName = ingredients.sortedBy { it.productName }
                        // Update RecyclerView with sorted ingredients
                        ingredientAdapter.updateIngredients(sortedByName)

                    }
                }
            } catch (e: Exception) {
                Log.e("SortError", "Error sorting ingredients: ${e.message}")
            }
        }
    }





    // Called after the view hierarchy associated with the fragment has been created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Re-initialize the RecyclerView and its adapter.
        val recyclerView = view.findViewById<RecyclerView>(R.id.Ingredient_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ingredientAdapter

        // Re-initialize the ViewModel.
        ingredientViewModel = ViewModelProvider(requireActivity()).get(IngredientViewModel::class.java)


        // Fetch ingredients from Firebase and observe the LiveData
       ingredientViewModel.fetchIngredientsFromFirebase()

        // Start observing for real-time changes
        ingredientViewModel.observeIngredientChanges()

        // Observe the LiveData to update the RecyclerView when data changes
        ingredientViewModel.filteredIngredients.observe(viewLifecycleOwner, Observer { ingredients ->
            ingredientAdapter.setIngredients(ingredients ?: emptyList())
        })

        /* Observe the LiveData and update the adapter when data changes
        ingredientViewModel.filteredIngredients.observe(viewLifecycleOwner, Observer { ingredients ->
            ingredientAdapter.setIngredients(ingredients)

        }) */


    }

    // Companion object to create a new instance of InventoryFragment with arguments.
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            InventoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}