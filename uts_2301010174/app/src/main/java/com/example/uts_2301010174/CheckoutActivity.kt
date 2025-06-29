package com.example.uts_2301010174 // Sesuaikan package Anda

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.uts_2301010174.data.OrderRepository
import com.example.uts_2301010174.user.CartItem // Import CartItem
import com.example.uts_2301010174.user.CartSimpleResponse // Import CartSimpleResponse
import com.example.uts_2301010174.user.MainActivity
import com.example.uts_2301010174.viewModel.OrderViewModel
import com.example.uts_2301010174.viewModel.OrderViewModelFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat

class CheckoutActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var editTextNamaLengkap: EditText
    private lateinit var editTextCatatan: EditText
    private lateinit var cardCashPayment: CardView
    private lateinit var cardQrisPayment: CardView
    private lateinit var radioCash: RadioButton
    private lateinit var radioQris: RadioButton
    private lateinit var textViewSubtotal: TextView
    private lateinit var textViewServiceFee: TextView
    private lateinit var textViewTotal: TextView
    private lateinit var btnConfirmPayment: CardView // Mengacu pada tombol "Konfirmasi Pembayaran"
    private lateinit var tvTenantName: TextView // TextView untuk nama tenant
    // private lateinit var bottomNavigation: BottomNavigationView // Dihapus karena visibility="gone" di XML

    private lateinit var linearLayoutCashPayment: LinearLayout // ID baru dari XML
    private lateinit var linearLayoutQrisPayment: LinearLayout // ID baru dari XML
    private lateinit var linearLayoutOrderItems: LinearLayout // Container untuk item order

    private var selectedPaymentMethod = "QRIS" // Default ke QRIS
    // private val subtotal = 15000 // Akan dihitung dinamis
    // private val serviceFee = 0 // Akan dihitung dinamis

    private var successDialog: Dialog?= null

    private var cartItems: ArrayList<CartItem> = arrayListOf() // Daftar item dari keranjang (Parcelable)
    private var userId: Int = -1

    private lateinit var orderViewModel: OrderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.checkout_page)

        // Dapatkan data cartItems dari Intent
        cartItems = intent.getParcelableArrayListExtra("cart_items") ?: arrayListOf()
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Keranjang kosong. Tidak dapat melakukan checkout.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Dapatkan userId dari SharedPreferences
        userId = getUserIdFromSharedPrefs()
        if (userId <= 0) {
            Toast.makeText(this, "User ID tidak valid. Mohon login ulang.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.background_yellow)

        initViews()
        setupClickListeners()
        displayOrderSummary() // Memanggil ini setelah views diinisialisasi

        // Inisialisasi OrderViewModel
        try {
            val apiService = RetrofitClient.instance
            val orderRepository = OrderRepository(apiService)
            val orderFactory = OrderViewModelFactory(application, orderRepository)
            orderViewModel = ViewModelProvider(this, orderFactory).get(OrderViewModel::class.java)

            // Mengamati hasil order
            orderViewModel?.checkoutResult?.observe(this) { event -> // Menerima Event<Boolean>
                Log.d("CheckoutActivity", "Observer checkoutResult triggered with event: $event")

                // Gunakan getContentIfNotHandled() untuk memastikan event hanya diproses sekali
                if (event != null) {
                    event.getContentIfNotHandled()?.let { success ->
                        Log.d("CheckoutActivity", "Processing handled event. Success: $success, Message: ${orderViewModel?.checkoutMessage?.value}")

                        if (success) { // Jika hasilnya true (order berhasil)
                            val orderId = orderViewModel?.checkoutOrderId?.value
                            val apiMessage = orderViewModel?.checkoutMessage?.value ?: "Order berhasil!"
                            Toast.makeText(this, apiMessage, Toast.LENGTH_LONG).show() // Tampilkan pesan sukses dari API

                            if (selectedPaymentMethod == "QRIS") {
                                val totalAmountString = textViewTotal.text.toString().replace("Rp ", "").replace(".", "").replace(",", "")
                                val total = totalAmountString.toDoubleOrNull() ?: 0.0
                                val customerName = editTextNamaLengkap.text.toString().trim()
                                val notes = editTextCatatan.text.toString().trim()

                                val intent = Intent(this, QrisPaymentActivity::class.java).apply {
                                    putExtra("total_amount", total)
                                    putExtra("customer_name", customerName)
                                    putExtra("notes", notes)
                                    putExtra("order_id", orderId)
                                    putParcelableArrayListExtra("cart_items", cartItems)
                                }
                                try {
                                    startActivity(intent)
                                    Log.d("CheckoutActivity", "Launching QrisPaymentActivity.")
                                    clearCartOnServer(userId) // Kosongkan keranjang di server setelah QRIS diluncurkan
                                } catch (e: Exception) {
                                    Log.e("CheckoutActivity", "Error launching QrisPaymentActivity: ${e.message}", e)
                                    Toast.makeText(this, "Gagal membuka halaman pembayaran QRIS.", Toast.LENGTH_LONG).show()
                                    // Jika QRIS gagal diluncurkan, order sudah di DB. Tidak mengosongkan keranjang di sini.
                                }

                            } else { // Metode pembayaran Cash
                                clearCartOnServer(userId) // Anda mungkin ingin memanggil ini setelah pembayaran benar-benar "selesai" (dialog ditutup)

                                // Inflate custom layout untuk dialog
                                val dialogView =
                                    LayoutInflater.from(this).inflate(R.layout.dialog_payment_success, null)

                                // Buat AlertDialog
                                val builder = AlertDialog.Builder(this) // Pastikan import androidx.appcompat.app.AlertDialog
                                builder.setView(dialogView)
                                builder.setCancelable(false) // Membuat dialog tidak bisa di-dismiss

                                successDialog = builder.create()
                                successDialog?.show()

                                // Tampilkan Toast ini SEGERA setelah order berhasil, sebelum dialog
                                Toast.makeText(this, "Pesanan berhasil! Silakan bayar di tempat.", Toast.LENGTH_LONG).show()

                                // Handler untuk menutup dialog setelah 3 detik, lalu intent, lalu reset state, lalu finish
                                Handler(Looper.getMainLooper()).postDelayed({
                                    successDialog?.dismiss() // Tutup dialog

                                    // Tampilkan Toast ini SETELAH dialog ditutup, sebelum pindah activity
                                    Toast.makeText(this, "Pembayaran dikonfirmasi!", Toast.LENGTH_SHORT).show()

                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Opsional: Membersihkan back stack
                                    startActivity(intent)

                                    // Reset ViewModel state SETELAH semua proses selesai dan sebelum menutup activity ini
                                    orderViewModel?.resetCheckoutState()

                                    // Tutup CheckoutActivity SETELAH intent ke MainActivity dan reset state
                                    finish()

                                }, 3000) // 3000 milidetik = 3 detik

                                // JANGAN panggil resetCheckoutState() dan finish() di sini lagi
                                // orderViewModel?.resetCheckoutState() // PINDAHKAN KE DALAM HANDLER
                                // finish() // PINDAHKAN KE DALAM HANDLER
                            }
//                            orderViewModel?.resetCheckoutState() // Reset ViewModel state setelah berhasil
//                            finish() // Tutup CheckoutActivity setelah navigasi/proses selesai

                        } else { // Jika success adalah FALSE (order gagal)
                            val errorMessage = orderViewModel?.checkoutMessage?.value ?: "Terjadi kesalahan tidak dikenal saat order."
                            Toast.makeText(this, "Checkout gagal: $errorMessage", Toast.LENGTH_LONG).show()
                            Log.e("CheckoutActivity", "Checkout failed: $errorMessage")
                            orderViewModel?.resetCheckoutState() // Reset ViewModel state setelah gagal
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("CheckoutActivity", "Error initializing OrderViewModel: ${e.message}", e)
            Toast.makeText(this, "Gagal menginisialisasi Order ViewModel: ${e.message}", Toast.LENGTH_LONG).show()
            btnConfirmPayment.isEnabled = false
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        editTextNamaLengkap = findViewById(R.id.editTextNamaLengkap)
        editTextCatatan = findViewById(R.id.editTextCatatan)
        cardCashPayment = findViewById(R.id.cardCashPayment)
        cardQrisPayment = findViewById(R.id.cardQrisPayment)
        radioCash = findViewById(R.id.radioCash)
        radioQris = findViewById(R.id.radioQris)
        textViewSubtotal = findViewById(R.id.textViewSubtotal)
        textViewServiceFee = findViewById(R.id.textViewServiceFee)
        textViewTotal = findViewById(R.id.textViewTotal)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment) // Sesuai ID tombol di XML Anda

        linearLayoutCashPayment = findViewById(R.id.linearLayoutCashPayment) // ID baru dari XML
        linearLayoutQrisPayment = findViewById(R.id.linearLayoutQrisPayment) // ID baru dari XML
        linearLayoutOrderItems = findViewById(R.id.linearLayoutOrderItems) // Container untuk item order
        tvTenantName = findViewById(R.id.tvTenantName)

        // Set nama user dari SharedPreferences jika ada
        val username = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("username", null)
        editTextNamaLengkap.setText(username)

        // Pilih metode pembayaran default saat inisialisasi
        selectPaymentMethod(selectedPaymentMethod)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // Payment method selection
        linearLayoutCashPayment.setOnClickListener { selectPaymentMethod("cash") }
        cardCashPayment.setOnClickListener { selectPaymentMethod("cash") } // Jika card juga bisa diklik
        radioCash.setOnClickListener { selectPaymentMethod("cash") }

        linearLayoutQrisPayment.setOnClickListener { selectPaymentMethod("qris") }
        cardQrisPayment.setOnClickListener { selectPaymentMethod("qris") } // Jika card juga bisa diklik
        radioQris.setOnClickListener { selectPaymentMethod("qris") }

        // Confirm payment
        btnConfirmPayment.setOnClickListener {
            placeOrder() // Memanggil fungsi placeOrder
        }
    }

    private fun selectPaymentMethod(method: String) {
        selectedPaymentMethod = method

        when (method) {
            "cash" -> {
                radioCash.isChecked = true
                radioQris.isChecked = false
                updateCardBackground(cardCashPayment, true)
                updateCardBackground(cardQrisPayment, false)
            }
            "qris" -> {
                radioCash.isChecked = false
                radioQris.isChecked = true
                updateCardBackground(cardCashPayment, false)
                updateCardBackground(cardQrisPayment, true)
            }
        }
    }

    private fun updateCardBackground(card: CardView, isSelected: Boolean) {
        if (isSelected) {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.background_yellow))
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.light_gray))
        }
    }

    // Menampilkan item keranjang dan menghitung total
    @SuppressLint("SetTextI18n")
    private fun displayOrderSummary() {
        linearLayoutOrderItems.removeAllViews() // Bersihkan view lama jika ada
        var subtotalAmount = 0.0

        val layoutInflater = LayoutInflater.from(this)

        cartItems.forEach { item ->
            val itemView = layoutInflater.inflate(R.layout.order_summary_item, linearLayoutOrderItems, false)
            val tvQuantityName: TextView = itemView.findViewById(R.id.textViewOrderItemQuantityName)
            val tvPrice: TextView = itemView.findViewById(R.id.textViewOrderItemPrice)

            tvQuantityName.text = "${item.quantity}x ${item.menuName}"
            tvPrice.text = "Rp ${formatPrice(item.menuPrice.toDouble() * item.quantity)}" // Hitung harga per item
            linearLayoutOrderItems.addView(itemView)

            subtotalAmount += item.menuPrice * item.quantity
        }

        // Set total harga dan biaya lainnya
        val serviceFee = 0 // Biaya layanan, bisa disesuaikan
        val totalAmount = subtotalAmount + serviceFee

        textViewSubtotal.text = "Rp ${formatPrice(subtotalAmount)}"
        textViewServiceFee.text = "Rp ${formatPrice(serviceFee.toDouble())}"
        textViewTotal.text = "Rp ${formatPrice(totalAmount)}"
    }

    // Fungsi untuk memformat harga
    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(java.util.Locale("in", "ID"))
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        return formatter.format(price).replace("Rp", "").trim().replace(",00", "")
    }

    // Mendapatkan user ID dari SharedPreferences
    private fun getUserIdFromSharedPrefs(): Int {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", null)?.toIntOrNull() ?: 0
    }

    // Fungsi untuk menempatkan order (membuat OrderRequest dan memanggil ViewModel)
    private fun placeOrder() {
        val customerName = editTextNamaLengkap.text.toString().trim()
        val notes = editTextCatatan.text.toString().trim().takeIf { it.isNotEmpty() }

        if (customerName.isEmpty()) {
            editTextNamaLengkap.error = "Nama pelanggan harus diisi"
            editTextNamaLengkap.requestFocus()
            return
        }
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Keranjang kosong! Tidak dapat order.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = getUserIdFromSharedPrefs()
        if (userId <= 0) {
            Toast.makeText(this, "User ID tidak valid. Mohon login ulang.", Toast.LENGTH_LONG).show()
            return
        }

        val tenantId = cartItems.firstOrNull()?.tenantId ?: -1

        if (tenantId <= 0) { // Validasi tenantIdForOrder
            Toast.makeText(this, "Tenant ID tidak ditemukan dari item keranjang. Order dibatalkan.", Toast.LENGTH_LONG).show()
            Log.e("CheckoutActivity", "Tenant ID is null or invalid from cart items (${tenantId}). Order aborted.")
            return
        }


        val orderItemRequests = cartItems.map { cartItem ->
            OrderItemRequest(
                menuId = cartItem.menuId,
                quantity = cartItem.quantity,
                price = cartItem.menuPrice, // Ubah Int ke Double untuk API OrderItemRequest
                subtotal = cartItem.getTotalPrice() // Subtotal per item (Int)
            )
        }

        val totalAmountFromUI = cartItems.sumOf { it.getTotalPrice() }

        val orderRequest = OrderRequest(
            userId = userId,
            tenantId = tenantId,
            customerName = customerName,
            notes = notes,
            totalAmount = totalAmountFromUI,
            paymentMethod = selectedPaymentMethod,
            cartItems = orderItemRequests
        )

        Log.d("CheckoutActivity", "Placing order: $orderRequest")
        orderViewModel?.placeOrder(orderRequest)
    }

    // Mengosongkan keranjang di server setelah order berhasil (Dipanggil dari observer ViewModel)
    private fun clearCartOnServer(userId: Int) {
        RetrofitClient.instance.clearCart(userId).enqueue(object : Callback<CartSimpleResponse> {
            override fun onResponse(call: Call<CartSimpleResponse>, response: Response<CartSimpleResponse>) {
                if (response.isSuccessful) {
                    Log.d("CheckoutActivity", "Cart cleared on server after successful order.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("CheckoutActivity", "Failed to clear cart on server: ${response.code()}, $errorBody")
                }
            }

            override fun onFailure(call: Call<CartSimpleResponse>, t: Throwable) {
                Log.e("CheckoutActivity", "Network error clearing cart on server: ${t.message}", t)
            }
        })
    }
}
