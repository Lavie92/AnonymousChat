package com.example.anonymousChat.presentation

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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.anonymousChat.R
import com.example.anonymousChat.adapter.MessageAdapter
import com.example.anonymousChat.model.User
import com.example.anonymousChat.model.toUser
import com.example.anonymousChat.chat.ImageUtils
import com.example.anonymousChat.chat.MessageUtils
import com.example.anonymousChat.model.Message
import com.example.anonymousChat.databinding.ActivityChatBinding
import com.example.anonymousChat.model.Reports
import com.example.anonymousChat.notification.NotificationUtils
import com.example.anonymousChat.repository.ChatRepository
import com.example.anonymousChat.service.ChatService
import com.example.anonymousChat.viewmodel.ChatViewModel
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
    private lateinit var chatViewModel: ChatViewModel


    companion object {
        const val REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messageUtils = MessageUtils(this, "chatRooms")
        chatViewModel = ChatViewModel(ChatRepository(ChatService("chatRooms")))
        messageRecyclerView = binding.rcMessage
        auth = FirebaseAuth.getInstance()
        imageUtils = ImageUtils(this)
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
        loadAndShowNotification()
//        btnHeart.setOnClickListener {
//            checkIfUserHasPressedHeart(chatRoomId, currentUserId.toString()) { hasPressedHeart ->
//                if (!hasPressedHeart) {
//                    val alertDialogBuilder = AlertDialog.Builder(this)
//                    alertDialogBuilder.setTitle("Cảnh báo")
//                    alertDialogBuilder.setMessage("Khi nhấn tim, bạn đồng ý chia sẻ thông tin cá nhân của bạn cho đối phương")
//                    alertDialogBuilder.setPositiveButton("Đồng ý") { dialog, which ->
//                        sendMessage(
//                            chatRoomId,
//                            "system",
//                            currentUserId.toString(),
//                            "Bạn đã nhấn yêu thích, nếu đối phương đồng ý thì bạn sẽ chia sẻ thông tin (tuổi, giới tính, username)"
//                        )
//                        sendMessage(
//                            chatRoomId,
//                            "system",
//                            receiverId,
//                            "Đối phương thích bạn, nếu bạn cũng vậy hãy nhấn tim để chia sẻ thông tin gồm (username, tuổi, giới tính)"
//                        )
//                        messageUtils.shareMoreInformation(
//                            chatRoomId,
//                            currentUserId.toString(),
//                            receiverId
//                        )
//
//                    }
//                    alertDialogBuilder.setNegativeButton("Hủy") { dialog, which ->
//                        dialog.dismiss()
//                    }
//
//                    val alertDialog = alertDialogBuilder.create()
//                    alertDialog.show()
//                } else {
//                    Toast.makeText(this, "Bạn chỉ được gửi tim 1 lần", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }

        btnRandom.setOnClickListener {
            if (chatRoomId.isNotEmpty()) {
                checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
                    if (!isChatRoomEnded) {
                        Toast.makeText(
                            this,
                            "Bạn cần kết thúc cuộc trò chuyện hiện tại trước!!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        removeMessage(chatRoomId) {
                            findRandomUserForChat()
                        }
                    }
                }
            } else {
                findRandomUserForChat()
            }
//            messageUtils.checkAndDeleteOldChatRooms()
        }
        btnEndChat.setOnClickListener {
            updateUserStatus(currentUserId.toString(), false)
            if (receiverId.isNotEmpty()) {
                updateUserStatus(receiverId, false)
            }
            if (chatRoomId.isNotEmpty()) {
//                messageUtils.endChat(chatRoomId, currentUserId.toString(), receiverId)
            }
            btnRandom.isEnabled = true
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

    private fun sendMessage(chatRoomId: String, senderId: String, receiverId: String, content: String) {
        chatViewModel.sendMessage(chatRoomId, senderId, receiverId, content)
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
    private fun checkIfUserHasPressedHeart(chatRoomId: String, currentUserId: String, callback: (Boolean) -> Unit) {
        val heartRef = chatRoomsRef.child(chatRoomId).child("heart").child(currentUserId)
        heartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hasPressedHeart = snapshot.getValue(Boolean::class.java) ?: false
                callback.invoke(hasPressedHeart)
            }

            override fun onCancelled(error: DatabaseError) {
                callback.invoke(false)
            }
        })
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
        btnRandom.isEnabled = true
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
         btnRandom.isEnabled = false
         val currentTime = SystemClock.uptimeMillis()
        val timeoutTime = currentTime + 30000
        handler.postAtTime(timeoutRunnable, timeoutTime)
        usersRef.get().addOnSuccessListener { snapshot ->
            val allUsers = snapshot.children.map {
                it.getValue(User::class.java)!!
            }.filter { it.ready && it.id != currentUserId }

            if (allUsers.isNotEmpty()) {
                handler.removeCallbacks(timeoutRunnable)
                btnRandom.isEnabled = true
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
        sendMessage(chatRoomId, "system", senderId, "Bạn đã tham gia chat!!")
        sendMessage(chatRoomId, "system", receiverId, "Bạn đã tham gia chat!!")
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

    private fun loadAndShowNotification() {
        var chatRoomValue = ""
        if (currentUserId != null) {
            val userRef = usersRef.child(currentUserId)
            userRef.child("randomChatRoom").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatRoomValue = snapshot.getValue(String::class.java).toString()
                    checkUsersInChatRoom(chatRoomValue)
                    chatRoomId = chatRoomValue
                    chatViewModel.loadMessageAsync(chatRoomId)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Xử lý lỗi nếu cần
                }
            })
        }

        chatViewModel.observeMessageLiveData().observe(this@ChatActivity, Observer { messageList ->
            messageAdapter.messageList = messageList
            messageAdapter.notifyDataSetChanged()
            if (messageList != null) {
                val content = messageList.last().content
                Log.d("MessageList","$messageList")
                Log.d("MessageListContent","$content")
                if (content != null) {
                    NotificationUtils.showNotification(this@ChatActivity, "new message", content, "chatRooms")
                }
            }
            messageRecyclerView.scrollToPosition(messageList.size - 1)
        })
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