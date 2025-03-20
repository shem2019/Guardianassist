package com.example.guardianassist.adminfragments

import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.HourlyCheckRecord

class HourlyCheckAdapter(private val hourlyCheckList: List<HourlyCheckRecord>) :
    RecyclerView.Adapter<HourlyCheckAdapter.HourlyCheckViewHolder>() {

    inner class HourlyCheckViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val siteName: TextView = view.findViewById(R.id.siteNameText)
        val realName: TextView = view.findViewById(R.id.realNameText)
        val checkTime: TextView = view.findViewById(R.id.checkTimeText)
        val comments: TextView = view.findViewById(R.id.commentsText)
        val itemLayout: View = view.findViewById(R.id.hourlyCheckItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyCheckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourlycheck, parent, false)
        return HourlyCheckViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourlyCheckViewHolder, position: Int) {
        val record = hourlyCheckList[position]
        holder.siteName.text = record.siteName
        holder.realName.text = record.realName
        holder.checkTime.text = record.checkTime

        holder.comments.text = record.comments
        holder.comments.movementMethod = ScrollingMovementMethod()

        // Highlight item if any issue is found
        if (record.personalSafety == 0 || record.siteSecure == 0 || record.equipmentFunctional == 0) {
            holder.itemLayout.setBackgroundResource(R.color.red)
        } else {
            holder.itemLayout.setBackgroundResource(R.color.white)
        }
    }

    override fun getItemCount(): Int = hourlyCheckList.size
}
