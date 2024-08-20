package com.example.fedup_foodwasteapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [InventoryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class InventoryFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private lateinit var ingredientViewModel: IngredientViewModel
    private lateinit var ingredientAdapter: IngredientAdapter

    private lateinit var imgCategory: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inventory1, container, false)

        // Initialize views
        val recyclerView: RecyclerView = view.findViewById(R.id.Ingredient_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        ingredientAdapter = IngredientAdapter()
        recyclerView.adapter = ingredientAdapter

        // Initialize ViewModel
        ingredientViewModel = ViewModelProvider(requireActivity()).get(IngredientViewModel::class.java)

        // Observe filtered ingredients
        ingredientViewModel.filteredIngredients.observe(viewLifecycleOwner, Observer { ingredients ->
            ingredientAdapter.setIngredients(ingredients)
        })

        // Initially observe all ingredients if needed
        ingredientViewModel.allIngredients.observe(viewLifecycleOwner, Observer { ingredients ->
            ingredientAdapter.setIngredients(ingredients)
        })

        return view

    }

    fun filterByCategory(category: String) {
        ingredientViewModel.filterIngredientsByCategory(category)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.Ingredient_recycler_view)
        val adapter = IngredientAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Set the adapter
        recyclerView.adapter = adapter

        // Initialize ViewModel
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        // Observe LiveData with viewLifecycleOwner
        ingredientViewModel.allIngredients.observe(viewLifecycleOwner, Observer { ingredients ->
            ingredients?.let { adapter.setIngredients(it) }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            InventoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
