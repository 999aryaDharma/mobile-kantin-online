package com.example.uts_2301010174

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.uts_2301010174.user.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class QrisPaymentActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var imageViewQRCode: ImageView
    private lateinit var textViewTotalAmount: TextView
    private lateinit var textViewTimer: TextView
    private lateinit var btnCancelPayment: CardView
    private lateinit var btnConfirmPayment: CardView
    private lateinit var bottomNavigation: BottomNavigationView

    private var totalAmount = 0
    private var customerName = ""
    private var notes = ""
    private var countDownTimer: CountDownTimer? = null
    private var successDialog: Dialog?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qris_payment)

        // Change status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_yellow)

        // Get data from intent
        totalAmount = intent.getDoubleExtra("total_amount", 0.0).toInt()
        customerName = intent.getStringExtra("customer_name") ?: ""
        notes = intent.getStringExtra("notes") ?: ""

        initViews()
        setupClickListeners()
        setupPaymentTimer()
        displayPaymentInfo()
        textViewTimer = findViewById(R.id.textViewTimer)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        imageViewQRCode = findViewById(R.id.imageViewQRCode)
        textViewTotalAmount = findViewById(R.id.textViewTotalAmount)
        btnCancelPayment = findViewById(R.id.btnCancelPayment)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnCancelPayment.setOnClickListener {
            cancelPayment()
        }

        btnConfirmPayment.setOnClickListener {
            confirmPayment()
        }

        // Bottom navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Navigate to home
                    finish()
                    true
                }
                R.id.navigation_orders -> {
                    // Handle orders navigation
                    true
                }
                R.id.navigation_profile -> {
                    // Handle profile navigation
                    true
                }
                else -> false
            }
        }
    }

    private fun displayPaymentInfo() {
        textViewTotalAmount.text = "Rp ${formatPrice(totalAmount)}"

        // In a real app, you would generate actual QR code here
        // For now, we'll use a placeholder icon
        imageViewQRCode.setImageResource(R.drawable.qrcode)
    }

    private fun setupPaymentTimer() {
        // 15 minutes countdown timer
        countDownTimer = object : CountDownTimer(15 * 60 * 1000, 1000) {
            @SuppressLint("DefaultLocale")
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                textViewTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                textViewTimer.text = "00:00"
                Toast.makeText(this@QrisPaymentActivity, "Waktu pembayaran habis", Toast.LENGTH_LONG).show()
                finish()
            }
        }
        countDownTimer?.start()
    }

    private fun cancelPayment() {
        countDownTimer?.cancel()
        Toast.makeText(this, "Pembayaran dibatalkan", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmPayment() { // Perhatikan perubahan nama method
        // Hentikan countdown timer jika pembayaran dikonfirmasi
        countDownTimer?.cancel()

        // Inflate custom layout untuk dialog
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.dialog_payment_success, null)

        // Buat AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        builder.setCancelable(false) // Membuat dialog tidak bisa di-dismiss dengan tombol back atau sentuhan di luar

        successDialog = builder.create()
        successDialog?.show()

        // Handler untuk menutup dialog setelah 3 detik dan kemudian menutup activity
        Handler(Looper.getMainLooper()).postDelayed({
            successDialog?.dismiss() // Tutup dialog
            // Tambahkan aksi setelah dialog ditutup, misalnya kembali ke halaman sebelumnya atau halaman utama
            Toast.makeText(this, "Pembayaran berhasil!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)// Menutup QrisPaymentActivity setelah pembayaran berhasil
        }, 3000) // 3000 milidetik = 3 detik
    }

    private fun formatPrice(price: Int): String {
        return String.format("%,d", price).replace(',', '.')
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}