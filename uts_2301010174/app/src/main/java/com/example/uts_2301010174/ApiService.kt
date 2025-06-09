package com.example.uts_2301010174


import com.example.uts_2301010174.user.CartResponse
import com.example.uts_2301010174.user.CartSimpleResponse
import com.example.uts_2301010174.user.CategoryModels
import com.example.uts_2301010174.user.MenuResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {
    @GET("get_menu.php")
    fun getDataMenu(): Call<MenuResponse>

    @GET("get_menu_by_tenant_id.php")
    fun getDataMenuByTenantId(@Query("tenant_id") tenantId: Int): Call<MenuResponse>

    @GET("get_available_menu_by_tenant.php")
    fun getAvailableMenuByTenantId(@Query("tenant_id") tenantId: Int): Call<MenuResponse>

    @GET("get_tenant.php")
    fun getTenants(): Call<List<Tenant>>

    // Tambahkan ini untuk mendapatkan kategori
    @GET("get_categories.php")
    fun getCategories(): Call<CategoryModels.CategoryResponse>

    @PUT("update_menu_availability.php")
    fun updateMenuAvailability(
        @Query("id") menuId: Int,
        @Body availability: Map<String, Int>
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("login.php")
    fun loginUser(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("add_to_cart.php")
    fun addToCart(
        @Field("user_id") userId: Int,
        @Field("menu_id") menuId: Int,
        @Field("quantity") quantity: Int
    ): Call<CartSimpleResponse>

    @GET("get_cart.php")
    fun getCartItems(
        @Query("user_id") userId: Int
    ): Call<CartResponse>

    @GET("clear_cart.php")
    fun clearCart(@Query("user_id") userId: Int): Call<CartSimpleResponse>
}
