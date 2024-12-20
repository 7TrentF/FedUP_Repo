package com.FedUpGroup.fedup_foodwasteapp

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.FedUpGroup.fedup_foodwasteapp.databinding.FragmentEditIngredientBinding
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
        }

        // Set OnClickListener to show DatePickerDialog
        binding.etExpirationDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Handle save button click
        binding.btnSaveIngredient.setOnClickListener {

            // Capture the updated ingredient data
            val updatedIngredient = Ingredient(
                id = ingredient.id, // We'll get the correct ID in the repository
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

            listener?.invoke(updatedIngredient)


            // Dismiss the dialog after saving
            dismiss()
        }
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
