package com.example.guardianassist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.appctrl.Site

class SiteAdapter(
    private val siteList: List<Site>,
    private val onToggleStatus: (Site, Switch) -> Unit,
    private val onSiteClick: (Site) -> Unit
) : RecyclerView.Adapter<SiteAdapter.SiteViewHolder>() {

    class SiteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val siteName: TextView = view.findViewById(R.id.tvSiteName)
        val siteStatus: TextView = view.findViewById(R.id.tvSiteStatus)
        val toggleSwitch: Switch = view.findViewById(R.id.switchStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_site, parent, false)
        return SiteViewHolder(view)
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        val site = siteList[position]
        holder.siteName.text = site.site_name
        holder.siteStatus.text = site.subscription_status

        // ✅ Change text color based on status
        if (site.subscription_status == "Active") {
            holder.siteStatus.setTextColor(Color.GREEN)
        } else {
            holder.siteStatus.setTextColor(Color.RED)
        }

        holder.toggleSwitch.isChecked = site.subscription_status == "Active"

        holder.toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggleStatus(site, holder.toggleSwitch)
        }

        holder.itemView.setOnClickListener {
            onSiteClick(site)
        }
    }

    override fun getItemCount(): Int = siteList.size
}
