package com.FedUpGroup.fedup_foodwasteapp

// DataResult.kt
sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Error(val exception: Exception) : DataResult<Nothing>()
    object Loading : DataResult<Nothing>()
}
