package com.example.fedup_foodwasteapp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.fedup_foodwasteapp.R.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.google.android.material.snackbar.Snackbar
import retrofit2.Response

class AddIngredientFragment : DialogFragment() {

    lateinit var ingredientViewModel: IngredientViewModel
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

            insertIngredient(
                name,
                quantity,
                categories[currentCategoryIndex].displayName,
                expirationDate,
                requireContext() // Pass context for network check
            )
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

    fun insertIngredient(
        name: String,
        quantity: String,
        category: String,
        expirationDate: String,
        context: Context // Pass context for network check
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            if (name.isBlank() || quantity.isBlank() || category.isBlank() || expirationDate.isBlank()) {
                // Show Snackbar for empty fields
                Snackbar.make(requireView(), "All fields are required.", Snackbar.LENGTH_LONG).show()
                return
            }

            val ingredient = Ingredient(
                id =0,
                productName = name,
                quantity = quantity,
                expirationDate = expirationDate,
                category = category,
                userId = user.uid, // Associate with user ID
                isSynced = false, // Default to unsynced for offline-first
            )

            // Add to RoomDB first
            GlobalScope.launch {
                ingredientViewModel.insertIngredient(ingredient)
            }

            if (NetworkUtils.isNetworkAvailable(context)) {
                // Insert ingredient via REST API if online
                AuthManager.getInstance().getIdToken { token, error ->
                    if (token != null) {
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                val response = RetrofitClient.apiService.addIngredient(ingredient)
                                if (response.isSuccessful) {
                                    val createdIngredient = response.body()
                                    if (createdIngredient != null) {
                                        // Update the ingredient object with the Firebase ID
                                        ingredient.firebaseId = createdIngredient.firebaseId
                                        ingredient.isSynced = true // Mark as synced

                                        // Update the RoomDB with the Firebase ID and sync status
                                        ingredientViewModel.updateIngredient(ingredient)

                                        withContext(Dispatchers.Main) {
                                            Snackbar.make(
                                                requireView(),
                                                "Ingredient added successfully!",
                                                Snackbar.LENGTH_LONG
                                            ).show()
                                            dismiss() // Close dialog
                                        }
                                    }
                                } else {
                                    handleApiError(response)
                                }
                            } catch (e: Exception) {
                                handleException(e)
                            }
                        }
                    } else {
                        Snackbar.make(requireView(), "Error retrieving token: $error", Snackbar.LENGTH_LONG).show()
                    }
                }
            } else {
                Snackbar.make(requireView(), "No network. Saved offline.", Snackbar.LENGTH_LONG).show()
            }
        } else {
            Snackbar.make(requireView(), "User not authenticated.", Snackbar.LENGTH_LONG).show()
        }
    }


    // Helper function for error handling
    private suspend fun handleApiError(response: Response<Ingredient>) {
        val errorMessage = response.errorBody()?.string()
        Log.e("API Error", "Error adding ingredient: $errorMessage")
        withContext(Dispatchers.Main) {
            Snackbar.make(requireView(), "Error adding ingredient: $errorMessage", Snackbar.LENGTH_LONG).show()
        }
    }

    // Helper function for exception handling
    private suspend fun handleException(e: Exception) {
        Log.e("Exception", "Exception: ${e.message}", e)
        withContext(Dispatchers.Main) {
            Snackbar.make(requireView(), "Exception: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showCustomSnackbar(message: String, ingredientId: String) {
        // Create Snackbar
        val snackbar = Snackbar.make(
            (context as Activity).findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        )

        // Customize Snackbar
        snackbar.setBackgroundTint(ContextCompat.getColor(context as Activity, R.color.darkgrey))
        snackbar.setActionTextColor(ContextCompat.getColor(context as Activity, R.color.white))

        // Get the TextView from the Snackbar and change the text color
        val snackbarView = snackbar.view
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(context as Activity, R.color.red))

        // Show the Snackbar
        snackbar.show()
    }

}