package com.example.fedup_foodwasteapp


import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TestableRetrofitClient(private val mockApiService: ApiService? = null) {
    private val BASE_URL = "https://fedupmanagementapi20240925105410.azurewebsites.net/"

    private val retrofit by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Use mockApiService if provided, else create the actual one
    val apiService: ApiService by lazy {
        mockApiService ?: retrofit.create(ApiService::class.java)
    }
}

