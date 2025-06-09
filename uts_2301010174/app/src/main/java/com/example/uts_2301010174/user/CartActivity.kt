package com.example.uts_2301010174.user

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uts_2301010174.R
import com.example.uts_2301010174.RetrofitClient
import com.example.uts_2301010174.databinding.CartPageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CartActivity : AppCompatActivity() {
    private lateinit var binding: CartPageBinding
    private lateinit var cartAdapter: CartAdapter
    private var cartItems: MutableList<CartItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CartPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup tombol kembali
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // kembali ke activity sebelumnya (MainActivity)
        }

        // Inisialisasi RecyclerView
        cartAdapter = CartAdapter(
            cartItems,
            onQuantityChanged = { cartItem, newQuantity ->
                updateCartItem(cartItem, newQuantity)
            },
            onItemDeleted = { cartItem ->
                deleteCartItem(cartItem)
            }
        )
        binding.rvCartItems.layoutManager = LinearLayoutManager(this)
        binding.rvCartItems.adapter = cartAdapter

        // Ambil data keranjang
        fetchCartItems()

        // Handle tombol Clear Cart
        binding.btnClearCart.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Keranjang sudah kosong!", Toast.LENGTH_SHORT).show()
            } else {
                clearCart()
            }
        }

        // Handle tombol Checkout
        binding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Keranjang kosong!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Fitur checkout belum diimplementasikan.", Toast.LENGTH_SHORT).show()
                // Implementasikan logika checkout di sini
            }
        }


        // Perbarui jumlah item di tvCartItemsTitle
        updateCartItemCount()


    }

    override fun onResume() {
        super.onResume()
        // Perbarui badge dengan data terbaru
        fetchCartItems()
    }

    private fun fetchCartItems() {
        val userId = getUserIdFromSharedPrefs() // Ganti dengan metode Anda untuk mendapatkan user_id
        if (userId <= 0) {
            Toast.makeText(this, "User ID tidak valid. Silakan login kembali.", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.instance.getCartItems(userId).enqueue(object : Callback<CartResponse> {
            override fun onResponse(call: Call<CartResponse>, response: Response<CartResponse>) {
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.success == true && res.data != null) {
                        cartItems.clear()
                        cartItems.addAll(res.data)
                        cartAdapter.updateCartItems(cartItems)
                        updateTotalPrice()
                        // Perbarui jumlah item di tvCartItemsTitle
                        updateCartItemCount()
                        Log.d("CartDebug", "Fetched ${res.data.size} items")
                    } else {
                        Toast.makeText(this@CartActivity, res?.message ?: "Gagal memuat keranjang.", Toast.LENGTH_SHORT).show()
                        Log.e("CartDebug", "API Success false: ${res?.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@CartActivity, "Error response ${response.code()}: $errorBody", Toast.LENGTH_LONG).show()
                    Log.e("CartDebug", "HTTP Error: ${response.code()}, Error Body: $errorBody")
                }
            }

            override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("CartDebug", "Network Error: ${t.localizedMessage}", t)
            }
        })
    }

    private fun updateCartItem(cartItem: CartItem, newQuantity: Int) {
        val userId = getUserIdFromSharedPrefs()
        if (userId <= 0) {
            Toast.makeText(this, "User ID tidak valid.", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.instance.addToCart(userId, cartItem.menuId, newQuantity).enqueue(object : Callback<CartSimpleResponse> {
            override fun onResponse(call: Call<CartSimpleResponse>, response: Response<CartSimpleResponse>) {
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.success == true) {
                        cartItem.quantity = newQuantity
                        cartAdapter.notifyDataSetChanged()
                        updateTotalPrice()
                        Toast.makeText(this@CartActivity, res.message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CartActivity, res?.message ?: "Gagal memperbarui kuantitas.", Toast.LENGTH_SHORT).show()
                        Log.e("CartDebug", "API Success false: ${res?.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@CartActivity, "Error response ${response.code()}: $errorBody", Toast.LENGTH_LONG).show()
                    Log.e("CartDebug", "HTTP Error: ${response.code()}, Error Body: $errorBody")
                }
            }

            override fun onFailure(call: Call<CartSimpleResponse>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("CartDebug", "Network Error: ${t.localizedMessage}", t)
            }
        })
    }

    private fun deleteCartItem(cartItem: CartItem) {
        val userId = getUserIdFromSharedPrefs()
        if (userId <= 0) {
            Toast.makeText(this, "User ID tidak valid.", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.instance.addToCart(userId, cartItem.menuId, 0).enqueue(object : Callback<CartSimpleResponse> {
            override fun onResponse(call: Call<CartSimpleResponse>, response: Response<CartSimpleResponse>) {
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.success == true) {
                        Toast.makeText(this@CartActivity, res.message, Toast.LENGTH_SHORT).show()
                        updateTotalPrice()
                    } else {
                        Toast.makeText(this@CartActivity, res?.message ?: "Gagal menghapus item.", Toast.LENGTH_SHORT).show()
                        Log.e("CartDebug", "API Success false: ${res?.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@CartActivity, "Error response ${response.code()}: $errorBody", Toast.LENGTH_LONG).show()
                    Log.e("CartDebug", "HTTP Error: ${response.code()}, Error Body: $errorBody")
                }
            }

            override fun onFailure(call: Call<CartSimpleResponse>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("CartDebug", "Network Error: ${t.localizedMessage}", t)
            }
        })
    }

    private fun updateTotalPrice() {
        val total = cartItems.sumOf { it.menuPrice * it.quantity }
        binding.tvTotalPrice.text = "${formatPrice(total)}"
    }

    private fun formatPrice(price: Int): String {
        return String.format("%,d", price).replace(',', '.')
    }

    private fun getUserIdFromSharedPrefs(): Int {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)?.toIntOrNull() ?: 0
        Log.d("User debug", "User ID dari SharedPreferences: $userId")
        return userId
    }

    private fun updateCartItemCount() {
        binding.tvCartItemsTitle.text = "Item Pesanan (${cartItems.size})"
    }


    private fun saveCartCount(count: Int) {
        val sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPrefs.edit().putInt("cart_count", count).apply()
    }

    private fun clearCart() {
        val userId = getUserIdFromSharedPrefs()
        if (userId <= 0) {
            Toast.makeText(this, "User ID tidak valid.", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.instance.clearCart(userId).enqueue(object : Callback<CartSimpleResponse> {
            override fun onResponse(call: Call<CartSimpleResponse>, response: Response<CartSimpleResponse>) {
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.success == true) {
                        cartItems.clear()
                        cartAdapter.updateCartItems(cartItems)
                        updateTotalPrice()
                        updateCartItemCount()
                        saveCartCount(0)
                        Toast.makeText(this@CartActivity, res.message, Toast.LENGTH_SHORT).show()
                        Log.d("CartDebug", "Cart cleared successfully")
                    } else {
                        Toast.makeText(this@CartActivity, res?.message ?: "Gagal menghapus keranjang.", Toast.LENGTH_SHORT).show()
                        Log.e("CartDebug", "API Success false: ${res?.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@CartActivity, "Error response ${response.code()}: $errorBody", Toast.LENGTH_LONG).show()
                    Log.e("CartDebug", "HTTP Error: ${response.code()}, Error Body: $errorBody")
                }
            }

            override fun onFailure(call: Call<CartSimpleResponse>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("CartDebug", "Network Error: ${t.localizedMessage}", t)
            }
        })
    }


}