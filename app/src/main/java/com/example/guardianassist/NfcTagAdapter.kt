package com.example.guardianassist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.guardianassist.appctrl.NfcTag
class NfcTagAdapter(
    private val tags: List<NfcTag>,
    private val onWriteTag: (NfcTag) -> Unit
) : RecyclerView.Adapter<NfcTagAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tagName: TextView = view.findViewById(R.id.tvTagName)
        val tagType: TextView = view.findViewById(R.id.tvTagType)
        val btnWrite: Button = view.findViewById(R.id.btnWriteTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nfc_tag, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tag = tags[position]

        holder.tagName.text = "Tag Name: ${tag.tagName}" // ✅ Fix binding for `tag_name`
        holder.tagType.text = "Type: ${tag.tagType}"

        holder.btnWrite.setOnClickListener {
            onWriteTag(tag)
        }
    }

    override fun getItemCount(): Int = tags.size
}
