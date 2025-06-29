package com.example.uts_2301010174.data // Sesuaikan package Anda

import android.util.Log
import com.example.uts_2301010174.ApiService // Import ApiService tunggal
import com.example.uts_2301010174.CheckoutResponse
import com.example.uts_2301010174.OrderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderRepository(private val apiService: ApiService) {

    suspend fun placeOrder(orderRequest: OrderRequest): CheckoutResponse {
        return withContext(Dispatchers.IO) {
            Log.d("OrderRepository", "Attempting to place order via API.")
            try {
                val call = apiService.checkout(orderRequest)
                val response = call.execute()

                Log.d("OrderRepository", "Checkout API Response: isSuccessful=${response.isSuccessful}, Code=${response.code()}, Message=${response.message()}")
                val responseBodyString = response.body()?.let { "Body: $it" } ?: "Body is null"
                val errorBodyString = response.errorBody()?.string()?.let { "ErrorBody: $it" } ?: "ErrorBody is null"
                Log.d("OrderRepository", "Checkout Response Details: $responseBodyString, $errorBodyString")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("OrderRepository", "Checkout successful (API returned success): ${body.success}, Message: ${body.message}")
                        body
                    } else {
                        Log.e("OrderRepository", "Checkout response body is null but successful. Likely parsing issue or no content.")
                        throw Exception("Empty response body from server (checkout).")
                    }
                } else {
                    Log.e("OrderRepository", "Checkout API failed: Code=${response.code()}, Message=${response.message()}, ErrorBody: $errorBodyString")
                    throw Exception("Failed to place order: ${response.code()} - $errorBodyString")
                }
            } catch (e: Exception) {
                Log.e("OrderRepository", "Checkout network call failed: ${e.message}", e)
                throw Exception("Network or API call failed: ${e.message}")
            }
        }
    }
}
