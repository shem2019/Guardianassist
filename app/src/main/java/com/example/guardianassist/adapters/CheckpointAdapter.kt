package com.example.guardianassist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.Tag

class CheckpointAdapter(
    private val checkpointList: MutableList<Tag>,
    private val onDeleteClick: (Tag) -> Unit
) : RecyclerView.Adapter<CheckpointAdapter.ViewHolder>() {

    /** ViewHolder to hold checkpoint views */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkpointName: TextView = view.findViewById(R.id.tvCheckpointName)
        val checkpointType: TextView = view.findViewById(R.id.tvCheckpointType)
        val checkpointCoordinates: TextView = view.findViewById(R.id.tvCheckpointCoordinates)
        val deleteIcon: ImageView = view.findViewById(R.id.ivDeleteCheckpoint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checkpoint, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val checkpoint = checkpointList[position]
        holder.checkpointName.text = checkpoint.name
        holder.checkpointType.text = "Type: ${checkpoint.type}"
        holder.checkpointCoordinates.text = "Lat: ${checkpoint.latitude}, Lng: ${checkpoint.longitude}"

        holder.deleteIcon.setOnClickListener {
            onDeleteClick(checkpoint)
        }
    }

    override fun getItemCount(): Int = checkpointList.size
}