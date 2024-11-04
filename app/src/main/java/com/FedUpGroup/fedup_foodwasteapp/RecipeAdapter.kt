package com.FedUpGroup.fedup_foodwasteapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class RecipeAdapter(
    private var recipeList: List<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit // Pass a lambda to handle clicks
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recipe_item, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.recipeTitle.text = recipe.title

        // Load the image using Picasso from the 'image' field
        Picasso.get()
            .load(recipe.image)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(holder.recipeImageView)

        // Set the text for the number of used and missing ingredients (if applicable)
        holder.usedIngredients.text = "Ingredients Used: ${recipe.usedIngredientCount}"
        holder.missedIngredients.text = "Ingredients Missing: ${recipe.missedIngredientCount}"

        // Handle click events
        holder.itemView.setOnClickListener {
            onRecipeClick(recipe)
        }
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    fun updateData(newRecipes: List<Recipe>) {
        val diffResult = DiffUtil.calculateDiff(RecipeDiffCallback(recipeList, newRecipes))
        recipeList = newRecipes
        diffResult.dispatchUpdatesTo(this)
    }


    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImageView: ImageView = itemView.findViewById(R.id.recipeImageView)
        val recipeTitle: TextView = itemView.findViewById(R.id.recipeTitle)
        val usedIngredients: TextView = itemView.findViewById(R.id.usedIngredients) // New TextView
        val missedIngredients: TextView = itemView.findViewById(R.id.missedIngredients) // New TextView

    }
}