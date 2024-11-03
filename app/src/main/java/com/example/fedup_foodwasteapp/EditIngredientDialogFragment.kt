package com.example.fedup_foodwasteapp

import android.app.DatePickerDialog
import android.app.Dialog
import com.example.fedup_foodwasteapp.Ingredient
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
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
    private lateinit var spinner : Spinner
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



// In your Fragment
            spinner = view.findViewById<Spinner>(R.id.spinnerUnit)
            val units = arrayOf("kg", "g", "lb", "oz", "L", "mL", "units")

            val textColor = ContextCompat.getColor(requireContext(), R.color.white)

// Use requireContext() instead of 'this' since we're in a Fragment
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, units.toList())
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    val selectedUnit = units[pos]
                    (view as? TextView)?.setTextColor(textColor)            }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Another interface callback
                }
            }




            binding.etIngredientName.setText(ingredient.productName)






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

            val selectedUnit = spinner.selectedItem.toString()

            val quantityValue = binding.etQuantity.setText(ingredient.quantity)

            // Combine quantity and unit
            val quantity = "$quantityValue $selectedUnit"
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

            Log.d("EditIngredientDialog", "Original ingredient ID: ${ingredient.id}")
            Log.d("EditIngredientDialog", "Updated ingredient ID: ${updatedIngredient.id}")

            // Trigger the callback
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
