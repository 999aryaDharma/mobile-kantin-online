package com.example.uts_2301010174

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: UserData?
)

data class UserData(
    val id: String,
    val username: String,
    val role: String,
    @SerializedName("tenant_id") val tenantId: String
)