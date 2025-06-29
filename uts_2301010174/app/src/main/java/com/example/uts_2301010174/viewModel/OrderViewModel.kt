package com.example.uts_2301010174.viewModel // Sesuaikan package Anda

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.uts_2301010174.Event
import com.example.uts_2301010174.OrderRequest
import com.example.uts_2301010174.data.OrderRepository
import kotlinx.coroutines.launch

class OrderViewModel(application: Application, private val repository: OrderRepository) : AndroidViewModel(application) {

    // >>> PERBAIKAN: _checkoutResult sekarang adalah MutableLiveData<Event<Boolean>> <<<
    private val _checkoutResult = MutableLiveData<Event<Boolean>?>()
    val checkoutResult: MutableLiveData<Event<Boolean>?> = _checkoutResult

    // checkoutMessage dan checkoutOrderId tetap seperti biasa karena mereka merepresentasikan state
    private val _checkoutMessage = MutableLiveData<String?>()
    val checkoutMessage: LiveData<String?> = _checkoutMessage

    private val _checkoutOrderId = MutableLiveData<Int?>()
    val checkoutOrderId: LiveData<Int?> = _checkoutOrderId

    fun placeOrder(orderRequest: OrderRequest) {
        Log.d("OrderViewModel", "placeOrder invoked. Request: $orderRequest")
        // Reset state awal
        _checkoutResult.value = null
        _checkoutMessage.value = null // AWALNYA DIHAPUS, SEKARANG NULL
        _checkoutOrderId.value = null
        // Tidak perlu set _checkoutMessage.value = "Memproses order..." di sini lagi,
        // karena kita tidak ingin observer bereaksi pada state transisi ini.
        viewModelScope.launch {
            try {
                val response = repository.placeOrder(orderRequest)
                _checkoutMessage.value = response.message // Set pesan final
                _checkoutOrderId.value = response.orderId
                _checkoutResult.value = Event(response.success) // Bungkus hasil final dengan Event
                Log.d("OrderViewModel", "Order placed: success=${response.success}, message=${response.message}, orderId=${response.orderId}")
            } catch (e: Exception) {
                _checkoutResult.value = Event(false) // Bungkus false dengan Event jika ada exception
                _checkoutMessage.value = e.message ?: "Terjadi kesalahan tidak dikenal saat order."
                _checkoutOrderId.value = null
                Log.e("OrderViewModel", "Error placing order: ${e.message}", e)
            }
        }
    }

    // Fungsi untuk mereset state ViewModel setelah dikonsumsi
    fun resetCheckoutState() {
        Log.d("OrderViewModel", "Resetting checkout state.")
        _checkoutResult.value = null // Set kembali ke null untuk membersihkan event
        _checkoutMessage.value = null
        _checkoutOrderId.value = null
    }
}

// Factory untuk OrderViewModel
class OrderViewModelFactory(private val application: Application, private val repository: OrderRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
