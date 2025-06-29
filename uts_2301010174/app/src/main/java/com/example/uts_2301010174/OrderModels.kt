package com.example.uts_2301010174// Sesuaikan package Anda

import com.google.gson.annotations.SerializedName

// Model untuk setiap item menu yang akan dikirim dalam order
// Model untuk setiap item menu yang akan dikirim dalam order
data class OrderItemRequest(
    @SerializedName("menu_id") val menuId: Int,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Int, // DOUBLE: Sesuai DB order_details.price (double(10,2))
    @SerializedName("subtotal") val subtotal: Int // DOUBLE: Sesuai DB order_details.subtotal (double(10,2))
)

// Model untuk keseluruhan permintaan order
data class OrderRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("tenant_id") val tenantId: Int?, // NULLABLE INT: Sesuai DB orders.tenant_id
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("notes") val notes: String?, // NULLABLE STRING: Sesuai DB orders.notes
    @SerializedName("total_amount") val totalAmount: Int, // DOUBLE: Sesuai DB orders.total_amount
    @SerializedName("payment_method") val paymentMethod: String, // STRING: Sesuai DB orders.payment_method ENUM
    @SerializedName("cart_items") val cartItems: List<OrderItemRequest>
)

// Model untuk respons dari API checkout
data class CheckoutResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("order_id") val orderId: Int?
)

