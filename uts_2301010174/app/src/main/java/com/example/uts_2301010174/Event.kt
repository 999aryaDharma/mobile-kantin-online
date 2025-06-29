package com.example.uts_2301010174 // Sesuaikan package Anda

/**
 * Digunakan sebagai wrapper untuk LiveData yang hanya boleh terpicu sekali.
 *
 * @param T Tipe data yang dibungkus oleh Event.
 * @param content Konten data yang dibungkus.
 */
open class Event<out T>(private val content: T) {

    // Status untuk melacak apakah event ini sudah ditangani atau belum.
    var hasBeenHandled = false
        private set // Memungkinkan hanya baca dari luar, set dari dalam kelas.

    /**
     * Mengembalikan konten data hanya jika belum ditangani, dan menandainya sebagai sudah ditangani.
     * Jika sudah ditangani, mengembalikan null. Ini berguna untuk event yang hanya boleh terjadi satu kali.
     *
     * @return Konten data jika belum ditangani, atau null jika sudah ditangani.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null // Event sudah ditangani, kembalikan null.
        } else {
            hasBeenHandled = true // Tandai event sebagai sudah ditangani.
            content // Kembalikan konten.
        }
    }

    /**
     * Mengembalikan konten data, terlepas dari apakah sudah ditangani atau belum.
     * Berguna jika Anda perlu mengakses konten bahkan setelah event 'ditangani' (misalnya untuk logging).
     *
     * @return Konten data.
     */
    fun peekContent(): T = content

    override fun toString(): String {
        return "Event(content=$content, hasBeenHandled=$hasBeenHandled)"
    }
}
