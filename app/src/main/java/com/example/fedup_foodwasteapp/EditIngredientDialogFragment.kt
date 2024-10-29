package com.example.fedup_foodwasteapp

import android.app.DatePickerDialog
import android.app.Dialog
import com.example.fedup_foodwasteapp.Ingredient
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.fedup_foodwasteapp.databinding.FragmentEditIngredientBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class EditIngredientDialogFragment : DialogFragment() {

    private var _binding: FragmentEditIngredientBinding? = null
    private val binding get() = _binding!!
    private val categories = Category.values()
    private var currentCategoryIndex = 0
    lateinit var ingredientViewModel: IngredientViewModel

    private lateinit var ingredient: Ingredient

    // Listener for edit events
    private var listener: ((Ingredient) -> Unit)? = null

    /**
     * Sets the listener that will be triggered when the ingredient is edited.
     */
    fun setOnSaveListener(listener: (Ingredient) -> Unit) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            ingredient = it.getSerializable("ingredient") as Ingredient
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(R.color.grey) // Setting the background color directly
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditIngredientBinding.inflate(inflater, container, false)
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure that ingredient is properly initialized
        if (::ingredient.isInitialized) {
            // Populate the fields with the current ingredient details

            // Log the initial ingredient ID
            Log.d("EditIngredientDialog", "Initial ingredient ID: ${ingredient.id}")

            binding.etIngredientName.setText(ingredient.productName)
            binding.etQuantity.setText(ingredient.quantity)

            // Check if the ingredient has an expiration date
            val expirationDate = ingredient.expirationDate
            if (expirationDate.isNullOrEmpty()) {
                binding.etExpirationDate.setText("YYYY-MM-DD")  // Placeholder text if no date is set
            } else {
                binding.etExpirationDate.setText(expirationDate)  // Set the actual date if available
            }

            binding.tvCategory.text = ingredient.category
            // Uncomment if 'notes' field is available and included in your data class
            // binding.editText.setText(ingredient.notes)
        } else {
            // Handle the case where ingredient is not initialized (optional logging or error handling)
            Log.e("EditIngredientDialog", "Ingredient is not initialized properly")
        }

        // Set OnClickListener to show DatePickerDialog
        binding.etExpirationDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Handle save button click
        binding.btnSaveIngredient.setOnClickListener {
            // Capture the updated ingredient data
            val updatedIngredient = Ingredient(
                id = 0, // We'll get the correct ID in the repository
                productName = binding.etIngredientName.text.toString(),
                quantity = binding.etQuantity.text.toString(),
                expirationDate = binding.etExpirationDate.text.toString(),
                category = binding.tvCategory.text.toString(),
                firebaseId = ingredient.firebaseId, // Make sure to keep the firebase_id
                userId = ingredient.userId,
                isSynced = false
            )
                // If you have a 'notes' field, update it similarly
                // notes = binding.editText.text.toString()


            Log.d("EditIngredientDialog", "Original ingredient ID: ${ingredient.id}")
            Log.d("EditIngredientDialog", "Updated ingredient ID: ${updatedIngredient.id}")

            // Trigger the callback
            listener?.invoke(updatedIngredient)


            // Dismiss the dialog after saving
            dismiss()
        }
    }

    private fun updateCategoryDisplay() {
        binding.tvCategory.text = categories[currentCategoryIndex].displayName
    }

    private fun changeCategory(direction: Int) {
        currentCategoryIndex = (currentCategoryIndex + direction + categories.size) % categories.size
        updateCategoryDisplay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format the selected date and set it to the EditText
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.etExpirationDate.setText(formattedDate)
            },
            year, month, day
        )

        datePickerDialog.show()
    }
    companion object {

        fun newInstance(ingredient: Ingredient): EditIngredientDialogFragment {
            val fragment = EditIngredientDialogFragment()
            val args = Bundle()
            args.putSerializable("ingredient", ingredient)
            fragment.arguments = args
            return fragment
        }
    }
}
