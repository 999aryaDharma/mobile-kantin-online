package com.example.uts_2301010174


import com.example.uts_2301010174.user.CartResponse
import com.example.uts_2301010174.user.CartSimpleResponse
import com.example.uts_2301010174.user.CategoryModels
import com.example.uts_2301010174.user.MenuAddResponse
import com.example.uts_2301010174.user.MenuResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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

    // Jika Anda juga perlu mengunggah foto sebagai multipart
    @Multipart
    @POST("add_menu.php") // Ganti dengan path API Anda yang sebenarnya
    fun addMenu(
        @Part("tenant_id") tenantId: RequestBody,
        @Part("category_id") categoryId: RequestBody?,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody?,
        @Part photo: MultipartBody.Part?, // Untuk file foto
        @Part("price") price: RequestBody
    ): Call<MenuAddResponse>

    @FormUrlEncoded // Untuk mengirim data sebagai form-urlencoded
    @POST("delete_menu.php") // Pastikan path benar
    fun deleteMenu(@Field("id") menuId: Int): Call<MenuAddResponse>

    // Menggunakan Multipart untuk fleksibilitas: bisa update teks dan/atau gambar
    @Multipart
    @POST("edit_menu.php") // Ganti dengan path API update Anda
    fun updateMenu(
        @Part("id") menuId: RequestBody, // ID menu yang akan diupdate
        @Part("tenant_id") tenantId: RequestBody, // Masih perlu untuk validasi keamanan di server
        @Part("category_id") categoryId: RequestBody?,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody?,
        @Part photo: MultipartBody.Part?, // Opsional: kirim gambar baru jika diubah
        @Part("price") price: RequestBody
        // Anda juga bisa menambahkan @Part("old_photo_url") untuk memberitahu server menghapus foto lama jika diganti
    ): Call<MenuAddResponse> // Menggunakan MenuAddResponse untuk hasil update
    // >>> AKHIR ENDPOINT BARU <<<

}
