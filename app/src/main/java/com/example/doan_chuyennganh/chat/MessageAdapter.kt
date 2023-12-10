package com.example.doan_chuyennganh.chat

import android.app.AlertDialog
import android.content.Context
import android.provider.Telephony.Mms.Sent
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.report.Reports
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MessageAdapter(val context: Context, val messageList: ArrayList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val ITEM_RECEIVE = 1
    val ITEM_SENT = 2
    private var isReported: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.item_message_receive, parent, false)
                ReceiveViewHolder(view)
            }
            2 -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false)
                SentViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid viewType: $viewType")
        }
    }


    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        val messageText = currentMessage.getMessageText()
        var lastMessageTimestamp :Long = 0

        when (holder) {
            is SentViewHolder -> {
                holder.sentMessage.text = messageText
                if (currentMessage.timestamp != lastMessageTimestamp) {
                    holder.tvSentTime.text = DateFormat.format("hh:mm aa", currentMessage.timestamp)
                } else {
                    holder.tvSentTime.text = ""

                }
            }
            is ReceiveViewHolder -> {
                holder.receiveMessage.text = messageText
                if (currentMessage.timestamp != lastMessageTimestamp) {
                    holder.tvSentTime.text = DateFormat.format("hh:mm aa", currentMessage.timestamp)
                } else {
                    holder.tvSentTime.text = ""
                }
            }

        }

        when (holder) {
            is ReceiveViewHolder -> {
                holder.receiveMessage.text = messageText

                // Set the OnLongClickListener
                holder.receiveMessage.setOnLongClickListener(object : View.OnLongClickListener {
                    override fun onLongClick(v: View?): Boolean {
                        // Show options dialog here
                        showOptionsDialog(currentMessage)
                        return true
                    }
                })
            }
            is ReceiveViewHolder -> {
                holder.receiveMessage.text = messageText
            }
        }
        when (holder) {
            is SentViewHolder -> {
                holder.sentMessage.text = messageText
                holder.sentMessage.setBackgroundResource(if (isReported) R.drawable.bg_reported_message else R.drawable.bg_message)
            }
            is ReceiveViewHolder -> {
                holder.receiveMessage.text = messageText
                holder.receiveMessage.setBackgroundResource(if (isReported) R.drawable.bg_reported_message else R.drawable.bg_message)
            }
        }
    }
    private fun showOptionsDialog(currentMessage: Message) {
        val optionsDialog = AlertDialog.Builder(context)
            .setTitle("Message Options")
            .setItems(arrayOf("Report", "Copy","Delete")) { _, which ->
                when (which) {
//                    0 -> reportMessage(currentMessage)
                    0 -> reportMessage(currentMessage, context)
                    1 -> copyMessage(currentMessage)
                    3 -> deleteMessage(currentMessage)
                }
            }
            .create()
        optionsDialog.show()

    }


    private fun deleteMessage(currentMessage: Message) {
        val db = FirebaseFirestore.getInstance()
        currentMessage.senderId?.let { db.collection("messages").document(it).delete() }
    }
    private fun reportMessage(message: Message, context: Context? = null) {
        if (context is ChatActivity) {
            context.reportMessage(message)
        } else if (context is ChatNearestActivity) {
            context.reportMessage(message)
        }
    }

    private fun copyMessage(currentMessage: Message) {
        val db = FirebaseFirestore.getInstance()
        currentMessage.senderId?.let { db.collection("messages").document(it).delete() }
    }
    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]
        if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId)) {
            return ITEM_SENT
        } else {
            return ITEM_RECEIVE
        }
    }
    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage = itemView.findViewById<TextView>(R.id.tvSentMessage)
        val tvSentTime = itemView.findViewById<TextView>(R.id.tvSentTime)

    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage = itemView.findViewById<TextView>(R.id.tvReceiveMessage)
        val tvSentTime = itemView.findViewById<TextView>(R.id.tvReceiveTime)
    }
}