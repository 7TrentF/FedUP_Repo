package com.example.fedup_foodwasteapp

import android.support.annotation.StringRes

enum class Category(@StringRes val displayNameResourceId: Int) {
    UNCATEGORIZED(R.string.category_uncategorized),
    FRIDGE(R.string.category_fridge),
    FREEZER(R.string.category_freezer),
    PANTRY(R.string.category_pantry)
}
