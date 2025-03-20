package com.example.guardianassist.adminfragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.ClockOutRecord

class ClockOutAdapter(private val clockOutList: List<ClockOutRecord>) :
    RecyclerView.Adapter<ClockOutAdapter.ClockOutViewHolder>() {

    inner class ClockOutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val siteName: TextView = view.findViewById(R.id.siteNameText)
        val realName: TextView = view.findViewById(R.id.realNameText)
        val clockOutTime: TextView = view.findViewById(R.id.clockOutTimeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClockOutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clock_out, parent, false)
        return ClockOutViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClockOutViewHolder, position: Int) {
        val record = clockOutList[position]
        holder.siteName.text = record.siteName
        holder.realName.text = record.realName
        holder.clockOutTime.text = record.clockOutTime
    }

    override fun getItemCount(): Int = clockOutList.size
}
