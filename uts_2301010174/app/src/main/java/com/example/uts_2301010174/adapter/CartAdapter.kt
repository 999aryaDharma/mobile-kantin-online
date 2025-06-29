package com.example.uts_2301010174.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.uts_2301010174.R
import com.example.uts_2301010174.user.CartItem
import java.text.NumberFormat

class CartAdapter(
    private var cartItems: MutableList<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onItemDeleted: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewMenuName: TextView = itemView.findViewById(R.id.textViewMenuName)
        val textViewMenuPrice: TextView = itemView.findViewById(R.id.textMenuPrice)
        //        val textViewQuantity: TextView = itemView.findViewById(R.id.textViewQuantity)
        val textQuantity: TextView = itemView.findViewById(R.id.textQuantity)
        val btnDecrease: CardView = itemView.findViewById(R.id.btnDecrease)
        val btnIncrease: CardView = itemView.findViewById(R.id.btnIncrease)
        val btnDelete: CardView = itemView.findViewById(R.id.btnDelete)
        val divider: View = itemView.findViewById(R.id.divider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = cartItems[position]

        holder.textViewMenuName.text = cartItem.menuName
        holder.textViewMenuPrice.text = "Rp ${formatPrice(cartItem.menuPrice.toDouble())}"
        holder.textQuantity.text = cartItem.quantity.toString()
//        holder.textViewQuantityDisplay.text = cartItem.quantity.toString()

        // Show divider for all items except the last one
        holder.divider.visibility = if (position == cartItems.size - 1) View.GONE else View.VISIBLE

        // Handle decrease button
        holder.btnDecrease.setOnClickListener {
            if (cartItem.quantity > 1) {
                cartItem.quantity--
                onQuantityChanged(cartItem, cartItem.quantity)
                notifyItemChanged(position)
            }
        }

        // Handle increase button
        holder.btnIncrease.setOnClickListener {
            cartItem.quantity++
            onQuantityChanged(cartItem, cartItem.quantity)
            notifyItemChanged(position)
        }

        // Handle delete button
        holder.btnDelete.setOnClickListener {
            onItemDeleted(cartItem)
            cartItems.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, cartItems.size)
        }
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateCartItems(newCartItems: MutableList<CartItem>) {
        cartItems = newCartItems
        notifyDataSetChanged()
    }

    fun clearCart() {
        cartItems.clear()
        notifyDataSetChanged()
    }

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(java.util.Locale("in", "ID"))
        formatter.minimumFractionDigits = 0 // No decimal places for whole numbers like 15000
        formatter.maximumFractionDigits = 2 // Max 2 decimal places if there are cents
        return formatter.format(price).replace("Rp", "").trim() // Remove currency symbol if desired
    }
}