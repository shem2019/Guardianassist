package com.example.guardianassist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.Organization

class OrganizationAdapter(
    private val organizations: List<Organization>,
    private val onItemClick: (Organization) -> Unit
) : RecyclerView.Adapter<OrganizationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orgName: TextView = view.findViewById(R.id.tvOrganizationName)
        val status: TextView = view.findViewById(R.id.tvSubscriptionStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_organization, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val organization = organizations[position]
        holder.orgName.text = organization.org_name
        holder.status.text = organization.subscription_status

        // Set color based on subscription status
        holder.status.setTextColor(
            if (organization.subscription_status == "Active")
                ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark)
            else
                ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
        )

        // Handle item click
        holder.itemView.setOnClickListener { onItemClick(organization) }
    }

    override fun getItemCount() = organizations.size
}