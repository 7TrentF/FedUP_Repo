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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.fedup_foodwasteapp.R.*
import java.util.Calendar

// TODO: Rename parameter arguments, choose names that match

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AddIngredientFragment : DialogFragment() {

    private lateinit var ingredientViewModel: IngredientViewModel
    private lateinit var tvCategory: TextView
    private lateinit var btnPlus: ImageButton
    private lateinit var btnMinus: ImageButton
    private var currentCategoryIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_ingredient, container, false)
        // Set the dialog background color programmatically
        dialog?.window?.setBackgroundDrawableResource(R.color.grey)
        // Initialize ViewModel
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        // Observe the result of the insertion operation
        ingredientViewModel.insertResult.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(requireContext(), "Ingredient was added", Toast.LENGTH_SHORT).show()
                dismiss() // Close the dialog
            } else {
                Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })

         tvCategory = view.findViewById(R.id.tv_category)
         btnPlus    = view.findViewById(R.id.btn_plus)
         btnMinus   = view.findViewById(R.id.btn_minus)
         val expirationDateEditText = view.findViewById<EditText>(R.id.et_expiration_date)        // Categories array
         val categories = Category.entries.toTypedArray()

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

        expirationDateEditText.setOnClickListener {
            // Get current date
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Create and show the DatePickerDialog
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Update EditText with the selected date
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
}