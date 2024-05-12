package com.example.anonymousChat.report

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.anonymousChat.R
import com.example.anonymousChat.model.ReportsCustom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportedMessagesAdapter (var ds:List<ReportsCustom>): RecyclerView.Adapter<ReportedMessagesAdapter.ViewHolder>(){
    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSTT: TextView = itemView.findViewById(R.id.tvSTT)
        val messageTextView: TextView = itemView.findViewById(R.id.message)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return ds.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val message = ds[position]
        holder.tvSTT.text = "${position+1}"
        holder.messageTextView.text = "${message.message}"
        val time = message.timestamp
        holder.tvTime.text = "${dateFormat.format(Date(time)).toString()}"

    }




    private fun animation(view: View) {
        val animation: Animation = AnimationUtils.loadAnimation(view.context ,android.R.anim.slide_in_left)
        view.animation = animation

    }
}