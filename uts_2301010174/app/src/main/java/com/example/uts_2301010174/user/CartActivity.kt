package com.example.uts_2301010174.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uts_2301010174.CheckoutActivity
import com.example.uts_2301010174.R
import com.example.uts_2301010174.RetrofitClient
import com.example.uts_2301010174.adapter.CartAdapter
import com.example.uts_2301010174.databinding.CartPageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat // Import NumberFormat

class CartActivity : AppCompatActivity() {
    private lateinit var binding: CartPageBinding
    private lateinit var recyclerViewCartItems: RecyclerView
    private lateinit var cartAdapter: CartAdapter
    private var cartItems: MutableList<CartItem> = mutableListOf()

    private lateinit var tvTotalPrice: TextView
    private lateinit var tvCartItemsTitle: TextView
    private lateinit var btnClearCart: View
    private lateinit var btnCheckout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CartPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setContentView(R.layout.cart_page) // Pastikan ID layout keranjang Anda

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Inisialisasi semua View menggunakan findViewById
        recyclerViewCartItems = findViewById(R.id.rvCartItems)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        tvCartItemsTitle = findViewById(R.id.tvCartItemsTitle)
        btnClearCart = findViewById(R.id.btnClearCart)
        btnCheckout = findViewById(R.id.btnCheckout)

        cartAdapter = CartAdapter(
            cartItems,
            onQuantityChanged = { cartItem, newQuantity ->
                Log.d("CartActivity", "Quantity changed callback: ${cartItem.menuName}, newQty: $newQuantity")
                updateCartItemQuantity(cartItem, newQuantity)
            },
            onItemDeleted = { cartItem ->
                Log.d("CartActivity", "Item deleted callback: ${cartItem.menuName}")
                deleteCartItem(cartItem)
            }
        )
        recyclerViewCartItems.layoutManager = LinearLayoutManager(this)
        recyclerViewCartItems.adapter = cartAdapter

        btnClearCart.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Keranjang sudah kosong!", Toast.LENGTH_SHORT).show()
            } else {
                clearCartConfirmation()
            }
        }

        btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Keranjang kosong!", Toast.LENGTH_SHORT).show()
            } else {
                // Lanjutkan ke CheckoutActivity
                val intent = Intent(this, CheckoutActivity::class.java).apply {
                    putParcelableArrayListExtra("cart_items", ArrayList(cartItems)) // Kirim list of CartItem
                }
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchCartItems()
    }

    private fun fetchCartItems() {
        val userId = getUserIdFromSharedPrefs()
        if (userId <= 0) {
            Toast.makeText(this, "User ID tidak valid. Silakan login kembali.", Toast.LENGTH_SHORT).show()
            Log.e("CartActivity", "User ID invalid for fetching cart items: $userId")
            cartItems.clear() // Hapus item lama
            cartAdapter.notifyDataSetChanged() // Beri tahu adapter data berubah
            updateTotalPrice()
            updateCartItemCount()
            return
        }

        Log.d("CartActivity", "Fetching cart items for User ID: $userId")
        RetrofitClient.instance.getCartItems(userId).enqueue(object : Callback<CartResponse> {
            override fun onResponse(call: Call<CartResponse>, response: Response<CartResponse>) {
                if (response.isSuccessful) {
                    val res = response.body()
                    Log.d("CartActivity", "API Response: success=${res?.success}, message=${res?.message}, data size=${res?.data?.size ?: 0}")
                    if (res?.data != null && res.data.isNotEmpty()) {
                        Log.d("CartActivity", "Sample first item from API: ${res.data.first()}")

                        cartItems.clear() // Hapus item lama
                        cartItems.addAll(res.data) // Tambahkan item baru
                        cartAdapter.notifyDataSetChanged() // Beri tahu adapter data berubah
                        Log.d("CartActivity", "Tenan ID dari API: ${res.data.first().tenantId}" )
                        updateTotalPrice()
                        updateCartItemCount()
                        Log.d("CartActivity", "Fetched ${res.data.size} items. Total price: ${cartItems.sumOf { it.getTotalPrice() }}")
                    } else {
                        Toast.makeText(this@CartActivity, res?.message ?: "Gagal memuat keranjang.", Toast.LENGTH_SHORT).show()
                        Log.d("CartActivity", "API Success false or data null/empty: ${res?.message}")
                        cartItems.clear() // Kosongkan jika data tidak ada
                        cartAdapter.notifyDataSetChanged() // Beri tahu adapter data berubah
                        updateTotalPrice()
                        updateCartItemCount()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@CartActivity, "Gagal memuat keranjang: ${response.code()}", Toast.LENGTH_SHORT).show()
                    Log.e("CartActivity", "HTTP Error fetching cart: ${response.code()}, Error Body: $errorBody")
                    cartItems.clear() // Kosongkan jika error HTTP
                    cartAdapter.notifyDataSetChanged() // Beri tahu adapter data berubah
                    updateTotalPrice()
                    updateCartItemCount()
                }
            }

            override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Koneksi gagal saat memuat keranjang: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("CartActivity", "Network Error fetching cart: ${t.localizedMessage}", t)
                cartItems.clear() // Kosongkan jika error network
                cartAdapter.notifyDataSetChanged() // Beri tahu adapter data berubah
                updateTotalPrice()
                updateCartItemCount()
            }
        })
    }

    private fun updateCartItemQuantity(cartItem: CartItem, newQuantity: Int) {
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
                        Toast.makeText(this@CartActivity, res.message, Toast.LENGTH_SHORT).show()
                        fetchCartItems() // Ambil ulang data dari server untuk refresh total dan kuantitas
                    } else {
                        Toast.makeText(this@CartActivity, res?.message ?: "Gagal memperbarui kuantitas.", Toast.LENGTH_SHORT).show()
                        Log.e("CartActivity", "API Success false in quantity update: ${res?.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@CartActivity, "Error response ${response.code()}: $errorBody", Toast.LENGTH_SHORT).show()
                    Log.e("CartActivity", "HTTP Error in quantity update: ${response.code()}, Error Body: $errorBody")
                }
            }

            override fun onFailure(call: Call<CartSimpleResponse>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("CartActivity", "Network Error: ${t.localizedMessage}", t)
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
                        fetchCartItems() // Ambil ulang data dari server untuk refresh daftar
                    } else {
                        Toast.makeText(this@CartActivity, res?.message ?: "Gagal menghapus item.", Toast.LENGTH_SHORT).show()
                        Log.e("CartActivity", "API Success false in delete item: ${res?.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@CartActivity, "Error response ${response.code()}: $errorBody", Toast.LENGTH_SHORT).show()
                    Log.e("CartActivity", "HTTP Error in delete item: ${response.code()}, Error Body: $errorBody")
                }
            }

            override fun onFailure(call: Call<CartSimpleResponse>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("CartActivity", "Network Error: ${t.localizedMessage}", t)
            }
        })
    }

    private fun updateTotalPrice() {
        val total = cartItems.sumOf { it.getTotalPrice().toDouble() } // Convert to Double for formatting
        binding.tvTotalPrice.text = "Rp ${formatPrice(total)}"
    }

    private fun updateSubtotalPrice() {
        val subtotal = cartItems.sumOf { it.getTotalPrice().toDouble() } // Convert to Double for formatting
        binding.textViewSubtotal.text = "Rp ${formatPrice(subtotal)}"
    }

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(java.util.Locale("in", "ID"))
        formatter.minimumFractionDigits = 0 // No decimal places for whole numbers like 15000
        formatter.maximumFractionDigits = 2 // Max 2 decimal places if there are cents
        return formatter.format(price).replace("Rp", "").trim() // Remove currency symbol if desired
    }


    private fun getUserIdFromSharedPrefs(): Int {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)?.toIntOrNull() ?: 0
        Log.d("User debug", "User ID dari SharedPreferences: $userId")
        return userId
    }

    private fun updateCartItemCount() {
        binding.tvCartItemsTitle.text = "Item Pesanan (${cartItems.size})"
    }


    private fun saveCartCount(count: Int) {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPrefs.edit().putInt("cart_count", count).apply()
    }

    private fun clearCartConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Konfirmasi Kosongkan Keranjang")
            .setMessage("Anda yakin ingin mengosongkan semua item di keranjang?")
            .setPositiveButton("Ya, Kosongkan") { dialog, _ ->
                clearCart()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
                        cartAdapter.notifyDataSetChanged() // Beri tahu adapter data berubah
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
