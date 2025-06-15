package com.example.kantininstiki

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.uts_2301010174.R
import com.google.android.material.bottomnavigation.BottomNavigationView

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
    private lateinit var btnConfirmPayment: CardView
    private lateinit var bottomNavigation: BottomNavigationView

    private var selectedPaymentMethod = "qris" // Default to QRIS
    private val subtotal = 15000
    private val serviceFee = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.checkout_page)

        // Change status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_yellow)

        initViews()
        setupClickListeners()
        updateOrderSummary()
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
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // Payment method selection
        cardCashPayment.setOnClickListener {
            selectPaymentMethod("cash")
        }

        cardQrisPayment.setOnClickListener {
            selectPaymentMethod("qris")
        }

        radioCash.setOnClickListener {
            selectPaymentMethod("cash")
        }

        radioQris.setOnClickListener {
            selectPaymentMethod("qris")
        }

        // Confirm payment
        btnConfirmPayment.setOnClickListener {
            processPayment()
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

    private fun updateOrderSummary() {
        val total = subtotal + serviceFee

        textViewSubtotal.text = "Rp ${formatPrice(subtotal)}"
        textViewServiceFee.text = "Rp ${formatPrice(serviceFee)}"
        textViewTotal.text = "Rp ${formatPrice(total)}"
    }

    private fun processPayment() {
        val namaLengkap = editTextNamaLengkap.text.toString().trim()

        // Validation
        if (namaLengkap.isEmpty()) {
            editTextNamaLengkap.error = "Nama lengkap harus diisi"
            editTextNamaLengkap.requestFocus()
            return
        }

        // Process payment based on selected method
        when (selectedPaymentMethod) {
            "cash" -> {
                // Process cash payment
                Toast.makeText(this, "Pesanan berhasil! Silakan bayar di tempat.", Toast.LENGTH_LONG).show()
                // Navigate to order confirmation or success page
                finish()
            }
            "qris" -> {
                // Navigate to QRIS payment page
                val intent = Intent(this, QrisPaymentActivity::class.java)
                intent.putExtra("total_amount", subtotal + serviceFee)
                intent.putExtra("customer_name", namaLengkap)
                intent.putExtra("notes", editTextCatatan.text.toString().trim())
                startActivity(intent)
            }
        }
    }

    private fun formatPrice(price: Int): String {
        return String.format("%,d", price).replace(',', '.')
    }
}

class QrisPaymentActivity {

}
