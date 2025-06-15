package com.example.uts_2301010174.user

import com.google.gson.annotations.SerializedName

class CategoryModels {
    // Ini adalah model untuk item kategori tunggal
    data class Category(
        @SerializedName("id_category")
        val id: Int, // Sesuaikan dengan tipe data ID kategori Anda (misal: Int)
        val name: String
    )

    // Ini adalah model untuk keseluruhan respons dari API Anda
    data class CategoryResponse(
        val success: Boolean,
        val message: String,
        val data: List<Category> // Ini adalah array kategori yang sebenarnya
    )
}