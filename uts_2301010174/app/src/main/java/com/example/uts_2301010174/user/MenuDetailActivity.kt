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

    private lateinit var binding: MenuDetailBinding // Jika menggunakan ViewBinding
    private lateinit var recyclerViewMenu: RecyclerView
    private lateinit var menuAdapter: MenuAdapter
    private var listMenuForAdapter = mutableListOf<Menu>() // List yang akan digunakan oleh adapter
    private lateinit var allMenus: List<Menu> // Simpan semua menu di sini

    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var searchMenu: EditText // Search EditText

    private lateinit var ivCartIcon: ImageView

    // Variabel untuk menyimpan filter yang sedang aktif
    private var currentCategoryId: String? = null
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MenuDetailBinding.inflate(layoutInflater) // Jika menggunakan ViewBinding
        setContentView(binding.root) // Jika menggunakan ViewBinding
        // setContentView(R.layout.activity_menu_detail) // Jika tidak menggunakan ViewBinding

        recyclerViewMenu = binding.rvMenu // Jika menggunakan ViewBinding dan ID-nya rvMenu
        // recyclerViewMenu = findViewById(R.id.rvMenu) // Jika tidak menggunakan ViewBinding

        recyclerViewMenu.layoutManager = LinearLayoutManager(this)

        // Inisialisasi adapter dengan list kosong (atau list yang sudah ada jika ada)
        menuAdapter = MenuAdapter(listMenuForAdapter) {menu, quantity ->
            saveToCart(menu, quantity)
            Log.d("AddMenuCart", "Menu added to cart: $menu, Quantity: $quantity")
        }
        recyclerViewMenu.adapter = menuAdapter

        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        chipAll = findViewById(R.id.chipAll)
        searchMenu = findViewById(R.id.searchMenu) // Inisialisasi search EditText

        setupSearchListener() // Setup listener untuk search

        chipAll.setOnClickListener {
            chipAll.isChecked = true
            currentCategoryId = null // Reset category filter
            Toast.makeText(this, "Menampilkan semua item", Toast.LENGTH_SHORT).show()
            applyFilters() // Terapkan filter dengan search query yang ada
        }

        allMenus = emptyList() // Inisialisasi kosong

        val tenantId = intent.getIntExtra("TENANT_ID", -1)
        Log.d("MenuDetail", "Received tenant ID: $tenantId")

        if (tenantId != -1) {
            // Panggil fungsi untuk mengambil kategori dari API
            fetchCategoriesFromApi()

            // Hanya panggil satu API - berdasarkan tenant ID
            getAvailableMenuByTenantId(tenantId)
        } else {
            Log.e("MenuDetailActivity", "Tenant ID tidak valid.")
            Toast.makeText(this, "Tenant ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Setup tombol kembali
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // kembali ke activity sebelumnya (MainActivity)
        }

        ivCartIcon = findViewById(R.id.ivCartIcon)

        ivCartIcon.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        // Perbarui badge dari SharedPreferences
        updateCartBadge(getCartCount())
    }

    // Setup listener untuk search EditText
    private fun setupSearchListener() {
        searchMenu.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString().trim()
                applyFilters() // Terapkan filter setiap kali text berubah
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun getAvailableMenuByTenantId(tenantId: Int) {
        Log.d("MenuDetailActivity", "Fetching menu for tenant ID: $tenantId")
        // Menggunakan Retrofit Call (seperti pada kode Anda sebelumnya)
        RetrofitClient.instance.getAvailableMenuByTenantId(tenantId) // Asumsi method ini mengembalikan Call<MenuResponse>
            .enqueue(object : Callback<MenuResponse> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(call: Call<MenuResponse>, response: Response<MenuResponse>) {
                    if (response.isSuccessful) {
                        val menuResponse = response.body()
                        if (menuResponse != null) {
                            // AMBIL LIST MENU DARI PROPERTI 'data' DI MenuResponse
                            val menusFromApi = menuResponse.data
                            Log.d("MenuDetailActivity", "Jumlah menu dari API: ${menusFromApi.size}")

                            // PENTING: Simpan ke allMenus untuk search functionality
                            allMenus = menusFromApi
                            Log.d("MenuFilter", "getDataMenuByTenantId: allMenus updated with ${allMenus.size} items")

                            // Update adapter
                            listMenuForAdapter.clear()
                            listMenuForAdapter.addAll(menusFromApi)
                            menuAdapter.notifyDataSetChanged()

                            // Log beberapa sample menu untuk debugging
                            if (menusFromApi.isNotEmpty()) {
                                Log.d("MenuFilter", "Sample menu item: ${menusFromApi.first()}")
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
            chip.tag = category.name // Simpan ID kategori di tag chip
            chip.setTextColor(resources.getColor(R.color.dark_gray, theme))
            chip.chipBackgroundColor = resources.getColorStateList(R.color.light_gray, theme)

            chip.setOnClickListener {
                chipAll.isChecked = false
                currentCategoryId = chip.tag.toString()
                Toast.makeText(this, "Kategori dipilih: ${chip.text}", Toast.LENGTH_SHORT).show()
                applyFilters()
            }

            chipGroupCategories.addView(chip)

        }
    }


    // Fungsi utama untuk menerapkan semua filter (kategori + search)
    private fun applyFilters() {
        var filteredList = allMenus

        // Debug: Log jumlah menu yang tersedia
        Log.d("MenuFilter", "Total menus available: ${allMenus.size}")
        Log.d("MenuFilter", "Current search query: '$currentSearchQuery'")
        Log.d("MenuFilter", "Current category ID: '$currentCategoryId'")

        // Filter berdasarkan kategori
        if (!currentCategoryId.isNullOrEmpty()) {
            filteredList = filteredList.filter { it.category == currentCategoryId }
            Log.d("MenuFilter", "After category filter: ${filteredList.size}")
        }

        // Filter berdasarkan search query
        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter { menu ->
                // Debug: Log semua properti menu menggunakan reflection
                try {
                    val menuClass = menu.javaClass
                    val fields = menuClass.declaredFields
                    Log.d("MenuFilter", "=== Menu Object Debug ===")
                    fields.forEach { field ->
                        field.isAccessible = true
                        val value = field.get(menu)
                        Log.d("MenuFilter", "Field: ${field.name} = $value (${field.type.simpleName})")
                    }
                    Log.d("MenuFilter", "========================")
                } catch (e: Exception) {
                    Log.e("MenuFilter", "Error debugging menu object: ${e.message}")
                }

                // Coba semua field String dalam object Menu
                val result = try {
                    val menuClass = menu.javaClass
                    val fields = menuClass.declaredFields

                    fields.any { field ->
                        if (field.type == String::class.java) {
                            field.isAccessible = true
                            val value = field.get(menu) as? String
                            val matches = value?.contains(currentSearchQuery, ignoreCase = true) == true
                            if (matches) {
                                Log.d("MenuFilter", "Match found in field '${field.name}': '$value'")
                            }
                            matches
                        } else {
                            false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MenuFilter", "Error in search filter: ${e.message}")
                    false
                }

                Log.d("MenuFilter", "Menu search result: $result for query '$currentSearchQuery'")
                result
            }
            Log.d("MenuFilter", "After search filter: ${filteredList.size}")
        }

        menuAdapter.updateData(filteredList) // Perbarui RecyclerView
        Log.d("MenuFilter", "Final filtered list size: ${filteredList.size}")

        // Tampilkan pesan jika tidak ada hasil
        if (filteredList.isEmpty()) {
            val message = when {
                currentSearchQuery.isNotEmpty() && !currentCategoryId.isNullOrEmpty() ->
                    "Tidak ada menu ditemukan untuk pencarian '$currentSearchQuery' dalam kategori ini."
                currentSearchQuery.isNotEmpty() ->
                    "Tidak ada menu ditemukan untuk pencarian '$currentSearchQuery'."
                !currentCategoryId.isNullOrEmpty() ->
                    "Tidak ada menu ditemukan untuk kategori ini."
                else ->
                    "Tidak ada menu tersedia."
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi untuk memfilter dan menampilkan menu secara lokal (DEPRECATED - diganti dengan applyFilters)
    private fun displayFilteredMenus(categoryId: String?) {
        currentCategoryId = categoryId
        applyFilters()
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

    private fun getUserIdFromSharedPrefs(): Int {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)?.toIntOrNull() ?: 0
        Log.d("User debug", "User ID dari SharedPreferences: $userId")
        return userId
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



}


