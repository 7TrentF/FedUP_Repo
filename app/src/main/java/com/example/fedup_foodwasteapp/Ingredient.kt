package com.example.fedup_foodwasteapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.type.DateTime
import java.io.Serializable
import java.time.LocalDate

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "ingredient_name") val productName: String = "",
    @ColumnInfo(name = "quantity") val quantity: String = "",
    @ColumnInfo(name = "expiration_date") val expirationDate: String = "",
    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "firebase_id") var firebaseId: String = "", // Use this to store Firebase ingredient ID
    @ColumnInfo(name = "user_id") val userId: String = "", // Added default value
    var isSynced: Boolean = false // Add a new column to track sync status
) : Serializable



/*





data class Ingredient(
    var productName: String = "",
    var quantity: String = "",
    var expirationDate: DateTime, // Use DateTime or any appropriate type
    var category: String = "",
    var firebaseId: String = "",
    var userId: String = ""
)
 */

