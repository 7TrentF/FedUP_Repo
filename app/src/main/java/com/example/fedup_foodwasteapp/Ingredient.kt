package com.example.fedup_foodwasteapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

class Ingredient

// The @Entity annotation marks this class as a table within the Room database.
// The table is named "ingredients".
@Entity(tableName = "ingredients")
data class Ingredients(
    // The @PrimaryKey annotation marks this field as the primary key of the table.
    // The 'autoGenerate = true' indicates that the ID will be automatically generated by Room.
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // The @ColumnInfo annotation specifies the name of the column in the table.
    // The 'ingredient_name' column stores the name of the ingredient.
    @ColumnInfo(name = "ingredient_name")
    val productName: String,

    // The 'quantity' column stores the quantity of the ingredient.
    @ColumnInfo(name = "quantity")
    val quantity: String,

    // The 'expiration_date' column stores the expiration date of the ingredient.
    @ColumnInfo(name = "expiration_date")
    val expirationDate: String,

    // The 'category' column stores the category of the ingredient.
    @ColumnInfo(name = "category")
    val category: String
)




