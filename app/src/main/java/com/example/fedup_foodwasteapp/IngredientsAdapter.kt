package com.example.fedup_foodwasteapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IngredientAdapter : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    private var ingredients = emptyList<Ingredients>()  // Change to Ingredients

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
    }

    override fun getItemCount(): Int = ingredients.size

    fun setIngredients(ingredients: List<Ingredients>) {  // Change to List<Ingredients>
        this.ingredients = ingredients
        notifyDataSetChanged()
    }

    class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_ingredient_name)
        val quantityTextView: TextView = itemView.findViewById(R.id.tv_quantity)
        val expirationDateTextView: TextView = itemView.findViewById(R.id.tv_expiration_date)
        val categoryTextView: TextView = itemView.findViewById(R.id.tv_category)
    }
}


