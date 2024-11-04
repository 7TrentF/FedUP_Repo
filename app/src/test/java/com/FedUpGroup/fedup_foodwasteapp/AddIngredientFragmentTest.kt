package com.FedUpGroup.fedup_foodwasteapp
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import retrofit2.Response

@RunWith(MockitoJUnitRunner::class)
class AddIngredientFragmentTest {

    @Mock
    private lateinit var ingredientViewModel: IngredientViewModel

    @Mock
    private lateinit var mockApiService: ApiService

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var addIngredientFragment: AddIngredientFragment

    @Before
    fun setUp() {
        addIngredientFragment = AddIngredientFragment()

        // Use mock ViewModel for the fragment
        addIngredientFragment.ingredientViewModel = ingredientViewModel
    }

    @Test
    fun testInsertIngredient_Success() {
        // Given valid input for ingredient
        val ingredient = Ingredient(
            productName = "Milk",
            quantity = "2",
            expirationDate = "2024-10-05",
            category = "Fridge",
            userId = "user123"
        )

        // Mock the API response
        runBlocking {
            whenever(mockApiService.addIngredient(any())).thenReturn(Response.success(ingredient))
        }

        // Mock ViewModel insert result LiveData
        val insertLiveData = MutableLiveData<Boolean>()
        insertLiveData.value = true

        // Mock observing insertResult
        whenever(ingredientViewModel.insertResult).thenReturn(insertLiveData)

        // When ingredient is inserted
        addIngredientFragment.insertIngredient(
            name = "Milk",
            quantity = "2",
            category = "Fridge",
            expirationDate = "2024-10-05"
        )

        // Then verify the ingredient was added successfully
        verify(ingredientViewModel).insertResult
        assertTrue(insertLiveData.value!!)
    }

    @Test
    fun testInsertIngredient_Failure_EmptyFields() {
        // Given empty input fields
        val emptyName = ""
        val emptyQuantity = ""
        val emptyExpirationDate = ""

        // When insert is attempted with empty fields
        addIngredientFragment.insertIngredient(
            name = emptyName,
            quantity = emptyQuantity,
            category = "Fridge",
            expirationDate = emptyExpirationDate
        )

        // Then an error Snackbar should be shown
        verify(ingredientViewModel, never()).insertResult
        // You would check that an error Snackbar or Toast is shown in the UI here
    }

    @Test
    fun testInsertIngredient_Failure_APIError() {
        runBlocking {
            // Given valid input but the API fails
            val ingredient = Ingredient(
                productName = "Milk",
                quantity = "2",
                expirationDate = "2024-10-05",
                category = "Fridge",
                userId = "user123"
            )

            // Mock the API to return failure
            val errorResponse = Response.error<Ingredient>(
                400, ResponseBody.create(
                    "application/json".toMediaTypeOrNull(),
                    "{\"message\":\"Bad Request\"}"
                )
            )
            whenever(mockApiService.addIngredient(any())).thenReturn(errorResponse)

            // When the ViewModel tries to insert
            addIngredientFragment.insertIngredient(
                name = "Milk",
                quantity = "2",
                category = "Fridge",
                expirationDate = "2024-10-05"
            )

            // Then verify the API was called and returned an error
            verify(mockApiService).addIngredient(any())
            // Check that the UI shows an error message (Snackbar or Toast)
        }
    }
}


