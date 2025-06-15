package com.example.uts_2301010174

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.uts_2301010174.repository.MenuRepository // Import MenuRepository
import com.example.uts_2301010174.viewModel.MenuViewModel

// Factory untuk membuat instance MenuViewModel
// Ini memungkinkan MenuViewModel memiliki konstruktor dengan argumen (misalnya, Repository)
class MenuViewModelFactory(
    private val application: Application, // Diperlukan karena MenuViewModel adalah AndroidViewModel
    private val repository: MenuRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Memeriksa apakah modelClass adalah instance dari MenuViewModel
        if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
            // Mengembalikan instance MenuViewModel dengan dependensi yang diperlukan
            @Suppress("UNCHECKED_CAST") // Suppress warning karena kita yakin tipe yang dikembalikan benar
            return MenuViewModel(application, repository) as T
        }
        // Melemparkan exception jika kelas ViewModel yang diminta tidak dikenal
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}