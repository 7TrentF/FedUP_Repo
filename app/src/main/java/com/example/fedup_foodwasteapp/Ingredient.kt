package com.example.fedup_foodwasteapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

class Ingredient

@Entity(tableName = "ingredients")
data class Ingredients(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    @ColumnInfo(name = "ingredient_name")
    val productName: String,

    @ColumnInfo(name = "quantity")
    val quantity: String,

    @ColumnInfo(name = "expiration_date")
    val expirationDate: String,

    @ColumnInfo(name = "category")
    val category: String
)



