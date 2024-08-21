package com.example.fedup_foodwasteapp

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.navigation.fragment.NavHostFragment
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

class MainActivity : AppCompatActivity() {

    private lateinit var addFabItem: FloatingActionButton
    private lateinit var categoryFab: FloatingActionButton
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_animate) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_animate) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom_animate) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom_animate) } // Corrected from_bottom_animate
    private lateinit var ingredientViewModel: IngredientViewModel
    private lateinit var FrameContainer : FrameLayout
    private lateinit var recipeContainer: FrameLayout
    private var clicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)
         recipeContainer = findViewById(R.id.fragment_Recipe_container)
        FrameContainer  = findViewById(R.id.fragment_container)



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

            val navInventory = findViewById<LinearLayout>(R.id.nav_inventory)
            val navRecipe = findViewById<LinearLayout>(R.id.nav_Recipe)
            val navSettings = findViewById<LinearLayout>(R.id.nav_settings)

            addFabItem.setOnClickListener {
                onAddButtonClicked()
               // AddIngredientDialog() // Show the dialog instead of loading a fragment
            }

            categoryFab.setOnClickListener {
                Toast.makeText(this, "Category button clicked", Toast.LENGTH_SHORT).show()
            }

            navInventory.setOnClickListener {
                loadFragment(InventoryFragment())
                updateSelectedNavItem(R.id.nav_inventory)
            }

            navRecipe.setOnClickListener {
                loadRecipeFragment(RecipeFragment())
                updateSelectedNavItem(R.id.nav_Recipe)
            }

            navSettings.setOnClickListener {
                loadFragment(SettingsFragment())
                updateSelectedNavItem(R.id.nav_settings)
            }

    }

    private fun onAddButtonClicked() {
        //setVisibility(clicked)
        //setAnimation(clicked)
        clicked = !clicked

        if (clicked) {
            // Show the dialog when the button is clicked
            val dialog = AddIngredientFragment()
            dialog.show(supportFragmentManager, "AddIngredientDialog")
        }
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
        recipeContainer.visibility = View.GONE // Ensure the container is visible
        FrameContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
           // .addToBackStack(null) // Optional: Adds the transaction to the back stack
            .commit()

       // adjustContainerSize(fullScreen)
    }

    private fun loadRecipeFragment(fragment: Fragment) {
         FrameContainer.visibility = View.GONE
        recipeContainer.visibility = View.VISIBLE // Ensure the container is visible

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_Recipe_container, fragment)
            // .addToBackStack(null) // Optional: Adds the transaction to the back stack
            .commit()
        // adjustContainerSize(fullScreen)
    }

    private fun adjustContainerSize(fullScreen: Boolean) {
        val container = findViewById<FrameLayout>(R.id.fragment_container)
        val params = container.layoutParams as ConstraintLayout.LayoutParams

        if (fullScreen) {
            params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            params.height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        } else {
            params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        }

        container.layoutParams = params
    }

    private fun updateFabVisibility(fragment: Fragment) {
        when (fragment) {
            is InventoryFragment -> addFabItem.visibility = View.VISIBLE
            is AddIngredientFragment -> addFabItem.visibility = View.GONE
            else -> addFabItem.visibility = View.GONE
        }
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
