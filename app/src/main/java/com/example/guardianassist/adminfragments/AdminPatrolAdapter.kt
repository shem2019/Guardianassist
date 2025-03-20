package com.example.guardianassist.adminfragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.GroupedPatrols

class AdminPatrolAdapter(private val groupedPatrols: List<GroupedPatrols>) :
    RecyclerView.Adapter<AdminPatrolAdapter.PatrolViewHolder>() {

    private val expandedSites = mutableSetOf<Int>() // ✅ Track expanded sites

    inner class PatrolViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val siteName: TextView = view.findViewById(R.id.siteNameText)
        val patrolContainer: LinearLayout = view.findViewById(R.id.patrolContainer)
        val expandIcon: ImageView = view.findViewById(R.id.expandIcon)
        val detailsContainer: View = view.findViewById(R.id.detailsContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatrolViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_site, parent, false)
        return PatrolViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatrolViewHolder, position: Int) {
        val groupedPatrol = groupedPatrols[position]
        holder.siteName.text = groupedPatrol.siteName

        // ✅ Ensure correct visibility for expanded/collapsed states
        if (expandedSites.contains(position)) {
            holder.detailsContainer.visibility = View.VISIBLE
            holder.expandIcon.setImageResource(R.drawable.cross)
        } else {
            holder.detailsContainer.visibility = View.GONE
            holder.expandIcon.setImageResource(R.drawable.expand)
        }

        // ✅ Handle expand/collapse click
        holder.expandIcon.setOnClickListener {
            if (expandedSites.contains(position)) {
                expandedSites.remove(position)
            } else {
                expandedSites.add(position)
            }
            notifyItemChanged(position) // ✅ Update only this item instead of resetting RecyclerView
        }

        // ✅ Ensure previous patrol records are cleared before adding new ones
        holder.patrolContainer.removeAllViews()

        for ((guardName, patrolAreas) in groupedPatrol.guards) {
            val guardView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_guard_patrol, holder.patrolContainer, false)

            val guardTextView: TextView = guardView.findViewById(R.id.guardName)
            guardTextView.text = guardName

            val patrolListContainer: LinearLayout = guardView.findViewById(R.id.patrolListContainer)

            for ((patrolArea, visitCount) in patrolAreas) {
                val patrolItemView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_patrol_record, patrolListContainer, false)

                val patrolTagName: TextView = patrolItemView.findViewById(R.id.patrolTagName)
                val patrolTimes: TextView = patrolItemView.findViewById(R.id.patrolTimes)

                patrolTagName.text = patrolArea
                patrolTimes.text = visitCount.toString()

                patrolListContainer.addView(patrolItemView)
            }

            holder.patrolContainer.addView(guardView)
        }
    }

    override fun getItemCount(): Int = groupedPatrols.size
}
