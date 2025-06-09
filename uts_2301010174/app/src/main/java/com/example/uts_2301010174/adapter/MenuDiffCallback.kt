package com.example.uts_2301010174.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.uts_2301010174.user.Menu // Pastikan import Menu benar

class MenuDiffCallback(
    private val oldList: List<Menu>,
    private val newList: List<Menu>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Idealnya, item Anda memiliki ID unik. Jika tidak, Anda bisa membandingkan
        // properti yang dianggap unik untuk item tersebut.
        // Misalnya, jika 'name' unik:
        return oldList[oldItemPosition].name == newList[newItemPosition].name
        // Jika Anda memiliki ID unik (misalnya menu.id):
        // return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Periksa apakah konten item sama. Jika areItemsTheSame mengembalikan true,
        // metode ini dipanggil untuk memeriksa apakah tampilan item perlu di-refresh.
        // Bandingkan semua field yang relevan untuk tampilan.
        return oldList[oldItemPosition] == newList[newItemPosition]
        // Jika Menu adalah data class, perbandingan '==' sudah cukup.
        // Jika tidak, Anda perlu membandingkan field secara manual:
        // val oldMenu = oldList[oldItemPosition]
        // val newMenu = newList[newItemPosition]
        // return oldMenu.name == newMenu.name &&
        //        oldMenu.description == newMenu.description &&
        //        oldMenu.category == newMenu.category &&
        //        oldMenu.price == newMenu.price &&
        //        oldMenu.photo == newMenu.photo
        //        // ... dan field lain yang relevan untuk tampilan
    }

    // Opsional: Implementasikan getChangePayload jika Anda ingin animasi yang lebih spesifik
    // untuk perubahan konten item.
    // override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
    //     // Implementasi jika diperlukan
    //     return super.getChangePayload(oldItemPosition, newItemPosition)
    // }
}