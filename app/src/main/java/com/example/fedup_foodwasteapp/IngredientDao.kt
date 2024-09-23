package com.example.fedup_foodwasteapp


import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


import androidx.room.*

@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredients")
    fun getAllIngredients(): LiveData<List<Ingredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: Ingredient): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<Ingredient>): List<Long>

    @Update
    suspend fun update(ingredient: Ingredient): Int

    @Delete
    suspend fun delete(ingredient: Ingredient): Int

    @Query("DELETE FROM ingredients")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM ingredients WHERE category = :category")
    fun getIngredientByCategory(category: String): LiveData<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    fun getIngredientById(id: Int): LiveData<Ingredient?>
}

