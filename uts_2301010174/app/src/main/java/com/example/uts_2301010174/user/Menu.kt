package com.example.uts_2301010174.user // Sesuaikan package Anda jika berbeda

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class MenuRequest(
    @SerializedName("tenant_id") val tenantId: Int,
    @SerializedName("category_id") val categoryId: Int?,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("photo") val photo: String?,
    @SerializedName("price") val price: Double
)


data class MenuAddResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: Menu?
)


data class MenuResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<Menu>
)


@Parcelize
data class Menu(
    @SerializedName("id") val id: Int,
    @SerializedName("tenant_id") val tenantId: Int,
    @SerializedName("category_id") val categoryId: Int?,
    @SerializedName("category_name") val category: String?,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("photo") val photo: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("is_available") var isAvailable: Int,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
) : Parcelable
