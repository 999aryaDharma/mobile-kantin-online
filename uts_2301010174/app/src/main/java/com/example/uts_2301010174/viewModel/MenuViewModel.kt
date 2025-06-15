package com.example.uts_2301010174.viewModel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.uts_2301010174.repository.MenuRepository
import kotlinx.coroutines.launch

class MenuViewModel(application: Application, private val repository: MenuRepository) : AndroidViewModel(application) {
    private val _addMenuResult = MutableLiveData<Boolean>()
    val addMenuResult: LiveData<Boolean> = _addMenuResult

    private val _addMenuError = MutableLiveData<String?>()
    val addMenuError: MutableLiveData<String?> = _addMenuError

    // >>> LIVE DATA UNTUK UPDATE MENU <<<
    private val _updateMenuResult = MutableLiveData<Boolean>()
    val updateMenuResult: LiveData<Boolean> = _updateMenuResult

    private val _updateMenuError = MutableLiveData<String?>()
    val updateMenuError: MutableLiveData<String?> = _updateMenuError

    // Fungsi untuk menambahkan menu baru dengan semua parameter
    fun addNewMenu(
        tenantId: Int,
        categoryId: Int?,
        name: String,
        description: String?,
        photoUri: Uri?,
        price: Double
    ) {
        _addMenuResult.value = false // Reset status
        _addMenuError.value = null
        viewModelScope.launch {
            try {
                // Panggil addMenu dari repository
                val response = repository.addMenu(tenantId, categoryId, name, description, photoUri, price)
                _addMenuResult.value = response.success
                if (!response.success) {
                    _addMenuError.value = response.message
                }
            } catch (e: Exception) {
                _addMenuError.value = e.message
                _addMenuResult.value = false
            }
        }
    }

    private val _deleteMenuResult = MutableLiveData<Boolean>()
    val deleteMenuResult: LiveData<Boolean> = _deleteMenuResult

    private val _deleteMenuError = MutableLiveData<String?>()
    val deleteMenuError: MutableLiveData<String?> = _deleteMenuError

    fun deleteMenu(menuId: Int) {
        _deleteMenuResult.value = false
        _deleteMenuError.value = null
        viewModelScope.launch {
            try {
                val response = repository.deleteMenu(menuId)
                Log.d("MenuViewModel", "Repository deleteMenu returned: success=${response.success}, message=${response.message}")
                _deleteMenuResult.value = response.success
                if (!response.success) {
                    _deleteMenuError.value = response.message
                }
            } catch (e: Exception) {
                _deleteMenuError.value = e.message
                _deleteMenuResult.value = false
            }
        }
    }

    fun updateMenu(
        menuId: Int,
        tenantId: Int,
        categoryId: Int?,
        name: String,
        description: String?,
        photoUri: Uri?,
        price: Double
    ) {
        Log.d("MenuViewModel", "updateMenu invoked for ID: $menuId")
        _updateMenuResult.value = false // Reset status
        _updateMenuError.value = null // Reset error
        viewModelScope.launch {
            try {
                val response = repository.updateMenu(menuId, tenantId, categoryId, name, description, photoUri, price)
                Log.d("MenuViewModel", "Repository updateMenu returned: success=${response.success}, message=${response.message}")
                _updateMenuResult.value = response.success
                if (!response.success) {
                    _updateMenuError.value = response.message
                }
            } catch (e: Exception) {
                Log.e("MenuViewModel", "Error in updateMenu coroutine: ${e.message}", e)
                _updateMenuError.value = e.message
                _updateMenuResult.value = false
            }
        }
    }
}