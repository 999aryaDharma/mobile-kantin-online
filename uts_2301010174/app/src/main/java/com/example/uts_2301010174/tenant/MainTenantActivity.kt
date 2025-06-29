package com.example.uts_2301010174.tenant

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uts_2301010174.ApiService
import com.example.uts_2301010174.MenuViewModelFactory
import com.example.uts_2301010174.R
import com.example.uts_2301010174.RetrofitClient
import com.example.uts_2301010174.adapter.MenuTenantAdapter
import com.example.uts_2301010174.databinding.ActivityMainTenantBinding
import com.example.uts_2301010174.repository.MenuRepository
import com.example.uts_2301010174.user.Menu
import com.example.uts_2301010174.user.MenuResponse
import com.example.uts_2301010174.viewModel.MenuViewModel
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Konstanta untuk kode permintaan (request codes)
const val ADD_MENU_REQUEST_CODE = 101
const val EDIT_MENU_REQUEST_CODE = 102

class MainTenantActivity : AppCompatActivity(), MenuTenantAdapter.OnItemActionListener {

    private lateinit var binding: ActivityMainTenantBinding
    private lateinit var menuAdapter: MenuTenantAdapter
    private lateinit var sharedPref: SharedPreferences
    private var allMenus: List<Menu> = emptyList()
    private val listMenuForAdapter = mutableListOf<Menu>()
    private lateinit var recyclerViewMenu: RecyclerView
    private lateinit var btnAddMenu: CardView

    private var menuViewModel: MenuViewModel? = null

    private lateinit var apiService: ApiService
    private lateinit var menuRepository: MenuRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViewBinding()
        setupSharedPreferences()
        checkTenantIdAndLoadData()

        recyclerViewMenu = binding.rvMenuTenant
        menuAdapter = MenuTenantAdapter(listMenuForAdapter)
        recyclerViewMenu.layoutManager = LinearLayoutManager(this)
        recyclerViewMenu.adapter = menuAdapter

        // Pasang listener ke adapter untuk menerima callback
        menuAdapter.setOnItemActionListener(this)

        setupSearchListener()

        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val tenantId = sharedPref.getInt("tenant_id", -1)
        if (tenantId == -1) {
            Toast.makeText(this, "Tenant ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnAddMenu: CardView = findViewById(R.id.btnAddMenu)

        btnAddMenu.setOnClickListener {
            val intent = Intent(this, AddMenuActivity::class.java)
            addMenuLauncher.launch(intent)
        }

        // --- SET UP LONG CLICK LISTENER DARI ADAPTER ---
        menuAdapter.setOnItemActionListener(this)

        val baseUrl = "http://10.0.2.2/api_menu/" // Ganti dengan Base URL API Anda
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // --- Inisialisasi ApiService tunggal yang akan digunakan ---
        try {
            apiService = retrofit.create(ApiService::class.java) // Inisialisasi di sini
            Log.d("MainTenantActivity", "ApiService initialized successfully.")
        } catch (e: Exception) {
            Log.e("MainTenantActivity", "FATAL ERROR during ApiService initialization: ${e.message}", e)
            Toast.makeText(this, "Gagal menginisialisasi layanan API: ${e.message}. Cek log!", Toast.LENGTH_LONG).show()
            finish() // Tutup activity jika API service gagal
            return
        }

        // --- Inisialisasi MenuViewModel ---
        try {
            Log.d("MainTenantActivity", "Attempting to initialize MenuViewModel...")
            // MenuRepository memerlukan context untuk operasi file
            menuRepository = MenuRepository(apiService, applicationContext) // Gunakan apiService yang sudah diinisialisasi
            Log.d("MainTenantActivity", "MenuRepository initialized.")

            val menuFactory = MenuViewModelFactory(application, menuRepository) // Pastikan MenuViewModelFactory Anda menerima Application
            Log.d("MainTenantActivity", "MenuViewModelFactory created.")

            menuViewModel = ViewModelProvider(this, menuFactory).get(MenuViewModel::class.java)
            Log.d("MainTenantActivity", "MenuViewModel initialized successfully.")

            // Mengamati hasil penambahan menu dari MenuViewModel
            menuViewModel?.addMenuResult?.observe(this) { success ->
                if (success) {
                    Toast.makeText(this, "Menu berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    // Refresh daftar menu setelah penambahan berhasil
                    val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val tenantId = sharedPref.getInt("tenant_id", -1)
                    if (tenantId != -1) {
                        fetchMenuByTenantId(tenantId)
                    }
                }
            }
            menuViewModel?.addMenuError?.observe(this) { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(this, "Gagal menambahkan menu: $it", Toast.LENGTH_LONG).show()
                    Log.e("MainTenantActivity", "Error adding menu: $it")
                }
            }
            // Mengamati hasil penghapusan menu dari MenuViewModel
            menuViewModel?.deleteMenuResult?.observe(this) { success ->
                if (success) {
                    Toast.makeText(this, "Menu berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val tenantId = sharedPref.getInt("tenant_id", -1)
                    if (tenantId != -1) {
                        fetchMenuByTenantId(tenantId)
                    }
                }
            }
            menuViewModel?.deleteMenuError?.observe(this) { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(this, "Gagal menghapus menu: $it", Toast.LENGTH_LONG).show()
                    Log.e("MainTenantActivity", "Error deleting menu: $it")
                }
            }


        } catch (e: Exception) {
            Log.e("MainTenantActivity", "FATAL ERROR during MenuViewModel initialization: ${e.message}", e)
            Toast.makeText(this, "Gagal menginisialisasi Menu ViewModel: ${e.message}. Cek log!", Toast.LENGTH_LONG).show()
            // Menutup activity atau menonaktifkan fitur terkait jika ViewModel gagal diinisialisasi
            finish() // Menghentikan aktivitas jika ViewModel gagal
            return
        }


    }

    // >>> DEKLARASI ActivityResultLauncher <<<
    private val addMenuLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Ini adalah callback ketika AddMenuActivity selesai
            if (result.resultCode == RESULT_OK) {
                // Menu berhasil ditambahkan/diubah, refresh daftar
                Toast.makeText(this, "Daftar menu di-refresh karena ada perubahan.", Toast.LENGTH_SHORT).show()
                val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val tenantId = sharedPref.getInt("tenant_id", -1)
                if (tenantId != -1) {
                    fetchMenuByTenantId(tenantId)
                }
            } else if (result.resultCode == RESULT_CANCELED) {
                // Aktivitas dibatalkan atau ada masalah
                Toast.makeText(this, "Operasi penambahan menu dibatalkan atau gagal.", Toast.LENGTH_SHORT).show()
            }
        }

    private val editMenuLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Daftar menu di-refresh karena ada perubahan (dari edit).", Toast.LENGTH_SHORT).show()
                val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val tenantId = sharedPref.getInt("tenant_id", -1)
                if (tenantId != -1) {
                    fetchMenuByTenantId(tenantId)
                }
            } else if (result.resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Operasi edit menu dibatalkan atau gagal.", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onResume() {
        super.onResume()
        // Panggil fetchMenuByTenantId di onResume
        // Ini memastikan data dimuat ulang setiap kali Activity kembali ke foreground
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val tenantId = sharedPref.getInt("tenant_id", -1)
        if (tenantId != -1) {
            fetchMenuByTenantId(tenantId)
            Log.d("MainTenantActivity", "Fetching menus in onResume for Tenant ID: $tenantId")
        } else {
            Toast.makeText(this, "ID Tenant tidak ditemukan. Tidak dapat memuat menu.", Toast.LENGTH_LONG).show()
            Log.e("MainTenantActivity", "Tenant ID is -1 in onResume. Cannot fetch menus.")
        }
    }


    private fun setupViewBinding() {
        binding = ActivityMainTenantBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun setupSharedPreferences() {
        sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    }

    private fun checkTenantIdAndLoadData() {
        val tenantId = sharedPref.getInt("tenant_id", -1)
        if (tenantId == -1) {
            showToast("Tenant ID tidak ditemukan")
            finish()
        } else {
            fetchMenuByTenantId(tenantId)
        }
    }

    private fun fetchMenuByTenantId(tenantId: Int) {
        Log.d("MainTenantActivity", "Fetching menu for tenant ID: $tenantId")

        RetrofitClient.instance.getDataMenuByTenantId(tenantId)
            .enqueue(object : Callback<MenuResponse> {

                override fun onResponse(call: Call<MenuResponse>, response: Response<MenuResponse>) {
                    if (response.isSuccessful) {
                        val menusFromApi = response.body()?.data.orEmpty()
                        Log.d("MainTenantActivity", "Jumlah menu dari API: ${menusFromApi.size}")

                        allMenus = menusFromApi
                        updateMenuList(menusFromApi)

                        menusFromApi.firstOrNull()?.let {
                            Log.d("MainTenantActivity", "Contoh menu item: $it")
                        }

                    } else {
                        Log.e("MainTenantActivity", "Gagal ambil data menu: ${response.code()} - ${response.message()}")
                        showToast("Gagal ambil data menu")
                        allMenus = emptyList()
                    }
                }

                override fun onFailure(call: Call<MenuResponse>, t: Throwable) {
                    Log.e("MainTenantActivity", "Retrofit error: ${t.localizedMessage}", t)
                    showToast("Koneksi gagal: ${t.message}")
                    allMenus = emptyList()
                }
            })
    }

    private fun setupSearchListener() {
        binding.edtSearchMenu.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applyFilter(query: String) {
        val filteredList = if (query.isEmpty()) {
            allMenus
        } else {
            allMenus.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }

        listMenuForAdapter.clear()
        listMenuForAdapter.addAll(filteredList)
        menuAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateMenuList(newMenus: List<Menu>) {
        listMenuForAdapter.clear()
        listMenuForAdapter.addAll(newMenus)
        menuAdapter.notifyDataSetChanged()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Implementasi callback dari adapter saat switch availability berubah
    override fun onSwitchAvailabilityChanged(menuId: Int, isChecked: Boolean) {
        // Cari menu di listMenuForAdapter dan update statusnya
        val index = listMenuForAdapter.indexOfFirst { it.id == menuId}
        if (index != -1) {
            val menu = listMenuForAdapter[index]
            menu.isAvailable = if (isChecked) 1 else 0
            // Notify perubahan pada item tersebut
            menuAdapter.notifyItemChanged(index)

            // Opsional: update data di allMenus juga supaya search tetap sinkron
            val indexAll = allMenus.indexOfFirst { it.id == menuId }
            if (indexAll != -1) {
                val mutableAllMenus = allMenus.toMutableList()
                mutableAllMenus[indexAll].isAvailable = menu.isAvailable
                allMenus = mutableAllMenus.toList()
            }

            // TODO: Kirim update ke server (backend API)
            updateMenuAvailability(menuId, menu.isAvailable)
        }
    }

    override fun onMenuItemLongClick(menu: Menu): Boolean {
        // Menampilkan dialog opsi Edit/Delete
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Pilih Aksi untuk ${menu.name}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Opsi Edit
                        Toast.makeText(this, "Edit menu: ${menu.name}", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, EditMenuActivity::class.java)
                        intent.putExtra("menu_data", menu) // Kirim objek Menu ke EditMenuActivity
                        // Menggunakan ActivityResultLauncher BARU untuk Edit
                        editMenuLauncher.launch(intent)
                    }
                    1 -> { // Opsi Delete
                        showCustomDeleteConfirmationDialog(menu) // Memanggil custom dialog
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        return true // Mengembalikan true untuk menandakan event dikonsumsi
    }

    private fun updateMenuAvailability(menuId: Int, isAvailable: Int) {
        val body = mapOf("isAvailable" to isAvailable)
        RetrofitClient.instance.updateMenuAvailability(menuId, body)
        // Contoh implementasi request retrofit update status menu ke server
        RetrofitClient.instance.updateMenuAvailability(menuId, body)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Log.d("MainTenantActivity", "Update status menu berhasil")
                    } else {
                        Log.e("MainTenantActivity", "Gagal update status menu: ${response.code()}")
                        showToast("Gagal update status menu di server")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("MainTenantActivity", "Error update status menu: ${t.localizedMessage}")
                    showToast("Error update status menu: ${t.message}")
                }
            })
    }

    @SuppressLint("MissingInflatedId")
    private fun showCustomDeleteConfirmationDialog(menu: Menu) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_delete_menu, null)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogPesan)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)

        dialogTitle.text = "Konfirmasi Hapus Menu"
        dialogMessage.text = "Anda yakin ingin menghapus menu '${menu.name}'?"

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
            Toast.makeText(this, "Penghapusan dibatalkan", Toast.LENGTH_SHORT).show()
        }

        btnDelete.setOnClickListener {
            alertDialog.dismiss()
            // --- VERIFIKASI SEBELUM PANGGIL VIEWMODEL ---
            Log.d("MainTenantActivity", "Attempting to delete menu ID: ${menu.id}")
            if (menuViewModel != null) {
                Log.d("MainTenantActivity", "menuViewModel is NOT null. Calling deleteMenu...")
                menuViewModel?.deleteMenu(menu.id)
            } else {
                Log.e("MainTenantActivity", "menuViewModel is NULL. Cannot delete menu ID: ${menu.id}")
                Toast.makeText(this, "Gagal menghapus: ViewModel tidak siap.", Toast.LENGTH_LONG).show()
            }
            // --- AKHIR VERIFIKASI ---
        }

        alertDialog.show()
    }
}


