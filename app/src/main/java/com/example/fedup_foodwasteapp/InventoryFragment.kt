package com.example.fedup_foodwasteapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
//import androidx.core.i18n.DateTimeFormatter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class InventoryFragment : Fragment() {

    private lateinit var ingredientViewModel: IngredientViewModel
    private lateinit var ingredientAdapter: IngredientAdapter
    private lateinit var categoryButton: ImageButton
    private lateinit var imgCategory: ImageButton
    private lateinit var imgSort: ImageButton
    private lateinit var freshTextView: TextView
    private lateinit var expiringSoonTextView: TextView
    private lateinit var expiredTextView: TextView

    // Called when the fragment is being created.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // Initialize TextViews
        freshTextView = view.findViewById(R.id.fresh_txt)
        expiringSoonTextView = view.findViewById(R.id.tv_warning)
        expiredTextView = view.findViewById(R.id.tv_expired)

        // Fetch ingredients from API and update TextViews
       // fetchAndDisplayIngredientCounts()


        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        //syncIngredientsIfOnline()


        // Initialize the RecyclerView and its layout manager.
        val recyclerView: RecyclerView = view.findViewById(R.id.Ingredient_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize ingredientDao using the correct application cast
        val ingredientDao = (requireActivity().application as FedUpFoodWaste).database.ingredientDao()

        // Set up the adapter for the RecyclerView.
        ingredientAdapter = IngredientAdapter(requireContext(), ingredientDao,ingredientViewModel )
        recyclerView.adapter = ingredientAdapter

        // Initialize the ViewModel associated with this fragment.
        ingredientViewModel = ViewModelProvider(requireActivity()).get(IngredientViewModel::class.java)


        // Initialize progress bar and error layout
        view.findViewById<ProgressBar>(R.id.progressBar)?.visibility = View.GONE
        view.findViewById<LinearLayout>(R.id.errorLayout)?.visibility = View.GONE


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

                            Snackbar.make(requireView(), "Bad Request: Invalid category", Snackbar.LENGTH_LONG).show()
                        }
                        404 -> {
                            Log.e("FilterCategory", "Not Found: No ingredients found for this category")
                            Snackbar.make(requireView(), "Not Found: No ingredients found for this category", Snackbar.LENGTH_LONG).show()
                        }
                        500 -> {
                            Log.e("FilterCategory", "Server Error: Please try again later")
                            Snackbar.make(requireView(), "Server Error: Please try again later", Snackbar.LENGTH_LONG).show()

                        }
                        else -> {
                            Log.e("FilterCategory", "Failed to load ingredients: ${response.message()}")
                            Snackbar.make(requireView(), "Failed to load ingredients: ${response.message()}", Snackbar.LENGTH_LONG).show()

                        }
                    }
                }
            } catch (e: IOException) {
                // Handle network errors
                Log.e("FilterCategory", "Network Error: ${e.message}", e)
                Snackbar.make(requireView(), "Network Error: ${e.message}", Snackbar.LENGTH_LONG).show()

            } catch (e: Exception) {
                // Handle any other errors
                Log.e("FilterCategory", "Error: ${e.message}", e)
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()

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
                   // sortIngredientsByExpirationDate()
                }
                1 -> { // Alphabetical
                   // sortIngredientsAlphabetically()
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

    private fun fetchAndDisplayIngredientCounts() {
        // Launch a coroutine in the IO dispatcher for making network requests.
        CoroutineScope(Dispatchers.IO).launch {
            // Fetch ingredients from the API.
            val response = RetrofitClient.apiService.getIngredients()

            if (response.isSuccessful) {
                // Get the list of ingredients from the response body.
                val ingredients = response.body()
                ingredients?.let { ingredientList ->
                    // Calculate ingredient counts based on their expiration dates.
                    val today = LocalDate.now()
                    var freshCount = 0
                    var expiringSoonCount = 0
                    var expiredCount = 0

                    for (ingredient in ingredientList) {
                        val expirationDate =
                            LocalDate.parse(ingredient.expirationDate, DateTimeFormatter.ISO_DATE)
                        when {
                            expirationDate.isAfter(today.plusDays(3)) -> freshCount++
                            expirationDate.isBefore(today) -> expiredCount++
                            else -> expiringSoonCount++
                        }
                    }

                    // Update UI on the main thread.
                    withContext(Dispatchers.Main) {
                        freshTextView.text = freshCount.toString()
                        expiringSoonTextView.text = expiringSoonCount.toString()
                        expiredTextView.text = expiredCount.toString()
                    }
                }
            } else {
                // Log the error or handle the unsuccessful response case.
                Log.e("InventoryFragment", "Failed to fetch ingredients: ${response.errorBody()?.string()}")
            }
        }
    }


    // Called after the view hierarchy associated with the fragment has been created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("InventoryFragmentLog", "this is the InventoryFragment onViewCreated")
        Log.d("flow", "this is the InventoryFragment onViewCreated")

        setupRecyclerView()
        setupViewModel()
        observeData()
        //syncIngredientsIfOnline()

    }
/*
    private fun syncIngredientsIfOnline() {
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            ingredientViewModel.syncUnsyncedIngredients()
        } else {
            // Show a message or UI element indicating offline mode
        }
    }
    */


    private fun setupRecyclerView() {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.Ingredient_recycler_view)
        recyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ingredientAdapter
        }
    }

    private fun setupViewModel() {
        ingredientViewModel = ViewModelProvider(requireActivity())[IngredientViewModel::class.java]

        // Load data based on network availability
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            // Online: Fetch from Firebase and sync to Room
            ingredientViewModel.fetchIngredientsFromFirebase()
            ingredientViewModel.observeIngredientChanges()
        } else if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            // Explicit Offline: Load from Room
            ingredientViewModel.loadFromRoomOffline()
        }
    }


    private fun observeData() {
        val noInventoryTextView = view?.findViewById<TextView>(R.id.no_inventory_text)
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
        val errorLayout = view?.findViewById<LinearLayout>(R.id.errorLayout)

        // Observe dataState for managing loading, success, and error states
        ingredientViewModel.dataState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DataResult.Loading -> {
                    //progressBar?.visibility = View.VISIBLE
                    errorLayout?.visibility = View.GONE
                }

                is DataResult.Success -> {
                   // progressBar?.visibility = View.GONE
                    errorLayout?.visibility = View.GONE

                    if (state.data.isEmpty()) {
                       // noInventoryTextView?.visibility = View.VISIBLE
                    } else {
                      //  noInventoryTextView?.visibility = View.GONE
                        ingredientAdapter.setIngredients(state.data)
                    }
                }

                is DataResult.Error -> {
                  //  progressBar?.visibility = View.GONE

                    if (NetworkUtils.isNetworkAvailable(requireContext())) {
                        // Show error state only if we're online
                        errorLayout?.visibility = View.VISIBLE
                        view?.findViewById<TextView>(R.id.errorText)?.text =
                            state.exception.localizedMessage ?: "An error occurred"

                        view?.findViewById<Button>(R.id.retryButton)?.setOnClickListener {
                            setupViewModel() // Retry loading data
                        }
                    } else if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                        // Explicit Offline: Load from Room
                        ingredientViewModel.loadFromRoomOffline()
                    }
                }
            }
        }

        // Observe filteredIngredients to ensure RecyclerView is updated when data changes
        ingredientViewModel.filteredIngredients.observe(viewLifecycleOwner, Observer { ingredients ->
            if (ingredients.isNullOrEmpty()) {
                noInventoryTextView?.visibility = View.VISIBLE
            } else {
                noInventoryTextView?.visibility = View.GONE
                ingredientAdapter.setIngredients(ingredients)
            }
        })

        // Observe sync state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ingredientViewModel.isSyncing.collect { isSyncing ->
                    progressBar?.visibility = if (isSyncing) View.VISIBLE else View.GONE
                }
            }
        }


      /*
        // Observe network changes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NetworkMonitor(requireContext()).isNetworkAvailable.collect { isAvailable ->
                    if (isAvailable) {
                        // Online: Sync with Firebase
                        ingredientViewModel.fetchIngredientsFromFirebase()
                        ingredientViewModel.observeIngredientChanges()
                    } else if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                        // Explicit Offline: Load from Room
                        ingredientViewModel.loadFromRoomOffline()
                    }
                }

            }
        }
       */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up any observers or callbacks if needed
    }

    // Companion object to create a new instance of InventoryFragment with arguments.
    companion object {

    }
}

/*


private fun observeData() {
    val noInventoryTextView = view?.findViewById<TextView>(R.id.no_inventory_text)
    val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
    val errorLayout = view?.findViewById<LinearLayout>(R.id.errorLayout)

    // Observe allIngredients directly for RoomDB updates
    ingredientViewModel.allIngredients.observe(viewLifecycleOwner) { ingredients ->
        progressBar?.visibility = View.GONE
        errorLayout?.visibility = View.GONE

        if (ingredients.isNullOrEmpty()) {
            noInventoryTextView?.visibility = View.VISIBLE
        } else {
            noInventoryTextView?.visibility = View.GONE
            ingredientAdapter.setIngredients(ingredients)
        }
    }
}



 */