package com.example.uts_2301010174.user

data class MenuResponse(
    val success: Boolean,
    val message: String,
    val data: List<Menu>
)