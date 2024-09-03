package com.example.fedup_foodwasteapp

import com.example.fedup_foodwasteapp.Ingredients
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IngredientAdapter(
    private val context: Context,
    private val ingredientDao: IngredientDao
) : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    private var ingredients = emptyList<Ingredients>()

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

    fun setIngredients(ingredients: List<Ingredients>) {
        this.ingredients = ingredients
        notifyDataSetChanged()
    }

    private fun showPopupMenu(view: View, ingredient: Ingredients) {
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
                    showDeleteConfirmationDialog(ingredient)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showEditIngredientDialog(ingredient: Ingredients) {
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

    private fun showDeleteConfirmationDialog(ingredient: Ingredients) {
        AlertDialog.Builder(context)
            .setTitle("Delete Ingredient")
            .setMessage("Are you sure you want to delete ${ingredient.productName}?")
            .setPositiveButton("Yes") { _, _ ->
                // Perform the delete operation on a background thread
                CoroutineScope(Dispatchers.IO).launch {
                    // Delete the ingredient from the database
                    ingredientDao.delete(ingredient)

                    // Switch to the main thread to update the UI
                    withContext(Dispatchers.Main) {
                        // Update the list to reflect the deletion
                        setIngredients(ingredientDao.getAllIngredients().value ?: emptyList())
                        Toast.makeText(context, "${ingredient.productName} deleted.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateIngredient(updatedIngredient: Ingredients) {
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
