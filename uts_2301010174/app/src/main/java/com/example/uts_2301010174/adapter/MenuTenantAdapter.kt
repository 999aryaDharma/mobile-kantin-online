package com.example.uts_2301010174.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.uts_2301010174.R
import com.example.uts_2301010174.databinding.ItemMenuTenantBinding
import com.example.uts_2301010174.tenant.MainTenantActivity
import com.example.uts_2301010174.user.Menu
import java.text.NumberFormat

class MenuTenantAdapter(
    private var listMenu: List<Menu>
) : RecyclerView.Adapter<MenuTenantAdapter.MenuTenantViewHolder>() {

    interface OnItemActionListener {
        fun onSwitchAvailabilityChanged(menuId: Int, isChecked: Boolean)
        fun onMenuItemLongClick(menu: Menu): Boolean // Metode baru untuk long click
    }


    private var actionListenerInstance: OnItemActionListener? = null

    fun setOnItemActionListener(listener: MainTenantActivity) {
        this.actionListenerInstance = listener
    }

    inner class MenuTenantViewHolder(private val binding: ItemMenuTenantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(menu: Menu, listenerCallback: OnItemActionListener?) {
            binding.tvFoodName.text = menu.name
            binding.tvFoodDescription.text = menu.description
            binding.tvFoodPrice.text = formatPriceToRupiah(menu.price)
            binding.tvAvailability.text = menu.isAvailable.toString()

            val cleanedPhoto = menu.photo?.trim()?.replace("\n", "")
            val imageUrl = "http://10.0.2.2/api_menu/$cleanedPhoto"

            Glide.with(binding.root.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.imgMenu)

            // Tampilkan status tersedia atau tidak
            binding.tvAvailability.text = if (menu.isAvailable == 1) "Tersedia" else "Tidak Ada"
            val bgColorRes = if (menu.isAvailable == 1)
                R.drawable.badge_background_avail else R.drawable.badge_background_unavail
            val txtColorRes = if (menu.isAvailable == 1)
                R.color.avail else R.color.unavail

            binding.tvAvailability.setBackgroundResource(bgColorRes)
            binding.tvAvailability.setTextColor(ContextCompat.getColor(binding.root.context, txtColorRes))

            // Atur toggle switch
            binding.switchAvailability.setOnCheckedChangeListener(null)
            binding.switchAvailability.isChecked = menu.isAvailable == 1
            binding.switchAvailability.setOnCheckedChangeListener { _, isChecked ->
                listenerCallback?.onSwitchAvailabilityChanged(menu.id, isChecked)
            }

            // --- MENAMBAHKAN LONG CLICK LISTENER KE SELURUH ITEM VIEW ---
            binding.root.setOnLongClickListener {
                listenerCallback?.onMenuItemLongClick(menu) ?: true // Panggil metode long click di listener
            }
        }
    }

    private fun formatPriceToRupiah(price: Double): String {
        val localeID = java.util.Locale("in", "ID")
        val currencyFormat = NumberFormat.getCurrencyInstance(localeID)
        return currencyFormat.format(price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuTenantViewHolder {
        val binding = ItemMenuTenantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuTenantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuTenantViewHolder, position: Int) {
        holder.bind(listMenu[position], actionListenerInstance)
    }

    override fun getItemCount(): Int = listMenu.size

    fun updateData(newMenuList: List<Menu>) {
        val diffCallback = MenuDiffCallback(this.listMenu, newMenuList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.listMenu = ArrayList(newMenuList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getMenuById(menuId: String): Menu? {
        return listMenu.find { it.id.toString() == menuId }
    }

    fun getPositionOfMenu(menu: Menu): Int {
        return listMenu.indexOf(menu)
    }
}
