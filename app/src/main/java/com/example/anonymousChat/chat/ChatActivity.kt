package com.example.anonymousChat.chat

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.anonymousChat.R
import com.example.anonymousChat.adapter.MessageAdapter
import com.example.anonymousChat.authentication.User
import com.example.anonymousChat.authentication.toUser
import com.example.anonymousChat.databinding.ActivityChatBinding
import com.example.anonymousChat.encrypt.EncryptionUtils
import com.example.anonymousChat.layout.SplashScreenActivity
import com.example.anonymousChat.notification.NotificationService
import com.example.anonymousChat.report.Reports
import com.example.filterbadwodslibrary.filterBadwords
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class ChatActivity : AppCompatActivity() {
    private var optionsVisible = false
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var btnSend: ImageView
    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var usersRef: DatabaseReference
    private lateinit var chatRoomsRef: DatabaseReference
    private var chatRoomId: String = ""
    private var messageList: ArrayList<Message>? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var receiverId: String = ""
    private val currentUser = FirebaseAuth.getInstance().currentUser
    val badwords = filterBadwords()
    private val handler = Handler()
    private lateinit var auth: FirebaseAuth
    private lateinit var btnHeart: Button
    private lateinit var ivSendImage: ImageView
    private lateinit var btnRandom: Button
    private lateinit var imageUtils: ImageUtils
    private lateinit var messageUtils: MessageUtils

    companion object {
        const val REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messageRecyclerView = binding.rcMessage
        auth = FirebaseAuth.getInstance()
        imageUtils = ImageUtils(this)
        messageUtils = MessageUtils(this, "chatRooms")
        chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms")
        usersRef = FirebaseDatabase.getInstance().getReference("users")
        messageBox = binding.messageBox
        btnSend = binding.ivSend
        btnHeart = binding.btnHeart
        btnRandom = binding.btnRandom
        ivSendImage = binding.ivSendImage
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList!!)
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        val btnEndChat: Button = binding.btnEndChat

        val btnShowOptions: FloatingActionButton = binding.btnShowOptions
        btnShowOptions.setOnClickListener {
            toggleOptions()
        }

        binding.btnBack.setOnClickListener {
            val splashIntent = Intent(this@ChatActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toMain")
            startActivity(splashIntent)
        }
        checkChatRoomId()
        var hasPressHeart = false
        btnHeart.setOnClickListener {
            if (!hasPressHeart) {
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setTitle("Cảnh báo")
                alertDialogBuilder.setMessage("Khi nhấn tim, bạn đồng ý chia sẻ thông tin cá nhân của bạn cho đối phương")
                alertDialogBuilder.setPositiveButton("Đồng ý") { dialog, which ->
                    hasPressHeart = true
                    messageUtils.sendMessage(
                        chatRoomId,
                        "system",
                        currentUserId.toString(),
                        "Bạn đã nhấn yêu thích, nếu đối phương đồng ý thì bạn sẽ chia sẻ thông tin (tuổi, giới tính, username)"
                    )
                    messageUtils.sendMessage(
                        chatRoomId,
                        "system",
                        receiverId,
                        "Đối phương thích bạn, nếu bạn cũng vậy hãy nhấn tim để chia sẻ thông tin gồm (username, tuổi, giới tính)"
                    )
                    messageUtils.shareMoreInformation(chatRoomId, currentUserId.toString(), receiverId)

                }
                alertDialogBuilder.setNegativeButton("Hủy") { dialog, which ->
                    dialog.dismiss()
                }

                val alertDialog = alertDialogBuilder.create()
                alertDialog.show()
            }
            else {
                Toast.makeText(this, "Bạn chỉ được gửi tim 1 lần", Toast.LENGTH_SHORT).show()
            }
        }

        btnRandom.setOnClickListener {
            if (chatRoomId.isNotEmpty()) {
                checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
                    if (!isChatRoomEnded) {
                        Toast.makeText(
                            this,
                            "Bạn cần kết thúc cuộc trò chuyện hiện tại trước!!",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("chatRoomIdRandom", "id $chatRoomId")
                    } else {
                        removeMessage(chatRoomId) {
                            findRandomUserForChat()
                        }
                    }
                }
            } else {
                findRandomUserForChat()
            }
        }
        btnEndChat.setOnClickListener {
            updateUserStatus(currentUserId.toString(), false)
            if (receiverId.isNotEmpty()) {
                updateUserStatus(receiverId, false)
            }
            if (chatRoomId.isNotEmpty()) {
                messageUtils.endChat(chatRoomId, currentUserId.toString(), receiverId)
            }
        }

        btnSend.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            if (messageText.isNotEmpty() && currentUserId != null && chatRoomId.isNotEmpty()) {
                messageUtils.sendMessage(chatRoomId, currentUserId, receiverId, messageText)
                messageBox.text.clear()
            }
        }
        ivSendImage.setOnClickListener {
            if (chatRoomId.isNotEmpty()) {
                checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
                    if (!isChatRoomEnded) {
                        imageUtils.showImagePickerDialog()
                    } else {
                        Toast.makeText(this, "Bạn cần tìm người chat trước!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri: Uri = data.data ?: return
            if (currentUserId != null) {
                imageUtils.uploadImageToFirebaseStorage(
                    selectedImageUri, chatRoomId, "chatRooms", currentUserId, receiverId
                )
            }
        }
    }

    private fun toggleOptions() {
        val btnEndChat: Button = findViewById(R.id.btnEndChat)
        val slideUp = TranslateAnimation(0f, 0f, btnHeart.height.toFloat(), 0f)
        slideUp.duration = 500

        val slideDown = TranslateAnimation(0f, 0f, 0f, btnHeart.height.toFloat())
        slideDown.duration = 50
        if (!optionsVisible) {
            btnEndChat.startAnimation(slideUp)
            ivSendImage.startAnimation(slideUp)
            btnHeart.startAnimation((slideUp))
            btnHeart.visibility = View.VISIBLE
            ivSendImage.visibility = View.VISIBLE
            btnRandom.startAnimation(slideUp)
            btnRandom.visibility = View.VISIBLE
            btnEndChat.visibility = View.VISIBLE
            checkChatRoomStatus(chatRoomId) {
            }
        } else {
            btnEndChat.startAnimation(slideDown)
            btnHeart.startAnimation(slideDown)
            btnHeart.visibility = View.GONE
            btnEndChat.visibility = View.GONE
            btnRandom.startAnimation(slideDown)
            btnRandom.visibility = View.GONE
            ivSendImage.startAnimation(slideDown)
            ivSendImage.visibility = View.GONE
        }
        optionsVisible = !optionsVisible
    }

    private val timeoutRunnable = Runnable {
        updateUserStatus(currentUserId.toString(), false)
        isFindByLocation(currentUserId.toString(), false)
        showNotification("Không tìm thấy người nào", "Vui lòng thử lại sau")
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun showMessage(message: String) {
        val systemMessage = Message(
            UUID.randomUUID().toString(),
            "system",
            currentUserId ?: "",
            message,
            "text",
            System.currentTimeMillis()
        )

        messageList?.add(systemMessage)
        messageAdapter.notifyDataSetChanged()

        messageRecyclerView.scrollToPosition(messageList!!.size - 1)
    }

    private fun findRandomUserForChat() {
        chatRoomId = ""
        receiverId = ""
        if (currentUserId != null) {
            updateUserStatus(currentUserId, true)
        }
        showMessage("Đang tìm kiếm...")
        val currentTime = SystemClock.uptimeMillis()
        val timeoutTime = currentTime + 30000
        handler.postAtTime(timeoutRunnable, timeoutTime)

        usersRef.get().addOnSuccessListener { snapshot ->
            val allUsers = snapshot.children.map {
                it.getValue(User::class.java)!!
            }.filter { it.ready && it.id != currentUserId }

            if (allUsers.isNotEmpty()) {
                    handler.removeCallbacks(timeoutRunnable)
                    val randomUser = allUsers.random()
                    receiverId = randomUser.id.toString()
                    chatRoomId =
                        currentUser?.toUser()?.let { createChatRoom(it, randomUser) }
                            .toString()
                    sendInitialMessages(chatRoomId, currentUserId.toString(), receiverId)
                    currentUserId?.let { updateUserStatus(it, false) }
                    updateUserStatus(receiverId, false)
            }
        }
    }

    private fun sendInitialMessages(chatRoomId: String, senderId: String, receiverId: String) {
        messageUtils.sendMessage(chatRoomId, "system", senderId, "Bạn đã tham gia chat!!")
        messageUtils.sendMessage(chatRoomId, "system", receiverId, "Bạn đã tham gia chat!!")
    }

    private fun updateUserStatus(userId: String, ready: Boolean) {
        val userRef = usersRef.child(userId)
        userRef.child("ready").setValue(ready)
    }

    private fun isFindByLocation(userId: String, isFindByLocation: Boolean) {
        val userRef = usersRef.child(userId)
        userRef.child("isFindByLocation").setValue(isFindByLocation)
    }


    private fun createChatRoom(user1: User, user2: User): String {
        chatRoomId = UUID.randomUUID().toString()
        user1.randomChatRoom = chatRoomId
        user2.randomChatRoom = chatRoomId
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        chatRoomRef.child("user1Id").setValue(user1.id)
        chatRoomRef.child("user2Id").setValue(user2.id)
        chatRoomRef.child("status").setValue("chatting")
        val user1Reference = user1.id?.let { usersRef.child(it) }
        user1Reference?.child("randomChatRoom")?.setValue(chatRoomId)
        val user2Reference = user2.id?.let { usersRef.child(it) }
        user2Reference?.child("randomChatRoom")?.setValue(chatRoomId)
        return chatRoomId
    }

    private fun loadMessages(chatRoomId: String) {
        var filter: Boolean
        val messagesRef = chatRoomsRef.child(chatRoomId).child("messages")
        if (currentUserId != null) {
            usersRef.child(currentUserId).get().addOnSuccessListener {
                filter = it.child("filter").value as Boolean
                messagesRef.addValueEventListener(object : ValueEventListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val newMessages = snapshot.children.mapNotNull { msgSnapshot ->
                            val messageId =
                                msgSnapshot.child("messageId").getValue(String::class.java)
                            val senderId =
                                msgSnapshot.child("senderId").getValue(String::class.java)
                            val receiverId =
                                msgSnapshot.child("receiverId").getValue(String::class.java)
                            if (senderId == currentUserId || receiverId == currentUserId) {
                                var content =
                                    msgSnapshot.child("content").getValue(String::class.java)
                                val type = msgSnapshot.child("type").getValue(String::class.java)
                                val timestamp =
                                    msgSnapshot.child("timestamp").getValue(Long::class.java)

                                if (filter) {
                                    content = badwords.filterBadWords(content)
                                }

                                messageId?.let {
                                    senderId?.let { sid ->
                                        receiverId?.let { rid ->
                                            content?.let { msg ->
                                                timestamp?.let { time ->
                                                    type?.let { it1 ->
                                                        Message(
                                                            it,
                                                            sid,
                                                            rid,
                                                            msg,
                                                            it1,
                                                            time
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                null
                            }
                        }

                        messageList?.clear()
                        newMessages.let { messageList?.addAll(it) }

                        messageAdapter.notifyDataSetChanged()
                        messageRecyclerView.scrollToPosition(messageList!!.size - 1)

                        if (newMessages.isNotEmpty()) {
                            var content = "";
                            val lastMessage = newMessages.last()
                            content = if (lastMessage.type!! == "image") {
                                "Người lạ đã gửi 1 hình ảnh"
                            } else {
                                lastMessage.content!!
                            }
                            if (lastMessage.senderId != currentUserId) {
                                lastMessage.content?.let {
                                    showNotification(
                                        "Chat ngẫu nhiên",
                                        content
                                    )
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }
        }
    }

    private fun showNotification(title: String, content: String) {
        val notificationService = NotificationService()
        notificationService.showNotification(this, title, content, "chatRooms")
    }

    private fun checkChatRoomStatus(chatRoomId: String, callback: (Boolean) -> Unit) {
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        chatRoomRef.child("status").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                callback.invoke(status == "ended")
            }

            override fun onCancelled(error: DatabaseError) {
                callback.invoke(false)
            }
        })
    }

    private fun checkChatRoomId() {
        var chatRoomValue = ""
        if (currentUserId != null) {
            val userRef = usersRef.child(currentUserId)
            userRef.child("randomChatRoom").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatRoomValue = snapshot.getValue(String::class.java).toString()
                    checkUsersInChatRoom(chatRoomValue)
                    chatRoomId = chatRoomValue
                    loadMessages(chatRoomId)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    private fun checkUsersInChatRoom(chatRoomId: String) {
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)

        chatRoomRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user1Id = snapshot.child("user1Id").getValue(String::class.java)
                val user2Id = snapshot.child("user2Id").getValue(String::class.java)
                if (user1Id != "" && user2Id != "" && !user1Id.isNullOrEmpty() && !user2Id.isNullOrEmpty())
                    receiverId = if (user1Id == currentUserId) user2Id else user1Id
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun removeMessage(chatRoomId: String, callback: () -> Unit) {
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        chatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                usersRef.child(currentUserId.toString()).child("randomChatRoom").setValue("")
                callback.invoke()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    fun reportMessage(message: Message) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val receiverUserId = currentUserId
        val senderUserId = message.senderId
        val messageMap = mapOf(
            "timestamp" to message.timestamp, "content" to message.content
        )
        val report = Reports(senderUserId!!, receiverUserId!!)
        val reportRef = FirebaseDatabase.getInstance().getReference("reports").child(chatRoomId)
            .child(message.messageId!!)
        reportRef.setValue(report)

        reportRef.child("status").setValue("doing")
        reportRef.child("messageMap").setValue(messageMap).addOnSuccessListener {
            Toast.makeText(this, "Report added successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Error adding report: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }
}