package com.example.uts_2301010174.tenant

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uts_2301010174.RetrofitClient
import com.example.uts_2301010174.adapter.MenuTenantAdapter
import com.example.uts_2301010174.databinding.ActivityMainTenantBinding
import com.example.uts_2301010174.user.Menu
import com.example.uts_2301010174.user.MenuResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainTenantActivity : AppCompatActivity(), MenuTenantAdapter.OnItemActionListener {

    private lateinit var binding: ActivityMainTenantBinding
    private lateinit var menuAdapter: MenuTenantAdapter
    private lateinit var sharedPref: SharedPreferences
    private var allMenus: List<Menu> = emptyList()
    private val listMenuForAdapter = mutableListOf<Menu>()
    private lateinit var recyclerViewMenu: RecyclerView


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
}


