package com.example.uts_2301010174.tenant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.uts_2301010174.ApiService
import com.example.uts_2301010174.MenuViewModelFactory
import com.example.uts_2301010174.R
import com.example.uts_2301010174.repository.CategoryRepository
import com.example.uts_2301010174.repository.MenuRepository
import com.example.uts_2301010174.viewModel.CategoryViewModel
import com.example.uts_2301010174.viewModel.CategoryViewModelFactory
import com.example.uts_2301010174.viewModel.MenuViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AddMenuActivity : AppCompatActivity() {

    private lateinit var editTextNamaMenu: EditText
    private lateinit var editTextDeskripsi: EditText
    private lateinit var editTextHarga: EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var cardUploadImage: CardView
    private lateinit var imageViewPreview: ImageView
    private lateinit var layoutUploadPrompt: LinearLayout
    private lateinit var textViewChangeImage: TextView
    private lateinit var buttonSimpanMenu: CardView

    private var selectedImageUri: Uri? = null
    private var selectedCategoryName: String = ""
    private var selectedCategoryId: Int = -1

    private lateinit var sharedPreferences: SharedPreferences

    // Deklarasi ViewModel sebagai nullable
    private var categoryViewModel: CategoryViewModel? = null
    private var menuViewModel: MenuViewModel? = null

    private lateinit var btnBack: ImageButton

    // Activity Result Launchers untuk galeri, kamera, dan izin
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            showImagePreview(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                showImagePreview(it)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Permission diperlukan untuk mengakses kamera", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_menu)

        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener(){
            setResult(RESULT_CANCELED)
            finish()
        }

        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE) // "user_prefs" adalah nama file SharedPreferences Anda

        // Ubah warna status bar
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_yellow)

        initViews() // Inisialisasi UI Views terlebih dahulu

        // BASE URL yang sama untuk semua API
        // PENTING: GANTI DENGAN BASE URL API ANDA YANG VALID!
        // Contoh: "https://your-api-domain.com/" atau "http://10.0.2.2:8080/" (untuk emulator lokal)
        val base_url = "http://10.0.2.2/api_menu/"

        // Inisialisasi Retrofit sekali
        val retrofit = Retrofit.Builder()
            .baseUrl(base_url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // --- Inisialisasi ApiService tunggal yang akan digunakan oleh kedua ViewModel ---
        // Ini adalah poin perubahan utama.
        val combinedApiService: ApiService // Deklarasi satu instance ApiService
        try {
            combinedApiService = retrofit.create(ApiService::class.java)
        } catch (e: Exception) {
            Log.e("AddMenuActivity", "Error creating combined ApiService: ${e.message}", e)
            Toast.makeText(this, "Gagal menginisialisasi layanan API: ${e.message}", Toast.LENGTH_LONG).show()
            // Nonaktifkan semua UI yang bergantung pada API jika layanan gagal diinisialisasi
            spinnerKategori.isEnabled = false
            buttonSimpanMenu.isEnabled = false
            return // Hentikan eksekusi onCreate jika ApiService gagal
        }


        // --- Inisialisasi Category ViewModel ---
        try {
            // Gunakan combinedApiService untuk CategoryRepository
            val categoryRepository = CategoryRepository(combinedApiService)
            val categoryFactory = CategoryViewModelFactory(application, categoryRepository)
            categoryViewModel = ViewModelProvider(this, categoryFactory).get(CategoryViewModel::class.java)

            // Mengamati perubahan pada LiveData dari CategoryViewModel
            observeViewModel() // Fungsi ini berisi pengamatan LiveData CategoryViewModel
            categoryViewModel?.fetchCategories() // Memulai pengambilan kategori

        } catch (e: Exception) {
            Log.e("AddMenuActivity", "Error during CategoryViewModel initialization: ${e.message}", e)
            Toast.makeText(this, "Gagal menginisialisasi Category ViewModel: ${e.message}", Toast.LENGTH_LONG).show()
            spinnerKategori.isEnabled = false // Nonaktifkan spinner jika ViewModel kategori gagal
            setResult(RESULT_CANCELED)
        }

        // --- Inisialisasi Menu ViewModel ---
        try {
            // Gunakan combinedApiService untuk MenuRepository
            val menuRepository = MenuRepository(combinedApiService, applicationContext)
            val menuFactory = MenuViewModelFactory(application, menuRepository)
            menuViewModel = ViewModelProvider(this, menuFactory)[MenuViewModel::class.java]

            // Mengamati hasil penambahan menu dari MenuViewModel
            menuViewModel?.addMenuResult?.observe(this) { success ->
                if (success) {
                    Toast.makeText(this, "Menu berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK) // Memberi sinyal bahwa ada perubahan
                    finish() // Tutup activity setelah berhasil menyimpan menu
                }
            }
            // Mengamati pesan error dari MenuViewModel
            menuViewModel?.addMenuError?.observe(this) { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(this, "Gagal menambahkan menu: $it", Toast.LENGTH_LONG).show()
                    Log.e("AddMenuActivity", "Error adding menu: $it")
                    setResult(RESULT_CANCELED)
                }
            }

        } catch (e: Exception) {
            Log.e("AddMenuActivity", "Error during MenuViewModel initialization: ${e.message}", e)
            Toast.makeText(this, "Gagal menginisialisasi Menu ViewModel: ${e.message}", Toast.LENGTH_LONG).show()
            // Anda bisa menonaktifkan tombol simpan atau elemen UI lain jika MenuViewModel gagal
            buttonSimpanMenu.isEnabled = false
        }

        setupClickListeners() // Menyiapkan listener untuk tombol dan elemen interaktif lainnya
    }

    private fun initViews() {
        editTextNamaMenu = findViewById(R.id.editTextNamaMenu)
        editTextDeskripsi = findViewById(R.id.editTextDeskripsi)
        editTextHarga = findViewById(R.id.editTextHarga)
        spinnerKategori = findViewById(R.id.spinnerKategori)
        cardUploadImage = findViewById(R.id.cardUploadImage)
        imageViewPreview = findViewById(R.id.imageViewPreview)
        layoutUploadPrompt = findViewById(R.id.layoutUploadPrompt)
        textViewChangeImage = findViewById(R.id.textViewChangeImage)
        buttonSimpanMenu = findViewById(R.id.buttonSimpanMenu)
    }

    private fun observeViewModel() {
        // Mengamati perubahan pada daftar kategori dari CategoryViewModel
        categoryViewModel?.categories?.observe(this) { categoryList ->
            if (categoryList.isNotEmpty()) {
                Log.d("AddMenuActivity", "Daftar kategori diterima: ${categoryList.size} item")

                val categoryNames = categoryList.map { it.name }.toMutableList()
                categoryNames.add(0, "Pilih Kategori") // Tambahkan placeholder di awal

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    categoryNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerKategori.adapter = adapter

                // Set listener untuk pemilihan item pada Spinner
                spinnerKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position == 0) { // Jika placeholder "Pilih Kategori" yang dipilih
                            selectedCategoryName = ""
                            selectedCategoryId = -1
                            Log.d("AddMenuActivity", "Placeholder 'Pilih Kategori' dipilih.")
                        } else {
                            // Indeks disesuaikan karena ada placeholder di indeks 0
                            val selectedCatObject = categoryList[position - 1]
                            selectedCategoryName = selectedCatObject.name
                            selectedCategoryId = selectedCatObject.id
                            Log.d("AddMenuActivity", "Kategori dipilih: ID=${selectedCategoryId}, Nama='${selectedCategoryName}'")
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedCategoryName = ""
                        selectedCategoryId = -1
                        Log.d("AddMenuActivity", "Tidak ada kategori yang dipilih.")
                    }
                }

                // Jika kategori sebelumnya telah dipilih (misalnya, saat rotasi layar), coba atur pilihan Spinner
                if (selectedCategoryId != -1) {
                    val index = categoryList.indexOfFirst { it.id == selectedCategoryId }
                    if (index != -1) {
                        spinnerKategori.setSelection(index + 1)
                    }
                }

            } else {
                Log.d("AddMenuActivity", "Daftar kategori kosong atau belum dimuat sepenuhnya.")
                Toast.makeText(this, "Tidak ada kategori tersedia.", Toast.LENGTH_SHORT).show()
                spinnerKategori.isEnabled = false // Nonaktifkan spinner
            }
        }

        // Mengamati status loading kategori
        categoryViewModel?.isLoading?.observe(this) { isLoading ->
            spinnerKategori.isEnabled = !isLoading // Nonaktifkan spinner saat loading
            if (isLoading) {
                // Opsional: Tampilkan indikator loading lain
            }
        }

        // Mengamati pesan error kategori
        categoryViewModel?.error?.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, "Error memuat kategori: $it", Toast.LENGTH_LONG).show()
                Log.e("AddMenuActivity", "Error dari CategoryViewModel: $it")
                spinnerKategori.isEnabled = false // Nonaktifkan spinner saat error
            }
        }
    }

    private fun setupClickListeners() {
        cardUploadImage.setOnClickListener {
            showImageSourceDialog()
        }

        textViewChangeImage.setOnClickListener {
            showImageSourceDialog()
        }

        buttonSimpanMenu.setOnClickListener {
            saveMenu() // Memanggil fungsi untuk menyimpan menu
        }
    }

    // Fungsi-fungsi untuk pemilihan dan preview gambar (tidak ada perubahan signifikan)
    private fun showImageSourceDialog() {
        val options = arrayOf("Galeri", "Kamera")

        AlertDialog.Builder(this)
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> checkCameraPermission()
                }
            }
            .show()
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun showImagePreview(uri: Uri) {
        layoutUploadPrompt.isVisible = false
        imageViewPreview.isVisible = true
        textViewChangeImage.isVisible = true

        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(imageViewPreview)
    }

    private fun showImagePreview(bitmap: Bitmap) {
        layoutUploadPrompt.isVisible = false
        imageViewPreview.isVisible = true
        textViewChangeImage.isVisible = true

        imageViewPreview.setImageBitmap(bitmap)
    }

    private fun saveMenu() {
        val namaMenu = editTextNamaMenu.text.toString().trim()
        val deskripsi = editTextDeskripsi.text.toString().trim()
        val hargaText = editTextHarga.text.toString().trim()
        val photoUri = selectedImageUri // Uri gambar yang dipilih

        // --- Validasi Input Data ---
        if (namaMenu.isEmpty()) {
            editTextNamaMenu.error = "Nama menu harus diisi"
            editTextNamaMenu.requestFocus()
            return
        }
        if (deskripsi.isEmpty()) {
            editTextDeskripsi.error = "Deskripsi harus diisi"
            editTextDeskripsi.requestFocus()
            return
        }
        val harga = hargaText.toDoubleOrNull()
        if (harga == null) {
            editTextHarga.error = "Harga tidak valid"
            editTextHarga.requestFocus()
            return
        }
        if (selectedCategoryId == -1) {
            Toast.makeText(this, "Mohon pilih kategori", Toast.LENGTH_SHORT).show()
            return
        }
        if (photoUri == null) {
            Toast.makeText(this, "Mohon pilih gambar menu", Toast.LENGTH_SHORT).show()
            return
        }

        // --- Mengambil tenant_id (ini harus datang dari sesi login atau SharedPreferences) ---
        // GANTI DENGAN LOGIKA PEMANGGILAN ID TENANT YANG SEBENARNYA!

        val tenantId = sharedPreferences.getInt("tenant_id", -1) // -1 sebagai nilai default jika tidak ditemukan
        Log.e("UserTenant", "Tenant ID retrieved from SharedPreferences: $tenantId")

        if (tenantId == -1) {
            Toast.makeText(this, "ID tenant tidak ditemukan. Mohon login ulang.", Toast.LENGTH_LONG).show()
            Log.e("AddMenuActivity", "Tenant ID not found in SharedPreferences!")
            return // Hentikan proses jika tenantId tidak valid
        }


        // Memanggil fungsi addNewMenu dari MenuViewModel
        menuViewModel?.addNewMenu(
            tenantId = tenantId,
            categoryId = selectedCategoryId.takeIf { it != -1 }, // Kirim null jika tidak ada kategori dipilih
            name = namaMenu,
            description = deskripsi.takeIf { it.isNotEmpty() }, // Kirim null jika deskripsi kosong
            photoUri = photoUri,
            price = harga
        )
    }
}
