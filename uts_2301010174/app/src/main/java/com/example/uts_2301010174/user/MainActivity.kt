package com.example.uts_2301010174.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uts_2301010174.R
import com.example.uts_2301010174.RetrofitClient
import com.example.uts_2301010174.Tenant
import com.example.uts_2301010174.adapter.TenantAdapter
import com.example.uts_2301010174.databinding.ActivityMainBinding
import com.example.uts_2301010174.utils.BadgeUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tenantAdapter: TenantAdapter
    private lateinit var rvTenant: RecyclerView
    private lateinit var ivCartIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MenuDetail", "Activity dimulai")

        ivCartIcon = findViewById(R.id.ivCartIcon)

        ivCartIcon.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        getTenantData()

        // Perbarui badge dari SharedPreferences
        updateCartBadge(getCartCount())
    }

    override fun onResume() {
        super.onResume()
        // Perbarui badge dengan data terbaru dari API
        fetchCartCount()
    }

    private fun fetchCartCount() {
        val userId = getUserIdFromSharedPrefs()
        if (userId <= 0) return

        RetrofitClient.instance.getCartItems(userId).enqueue(object : Callback<CartResponse> {
            override fun onResponse(call: Call<CartResponse>, response: Response<CartResponse>) {
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.success == true && res.data != null) {
                        saveCartCount(res.data.size)
                        updateCartBadge(res.data.size)
                        Log.d("MainActivity", "Cart count: ${res.data.size}")
                    }
                }
            }

            override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                Log.e("MainActivity", "Failed to fetch cart items: ${t.message}")
            }
        })
    }

    private fun getTenantData() {
        RetrofitClient.instance.getTenants().enqueue(object : Callback<List<Tenant>> {
            override fun onResponse(call: Call<List<Tenant>>, response: Response<List<Tenant>>) {
                if (response.isSuccessful) {
                    val tenants = response.body() ?: emptyList()
                    Log.d("MainActivity", "Received tenants: ${tenants.size}")
                    tenants.forEach { Log.d("MainActivity", "Tenant: ${it.name}") }

                    tenantAdapter = TenantAdapter(tenants) { tenant ->
                        val intent = Intent(this@MainActivity, MenuDetailActivity::class.java)
                        intent.putExtra("TENANT_ID", tenant.id)
                        startActivity(intent)
                    }

                    binding.rvTenant.layoutManager = LinearLayoutManager(this@MainActivity)
                    binding.rvTenant.adapter = tenantAdapter
                    binding.rvTenant.apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = tenantAdapter
                        isNestedScrollingEnabled = false
                    }

                } else {
                    Log.e("MainActivity", "Response failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Tenant>>, t: Throwable) {
                Log.e("MainActivity", "Failed to load tenants: ${t.message}")
            }
        })
    }

    private fun updateCartBadge(count: Int) {
        BadgeUtils.setCartBadge(this, binding.ivCartIcon, binding.tvCartBadge, count)
    }

    private fun saveCartCount(count: Int) {
        val sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPrefs.edit().putInt("cart_count", count).apply()
    }

    private fun getCartCount(): Int {
        val sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        return sharedPrefs.getInt("cart_count", 0)
    }

    private fun getUserIdFromSharedPrefs(): Int {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)?.toIntOrNull() ?: 0
        Log.d("User debug", "User ID dari SharedPreferences: $userId")
        return userId
    }



}
