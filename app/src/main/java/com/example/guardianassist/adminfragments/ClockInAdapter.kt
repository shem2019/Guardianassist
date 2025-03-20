package com.example.guardianassist.adminfragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.ClockInRecord

class ClockInAdapter(private val clockInList: List<ClockInRecord>) :
    RecyclerView.Adapter<ClockInAdapter.ClockInViewHolder>() {

    inner class ClockInViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val siteName: TextView = view.findViewById(R.id.siteNameText)
        val realName: TextView = view.findViewById(R.id.realNameText)
        val clockInTime: TextView = view.findViewById(R.id.clockInTimeText)
        val isOnSiteIcon: ImageView = view.findViewById(R.id.isOnSiteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClockInViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clock_in, parent, false)
        return ClockInViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClockInViewHolder, position: Int) {
        val record = clockInList[position]
        holder.siteName.text = record.siteName
        holder.realName.text = record.realName
        holder.clockInTime.text = record.clockInTime

        // ✅ Show a tick if "is_on_site" is 1, otherwise show an "X"
        holder.isOnSiteIcon.setImageResource(
            if (record.isOnSite == 1) R.drawable.tick else R.drawable.cross
        )
    }

    override fun getItemCount(): Int = clockInList.size
}
