package com.example.fedup_foodwasteapp


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

    @Query("SELECT * FROM ingredients")
    suspend fun getAllIngredientsNonLive(): List<Ingredient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: Ingredient): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<Ingredient>): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(ingredient: Ingredient):Int



    @Update
    suspend fun updateIng(ingredient: Ingredient)

    @Delete
    suspend fun delete(ingredient: Ingredient): Int

    @Query("DELETE FROM ingredients WHERE firebase_id = :firebaseId")
    suspend fun deleteByFirebaseId(firebaseId: String): Int

    @Query("DELETE FROM ingredients")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM ingredients WHERE category = :category")
    fun getIngredientByCategory(category: String): LiveData<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    fun getIngredientById(id: Int): LiveData<Ingredient?>



    @Update
    suspend fun updateIngredients(ingredients: List<Ingredient>)

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    suspend fun getIngredientByIdSuspend(id: Long): Ingredient?




    /////////////////////////////////sync//////////////////////////////////////

    @Query("SELECT * FROM ingredients WHERE is_synced = 0 AND is_deleted = 0")
    suspend fun getUnsyncedIngredients(): List<Ingredient>


    @Query("SELECT * FROM ingredients WHERE is_deleted = 1")
    suspend fun getDeletedIngredients(): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE firebase_id = :firebaseId AND is_deleted = 0 LIMIT 1")
    suspend fun getIngredientByFirebaseId(firebaseId: String): Ingredient?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(ingredient: Ingredient)


    @Query("SELECT * FROM ingredients WHERE is_synced = 0")
    suspend fun getUnSyncedIngredients(): List<Ingredient>

    @Query("SELECT * FROM ingredients")
    suspend fun getIngredients(): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE is_deleted = 0")
    fun getActiveIngredients(): Flow<List<Ingredient>>


}

