package com.example.guardianassist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.User

class PasswordUpdateAdapter(
    private val users: List<User>,
    private val onUpdatePassword: (User, String) -> Unit
) : RecyclerView.Adapter<PasswordUpdateAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val etNewPassword: EditText = view.findViewById(R.id.etNewPassword)
        val btnUpdatePassword: Button = view.findViewById(R.id.btnUpdatePassword)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_password_update, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.tvUserName.text = user.realName

        holder.btnUpdatePassword.setOnClickListener {
            val newPassword = holder.etNewPassword.text.toString().trim()
            if (newPassword.isNotEmpty()) {
                onUpdatePassword(user, newPassword)
            }
        }
    }

    override fun getItemCount(): Int = users.size
}