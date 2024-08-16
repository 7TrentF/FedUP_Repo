package com.example.fedup_foodwasteapp

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
 interface
IngredientDao {

    @Insert
    fun insert(ingredients: Ingredients)

    @Update
    fun update(ingredients: Ingredients)

    @Delete
    fun delete(ingredients: Ingredients)

    @Query("SELECT * FROM ingredients ORDER BY expiration_date ASC")
    fun getAllIngredients(): LiveData<List<Ingredients>>  // Change to Ingredients

    @Query("SELECT * FROM ingredients WHERE category = :category")
    fun getIngredientByCategory(category: String): LiveData<List<Ingredients>>

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    fun getIngredientById(id: Int): Ingredients?
}