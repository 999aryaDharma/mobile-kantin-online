package com.example.uts_2301010174.user

import com.google.gson.annotations.SerializedName

data class CartItem(
    val id: Int,

    @SerializedName("menu_id")
    val menuId: Int,

    @SerializedName("menu_name")
    val menuName: String,

    @SerializedName("menu_price")
    val menuPrice: Int,

    var quantity: Int,

    @SerializedName("image_url")
    val imageUrl: String = ""
) {
    fun getTotalPrice(): Int {
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
