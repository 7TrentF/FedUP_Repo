import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://spoonacular-recipe-food-nutrition-v1.p.rapidapi.com/"  // Change this to your base URL

    private val client = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> createService(service: Class<T>): T {
        return retrofit.create(service)
    }
}
