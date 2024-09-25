package com.example.fedup_foodwasteapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
        imgCategory.setOnClickListener {
            // Show a dialog to select a category when the button is clicked.
            showCategorySelectionDialog()
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


        // Observe the filtered ingredients LiveData from the ViewModel.
        // Update the adapter when the data changes.
        ingredientViewModel.filteredIngredients.observe(viewLifecycleOwner, Observer { ingredients ->
            ingredientAdapter.setIngredients(ingredients)
        })

        return view
    }

    // Filters the ingredients by the selected category.
    private fun filterByCategory(category: String) {
        ingredientViewModel.filterIngredientsByCategory(category)
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
            filterByCategory(selectedCategory)
        }
        builder.show()
    }

    // Called after the view hierarchy associated with the fragment has been created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Re-initialize the RecyclerView and its adapter.
        val recyclerView = view.findViewById<RecyclerView>(R.id.Ingredient_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ingredientAdapter

        // Re-initialize the ViewModel.
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        // Fetch ingredients from Firebase and observe the LiveData
        ingredientViewModel.fetchIngredientsFromFirebase()

        // Observe the LiveData and update the adapter when data changes
        ingredientViewModel.filteredIngredients.observe(viewLifecycleOwner, Observer { ingredients ->
            ingredientAdapter.setIngredients(ingredients)

        })
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