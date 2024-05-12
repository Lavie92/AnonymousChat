package com.example.anonymousChat.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.anonymousChat.R
import com.example.anonymousChat.presentation.ChatActivity
import com.example.anonymousChat.presentation.ChatNearestActivity
import com.example.anonymousChat.presentation.FullImageActivity
import com.example.anonymousChat.model.Message
import com.example.anonymousChat.encryptimport.BlurTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nipunru.nsfwdetector.NSFWDetector
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MessageAdapter(val context: Context) :
    ListAdapter<Message, RecyclerView.ViewHolder>(MessagesComparator()) {
    private val ITEM_RECEIVE = 1
    private val ITEM_SENT = 2
    private val ITEM_SYSTEM = 3
    private val ITEM_IMAGE_SENT = 4
    private val ITEM_IMAGE_RECEIVE = 5
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = getItem(position)
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
                        val originalBitmap =
                            (holder.ivReceiveMessage.drawable as BitmapDrawable).bitmap
                        val copiedBitmap = originalBitmap.copy(originalBitmap.config, true)

                        val confidenceThreshold = 0.7f
                        NSFWDetector.isNSFW(copiedBitmap, confidenceThreshold) { isNSFW, _, _ ->
                            if (isNSFW) {
                                val blurredBitmap =
                                    BlurTransformation(context).transform(copiedBitmap)
                                holder.ivReceiveMessage.setImageBitmap(blurredBitmap)
                                holder.ivReceiveMessage.setOnClickListener {
                                    imageUrl?.let { it1 -> showFullImage(it1) }
                                }
                            } else {
                                holder.ivReceiveMessage.setOnClickListener {
                                    imageUrl?.let { it1 -> showFullImage(it1) }
                                }
                            }
                        }
                    }

                    override fun onError(e: Exception?) {
                    }
                })

                if (currentMessage.timestamp != lastMessageTimestamp) {
                    holder.tvImageSentTime.text =
                        DateFormat.format("hh:mm aa", currentMessage.timestamp)
                } else {
                    holder.tvImageSentTime.text = ""
                }
            }

            is SystemViewHolder -> {
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

                holder.receiveMessage.setOnLongClickListener(object : View.OnLongClickListener {
                    override fun onLongClick(v: View?): Boolean {
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

    private fun showFullImage(imageUrl: String) {
        val intent = Intent(context, FullImageActivity::class.java)
        intent.putExtra("image_url", imageUrl)
        context.startActivity(intent)
    }

    private fun showOptionsDialog(currentMessage: Message) {
        val optionsDialog = AlertDialog.Builder(context)
            .setTitle("Message Options")
            .setItems(arrayOf("Report", "Copy", "Delete")) { _, which ->
                when (which) {
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
        val currentMessage = getItem(position)
        val type = currentMessage.type
        if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId) && type.equals(
                "text"
            )
        ) {
            return ITEM_SENT
        } else if (currentMessage.senderId.equals("system")) {
            return ITEM_SYSTEM
        } else if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.receiverId) && type.equals(
                "text"
            )
        ) {
            return ITEM_RECEIVE
        } else if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId) && type.equals(
                "image"
            )
        ) {
            return ITEM_IMAGE_SENT
        } else {
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
    class MessagesComparator: DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.messageId == newItem.messageId
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}


