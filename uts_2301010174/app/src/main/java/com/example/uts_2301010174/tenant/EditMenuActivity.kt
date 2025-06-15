package com.example.uts_2301010174.tenant

// Import ViewModel, Factory, Repository, ApiService, dan Models yang diperlukan

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
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
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
import com.example.uts_2301010174.user.Menu
import com.example.uts_2301010174.viewModel.CategoryViewModel
import com.example.uts_2301010174.viewModel.CategoryViewModelFactory
import com.example.uts_2301010174.viewModel.MenuViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EditMenuActivity : AppCompatActivity() {

    // UI elements
    private lateinit var editTextNamaMenu: EditText
    private lateinit var editTextDeskripsi: EditText
    private lateinit var editTextHarga: EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var cardUploadImage: CardView
    private lateinit var imageViewPreview: ImageView
    private lateinit var layoutUploadPrompt: LinearLayout
    private lateinit var textViewChangeImage: TextView
    private lateinit var buttonSimpanPerubahan: CardView // Berubah nama untuk kejelasan
    private lateinit var btnBack: ImageButton

    // Data terkait menu yang akan diedit
    private var currentMenu: Menu? = null // Objek Menu yang sedang diedit
    private var selectedImageUri: Uri? = null // Uri baru jika gambar diubah
    private var selectedCategoryName: String = ""
    private var selectedCategoryId: Int = -1

    // ViewModel dan SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences
    private var categoryViewModel: CategoryViewModel? = null
    private var menuViewModel: MenuViewModel? = null

    // Activity Result Launchers
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
                // Konversi bitmap ke Uri untuk penanganan yang konsisten
                val tempUri = getImageUri(applicationContext, it)
                selectedImageUri = tempUri
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
        setContentView(R.layout.edit_menu) // Menggunakan layout edit_menu

        initViews()

        // Dapatkan data menu dari Intent
        currentMenu = intent.getParcelableExtra("menu_data") // Asumsi Menu sudah Parcelable
        if (currentMenu == null) {
            Toast.makeText(this, "Tidak ada data menu untuk diedit.", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_yellow)

         // Inisialisasi UI Views
        loadMenuDataIntoViews() // Memuat data menu ke UI
        setupClickListeners() // Menyiapkan listener

        val baseUrl = "http://10.0.2.2/api_menu/" // Ganti dengan URL valid Anda
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val combinedApiService: ApiService
        try {
            combinedApiService = retrofit.create(ApiService::class.java)
        } catch (e: Exception) {
            Log.e("EditMenuActivity", "Error creating combined ApiService: ${e.message}", e)
            Toast.makeText(this, "Gagal menginisialisasi layanan API: ${e.message}", Toast.LENGTH_LONG).show()
            spinnerKategori.isEnabled = false
            buttonSimpanPerubahan.isEnabled = false
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Inisialisasi Category ViewModel
        try {
            val categoryRepository = CategoryRepository(combinedApiService)
            val categoryFactory = CategoryViewModelFactory(application, categoryRepository)
            categoryViewModel = ViewModelProvider(this, categoryFactory).get(CategoryViewModel::class.java)
            observeCategoryViewModel() // Mengamati kategori
            Log.d("EditMenuActivity", "Calling categoryViewModel?.fetchCategories()...")
            categoryViewModel?.fetchCategories() // Memulai pengambilan kategori
            Log.d("EditMenuActivity", "CategoryViewModel initialized successfully.")

        } catch (e: Exception) {
            Log.e("EditMenuActivity", "Error during CategoryViewModel initialization: ${e.message}", e)
            Toast.makeText(this, "Gagal menginisialisasi Category ViewModel: ${e.message}", Toast.LENGTH_LONG).show()
            spinnerKategori.isEnabled = false
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Inisialisasi Menu ViewModel
        try {
            val menuRepository = MenuRepository(combinedApiService, applicationContext)
            val menuFactory = MenuViewModelFactory(application, menuRepository)
            menuViewModel = ViewModelProvider(this, menuFactory)[MenuViewModel::class.java]

            // Mengamati hasil update menu dari MenuViewModel
            menuViewModel?.updateMenuResult?.observe(this) { success ->
                if (success) {
                    Toast.makeText(this, "Menu berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK) // Sinyal perubahan berhasil
                    finish()
                }
            }
            menuViewModel?.updateMenuError?.observe(this) { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(this, "Gagal memperbarui menu: $it", Toast.LENGTH_LONG).show()
                    Log.e("EditMenuActivity", "Error updating menu: $it")
                    setResult(RESULT_CANCELED) // Sinyal bahwa ada masalah
                }
            }

        } catch (e: Exception) {
            Log.e("EditMenuActivity", "Error during MenuViewModel initialization: ${e.message}", e)
            Toast.makeText(this, "Gagal menginisialisasi Menu ViewModel: ${e.message}", Toast.LENGTH_LONG).show()
            buttonSimpanPerubahan.isEnabled = false
            setResult(RESULT_CANCELED)
            finish()
            return
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        editTextNamaMenu = findViewById(R.id.editTextNamaMenu)
        editTextDeskripsi = findViewById(R.id.editTextDeskripsi)
        editTextHarga = findViewById(R.id.editTextHarga)
        spinnerKategori = findViewById(R.id.spinnerKategori)
        cardUploadImage = findViewById(R.id.cardUploadGambar)
        imageViewPreview = findViewById(R.id.previewGambar)
        layoutUploadPrompt = findViewById(R.id.layoutUploadGambar)
        textViewChangeImage = findViewById(R.id.textGantiGambar)
        buttonSimpanPerubahan = findViewById(R.id.buttonSimpanMenu) // Menggunakan ID yang sama dengan add_menu
    }


    private fun loadMenuDataIntoViews() {
        currentMenu?.let { menu ->
            editTextNamaMenu.setText(menu.name)
            editTextDeskripsi.setText(menu.description)
            editTextHarga.setText(menu.price.toString())

            // Tampilkan gambar yang sudah ada
            menu.photo?.let { photoUrl ->
                // Pastikan BASE_URL_IMAGE sesuai dengan server Anda
                val baseImageUrl = "http://10.0.2.2/api_menu/" // Contoh: Ganti dengan URL dasar untuk folder images/
                val fullImageUrl = "$baseImageUrl$photoUrl" // Gabungkan jika photoUrl relatif

                Log.d("EditMenuActivity", "Loading existing image: $fullImageUrl")
                Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(imageViewPreview)

                imageViewPreview.isVisible = true
                layoutUploadPrompt.isVisible = false
                textViewChangeImage.isVisible = true
                selectedImageUri = Uri.parse(fullImageUrl) // Simpan Uri dari gambar yang sudah ada
            }

            // Atur kategori yang dipilih (akan disinkronkan di observeCategoryViewModel)
            selectedCategoryId = menu.categoryId ?: -1
            selectedCategoryName = menu.category ?: ""
            Log.d("EditMenuActivity", "Initial menu data loaded: ID=${menu.id}, Name=${menu.name}, CategoryID=${selectedCategoryId}")
        }
    }

    private fun observeCategoryViewModel() {
        categoryViewModel?.categories?.observe(this) { categoryList ->
            Log.d("EditMenuActivity", "CategoryViewModel categories LiveData updated. List size: ${categoryList.size}")
            if (categoryList.isNotEmpty()) {
                val categoryNames = categoryList.map { it.name }.toMutableList()
                categoryNames.add(0, "Pilih Kategori")

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    categoryNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerKategori.adapter = adapter

                // Set pilihan spinner ke kategori menu yang sedang diedit
                if (selectedCategoryId != -1) {
                    val index = categoryList.indexOfFirst { it.id == selectedCategoryId }
                    if (index != -1) {
                        spinnerKategori.setSelection(index + 1) // +1 karena ada placeholder
                        Log.d("EditMenuActivity", "Spinner set to category: ${selectedCategoryName} (ID: ${selectedCategoryId}) at index ${index + 1}")
                    } else {
                        Log.w("EditMenuActivity", "Selected category ID $selectedCategoryId not found in loaded category list. Setting to placeholder.")
                        spinnerKategori.setSelection(0) // Kembali ke placeholder jika tidak ditemukan
                    }
                } else {
                    spinnerKategori.setSelection(0) // Set ke placeholder jika currentMenu.categoryId adalah null atau -1
                    Log.d("EditMenuActivity", "Initial menu category is null or -1, setting spinner to placeholder.")
                }


                spinnerKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position == 0) {
                            selectedCategoryName = ""
                            selectedCategoryId = -1

                        } else {
                            val selectedCatObject = categoryList[position - 1]
                            selectedCategoryName = selectedCatObject.name
                            selectedCategoryId = selectedCatObject.id
                            Log.d("EditMenuActivity", "Kategori dipilih: ID=${selectedCategoryId}, Nama='${selectedCategoryName}'")
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) { /* Do nothing */ }
                }
            } else {
                Toast.makeText(this, "Tidak ada kategori tersedia.", Toast.LENGTH_SHORT).show()
                spinnerKategori.isEnabled = false
            }
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            setResult(RESULT_CANCELED) // Sinyal tidak ada perubahan
            finish()
        }
        cardUploadImage.setOnClickListener { showImageSourceDialog() }
        textViewChangeImage.setOnClickListener { showImageSourceDialog() }
        buttonSimpanPerubahan.setOnClickListener { saveMenuChanges() } // Memanggil fungsi penyimpanan
    }

    // Fungsi-fungsi Image Picker (sama dengan AddMenuActivity)
    private fun showImageSourceDialog() {
        val options = arrayOf("Galeri", "Kamera")
        AlertDialog.Builder(this)
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> galleryLauncher.launch("image/*")
                    1 -> checkCameraPermission()
                }
            }.show()
    }

    private fun openGallery() { galleryLauncher.launch("image/*") }
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> { permissionLauncher.launch(Manifest.permission.CAMERA) }
        }
    }

    private fun openCamera() { cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) }

    private fun showImagePreview(uri: Uri) {
        layoutUploadPrompt.isVisible = false
        imageViewPreview.isVisible = true
        textViewChangeImage.isVisible = true
        Glide.with(this).load(uri).centerCrop().into(imageViewPreview)
    }

    private fun showImagePreview(bitmap: Bitmap) {
        layoutUploadPrompt.isVisible = false
        imageViewPreview.isVisible = true
        textViewChangeImage.isVisible = true
        imageViewPreview.setImageBitmap(bitmap)
    }

    // Helper untuk mengonversi Bitmap ke Uri (diperlukan untuk kamera)
    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = java.io.ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }


    // >>> FUNGSI UNTUK MENYIMPAN PERUBAHAN MENU <<<
    private fun saveMenuChanges() {
        val namaMenu = editTextNamaMenu.text.toString().trim()
        val deskripsi = editTextDeskripsi.text.toString().trim()
        val hargaText = editTextHarga.text.toString().trim()
        val photoUriToUpload = selectedImageUri // Uri gambar baru jika diubah

        // Validasi
        if (namaMenu.isEmpty() || deskripsi.isEmpty() || hargaText.isEmpty() || selectedCategoryId == -1) {
            Toast.makeText(this, "Semua field harus diisi dan kategori dipilih.", Toast.LENGTH_SHORT).show()
            return
        }
        val harga = hargaText.toDoubleOrNull()
        if (harga == null) {
            editTextHarga.error = "Harga tidak valid"
            editTextHarga.requestFocus()
            return
        }

        // Dapatkan tenant_id dari SharedPreferences
        val tenantId = sharedPreferences.getInt("tenant_id", -1)
        if (tenantId == -1) {
            Toast.makeText(this, "ID tenant tidak ditemukan. Mohon login ulang.", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Tampilkan custom dialog konfirmasi edit menu
        showCustomEditConfirmationDialog(
            menuId = currentMenu?.id ?: -1, // ID menu saat ini
            tenantId = tenantId,
            categoryId = selectedCategoryId,
            name = namaMenu,
            description = deskripsi,
            photoUri = photoUriToUpload,
            price = harga
        )
    }
    // >>> AKHIR FUNGSI PENYIMPANAN <<<


    // --- FUNGSI UNTUK MENAMPILKAN CUSTOM DIALOG EDIT (DIAMBIL DARI MainTenantActivity) ---
    private fun showCustomEditConfirmationDialog(
        menuId: Int,
        tenantId: Int,
        categoryId: Int?,
        name: String,
        description: String?,
        photoUri: Uri?,
        price: Double
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_edit_menu, null)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogPesan)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        dialogMessage.text = "Anda yakin ingin menyimpan perubahan pada menu '${name}'?"

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
            Toast.makeText(this, "Perubahan dibatalkan", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            alertDialog.dismiss()
            // Panggil fungsi updateMenu dari MenuViewModel
            menuViewModel?.updateMenu(
                menuId = menuId,
                tenantId = tenantId,
                categoryId = categoryId,
                name = name,
                description = description,
                photoUri = photoUri,
                price = price
            )
            // Hasil akan diamati oleh observer di onCreate
        }

        alertDialog.show()
    }
    // --- AKHIR FUNGSI CUSTOM DIALOG EDIT ---
}
