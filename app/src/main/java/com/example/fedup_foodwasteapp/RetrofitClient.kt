package com.example.fedup_foodwasteapp

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.68:7043/"

    private val retrofit by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY) // Use BASIC or NONE in production

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // Add the token to each request
                val original = chain.request()
                val requestBuilder = original.newBuilder()

                AuthManager.getInstance().getIdToken { token, _ ->
                    token?.let {
                        val jwtToken = "Bearer $it"
                        requestBuilder.addHeader("Authorization", jwtToken)
                    }
                }

                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

