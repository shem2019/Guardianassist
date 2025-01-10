package com.example.guardianassist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.appctrl.Site

class SiteAdapter(
    private val sites: List<Site>,
    private val onToggleStatus: (Site, Switch) -> Unit,
    private val onItemClick: (Site) -> Unit // ✅ Click event callback
) : RecyclerView.Adapter<SiteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val siteName: TextView = view.findViewById(R.id.tvSiteName)
        val siteAddress: TextView = view.findViewById(R.id.tvSiteAddress)
        val switchStatus: Switch = view.findViewById(R.id.switchSubscriptionStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_site, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val site = sites[position]
        holder.siteName.text = site.site_name
        holder.siteAddress.text = site.site_address

        // ✅ Clicking a site triggers onItemClick
        holder.itemView.setOnClickListener { onItemClick(site) }

        // ✅ Handle toggle switch for activating/deactivating site
        holder.switchStatus.isChecked = site.subscription_status == "Active"
        holder.switchStatus.setOnCheckedChangeListener { _, _ ->
            onToggleStatus(site, holder.switchStatus)
        }
    }

    override fun getItemCount(): Int = sites.size
}
