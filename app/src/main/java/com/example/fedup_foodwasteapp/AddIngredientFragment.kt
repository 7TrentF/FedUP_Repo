package com.example.fedup_foodwasteapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.fedup_foodwasteapp.R.*

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
    private var currentCategoryIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_ingredient, container, false)

        // Initialize ViewModel
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        // Observe the result of the insertion operation
        ingredientViewModel.insertResult.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(requireContext(), "Ingredient was added", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // Go back to previous fragment
            } else {
                Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })

        val tvCategory: TextView = view.findViewById(R.id.tv_category)
        val btnPlus: ImageButton = view.findViewById(R.id.btn_plus)
        val btnMinus: ImageButton = view.findViewById(R.id.btn_minus)

        // Categories array
        val categories = Category.values()

        // Initialize TextView with the first category
        tvCategory.text = categories[currentCategoryIndex].displayName

        // Plus button click
        btnPlus.setOnClickListener {
            currentCategoryIndex = (currentCategoryIndex + 1) % categories.size
            tvCategory.text = categories[currentCategoryIndex].displayName
        }

        // Minus button click
        btnMinus.setOnClickListener {
            currentCategoryIndex = if (currentCategoryIndex - 1 < 0) {
                categories.size - 1
            } else {
                currentCategoryIndex - 1
            }
            tvCategory.text = categories[currentCategoryIndex].displayName
        }

        // Save button click
        view.findViewById<ImageButton>(R.id.btn_save_ingredient).setOnClickListener {
            val name = view.findViewById<EditText>(R.id.et_ingredient_name).text.toString()
            val quantity = view.findViewById<EditText>(R.id.et_quantity).text.toString()
            val expirationDate = view.findViewById<EditText>(R.id.et_expiration_date).text.toString()
            val category = categories[currentCategoryIndex].displayName

            // Create Ingredient object and insert into the database
            val ingredient = Ingredients(
                productName = name,
                quantity = quantity,
                expirationDate = expirationDate,
                category = category
            )
            ingredientViewModel.insert(ingredient)
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
