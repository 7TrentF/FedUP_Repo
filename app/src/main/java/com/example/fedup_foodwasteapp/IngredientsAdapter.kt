package com.example.fedup_foodwasteapp
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class IngredientAdapter(
    private val context: Context,
    private val ingredientDao: IngredientDao
) : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    private var ingredients = emptyList<Ingredient>()
    private val authManager = AuthManager.getInstance()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ingredient_item, parent, false)
        return IngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val current = ingredients[position]
        holder.nameTextView.text = current.productName
        holder.quantityTextView.text = current.quantity
        holder.expirationDateTextView.text = current.expirationDate
        holder.categoryTextView.text = current.category


        // Handle the options button click to show popup menu
        holder.optionsButton.setOnClickListener {
            showPopupMenu(it, current)
        }
    }

    override fun getItemCount(): Int = ingredients.size

    fun setIngredients(ingredients: List<Ingredient>) {
        this.ingredients = ingredients
        notifyDataSetChanged()
    }

    private fun showPopupMenu(view: View, ingredient: Ingredient) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(context, view)
        popupMenu.inflate(R.menu.ingredient_options_menu)

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    // Show the edit dialog
                    showEditIngredientDialog(ingredient)
                    true
                }
                R.id.delete -> {
                    // Show confirmation dialog before deleting
                    showDeleteConfirmationDialog(ingredient.firebaseId) // Use the Firebase ID from the ingredient
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showEditIngredientDialog(ingredient: Ingredient) {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null) {
            try {
                val editDialogFragment = EditIngredientDialogFragment.newInstance(ingredient)

                // Set the listener to handle the update
                editDialogFragment.setOnSaveListener { updatedIngredient ->
                    // Perform the update operation in a coroutine
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            ingredientDao.update(updatedIngredient)

                            // Switch to the main thread to observe LiveData
                            withContext(Dispatchers.Main) {
                                ingredientDao.getAllIngredients().observe(fragmentActivity) { updatedList ->
                                    setIngredients(updatedList)
                                }
                            }
                        } catch (e: Exception) {
                            // Log or handle the exception
                            Log.e("EditIngredient", "Update failed", e)
                        }
                    }
                }

                // Show the dialog fragment
                editDialogFragment.show(fragmentActivity.supportFragmentManager, "EditIngredientDialogFragment")
            } catch (e: Exception) {
                // Log or handle the exception
                Log.e("EditIngredient", "Dialog creation failed", e)
                Toast.makeText(context, "Unable to edit ingredient.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Unable to edit ingredient.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(ingredientId: String) {
        AlertDialog.Builder(context)
            .setTitle("Delete Ingredient")
            .setMessage("Are you sure you want to delete this ingredient?")
            .setPositiveButton("Yes") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Call the method to delete the ingredient via the API using its Firebase ID
                        deleteIngredientFromFirebase(ingredientId)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ingredient deleted.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("DeleteIngredientException", "Exception while deleting ingredient: ${e.message}", e)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to delete ingredient: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private suspend fun deleteIngredientFromFirebase(firebaseId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                Log.d("DeleteIngredientDebug", "Attempting to delete ingredient with Firebase ID: $firebaseId")

                // Call the API to delete the ingredient by its Firebase ID
                val response = RetrofitClient.apiService.deleteIngredient(firebaseId)

                if (response.isSuccessful) {
                    Log.d("DeleteIngredient", "Ingredient successfully deleted from Firebase with ID: $firebaseId")
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("DeleteIngredientError", "Error deleting ingredient: $firebaseId")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to delete ingredient: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("DeleteIngredientException", "Exception while deleting ingredient: ${e.message}", e)
            }
        } else {
            Log.e("DeleteIngredientError", "User not authenticated.")
        }
    }





    private fun updateIngredient(updatedIngredient: Ingredient) {
        CoroutineScope(Dispatchers.IO).launch {
            // Update the ingredient in the database
            ingredientDao.update(updatedIngredient)

            // Switch to the main thread to update the UI
            withContext(Dispatchers.Main) {
                // Update the list to reflect the updated ingredient
                setIngredients(ingredientDao.getAllIngredients().value ?: emptyList())
                Toast.makeText(context, "${updatedIngredient.productName} updated.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_ingredient_name)
        val quantityTextView: TextView = itemView.findViewById(R.id.tv_quantity)
        val expirationDateTextView: TextView = itemView.findViewById(R.id.tv_expiration_date)
        val categoryTextView: TextView = itemView.findViewById(R.id.tv_category)
        val optionsButton: ImageButton = itemView.findViewById(R.id.options_button)
    }
}