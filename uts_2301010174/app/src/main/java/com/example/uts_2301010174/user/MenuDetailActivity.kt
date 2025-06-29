package com.example.uts_2301010174.user

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uts_2301010174.R
import com.example.uts_2301010174.RetrofitClient
import com.example.uts_2301010174.adapter.MenuAdapter
import com.example.uts_2301010174.databinding.MenuDetailBinding
import com.example.uts_2301010174.utils.BadgeUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MenuDetailActivity : AppCompatActivity() {

    private lateinit var binding: MenuDetailBinding
    private lateinit var recyclerViewMenu: RecyclerView
    private lateinit var menuAdapter: MenuAdapter
    private var listMenuForAdapter = mutableListOf<Menu>()
    private lateinit var allMenus: List<Menu>

    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var searchMenu: EditText

    private lateinit var ivCartIcon: ImageView

    private var currentCategoryId: Int? = null // Ubah ke Int? untuk ID kategori
    private var currentSearchQuery: String = ""

    private var currentTenantId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MenuDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerViewMenu = binding.rvMenu

        recyclerViewMenu.layoutManager = LinearLayoutManager(this)

        menuAdapter = MenuAdapter(listMenuForAdapter) {menu, quantity ->
            saveToCart(menu, quantity)
            Log.d("AddMenuCart", "Menu added to cart: $menu, Quantity: $quantity")
        }
        recyclerViewMenu.adapter = menuAdapter

        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        chipAll = findViewById(R.id.chipAll)
        searchMenu = findViewById(R.id.searchMenu)

        setupSearchListener()

        chipAll.setOnClickListener {
            chipAll.isChecked = true
            currentCategoryId = null
            Toast.makeText(this, "Menampilkan semua item", Toast.LENGTH_SHORT).show()
            applyFilters()
        }

        allMenus = emptyList()

        val tenantId = intent.getIntExtra("TENANT_ID", -1)
        Log.d("MenuDetail", "Received tenant ID: $tenantId")

        if (tenantId != -1) {
            fetchCategoriesFromApi()
            getAvailableMenuByTenantId(tenantId)
        } else {
            Log.e("MenuDetailActivity", "Tenant ID tidak valid.")
            Toast.makeText(this, "Tenant ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Ambil tenantId saat Activity ini dibuat
        currentTenantId = intent.getIntExtra("TENANT_ID", -1) // Pastikan ini diambil dari Intent saat MenuDetailActivity diluncurkan
        Log.d("MenuDetail", "Received tenant ID: $currentTenantId")


        if (currentTenantId != -1) {
            fetchCategoriesFromApi()
            getAvailableMenuByTenantId(currentTenantId)
        } else {
            Log.e("MenuDetailActivity", "Tenant ID tidak valid.")
            Toast.makeText(this, "Tenant ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }

        ivCartIcon = findViewById(R.id.ivCartIcon)

        ivCartIcon.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            // >>> PERBAIKAN: Kirim tenantId ke CartActivity <<<
            intent.putExtra("TENANT_ID", currentTenantId) // Kirim tenantId yang sudah didapatkan
            // >>> AKHIR PERBAIKAN <<<
            startActivity(intent)
            Log.d("MenuDetailActivity", "Cart icon clicked. Tenant ID: $currentTenantId")
        }

        // Tidak perlu memanggil updateCartBadge(getCartCount()) di onCreate, onResume sudah cukup
    }

    private fun setupSearchListener() {
        searchMenu.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString().trim()
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun getAvailableMenuByTenantId(tenantId: Int) {
        Log.d("MenuDetailActivity", "Fetching menu for tenant ID: $tenantId")
        RetrofitClient.instance.getAvailableMenuByTenantId(tenantId)
            .enqueue(object : Callback<MenuResponse> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(call: Call<MenuResponse>, response: Response<MenuResponse>) {
                    if (response.isSuccessful) {
                        val menuResponse = response.body()
                        if (menuResponse != null) {
                            val menusFromApi = menuResponse.data
                            Log.d("MenuDetailActivity", "Jumlah menu dari API: ${menusFromApi.size}")

                            allMenus = menusFromApi
                            Log.d("MenuFilter", "getDataMenuByTenantId: allMenus updated with ${allMenus.size} items")

                            listMenuForAdapter.clear()
                            listMenuForAdapter.addAll(menusFromApi)
                            menuAdapter.notifyDataSetChanged()

                            if (menusFromApi.isNotEmpty()) {
                                Log.d("MenuFilter", "Sample menu item: ${menusFromApi.first()}")
                                menusFromApi.first().let { sampleMenu ->
                                    Log.d("MenuFilter", "Sample menu categoryId: ${sampleMenu.categoryId}, categoryName/category: ${sampleMenu.category ?: sampleMenu.category}")
                                }
                            }
                        } else {
                            Log.e("MenuDetailActivity", "Response body is null")
                            Toast.makeText(this@MenuDetailActivity, "Data menu kosong", Toast.LENGTH_SHORT).show()
                            allMenus = emptyList()
                        }
                    } else {
                        Log.e("MenuDetailActivity", "Gagal ambil data menu: ${response.code()} - ${response.message()}")
                        Toast.makeText(this@MenuDetailActivity, "Gagal ambil data menu", Toast.LENGTH_SHORT).show()
                        allMenus = emptyList()
                    }
                }

                override fun onFailure(call: Call<MenuResponse>, t: Throwable) {
                    Log.e("RetrofitError", "Error: ${t.localizedMessage}", t)
                    Toast.makeText(this@MenuDetailActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                    allMenus = emptyList()
                }
            })
    }

    private fun fetchCategoriesFromApi() {
        RetrofitClient.instance.getCategories()
            .enqueue(object : Callback<CategoryModels.CategoryResponse> {
                override fun onResponse(call: Call<CategoryModels.CategoryResponse>, response: Response<CategoryModels.CategoryResponse>) {
                    if (response.isSuccessful) {
                        val categoryResponse = response.body()
                        if (categoryResponse != null && categoryResponse.success) {
                            val categories = categoryResponse.data
                            if (categories.isNotEmpty()) {
                                addChipsToChipGroup(categories)
                            } else {
                                Toast.makeText(this@MenuDetailActivity, "Tidak ada kategori ditemukan.", Toast.LENGTH_SHORT).show()
                                Log.d("API_CALL", "No categories found or empty response body.")
                            }
                        } else {
                            val errorMessage = categoryResponse?.message ?: "Gagal mengambil kategori."
                            Toast.makeText(this@MenuDetailActivity, errorMessage, Toast.LENGTH_SHORT).show()
                            Log.e("API_CALL", "Failed to fetch categories: ${categoryResponse?.message}")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@MenuDetailActivity, "Error: ${response.code()} - ${response.message()} - $errorBody", Toast.LENGTH_SHORT).show()
                        Log.e("API_CALL", "HTTP Error: ${response.code()} - ${response.message()} - $errorBody")
                    }
                }

                override fun onFailure(call: Call<CategoryModels.CategoryResponse>, t: Throwable) {
                    Toast.makeText(this@MenuDetailActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("API_CALL", "Network Error: ${t.message}", t)
                }
            })
    }

    private fun addChipsToChipGroup(categories: List<CategoryModels.Category>) {
        if (chipGroupCategories.childCount > 1) { // Hapus chip dinamis setelah chip "Semua"
            chipGroupCategories.removeViews(1, chipGroupCategories.childCount - 1)
        }

        val layoutInflater = LayoutInflater.from(this)

        categories.forEach { category ->
            val chip = layoutInflater.inflate(R.layout.item_category_chip, chipGroupCategories, false) as Chip
            chip.text = category.name
            chip.tag = category.id // Simpan ID kategori di tag chip
            chip.setTextColor(resources.getColor(R.color.dark_gray, theme))
            chip.chipBackgroundColor = resources.getColorStateList(R.color.light_gray, theme)

            chip.setOnClickListener {
                chipAll.isChecked = false
                currentCategoryId = chip.tag.toString().toIntOrNull() // Konversi tag (ID) ke Int
                Toast.makeText(this, "Kategori dipilih: ${chip.text}", Toast.LENGTH_SHORT).show()
                applyFilters()
            }

            chipGroupCategories.addView(chip)

        }
    }


    private fun applyFilters() {
        var filteredList = allMenus

        Log.d("MenuFilter", "Total menus available: ${allMenus.size}")
        Log.d("MenuFilter", "Current search query: '$currentSearchQuery'")
        Log.d("MenuFilter", "Current category ID (for filter): '$currentCategoryId'")

        if (currentCategoryId != null) {
            filteredList = filteredList.filter { it.categoryId == currentCategoryId }
            Log.d("MenuFilter", "After category filter: ${filteredList.size}")
        }

        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter { menu ->
                val matches = menu.name.contains(currentSearchQuery, ignoreCase = true) ||
                        (menu.description?.contains(currentSearchQuery, ignoreCase = true) == true) ||
                        (menu.category?.contains(currentSearchQuery, ignoreCase = true) == true) ||
                        (menu.category?.contains(currentSearchQuery, ignoreCase = true) == true)

                Log.d("MenuFilter", "Menu search result: $matches for query '$currentSearchQuery'")
                matches
            }
            Log.d("MenuFilter", "After search filter: ${filteredList.size}")
        }

        menuAdapter.updateData(filteredList)
        Log.d("MenuFilter", "Final filtered list size: ${filteredList.size}")

        if (filteredList.isEmpty()) {
            val message = when {
                currentSearchQuery.isNotEmpty() && currentCategoryId != null ->
                    "Tidak ada menu ditemukan untuk pencarian '$currentSearchQuery' dalam kategori ini."
                currentSearchQuery.isNotEmpty() ->
                    "Tidak ada menu ditemukan untuk pencarian '$currentSearchQuery'."
                currentCategoryId != null ->
                    "Tidak ada menu ditemukan untuk kategori ini."
                else ->
                    "Tidak ada menu tersedia."
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToCart(menu: Menu, quantity: Int) {
        val userId = getUserIdFromSharedPrefs()
        Log.d("CartDebug", "⏩ saveToCart dipanggil. userId=$userId, menuId=${menu.id}, qty=$quantity")


        RetrofitClient.instance.addToCart(userId, menu.id, quantity)
            .enqueue(object : Callback<CartSimpleResponse> {
                override fun onResponse(call: Call<CartSimpleResponse>, response: Response<CartSimpleResponse>) {
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.success == true) {
                            Toast.makeText(this@MenuDetailActivity, res.message, Toast.LENGTH_SHORT).show()
                            // Panggil fetchCartCount() untuk update badge secara real-time
                            fetchCartCount()
                        } else {
                            Toast.makeText(this@MenuDetailActivity, res?.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MenuDetailActivity, "Gagal response ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CartSimpleResponse>, t: Throwable) {
                    Toast.makeText(this@MenuDetailActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        // Ini akan selalu memuat ulang jumlah keranjang saat Activity kembali ke foreground
        fetchCartCount()
    }

    private fun fetchCartCount() {
        val userId = getUserIdFromSharedPrefs()
        // Log ini akan memberitahu Anda user ID yang digunakan untuk API call
        Log.d("MenuDetailActivity", "Fetching cart count for User ID: $userId")

        if (userId <= 0) { // Jika user ID tidak valid, set badge ke 0 dan jangan panggil API
            Log.e("MenuDetailActivity", "User ID is invalid or not found in SharedPreferences for fetchCartCount: $userId")
            updateCartBadge(0)
            return
        }

        RetrofitClient.instance.getCartItems(userId).enqueue(object : Callback<CartResponse> {
            override fun onResponse(call: Call<CartResponse>, response: Response<CartResponse>) {
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.success == true && res.data != null) {
                        saveCartCount(res.data.size)
                        updateCartBadge(res.data.size)
                        Log.d("MenuDetailActivity", "Cart count: ${res.data.size} fetched and badge updated.")
                    } else {
                        Log.e("MenuDetailActivity", "Cart count API success but data null or success=false: ${res?.message}")
                        saveCartCount(0)
                        updateCartBadge(0)
                    }
                } else {
                    Log.e("MenuDetailActivity", "Failed to fetch cart items (HTTP error): ${response.code()}")
                    saveCartCount(0)
                    updateCartBadge(0)
                }
            }

            override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                Log.e("MenuDetailActivity", "Failed to fetch cart items (network error): ${t.message}")
                saveCartCount(0)
                updateCartBadge(0)
            }
        })
    }

    private fun getUserIdFromSharedPrefs(): Int {
        // PERBAIKAN PENTING: Gunakan nama file Shared Preferences yang konsisten
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE) // Ganti "MyAppPrefs"
        val userId = sharedPref.getString("user_id", null)?.toIntOrNull() ?: 0
        Log.d("User debug", "User ID dari SharedPreferences di MenuDetailActivity: $userId")
        return userId
    }

    private fun updateCartBadge(count: Int) {
        BadgeUtils.setCartBadge(this, binding.ivCartIcon, binding.tvCartBadge, count)
    }

    private fun saveCartCount(count: Int) {
        // PERBAIKAN PENTING: Gunakan nama file Shared Preferences yang konsisten
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE) // Ganti "UserPrefs"
        sharedPrefs.edit().putInt("cart_count", count).apply()
    }

    private fun getCartCount(): Int {
        // PERBAIKAN PENTING: Gunakan nama file Shared Preferences yang konsisten
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE) // Ganti "UserPrefs"
        return sharedPrefs.getInt("cart_count", 0)
    }
}
