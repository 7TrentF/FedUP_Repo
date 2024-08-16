package com.example.fedup_foodwasteapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AddIngredientFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddIngredientFragment : Fragment() {
    private lateinit var ingredientViewModel: IngredientViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_ingredient, container, false)

        // Initialize ViewModel
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        // Setup category spinner
        val spinner: Spinner = view.findViewById(R.id.spinner_category)
        val categories = Category.values().map { it.displayName }
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinner.adapter = adapter

        // Save button click
        view.findViewById<Button>(R.id.btn_save_ingredient).setOnClickListener {
            val name = view.findViewById<EditText>(R.id.et_ingredient_name).text.toString()
            val quantity = view.findViewById<EditText>(R.id.et_quantity).text.toString()
            val expirationDate = view.findViewById<EditText>(R.id.et_expiration_date).text.toString()
            val category = spinner.selectedItem.toString()

            // Create Ingredient object and insert into the database
            val ingredient = Ingredients(
                productName = name,
                quantity = quantity,
                expirationDate = expirationDate,
                category = category
            )
            ingredientViewModel.insert(ingredient)

            // Close fragment or show confirmation
            parentFragmentManager.popBackStack()
        }
        return view
    }

    companion object {
        fun newInstance(param1: String, param2: String) =
            AddIngredientFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
