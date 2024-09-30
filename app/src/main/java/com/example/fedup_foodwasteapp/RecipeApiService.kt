import com.example.fedup_foodwasteapp.Recipe
import com.example.fedup_foodwasteapp.RecipeDetails
import com.example.fedup_foodwasteapp.RecipeResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipeApiService {

    @Headers(
        "X-RapidAPI-Key: 649a3d770bmsh6d6d3423d8e5a25p139e64jsne6fc42e17ee2",
        "X-RapidAPI-Host: spoonacular-recipe-food-nutrition-v1.p.rapidapi.com"
    )
    @GET("recipes/complexSearch")
    suspend fun getRecipes(
        @Query("query") query: String,
        @Query("number") number: String = "10"
    ): RecipeResponse


    @GET("recipes/findByIngredients")
    suspend fun getRecipesByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 10,
        @Query("ranking") ranking: Int = 1,
        @Query("ignorePantry") ignorePantry: Boolean = true
    ): List<Recipe>


    @GET("recipes/{id}/information")
    suspend fun getRecipeDetails(
        @Path("id") recipeId: Int,
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") apiHost: String
    ): RecipeDetails
}

