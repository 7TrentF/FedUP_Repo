package com.example.fedup_foodwasteapp

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.google.type.DateTime
import java.io.Serializable
import java.time.LocalDate

data class FirebaseIngredient(

    var ingredient_name: String = "",
    var quantity: String = "",
    var expirationDate: DateTime, // Use DateTime or any appropriate type
    var category: String = "",
    var firebase_id: String = "",
    var user_id: String = ""
)


