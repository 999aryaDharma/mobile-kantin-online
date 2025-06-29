package com.example.uts_2301010174.user

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    val id: Int, // ID unik dari item keranjang (bukan ID menu)
    @SerializedName("user_id") val userId: Int, // BARU: Tambahkan user_id dari API response
    @SerializedName("menu_id") val menuId: Int,
    @SerializedName("tenant_id") val tenantId: Int?,
    @SerializedName("menu_name") val menuName: String,
    @SerializedName("menu_price") val menuPrice: Int, // PERBAIKAN: Ubah ke Int agar sesuai dengan API
    var quantity: Int,
    @SerializedName("photo") val imageUrl: String? // GAMBAR: Dibuat nullable karena mungkin tidak selalu ada
) : Parcelable {
    fun getTotalPrice(): Int { // PERBAIKAN: Ubah return type ke Int
        return menuPrice * quantity
    }
}


data class CartSimpleResponse(
    val success: Boolean,
    val message: String,
)

data class CartResponse(
    val success: Boolean,
    val message: String? = null,
    val data: List<CartItem>? = null
)
