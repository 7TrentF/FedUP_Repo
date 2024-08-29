package com.example.fedup_foodwasteapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import java.util.Calendar

class EditIngredientFragment : DialogFragment() {

    private lateinit var ingredientViewModel: IngredientViewModel
    private lateinit var tvCategory: TextView
    private lateinit var btnPlus: ImageButton
    private lateinit var btnMinus: ImageButton
    private var currentCategoryIndex = 0
    private var ingredientId: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_ingredient, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.color.grey)

        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        // Retrieve the ingredient details from the arguments
        val ingredientName = arguments?.getString("ingredientName") ?: ""
        val ingredientQuantity = arguments?.getString("ingredientQuantity") ?: ""
        val ingredientExpirationDate = arguments?.getString("ingredientExpirationDate") ?: ""
        val ingredientCategory = arguments?.getString("ingredientCategory") ?: ""
        ingredientId = arguments?.getLong("ingredientId") ?: -1

        // Initialize UI components
        tvCategory = view.findViewById(R.id.tv_category)
        btnPlus = view.findViewById(R.id.btn_plus)
        btnMinus = view.findViewById(R.id.btn_minus)
        val expirationDateEditText = view.findViewById<EditText>(R.id.et_expiration_date)

        // Set initial values in UI
        view.findViewById<EditText>(R.id.et_ingredient_name).setText(ingredientName)
        view.findViewById<EditText>(R.id.et_quantity).setText(ingredientQuantity)
        expirationDateEditText.setText(ingredientExpirationDate)

        // Categories array
        val categories = Category.entries.toTypedArray()
        currentCategoryIndex = categories.indexOfFirst { it.displayName == ingredientCategory }
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

        // Date picker for expiration date
        expirationDateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    expirationDateEditText.setText(formattedDate)
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        // Save button click
        view.findViewById<ImageButton>(R.id.btn_save_ingredient).setOnClickListener {
            val name = view.findViewById<EditText>(R.id.et_ingredient_name).text.toString()
            val quantity = view.findViewById<EditText>(R.id.et_quantity).text.toString()
            val category = categories[currentCategoryIndex].displayName
            val expirationDate = expirationDateEditText.text.toString()

            // Create Ingredient object with updated data
            val updatedIngredient = Ingredients(
                id = ingredientId,  // Make sure you use the correct ID for the existing ingredient
                productName = name,
                quantity = quantity,
                expirationDate = expirationDate,
                category = category
            )

            // Call the update method in the ViewModel
            ingredientViewModel.update(updatedIngredient)

            Toast.makeText(requireContext(), "Ingredient updated successfully", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        return view
    }
}
