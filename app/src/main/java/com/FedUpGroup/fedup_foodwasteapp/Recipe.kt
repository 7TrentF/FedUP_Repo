package com.FedUpGroup.fedup_foodwasteapp

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize


@Parcelize
data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val missedIngredientCount: Int,
    val usedIngredientCount: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt()

    )

    override fun describeContents(): Int = 0

    companion object : Parceler<Recipe> {
        override fun Recipe.write(parcel: Parcel, flags: Int) {
            parcel.writeInt(id)
            parcel.writeString(title)
            parcel.writeString(image)
        }

        override fun create(parcel: Parcel): Recipe = Recipe(parcel)
    }
}