package com.example.uts_2301010174

import android.icu.text.NumberFormat


class FormatUtils {
    fun formatPriceToRupiah(price: Double): String {
        val localeID = java.util.Locale("in", "ID")
        val currencyFormat = NumberFormat.getCurrencyInstance(localeID)
        return currencyFormat.format(price)
    }
}