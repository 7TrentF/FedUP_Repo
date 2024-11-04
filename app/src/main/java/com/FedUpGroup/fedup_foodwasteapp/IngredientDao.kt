package com.FedUpGroup.fedup_foodwasteapp


import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients")
    fun getAllIngredients(): LiveData<List<Ingredient>>

    // Add these queries
    @Query("SELECT * FROM ingredients WHERE ingredient_name LIKE '%' || :searchText || '%'")
    fun searchIngredients(searchText: String): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE is_deleted = 0")
    fun getAllIngredientsWhereDeleted(): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients")
    suspend fun getAllIngredientsNonLive(): List<Ingredient>
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: Ingredient): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<Ingredient>): List<Long>

    @Update
    suspend fun update(ingredient: Ingredient):Int

    @Delete
    suspend fun delete(ingredient: Ingredient): Int
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Update
    suspend fun updateIng(ingredient: Ingredient):Int

    @Query("DELETE FROM ingredients WHERE firebase_id = :firebaseId")
    suspend fun deleteByFirebaseId(firebaseId: String): Int

    @Query("DELETE FROM ingredients")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM ingredients WHERE category = :category")
    fun getIngredientByCategory(category: String): LiveData<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    fun getIngredientssById(id: Int): LiveData<Ingredient?>

    @Update
    suspend fun updateIngredients(ingredients: List<Ingredient>)

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    suspend fun getIngredientByIdSuspend(id: Long): Ingredient?

    @Query("SELECT * FROM ingredients WHERE category = :category")
    fun getIngredientsByCategory(category: String): Flow<List<Ingredient>>


    /////////////////////////////////sync//////////////////////////////////////

    @Query("SELECT * FROM ingredients WHERE is_synced = 0 OR is_deleted = 1")
    suspend fun getUnsyncedIngredients(): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE is_deleted = 1")
    suspend fun getDeletedIngredients(): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE firebase_id = :firebaseId AND is_deleted = 0 LIMIT 1")
    suspend fun getIngredientByFirebaseId(firebaseId: String): Ingredient?

    @Transaction
    suspend fun insertOrUpdate(ingredient: Ingredient) {
        val existingIngredient = getIngredientByFirebaseId(ingredient.firebaseId)
        if (existingIngredient != null) {
            // Update the existing ingredient in Room
            update(ingredient.copy(id = existingIngredient.id))
        } else {
            // Insert the new ingredient into Room
            insert(ingredient)
        }
    }
    @Query("SELECT * FROM ingredients WHERE is_synced = 0")
    suspend fun getUnSyncedIngredients(): List<Ingredient>

    @Query("SELECT * FROM ingredients")
    suspend fun getIngredients(): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE is_deleted = 0")
    fun getActiveIngredients(): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getIngredientById(id: Long): Ingredient?


    @Query("""
        SELECT 
        SUM(CASE WHEN date(expiration_date) > date('now', '+3 days') THEN 1 ELSE 0 END) as freshCount,
        SUM(CASE 
            WHEN date(expiration_date) <= date('now', '+3 days') 
            AND date(expiration_date) >= date('now') THEN 1 
            ELSE 0 END) as expiringSoonCount,
        SUM(CASE WHEN date(expiration_date) < date('now') THEN 1 ELSE 0 END) as expiredCount
        FROM ingredients
    """)
    suspend fun getIngredientCounts(): IngredientCounts





}




