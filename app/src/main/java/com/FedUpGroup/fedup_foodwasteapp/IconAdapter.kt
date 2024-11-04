package com.FedUpGroup.fedup_foodwasteapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

// IconAdapter.kt
class IconAdapter(
    private val icons: List<Int>,
    private val onIconClick: (Int) -> Unit
) : RecyclerView.Adapter<IconAdapter.IconViewHolder>() {

    class IconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iconImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val icon = icons[position]
        holder.imageView.setImageResource(icon)
        holder.itemView.setOnClickListener { onIconClick(icon) }
    }

    override fun getItemCount() = icons.size
}