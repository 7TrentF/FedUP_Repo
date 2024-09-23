package com.example.fedup_foodwasteapp

import com.example.fedup_foodwasteapp.Ingredients
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


// The @Dao annotation marks this interface as a Data Access Object (DAO),
// which provides methods for interacting with the database.
@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredients")
    fun getAllIngredients(): LiveData<List<Ingredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: Ingredient)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<Ingredient>)

    @Update
    suspend fun update(ingredient: Ingredient)

    @Delete
    suspend fun delete(ingredient: Ingredient)

    @Query("DELETE FROM ingredients")
    suspend fun deleteAll()

    @Query("SELECT * FROM ingredients WHERE category = :category")
    fun getIngredientByCategory(category: String): LiveData<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    fun getIngredientById(id: Int): Ingredient?
}
