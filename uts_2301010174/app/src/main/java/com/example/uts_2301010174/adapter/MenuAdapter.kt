package com.example.uts_2301010174.adapter


import android.annotation.SuppressLint
import android.icu.text.NumberFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.uts_2301010174.R
import com.example.uts_2301010174.databinding.ItemMenuBinding
import com.example.uts_2301010174.user.Menu


class MenuAdapter(
    private var listMenu: List<Menu>,
    val onAddToCart: (Menu, Int) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    // Map untuk menyimpan quantity setiap menu berdasarkan ID
    private val quantityMap = mutableMapOf<Int, Int>()
    private var isProcessing = false // Tambahkan flag untuk mencegah klik berulang

    inner class MenuViewHolder(private val binding: ItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(menu: Menu) {
            binding.txtNamaMenu.text = menu.name
            binding.txtDescription.text = menu.description
            binding.txtCategory.text = menu.category
            binding.txtPrice.text = formatPriceToRupiah(menu.price)

            val cleanedPhoto = menu.photo?.trim()?.replace("\n", "")
            val imageUrl = "http://10.0.2.2/api_menu/$cleanedPhoto"
            Log.e("MenuAdapter", "Menu photo: $cleanedPhoto")
            Glide.with(binding.root.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.imgMenu)

            // Get current quantity for this menu
            val currentQuantity = quantityMap[menu.id] ?: 0

            // Update UI berdasarkan quantity
            updateQuantityUI(menu, currentQuantity)

            // Setup click listeners
            setupClickListeners(menu)
        }

        @SuppressLint("SetTextI18n")
        private fun updateQuantityUI(menu: Menu, quantity: Int) {
            binding.textViewQuantity.text = quantity.toString()

            if (quantity > 0) {
                showQuantityControls()
            } else {
                hideQuantityControls()
            }
        }

        private fun setupClickListeners(menu: Menu) {
            // Tombol "Tambahkan" AWAL
            binding.btnTambahMenuCart.setOnClickListener {
                val currentQuantityInMap = quantityMap[menu.id] ?: 0
                if (currentQuantityInMap == 0) {
                    // Pertama kali menambahkan item ini
                    val newQuantity = 1
                    quantityMap[menu.id] = newQuantity
                    updateQuantityUI(menu, newQuantity) // Ini akan menampilkan kontrol kuantitas
                    Log.d("MenuAdapter_Cart", "btnTambahMenuCart (first time): Calling onAddToCart for menu ${menu.name}, quantity $newQuantity")
                    onAddToCart(menu, newQuantity) // Panggil onAddToCart saat pertama kali ditambahkan
                } else {
                    // Ini seharusnya tidak terjadi jika btnTambahMenuCart disembunyikan setelah qty > 0.
                    // Namun, jika bisa terjadi, ini berarti pengguna mengklik "Tambahkan" lagi
                    // padahal item sudah ada (mungkin UI belum update sempurna).
                    // Kita bisa memilih untuk memanggil onAddToCart lagi atau mengabaikannya jika qty sudah ada.
                    // Untuk konsistensi, panggil lagi dengan kuantitas saat ini dari map.
                    Log.d("MenuAdapter_Cart", "btnTambahMenuCart (already >0): Calling onAddToCart for menu ${menu.name}, quantity $currentQuantityInMap")
                    onAddToCart(menu, currentQuantityInMap)
                }
            }

            // Tombol "-" di kontrol kuantitas
            binding.btnDecrease.setOnClickListener {
                if (isProcessing) {
                    Log.d("MenuAdapter_Cart", "btnDecrease: Sedang memproses, klik diabaikan")
                    return@setOnClickListener
                }
                isProcessing = true
                val currentQuantityInMap = quantityMap[menu.id] ?: 0
                if (currentQuantityInMap > 1) {
                    val newQuantity = currentQuantityInMap - 1
                    quantityMap[menu.id] = newQuantity
                    updateQuantityUI(menu, newQuantity)
                    Log.d("MenuAdapter_Cart", "btnDecrease: Calling onAddToCart for menu ${menu.name}, quantity $newQuantity")
                    onAddToCart(menu, newQuantity) // Update keranjang
                } else if (currentQuantityInMap == 1) {
                    // Kuantitas menjadi 0
                    val newQuantity = 0
                    quantityMap[menu.id] = newQuantity // Atau remove(menu.id) jika lebih sesuai
                    updateQuantityUI(menu, newQuantity) // Ini akan menyembunyikan kontrol kuantitas
                    Log.d("MenuAdapter_Cart", "btnDecrease (to 0): Calling onAddToCart for menu ${menu.name}, quantity $newQuantity")
                    onAddToCart(menu, newQuantity) // Update keranjang (item dihapus/kuantitas 0)
                }
                isProcessing = false;
            }

            // Tombol "+" di kontrol kuantitas
            binding.btnIncrease.setOnClickListener {
                if (isProcessing) {
                    Log.d("MenuAdapter_Cart", "btnIncrease: Sedang memproses, klik diabaikan")
                    return@setOnClickListener
                }
                isProcessing = true
                val currentQuantityInMap = quantityMap[menu.id] ?: 0
                val newQuantity = currentQuantityInMap + 1
                quantityMap[menu.id] = newQuantity
                updateQuantityUI(menu, newQuantity)
                Log.d("MenuAdapter_Cart", "btnIncrease: Calling onAddToCart for menu ${menu.name}, quantity $newQuantity")
                onAddToCart(menu, newQuantity) // Update keranjang
                isProcessing = false
            }
        }

        private fun showQuantityControls() {
            binding.layoutQuantityControl.visibility = View.VISIBLE
            binding.btnTambahMenuCart.visibility = View.GONE
        }

        private fun hideQuantityControls() {
            binding.layoutQuantityControl.visibility = View.GONE
            binding.btnTambahMenuCart.visibility = View.VISIBLE
        }
    }

    fun formatPriceToRupiah(price: Double): String {
        val localeID = java.util.Locale("in", "ID")
        val currencyFormat = NumberFormat.getCurrencyInstance(localeID)
        return currencyFormat.format(price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
//        Log.d("MenuAdapter", "Binding item at position $position: ${listMenu[position].name}")
        holder.bind(listMenu[position])
    }

    override fun getItemCount(): Int {
        val count = listMenu.size
//        Log.d("MenuAdapter", "Item count: $count")
        return count
    }

    // Fungsi untuk memperbarui data adapter
    fun updateData(newMenuList: List<Menu>) {
        val diffCallback = MenuDiffCallback(this.listMenu, newMenuList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.listMenu = ArrayList(newMenuList)
        diffResult.dispatchUpdatesTo(this)
    }

    // Fungsi untuk mendapatkan quantity menu tertentu
    fun getMenuQuantity(menuId: Int): Int {
        return quantityMap[menuId] ?: 0
    }

    // Fungsi untuk reset quantity semua menu
    fun resetAllQuantities() {
        quantityMap.clear()
        notifyDataSetChanged()
    }

    // Fungsi untuk set quantity menu tertentu (berguna jika ada data dari cart)
    fun setMenuQuantity(menuId: Int, quantity: Int) {
        quantityMap[menuId] = quantity
        notifyDataSetChanged()
    }
}
