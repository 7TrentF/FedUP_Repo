package com.FedUpGroup.fedup_foodwasteapp
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IngredientAdapter(
    private val context: Context,
    private val ingredientDao: IngredientDao,
    private val ingredientViewModel: IngredientViewModel

) : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    private var ingredients = emptyList<Ingredient>()
    private var Ingredients = mutableListOf<Ingredient>()

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
        if (ingredients.isEmpty()) {

        } else {
        }
        notifyDataSetChanged()
    }

    fun updateIngredients(newList: List<Ingredient>) {
        ingredients = newList
        if (ingredients.isEmpty()) {

        } else {
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
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        //roomDB update
                        ingredientViewModel.updateIngredientDetails(updatedIngredient)

                        val response = RetrofitClient.apiService.updateIngredient(
                            updatedIngredient.firebaseId, updatedIngredient
                        )
                        if (response.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                Snackbar.make(
                                    fragmentActivity.findViewById(android.R.id.content),
                                    "Ingredient updated successfully",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                        }
                    } catch (e: Exception) {
                    }
                }
            }
            editDialogFragment.show(fragmentActivity.supportFragmentManager, "EditIngredientDialogFragment")
        } ?: run {
        }
    }

    private fun showDeleteConfirmationDialog(ingredientId: String) {
        AlertDialog.Builder(context)
            .setTitle("Delete Ingredient")
            .setMessage("Are you sure you want to delete this ingredient?")
            .setPositiveButton("Yes") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (NetworkUtils.isNetworkAvailable(context)) {
                            // Online flow
                            deleteIngredientFromFirebase(ingredientId)
                        } else {
                            // Offline flow
                            deleteIngredientFromRoom(ingredientId)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
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

                ingredientViewModel.deleteIngredientByFirebaseId(firebaseId)
               // deleteIngredientFromRoom(firebaseId)

                // Call the API to delete the ingredient by its Firebase ID
                val response = RetrofitClient.apiService.deleteIngredient(firebaseId)

                if (response.isSuccessful) {


                    withContext(Dispatchers.Main) {
                        showCustomSnackbar("Ingredient deleted.", firebaseId)
                    }

                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"

                    withContext(Dispatchers.Main) {
                        Snackbar.make((context as Activity).findViewById(android.R.id.content), "Error deleting ingredient", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {

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
        try {

            // First get the ingredient from Room
            val ingredient = ingredientDao.getIngredientByFirebaseId(firebaseId)

            val roomId = ingredientDao.getIngredientByFirebaseId(firebaseId)


            if (ingredient != null) {
                // Instead of immediate deletion, mark it for deletion
                ingredient.apply {
                    isDeleted = true     // Mark for deletion
                    isSynced = false     // Mark as unsynced so it will be processed when online
                    lastModified = System.currentTimeMillis()
                    version += 1         // Increment version
                }

                // Update the ingredient in Room
                ingredientDao.update(ingredient)

            } else {
            }
        } catch (e: Exception) {
            throw e
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


    class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_ingredient_name)
        val quantityTextView: TextView = itemView.findViewById(R.id.tv_quantity)
        val expirationDateTextView: TextView = itemView.findViewById(R.id.tv_expiration_date)
        val categoryTextView: TextView = itemView.findViewById(R.id.tv_category)
        val optionsButton: ImageButton = itemView.findViewById(R.id.options_button)
    }
}