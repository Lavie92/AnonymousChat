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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.anonymousChat.R
import com.example.anonymousChat.databinding.ItemImageReceiveBinding
import com.example.anonymousChat.databinding.ItemImageSentBinding
import com.example.anonymousChat.databinding.ItemMessageReceiveBinding
import com.example.anonymousChat.databinding.ItemMessageSentBinding
import com.example.anonymousChat.databinding.ItemSystemBinding
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
                return ReceiveViewHolder(ItemMessageReceiveBinding.inflate(LayoutInflater.from(parent.context)))
            }

            2 -> {
                return SentViewHolder(ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context)))

            }

            3 -> {
                return SystemViewHolder(ItemSystemBinding.inflate(LayoutInflater.from(parent.context)))
            }

            4 -> {
                return ImageSentViewHolder(ItemImageSentBinding.inflate(LayoutInflater.from(parent.context)))
            }

            5 -> {
                return ImageReceiveViewHolder(ItemImageReceiveBinding.inflate(LayoutInflater.from(parent.context)))
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

    class SentViewHolder(binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        val sentMessage = binding.tvSentMessage
        val tvSentTime = binding.tvSentTime

    }

    class ReceiveViewHolder(binding: ItemMessageReceiveBinding) : RecyclerView.ViewHolder(binding.root) {
        val receiveMessage = binding.tvReceiveMessage
        val tvSentTime = binding.tvReceiveTime
    }

    class SystemViewHolder(binding: ItemSystemBinding) : RecyclerView.ViewHolder(binding.root) {
        val systemMessage = binding.tvSystemMessage
        val tvSentTime = binding.tvSystemTime
    }

    class ImageSentViewHolder(binding: ItemImageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        val ivSentMessage = binding.ivImageSent
        val tvSentTime = binding.tvImageSentTime
    }

    class ImageReceiveViewHolder(binding: ItemImageReceiveBinding) : RecyclerView.ViewHolder(binding.root) {
        val ivReceiveMessage = binding.ivImageReceive
        val tvImageSentTime = binding.tvImageReceiveTime
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


