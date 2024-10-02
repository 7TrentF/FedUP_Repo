package com.example.fedup_foodwasteapp

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import android.util.Log
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://fedupapi20240922180228.azurewebsites.net/"


    private val retrofit by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY) // Use BASIC or NONE in production

        // Reference: https://square.github.io/okhttp/features/interceptors/
        // The official OkHttp documentation explains the use of interceptors for logging and modifying requests.



        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val token = AuthManager.getInstance().getCachedToken() // Get cached token if available

                val requestBuilder = originalRequest.newBuilder()
                //check if token is null
                if (token != null) {
                    Log.d("Interceptor", "Token added to header: $token")
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .build()
        // Reference: https://square.github.io/retrofit/
        // Retrofit is a type-safe HTTP client for Android and Java, as stated in its official documentation.


        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    // Reference: https://square.github.io/retrofit/2.x/retrofit/retrofit2/Converter.Factory.html
    // GsonConverterFactory is used to convert JSON responses into Java objects.


    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
        // Reference: https://square.github.io/retrofit/2.x/retrofit/retrofit2/Retrofit.html#create-java.lang.Class-
        // The create() method dynamically implements the ApiService interface, allowing us to use it for network requests.
    }
}
