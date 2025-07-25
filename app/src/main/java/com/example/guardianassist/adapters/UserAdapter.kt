package com.example.guardianassist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.User

class UserAdapter(
    private val users: List<User>,
    private val onToggleActivation: (User, Boolean) -> Unit
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val switchActive: Switch = view.findViewById(R.id.switchActive)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.tvUserName.text = user.realName
        holder.switchActive.isChecked = user.isActive

        holder.switchActive.setOnCheckedChangeListener { _, isChecked ->
            onToggleActivation(user, isChecked)
        }
    }

    override fun getItemCount(): Int = users.size
}