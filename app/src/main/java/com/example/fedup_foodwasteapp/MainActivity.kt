package com.example.fedup_foodwasteapp

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.navigation.fragment.NavHostFragment


class MainActivity : AppCompatActivity() {

    private lateinit var addFabItem: FloatingActionButton
    private lateinit var categoryFab: FloatingActionButton
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_animate) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_animate) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom_animate) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom_animate) } // Corrected from_bottom_animate

    private var clicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        addFabItem = findViewById(R.id.add_fab_item)
        categoryFab = findViewById(R.id.category_fab)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, InventoryFragment())
                .commit()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHome = findViewById<LinearLayout>(R.id.nav_inventory)
        val navSearch = findViewById<LinearLayout>(R.id.nav_Recipe)
        val navProfile = findViewById<LinearLayout>(R.id.nav_settings)

        addFabItem.setOnClickListener {
            onAddButtonClicked()
            loadFragment(AddIngredientFragment())
        }

        categoryFab.setOnClickListener {
            Toast.makeText(this, "Category button clicked", Toast.LENGTH_SHORT).show()
        }

        navHome.setOnClickListener {
            loadFragment(InventoryFragment())
            updateSelectedNavItem(R.id.nav_inventory)
        }

        navSearch.setOnClickListener {
            loadFragment(RecipeFragment())
            updateSelectedNavItem(R.id.nav_Recipe)
        }

        navProfile.setOnClickListener {
            loadFragment(SettingsFragment())
            updateSelectedNavItem(R.id.nav_settings)
        }
    }

    private fun onAddButtonClicked() {
        setVisibility(clicked)
        setAnimation(clicked)
        clicked = !clicked
    }

    private fun setAnimation(clicked: Boolean) {
        if (!clicked) {
            categoryFab.startAnimation(fromBottom)
            addFabItem.startAnimation(rotateOpen)
        } else {
            categoryFab.startAnimation(toBottom)
            addFabItem.startAnimation(rotateClose)
        }
    }

    private fun setVisibility(clicked: Boolean) {
        if (!clicked) {
            categoryFab.visibility = View.VISIBLE
        } else {
            categoryFab.visibility = View.INVISIBLE
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun updateSelectedNavItem(selectedItemId: Int) {
        // Reset all items to default state
        resetNavItem(R.id.nav_inventory, R.id.nav_inventory_icon, R.id.nav_inventory_text)
        resetNavItem(R.id.nav_Recipe, R.id.nav_Recipe_icon, R.id.nav_Recipe_text)
        resetNavItem(R.id.nav_settings, R.id.nav_settings_icon, R.id.nav_settings_text)

        // Highlight the selected item
        highlightSelectedItem(selectedItemId)
    }

    private fun resetNavItem(layoutId: Int, iconId: Int, textId: Int) {
        findViewById<ImageView>(iconId).setColorFilter(ContextCompat.getColor(this, R.color.white))
        findViewById<TextView>(textId).setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun highlightSelectedItem(selectedItemId: Int) {
        val selectedIconId: Int
        val selectedTextId: Int

        when (selectedItemId) {
            R.id.nav_inventory -> {
                selectedIconId = R.id.nav_inventory_icon
                selectedTextId = R.id.nav_inventory_text
            }
            R.id.nav_Recipe -> {
                selectedIconId = R.id.nav_Recipe_icon
                selectedTextId = R.id.nav_Recipe_text
            }
            R.id.nav_settings -> {
                selectedIconId = R.id.nav_settings_icon
                selectedTextId = R.id.nav_settings_text
            }
            else -> return
        }

        // Set selected colors for the icon and text
        findViewById<ImageView>(selectedIconId).setColorFilter(ContextCompat.getColor(this, R.color.green))
        findViewById<TextView>(selectedTextId).setTextColor(ContextCompat.getColor(this, R.color.green))
    }
}
