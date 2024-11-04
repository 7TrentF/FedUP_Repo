package com.FedUpGroup.fedup_foodwasteapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

// IconSelectorDialog.kt
class IconSelectorDialog : DialogFragment() {
    private var onIconSelectedListener: ((Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_icon_selector, container, false)
        setupRecyclerView(view)
        return view
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.iconRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 4)
        recyclerView.adapter = IconAdapter(getIconList()) { iconResId ->
            onIconSelectedListener?.invoke(iconResId)
            dismiss()
        }
    }

    private fun getIconList(): List<Int> {
        return listOf(
            R.drawable.ic_fruit,
            R.drawable.ic_vegetable,
            R.drawable.ic_meat,
            R.drawable.ic_dairy,
            R.drawable.ic_grain,
            R.drawable.ic_beverage,
            R.drawable.ic_fridge,
            R.drawable.ic_condiment,
            // Add more icons as needed
        )
    }

    fun setOnIconSelectedListener(listener: (Int) -> Unit) {
        onIconSelectedListener = listener
    }
}