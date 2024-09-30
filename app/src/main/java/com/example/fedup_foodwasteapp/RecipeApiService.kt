import com.example.fedup_foodwasteapp.Recipe
import com.example.fedup_foodwasteapp.RecipeDetails
import com.example.fedup_foodwasteapp.RecipeResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
interface RecipeApiService {

    // Specify headers required for the API requests, including the RapidAPI key and host.
    @Headers(
        "X-RapidAPI-Key: 649a3d770bmsh6d6d3423d8e5a25p139e64jsne6fc42e17ee2", // The RapidAPI key
        "X-RapidAPI-Host: spoonacular-recipe-food-nutrition-v1.p.rapidapi.com" // Host for the Spoonacular API
    )
    // GET request to search for recipes based on a query.
    @GET("recipes/complexSearch")
    suspend fun getRecipes(
        @Query("query") query: String, // The search query (e.g., "pasta")
        @Query("number") number: String = "10" // Optional parameter to specify the number of recipes to return (default is 10)
    ): RecipeResponse // Returns a RecipeResponse object containing the list of recipes

    // GET request to find recipes that can be made with the specified ingredients.
    @GET("recipes/findByIngredients")
    suspend fun getRecipesByIngredients(
        @Query("ingredients") ingredients: String, // A comma-separated list of ingredients (e.g., "chicken,tomato")
        @Query("number") number: Int = 10,
        @Query("ranking") ranking: Int = 1,
        @Query("ignorePantry") ignorePantry: Boolean = true // Flag to ignore pantry items when searching for ingredients
    ): List<Recipe> // Returns a list of Recipe objects that match the ingredients

    // GET request to retrieve detailed information about a specific recipe by its ID.
    @GET("recipes/{id}/information")
    suspend fun getRecipeDetails(
        @Path("id") recipeId: Int,
        @Header("X-RapidAPI-Key") apiKey: String, // API key passed as a header for authorization
        @Header("X-RapidAPI-Host") apiHost: String // Host for the API passed as a header
    ): RecipeDetails // Returns a RecipeDetails object containing detailed information about the recipe
}
