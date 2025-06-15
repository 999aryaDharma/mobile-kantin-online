package com.example.uts_2301010174.repository

import com.example.uts_2301010174.ApiService
import com.example.uts_2301010174.user.CategoryModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CategoryRepository(private val apiService: ApiService) {

    suspend fun getCategories(): List<CategoryModels.Category> {
        return withContext(Dispatchers.IO) { // Lakukan operasi jaringan di background thread
            val response = apiService.getCategories().execute() // Menggunakan execute() untuk suspend function
            if (response.isSuccessful) {
                response.body()?.data ?: emptyList() // Asumsikan CategoryResponse memiliki 'data' field
            } else {
                throw Exception("Failed to fetch categories: ${response.code()}")
            }
        }
    }
}