package com.example.uts_2301010174.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView

object BadgeUtils {
    @SuppressLint("SetTextI18n")
    fun setCartBadge(context: Context, imageView: ImageView, badgeTextView: TextView, count: Int) {
        if (count > 0) {
            badgeTextView.text = count.toString()
            badgeTextView.visibility = View.VISIBLE
        } else {
            badgeTextView.visibility = View.GONE
        }
    }
}