package com.example.uts_2301010174.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.uts_2301010174.repository.CategoryRepository // Import Repository
import com.example.uts_2301010174.user.CategoryModels
import kotlinx.coroutines.launch

class CategoryViewModel(
    application: Application, // AndroidViewModel requires Application
    private val repository: CategoryRepository // Repository sebagai dependensi
) : AndroidViewModel(application) {

    private val _categories = MutableLiveData<List<CategoryModels.Category>>()
    val categories: LiveData<List<CategoryModels.Category>> = _categories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: MutableLiveData<String?> = _error

    fun fetchCategories() {
        Log.d("CategoryViewModel", "fetchCategories invoked.")
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val result = repository.getCategories()
                _categories.value = result
                Log.d("CategoryViewModel", "Categories fetched successfully. Count: ${result.size}")
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                Log.e("CategoryViewModel", "Error fetching categories: ${e.message}", e)
            } finally {
                _isLoading.value = false
                Log.d("CategoryViewModel", "fetchCategories finished. isLoading set to false.")
            }
        }
    }
}