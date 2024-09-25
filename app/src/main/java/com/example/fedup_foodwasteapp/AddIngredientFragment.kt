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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.fedup_foodwasteapp.R.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

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
        dialog?.window?.setBackgroundDrawableResource(R.color.grey)

        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)
        tvCategory = view.findViewById(R.id.tv_category)
        btnPlus = view.findViewById(R.id.btn_plus)
        btnMinus = view.findViewById(R.id.btn_minus)
        val expirationDateEditText = view.findViewById<EditText>(R.id.et_expiration_date)
        val categories = Category.entries.toTypedArray()
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
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format(
                        "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay
                    )
                    expirationDateEditText.setText(formattedDate)
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        view.findViewById<ImageButton>(R.id.btnSaveIngredient).setOnClickListener {
            val name = view.findViewById<EditText>(R.id.et_ingredient_name).text.toString()
            val quantity = view.findViewById<EditText>(R.id.et_quantity).text.toString()
            val expirationDate = expirationDateEditText.text.toString()

            insertIngredient(name, quantity, categories[currentCategoryIndex].displayName, expirationDate)
        }

        ingredientViewModel.insertResult.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(requireContext(), "Ingredient was added", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })
        return view
    }

    private fun insertIngredient(name: String, quantity: String, category: String, expirationDate: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val ingredient = Ingredient(
                productName = name,
                quantity = quantity,
                expirationDate = expirationDate,
                category = category,
                userId = user.uid
            )

            // Insert ingredient via ViewModel (this will handle both RoomDB and Firebase sync)
            ingredientViewModel.insert(ingredient)
            dismiss()
        } else {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }
}