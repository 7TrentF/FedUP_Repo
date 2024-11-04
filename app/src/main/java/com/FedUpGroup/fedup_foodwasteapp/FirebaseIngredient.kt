package com.FedUpGroup.fedup_foodwasteapp

import com.google.type.DateTime

data class FirebaseIngredient(

    var ingredient_name: String = "",
    var quantity: String = "",
    var expirationDate: DateTime, // Use DateTime or any appropriate type
    var category: String = "",
    var firebase_id: String = "",
    var user_id: String = ""
)


