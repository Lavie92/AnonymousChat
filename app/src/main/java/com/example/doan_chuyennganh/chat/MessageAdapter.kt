package com.example.doan_chuyennganh.chat

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.provider.Telephony.Mms.Sent
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.encryptimport.BlurTransformation
import com.example.doan_chuyennganh.report.Reports
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nipunru.nsfwdetector.NSFWDetector
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.util.UUID

class MessageAdapter(val context: Context, val messageList: ArrayList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val ITEM_RECEIVE = 1
    val ITEM_SENT = 2
    val ITEM_SYSTEM = 3
    val ITEM_IMAGE_SENT = 4
    val ITEM_IMAGE_RECEIVE = 5
    private var isReported: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                val view: View = LayoutInflater.from(context)
                    .inflate(R.layout.item_message_receive, parent, false)
                ReceiveViewHolder(view)
            }

            2 -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false)
                SentViewHolder(view)
            }

            3 -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.item_system, parent, false)
                SystemViewHolder(view)
            }
            4 -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.item_image_sent, parent, false)
                ImageSentViewHolder(view)
            }
            5 -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.item_image_receive, parent, false)
                ImageReceiveViewHolder(view)
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
        var lastMessageTimestamp: Long = 0

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
            is ImageSentViewHolder -> {
                Picasso.get().load(currentMessage.content).into(holder.ivSentMessage)
                if (currentMessage.timestamp != lastMessageTimestamp) {
                    holder.tvSentTime.text = DateFormat.format("hh:mm aa", currentMessage.timestamp)
                } else {
                    holder.tvSentTime.text = ""
                }
            }
            is ImageReceiveViewHolder -> {
                val imageUrl = currentMessage.content

                Picasso.get().load(imageUrl).into(holder.ivReceiveMessage, object : Callback {
                    override fun onSuccess() {
                        val originalBitmap = (holder.ivReceiveMessage.drawable as BitmapDrawable).bitmap

                        val copiedBitmap = originalBitmap.copy(originalBitmap.config, true)

                        val confidenceThreshold = 0.7f // Set your desired threshold
                        NSFWDetector.isNSFW(copiedBitmap, confidenceThreshold) { isNSFW, _, _ ->
                            if (isNSFW) {
                                val blurredBitmap = BlurTransformation(context).transform(copiedBitmap)
                                holder.ivReceiveMessage.setImageBitmap(blurredBitmap)
                            }
                        }
                    }

                    override fun onError(e: Exception?) {
                    }
                })

                if (currentMessage.timestamp != lastMessageTimestamp) {
                    holder.tvImageSentTime.text = DateFormat.format("hh:mm aa", currentMessage.timestamp)
                } else {
                    holder.tvImageSentTime.text = ""
                }
            }            is SystemViewHolder -> {
                holder.systemMessage.text = messageText
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
            .setItems(arrayOf("Report", "Copy", "Delete")) { _, which ->
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
        val type = currentMessage.type
        if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId) && type.equals("text")) {
            return ITEM_SENT
        } else if (currentMessage.senderId.equals( "system")) {
            return ITEM_SYSTEM
        } else if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.receiverId) && type.equals("text")){
            return ITEM_RECEIVE
        }
        else if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId) && type.equals("image")) {
            return ITEM_IMAGE_SENT
        }
        else {
            return ITEM_IMAGE_RECEIVE
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
    class SystemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val systemMessage = itemView.findViewById<TextView>(R.id.tvSystemMessage)
        val tvSentTime = itemView.findViewById<TextView>(R.id.tvSystemTime)
    }

    class ImageSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivSentMessage = itemView.findViewById<ImageView>(R.id.ivImageSent)
        val tvSentTime = itemView.findViewById<TextView>(R.id.tvImageSentTime)
    }
    class ImageReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivReceiveMessage = itemView.findViewById<ImageView>(R.id.ivImageReceive)
        val tvImageSentTime = itemView.findViewById<TextView>(R.id.tvImageReceiveTime)
    }
}