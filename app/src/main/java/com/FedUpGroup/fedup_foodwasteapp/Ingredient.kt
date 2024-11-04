package com.FedUpGroup.fedup_foodwasteapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "ingredients",
)
data class Ingredient(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @ColumnInfo(name = "ingredient_name") val productName: String = "",
    @ColumnInfo(name = "quantity") val quantity: String = "",
    @ColumnInfo(name = "expiration_date") val expirationDate: String = "",
    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "firebase_id") var firebaseId: String = "", // Use this to store Firebase ingredient ID
    @ColumnInfo(name = "user_id") val userId: String = "", // Added default value
    @ColumnInfo(name = "version") var version: Long = 0,
    @ColumnInfo(name = "last_modified") var lastModified: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_synced") var isSynced: Boolean = false,
    @ColumnInfo(name = "is_deleted") var isDeleted: Boolean = false
) : Serializable

