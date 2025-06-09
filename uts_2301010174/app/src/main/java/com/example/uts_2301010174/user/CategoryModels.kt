package com.example.uts_2301010174.user

class CategoryModels {
    // Ini adalah model untuk item kategori tunggal
    data class Category(
        val id: String, // Sesuaikan dengan tipe data ID kategori Anda (misal: Int)
        val name: String
    )

    // Ini adalah model untuk keseluruhan respons dari API Anda
    data class CategoryResponse(
        val success: Boolean,
        val message: String,
        val data: List<Category> // Ini adalah array kategori yang sebenarnya
    )
}