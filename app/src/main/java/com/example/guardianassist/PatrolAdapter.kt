package com.example.guardianassist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

data class PatrolRecord(val tagName: String, val patrolTime: String)

class PatrolAdapter(private val patrolList: List<PatrolRecord>) :
    RecyclerView.Adapter<PatrolAdapter.PatrolViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatrolViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_patrol_log, parent, false)
        return PatrolViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatrolViewHolder, position: Int) {
        val patrol = patrolList[position]
        holder.tvPatrolTag.text = patrol.tagName
        holder.tvPatrolTime.text = "Time: ${patrol.patrolTime}"

        // ✅ Alternate Row Colors
        val backgroundColor = if (position % 2 == 0) Color.parseColor("#E3F2FD") else Color.parseColor("#FFFFFF")
        holder.itemView.setBackgroundColor(backgroundColor)
    }

    override fun getItemCount(): Int = patrolList.size

    class PatrolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPatrolTag: TextView = itemView.findViewById(R.id.tvPatrolTag)
        val tvPatrolTime: TextView = itemView.findViewById(R.id.tvPatrolTime)
    }
}
