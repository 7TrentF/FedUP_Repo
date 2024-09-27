package com.example.fedup_foodwasteapp



import com.example.fedup_foodwasteapp.Ingredient
import com.example.fedup_foodwasteapp.ApiService
import com.example.fedup_foodwasteapp.RetrofitClient
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import retrofit2.Response




class AddIngredientFragmentTest {

    private lateinit var mockApiService: ApiService
    private lateinit var testableRetrofitClient: TestableRetrofitClient

    @Before
    fun setup() {
        mockApiService = mock(ApiService::class.java)
        testableRetrofitClient = TestableRetrofitClient(mockApiService)
    }

    fun String.toMediaType(): MediaType? {
        return this.toMediaTypeOrNull()
    }

    @Test
    fun `test insert ingredient success`() = runBlocking {
        val mockIngredient = Ingredient(
            productName = "Tomato",
            quantity = "5",
            expirationDate = "2024-12-01",
            category = "Vegetable",
            userId = "testUserId"
        )
        val mockResponse = Ingredient(
            productName = "Tomato",
            quantity = "5",
            expirationDate = "2024-12-01",
            category = "Vegetable",
            userId = "testUserId",
            firebaseId = "testFirebaseId"
        )

        // Specify the expected parameter type
      //  whenever(mockApiService.addIngredient(any())).thenReturn(Response.success(mockResponse))

        // Directly call the addIngredient method with the mock ingredient
        val result = testableRetrofitClient.apiService.addIngredient(mockIngredient)

        assertNotNull(result)
        assertEquals(true, result.isSuccessful)
        assertEquals("testFirebaseId", result.body()?.firebaseId)
    }


    @Test
    fun `test insert ingredient failure`() = runBlocking {
        val mockIngredient = Ingredient(
            productName = "Tomato",
            quantity = "5",
            expirationDate = "2024-12-01",
            category = "Vegetable",
            userId = "testUserId"
        )

        val mockErrorResponse = Response.error<Ingredient>(
            400,
            ResponseBody.create("application/json".toMediaType(), "Bad Request")
        )

        // Ensure that you are calling any() correctly
        `when`(mockApiService.addIngredient(any())).thenReturn(mockErrorResponse)

        val result = testableRetrofitClient.apiService.addIngredient(mockIngredient)

        assertNotNull(result)
        assertEquals(false, result.isSuccessful)
    }
    }

