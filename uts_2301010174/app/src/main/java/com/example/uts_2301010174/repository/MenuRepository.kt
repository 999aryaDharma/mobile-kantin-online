package com.example.uts_2301010174.repository // Sesuaikan package Anda

import android.content.Context // Import Context untuk akses content resolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.uts_2301010174.ApiService
import com.example.uts_2301010174.user.MenuAddResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull // Pastikan ini terimport
import okhttp3.MultipartBody
import okhttp3.RequestBody // Pastikan ini terimport
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MenuRepository(private val apiService: ApiService, private val context: Context) {

    // Helper function untuk membuat RequestBody dari String
    private fun createPartFromString(descriptionString: String?): RequestBody? {
        return descriptionString?.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    // Helper function untuk membuat MultipartBody.Part dari Uri
    private fun prepareFilePart(partName: String, fileUri: Uri?): MultipartBody.Part? {
        if (fileUri == null) return null

        // Jika Uri adalah URL jaringan (http/https), ini berarti gambar yang sudah ada dan tidak diubah.
        // Kita tidak perlu mengirim ulang file ini.
        if (fileUri.scheme == "http" || fileUri.scheme == "https") {
            Log.d("MenuRepository", "prepareFilePart: Skipping upload for network URI (existing photo): $fileUri")
            return null // Mengembalikan null, tidak ada part foto yang akan dikirim
        }

        val contentResolver = context.contentResolver
        val fileName = getFileName(fileUri)

        // Buat file sementara dari Uri
        val file = File(context.cacheDir, fileName ?: "temp_image.jpg")
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(fileUri)
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        // Buat RequestBody dari file
        val requestFile: RequestBody = file.asRequestBody(contentResolver.getType(fileUri)?.toMediaTypeOrNull())

        // Buat MultipartBody.Part dari RequestBody
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }

    // Helper function untuk mendapatkan nama file dari Uri
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) { // Check if column exists
                        result = cursor.getString(nameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }

    suspend fun addMenu(
        tenantId: Int,
        categoryId: Int?,
        name: String,
        description: String?,
        photoUri: Uri?, // Sekarang menerima Uri
        price: Double
    ): MenuAddResponse {
        return withContext(Dispatchers.IO) {
            // Konversi semua parameter menjadi RequestBody dan MultipartBody.Part
            val tenantIdPart = createPartFromString(tenantId.toString())
            val categoryIdPart = createPartFromString(categoryId?.toString()) // categoryId bisa null
            val namePart = createPartFromString(name)
            val descriptionPart = createPartFromString(description)
            val pricePart = createPartFromString(price.toString())
            val photoPart = prepareFilePart("photo", photoUri) // Nama 'photo' harus sesuai dengan nama field di PHP $_FILES

            // Pastikan nilai non-null untuk RequestBody yang tidak boleh null
            if (tenantIdPart == null || namePart == null || pricePart == null) {
                throw IllegalArgumentException("Required fields cannot be null for RequestBody creation.")
            }

            try {
                val response = apiService.addMenu(
                    tenantIdPart,
                    categoryIdPart,
                    namePart,
                    descriptionPart,
                    photoPart,
                    pricePart
                ).execute()

                if (response.isSuccessful) {
                    response.body() ?: throw Exception("Response body is null")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    throw Exception("Failed to add menu: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Network or API call failed: ${e.message}")
            }
        }


    }

    suspend fun deleteMenu(menuId: Int): MenuAddResponse {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteMenu(menuId).execute()
                if (response.isSuccessful) {
                    response.body() ?: throw Exception("Response body is null")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    throw Exception("Failed to delete menu: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Network or API call failed: ${e.message}")
            }
        }
    }

    suspend fun updateMenu(
        menuId: Int,
        tenantId: Int,
        categoryId: Int?,
        name: String,
        description: String?,
        photoUri: Uri?, // Mengirim Uri jika ada perubahan gambar
        price: Double
    ): MenuAddResponse {
        return withContext(Dispatchers.IO) {
            Log.d("MenuRepository", "Attempting to update menu ID: $menuId via API.")
            val menuIdPart = createPartFromString(menuId.toString())
            val tenantIdPart = createPartFromString(tenantId.toString())
            val categoryIdPart = createPartFromString(categoryId?.toString())
            val namePart = createPartFromString(name)
            val descriptionPart = createPartFromString(description)
            val pricePart = createPartFromString(price.toString())
            val photoPart = prepareFilePart("photo", photoUri) // Akan null jika photoUri juga null

            if (menuIdPart == null || tenantIdPart == null || namePart == null || pricePart == null) {
                throw IllegalArgumentException("Required fields for update cannot be null for RequestBody creation.")
            }

            try {
                val response = apiService.updateMenu(
                    menuIdPart,
                    tenantIdPart,
                    categoryIdPart,
                    namePart,
                    descriptionPart,
                    photoPart,
                    pricePart
                ).execute()

                // Log detail respons dari server untuk update
                Log.d("MenuRepository", "Update menu API Response: isSuccessful=${response.isSuccessful}, Code=${response.code()}, Message=${response.message()}")
                val responseBodyString = response.body()?.let { "Body: $it" } ?: "Body is null"
                val errorBodyString = response.errorBody()?.string()?.let { "ErrorBody: $it" } ?: "ErrorBody is null"
                Log.d("MenuRepository", "Update menu Response Details: $responseBodyString, $errorBodyString")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("MenuRepository", "Update menu successful (API returned success): ${body.success}, Message: ${body.message}")
                        body
                    } else {
                        Log.e("MenuRepository", "Update menu response body is null but successful. Likely parsing issue or no content.")
                        throw Exception("Empty response body from server (update operation).")
                    }
                } else {
                    Log.e("MenuRepository", "Update menu API failed: Code=${response.code()}, Message=${response.message()}, ErrorBody: $errorBodyString")
                    throw Exception("Failed to update menu: ${response.code()} - $errorBodyString")
                }
            } catch (e: Exception) {
                Log.e("MenuRepository", "Update menu network call failed: ${e.message}", e)
                throw Exception("Network or API call failed: ${e.message}")
            }
        }
    }
}
