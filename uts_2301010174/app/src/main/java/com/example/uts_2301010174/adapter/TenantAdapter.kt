package com.example.uts_2301010174.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uts_2301010174.R
import com.example.uts_2301010174.Tenant

class TenantAdapter(
    private val tenants: List<Tenant>,
    private val onClick: (Tenant) -> Unit
) : RecyclerView.Adapter<TenantAdapter.TenantViewHolder>() {

    inner class TenantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnLihatMenu: TextView = itemView.findViewById(R.id.btnLihatMenu)
        private val txtNamaTenant: TextView = itemView.findViewById(R.id.txtNamaTenant)
        private val txtDescriptionTenant: TextView = itemView.findViewById(R.id.txtDescriptionTenant)

        fun bind(tenant: Tenant) {
            txtNamaTenant.text = tenant.name
            txtDescriptionTenant.text = tenant.description

            btnLihatMenu.setOnClickListener {
                onClick(tenant)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TenantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tenant, parent, false)
        return TenantViewHolder(view)
    }

    override fun onBindViewHolder(holder: TenantViewHolder, position: Int) {
        val tenant = tenants[position]
        holder.bind(tenant)  // panggil bind di sini
    }

    override fun getItemCount(): Int = tenants.size
}


