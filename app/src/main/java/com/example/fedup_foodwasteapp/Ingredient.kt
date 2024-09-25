package com.example.fedup_foodwasteapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "ingredient_name") val productName: String = "",
    @ColumnInfo(name = "quantity") val quantity: String = "",
    @ColumnInfo(name = "expiration_date") val expirationDate: String = "",
    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "user_id") val userId: String = "", // Added default value
    var isSynced: Boolean = false // Add a new column to track sync status
) : Serializable


