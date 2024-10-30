package com.example.fedup_foodwasteapp
import android.app.Activity
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class IngredientAdapter(
    private val context: Context,
    private val ingredientDao: IngredientDao,
    private val ingredientViewModel: IngredientViewModel

) : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    private var ingredients = emptyList<Ingredient>()

    private val authManager = AuthManager.getInstance()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ingredient_item, parent, false)
        Log.d("flow", "this is the IngredientAdapter")

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
        if (ingredients.isEmpty()) {
            Log.w("IngredientAdapter", "No ingredients available to display in RecyclerView.")
            Log.w("flow", "No ingredients available to display in RecyclerView.")


        } else {
            Log.d("IngredientAdapter", "Setting ${ingredients.size} ingredients in RecyclerView.")
            Log.d("flow", "Setting ${ingredients.size} ingredients in RecyclerView.")

        }
        notifyDataSetChanged()
    }

    fun updateIngredients(newList: List<Ingredient>) {
        ingredients = newList
        if (ingredients.isEmpty()) {
            Log.w("IngredientAdapter", "No updated ingredients available to display in RecyclerView.")
            Log.w("flow", "No  updated ingredients available to display in RecyclerView.")


        } else {
            Log.d("IngredientAdapter", "Setting updateIngredients ${ingredients.size} ingredients in RecyclerView.")
            Log.d("flow", "Setting updateIngredients ${ingredients.size} ingredients in RecyclerView.")

        }
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
        fragmentActivity?.let {
            val editDialogFragment = EditIngredientDialogFragment.newInstance(ingredient)
            // Set the listener to handle the update
            editDialogFragment.setOnSaveListener { updatedIngredient ->
                Log.d("EditIngredientDialog", "Received ingredient for update - ID: ${updatedIngredient.id}")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ingredientViewModel.updateIngredient(updatedIngredient)
                        val response = RetrofitClient.apiService.updateIngredient(
                            updatedIngredient.firebaseId!!, updatedIngredient
                        )
                        if (response.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                Log.d("EditIngredientDialog", "API update successful for ingredient: ${updatedIngredient.id}")
                                Snackbar.make(
                                    fragmentActivity.findViewById(android.R.id.content),
                                    "Ingredient updated successfully",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Log.e("EditIngredientDialog", "API update failed: ${response.message()}")
                        }
                    } catch (e: Exception) {
                        Log.e("EditIngredientDialog", "Update failed due to exception", e)
                    }
                }
            }
            editDialogFragment.show(fragmentActivity.supportFragmentManager, "EditIngredientDialogFragment")
        } ?: run {
            Log.e("EditIngredientDialog", "FragmentActivity is null")
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
                            showCustomSnackbar("Ingredient deleted.", ingredientId)
                        }
                    } catch (e: Exception) {
                        Log.e("DeleteIngredientException", "Exception while deleting ingredient: ${e.message}", e)

                        withContext(Dispatchers.Main) {
                            Snackbar.make((context as Activity).findViewById(android.R.id.content), "Failed to delete ingredient: ${e.message}", Snackbar.LENGTH_SHORT).show()
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


                //  ingredientViewModel.deleteIngredient()
                deleteIngredientFromRoom(firebaseId)

                // Call the API to delete the ingredient by its Firebase ID
                val response = RetrofitClient.apiService.deleteIngredient(firebaseId)

                if (response.isSuccessful) {


                    withContext(Dispatchers.Main) {
                        showCustomSnackbar("Ingredient deleted.", firebaseId)
                    }

                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("DeleteIngredientError", "Error deleting ingredient: $firebaseId")

                    withContext(Dispatchers.Main) {
                        Snackbar.make((context as Activity).findViewById(android.R.id.content), "Error deleting ingredient", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("DeleteIngredientException", "Exception while deleting ingredient: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    Snackbar.make((context as Activity).findViewById(android.R.id.content), "Failed to delete ingredient: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }

            }
        } else {
            Log.e("DeleteIngredientError", "User not authenticated.")

            withContext(Dispatchers.Main) {
                Snackbar.make((context as Activity).findViewById(android.R.id.content), "User not authenticated", Snackbar.LENGTH_SHORT).show()
            }

        }
    }

    private suspend fun deleteIngredientFromRoom(firebaseId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Log.d(
                    "DeleteIngredientDebug",
                    "Attempting to delete ingredient with Firebase ID: $firebaseId"
                )

                val ingredientToDelete = ingredientViewModel.getIngredientByFirebaseId(firebaseId)

                if (ingredientToDelete != null) {
                    ingredientViewModel.deleteIngredientByFirebaseId(firebaseId)
                    withContext(Dispatchers.Main) {
                        showCustomSnackbar("Ingredient deleted.", firebaseId)
                    }
                } else {
                    Log.e(
                        "DeleteIngredientError",
                        "Ingredient with Firebase ID $firebaseId not found in local database"
                    )
                    withContext(Dispatchers.Main) {
                        Snackbar.make(
                            (context as Activity).findViewById(android.R.id.content),
                            "Ingredient not found in local database",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "DeleteIngredientException",
                    "Exception while deleting ingredient: ${e.message}",
                    e
                )
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        (context as Activity).findViewById(android.R.id.content),
                        "Failed to delete ingredient: ${e.message}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showCustomSnackbar(message: String, ingredientId: String) {
        // Create Snackbar
        val snackbar = Snackbar.make(
            (context as Activity).findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        ).setAction("UNDO") {
            // Add undo action if needed
            undoDelete(ingredientId)
        }

        // Customize Snackbar
        snackbar.setBackgroundTint(ContextCompat.getColor(context, R.color.darkgrey))
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.white))

        // Get the TextView from the Snackbar and change the text color
        val snackbarView = snackbar.view
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(context, R.color.red))

        // Show the Snackbar
        snackbar.show()
    }

    private fun undoDelete(ingredientId: String) {
        // Logic to undo delete (you can implement this as needed)
    }

    private fun deleteIngredient(updatedIngredient: Ingredient) {

    }






    class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_ingredient_name)
        val quantityTextView: TextView = itemView.findViewById(R.id.tv_quantity)
        val expirationDateTextView: TextView = itemView.findViewById(R.id.tv_expiration_date)
        val categoryTextView: TextView = itemView.findViewById(R.id.tv_category)
        val optionsButton: ImageButton = itemView.findViewById(R.id.options_button)
    }
}