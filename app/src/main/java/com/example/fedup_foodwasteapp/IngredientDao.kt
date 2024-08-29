package com.example.fedup_foodwasteapp

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


// The @Dao annotation marks this interface as a Data Access Object (DAO),
// which provides methods for interacting with the database.
@Dao
interface IngredientDao {

    // The @Insert annotation marks this function to insert a new record into the 'ingredients' table.
    // The parameter is an instance of the Ingredients class, representing the record to be inserted.
    @Insert
    fun insert(ingredients: Ingredients)

    // The @Update annotation marks this function to update an existing record in the 'ingredients' table.
    // The parameter is an instance of the Ingredients class, representing the record to be updated.
    @Update
    fun update(ingredient: Ingredients)

    // The @Delete annotation marks this function to delete a record from the 'ingredients' table.
    // The parameter is an instance of the Ingredients class, representing the record to be deleted.
    @Delete
    fun delete(ingredient: Ingredients)

    // This function retrieves all records from the 'ingredients' table,
    // ordered by the expiration date in ascending order.
    // It returns a LiveData object, which allows the UI to observe changes in the data.
    @Query("SELECT * FROM ingredients ORDER BY expiration_date ASC")
    fun getAllIngredients(): LiveData<List<Ingredients>>

    // This function retrieves all records from the 'ingredients' table that belong to a specific category.
    // The category is passed as a parameter, and it returns a LiveData object to observe the results.
    @Query("SELECT * FROM ingredients WHERE category = :category")
    fun getIngredientByCategory(category: String): LiveData<List<Ingredients>>

    // This function retrieves a single record from the 'ingredients' table by its ID.
    // It returns an instance of the Ingredients class, representing the record found.
    // If no record is found, it returns null.
    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    fun getIngredientById(id: Int): Ingredients?
}
