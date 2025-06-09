package com.example.uts_2301010174.user

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Menu(
    val id: Int,
    val tenantId: Int,
    val name: String,
    val description: String,
    val categoryId: Int,
    val category: String?,
    val photo: String,
    val price: Double,
    @SerializedName("is_available") var isAvailable: Int,
    val createdAt: Date,
    val updatedAt: Date
)
