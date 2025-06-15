package com.example.uts_2301010174.ui.login

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.uts_2301010174.LoginResponse
import com.example.uts_2301010174.R
import com.example.uts_2301010174.RetrofitClient
import com.example.uts_2301010174.tenant.MainTenantActivity
import com.example.uts_2301010174.user.MainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var edtUsername: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtResult: TextView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        edtUsername = findViewById(R.id.edtUsername)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        txtResult = findViewById(R.id.txtResult)
        sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Logging in...")
            setCancelable(false)
        }

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            // Validasi form
            if (username.isEmpty() || password.isEmpty()) {
                txtResult.text = "Username dan password tidak boleh kosong"
                txtResult.setTextColor(ContextCompat.getColor(this@LoginActivity, R.color.unavail))
                return@setOnClickListener
            }

            // Tampilkan loading
            progressDialog.show()

            // Lakukan login
            val api = RetrofitClient.instance
            api.loginUser(username, password).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    progressDialog.dismiss()
                    val res = response.body()
                    if (res?.success == true) {
                        val user = res.user!!
                        txtResult.text = "Login success. Welcome ${user.username} as ${user.role}"
                        txtResult.setTextColor(ContextCompat.getColor(this@LoginActivity, R.color.avail))

                        // Simpan data ke SharedPreferences
                        // Simpan data ke SharedPreferences
                        val editor = sharedPref.edit() // Dapatkan editor
                        editor.putString("user_id", user.id) // Asumsikan user.id adalah String
                        editor.putString("username", user.username)
                        editor.putString("role", user.role)

                        // Ambil tenant_id sebagai String dari model User
                        val tenantIdString = user.tenantId
                        Log.d("LoginActivity", "Retrieved User: ID=${user.id}, Username=${user.username}, Role=${user.role}, Tenant ID (String)=${tenantIdString}")

                        // Konversi tenantId dari String ke Int secara aman sebelum disimpan
                        tenantIdString?.toIntOrNull()?.let { tenantIdInt ->
                            editor.putInt("tenant_id", tenantIdInt)
                            Log.d("LoginActivity", "Tenant ID saved to SharedPreferences: $tenantIdInt")
                        } ?: run {
                            // Ini akan dieksekusi jika tenantIdString adalah null atau tidak bisa dikonversi ke Int
                            Log.w("LoginActivity", "Tenant ID is NULL or not a valid number (${tenantIdString}) for user ${user.username}. Not saving to SharedPreferences.")
                            editor.remove("tenant_id") // Hapus kunci jika tidak valid
                        }
                        editor.apply() // <<< PERBAIKAN UTAMA: GANTI apply() menjadi commit() >>>
                        Log.d("LoginActivity", "SharedPreferences commit() executed.") // Log konfirmasi

                        // Arahkan berdasarkan role
                        when (user.role.lowercase()) {
                            "user" -> {
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            }
                            "tenant" -> {
                                startActivity(Intent(this@LoginActivity, MainTenantActivity::class.java))
                            }
                            else -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Unknown role: ${user.role}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
                            }
                        }
                        finish() // Tidak bisa kembali ke login
                    } else {
                        txtResult.text = res?.message ?: "Login gagal"
                        txtResult.setTextColor(ContextCompat.getColor(this@LoginActivity, R.color.unavail))
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    progressDialog.dismiss()
                    txtResult.text = "Gagal terhubung: ${t.localizedMessage}"
                    Log.e("Login", t.message.toString())
                }
            })
        }
    }
}
