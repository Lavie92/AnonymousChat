package com.example.doan_chuyennganh.chat

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
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
import com.example.doan_chuyennganh.LoginActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.adapter.MessageAdapter
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.authentication.toUser
import com.example.doan_chuyennganh.databinding.ActivityChatBinding
import com.example.doan_chuyennganh.encrypt.EncryptionUtils
import com.example.doan_chuyennganh.layout.SplashScreenActivity
import com.example.doan_chuyennganh.notification.NotificationService
import com.example.doan_chuyennganh.report.Reports
import com.example.filterbadwodslibrary.filterBadwords
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID
class ChatActivity : AppCompatActivity(), MessageHandler {
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
    private lateinit var databaseReferences: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var btnHeart: Button
    private lateinit var ivSendImage: ImageView
    private lateinit var btnRandom: Button
    private lateinit var imageUtils: ImageUtils

    companion object {
        const val REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        checkSession()
        imageUtils = ImageUtils(this)
        chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms")
        usersRef = FirebaseDatabase.getInstance().getReference("users")
        messageRecyclerView = binding.rcMessage
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

        val btnShowOptions: FloatingActionButton = findViewById(R.id.btnShowOptions)
        btnShowOptions.setOnClickListener {
            toggleOptions()
        }

        binding.btnBack.setOnClickListener {
            val splashIntent = Intent(this@ChatActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toMain")
            startActivity(splashIntent)
        }
        checkChatRoomId()
        btnHeart.setOnClickListener {
            sendMessage(
                chatRoomId,
                "system",
                currentUserId.toString(),
                "Bạn đã nhấn yêu thích, nếu đối phương đồng ý thì bạn sẽ chia sẻ thông tin (tuổi, giới tính, username)"
            )
            sendMessage(
                chatRoomId,
                "system",
                receiverId,
                "Đối phương thích bạn, nếu bạn cũng vậy hãy nhấn tim để chia sẻ thông tin gồm (username, tuổi, giới tính)"
            )
            shareMoreInformation()
        }
        btnRandom.setOnClickListener {
            findRandomUserForChat()
        }
        btnEndChat.setOnClickListener {
            updateUserStatus(currentUserId.toString(), false)
            if (receiverId.isNotEmpty()) {
                updateUserStatus(receiverId, false)
            }
            endChat(chatRoomId) { success ->
                if (success) {
                }
            }
        }

        btnSend.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            if (messageText.isNotEmpty() && currentUserId != null && chatRoomId.isNotEmpty()) {
                sendMessage(chatRoomId, currentUserId, receiverId, messageText)
                messageBox.text.clear()
            }
        }
        ivSendImage.setOnClickListener {
            checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
                if (!isChatRoomEnded) {
                    imageUtils.showImagePickerDialog()
                } else {
                    Toast.makeText(this, "Bạn cần tìm người chat trước!", Toast.LENGTH_SHORT).show()
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

    private fun shareMoreInformation() {
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        val heartRef = chatRoomRef.child("heart")
        heartRef.child(currentUserId!!).setValue(true)
        heartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updatePoints(receiverId)
                if (snapshot.childrenCount.toInt() == 2) {
                    sendMessage(chatRoomId, "system", currentUserId, "Thông tin đã được chia sẻ.")
                    sendMessage(chatRoomId, "system", receiverId, "Thông tin đã được chia sẻ.")
                    shareUserInfo(currentUserId, receiverId)
                    shareUserInfo(receiverId, currentUserId)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    override fun updatePoints(userId: String) {
        super<MessageHandler>.updatePoints(userId)
    }

    private fun shareUserInfo(shareFromUserId: String, shareToUserId: String) {
        usersRef.child(shareFromUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                val gender = snapshot.child("gender").getValue(String::class.java) ?: "Unknown"
                val age = snapshot.child("age").getValue(String::class.java) ?: "Unknown"
                val info =
                    " Thông tin của người ấy: Username: $username, Giới tính: $gender, Tuổi: $age"
                sendMessage(chatRoomId, "system", shareToUserId, info)
            }

            override fun onCancelled(databaseError: DatabaseError) {
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
            checkChatRoomStatus(chatRoomId) { isChatRoomchatting ->
                if (!isChatRoomchatting) {
                }
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
            "",
            System.currentTimeMillis()
        )

        messageList?.add(systemMessage)
        messageAdapter.notifyDataSetChanged()

        messageRecyclerView.scrollToPosition(messageList!!.size - 1)
    }

    private fun findRandomUserForChat() {
        chatRoomId = ""
        receiverId = ""
        checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
            if (isChatRoomEnded) {
                removeMessage(chatRoomId)
            }
        }
        if (currentUserId != null) {
            updateUserStatus(currentUserId, true)
        }
        showMessage("Đang tìm kiếm...")
        handler.postDelayed(timeoutRunnable, 15000)

        usersRef.get().addOnSuccessListener { snapshot ->
            val allUsers = snapshot.children.map {
                it.getValue(User::class.java)!!
            }.filter { it.ready && it.id != currentUserId }

            if (allUsers.isNotEmpty()) {
                handler.removeCallbacks(timeoutRunnable)
                val randomUser = allUsers.random()
                receiverId = randomUser.id.toString()
                chatRoomId =
                    currentUser?.toUser()?.let { createChatRoom(it, randomUser) }.toString()
                sendInitialMessages(chatRoomId, currentUserId.toString(), receiverId)
                currentUserId?.let { updateUserStatus(it, false) }
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

    private fun loadMessages(chatRoomId: String) {
        var filter: Boolean
        val messagesRef = chatRoomsRef.child(chatRoomId).child("messages")
        if (currentUserId != null) {
            usersRef.child(currentUserId!!).get().addOnSuccessListener {
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
                                val encryptedMessage =
                                    msgSnapshot.child("content").getValue(String::class.java)
                                val type = msgSnapshot.child("type").getValue(String::class.java)
                                val encryptedKey =
                                    msgSnapshot.child("encryptKey").getValue(String::class.java)
                                val timestamp =
                                    msgSnapshot.child("timestamp").getValue(Long::class.java)
                                var decryptedMessage = encryptedMessage?.let {
                                    encryptedKey?.let { key ->
                                        EncryptionUtils.decrypt(
                                            it, EncryptionUtils.getKeyFromString(key)
                                        )
                                    }
                                }
                                if (filter) {
                                    decryptedMessage = badwords.filterBadWords(decryptedMessage)
                                }

                                messageId?.let {
                                    senderId?.let { sid ->
                                        receiverId?.let { rid ->
                                            decryptedMessage?.let { msg ->
                                                timestamp?.let { time ->
                                                    type?.let { it1 ->
                                                        Message(
                                                            it,
                                                            sid,
                                                            rid,
                                                            msg,
                                                            it1,
                                                            encryptedKey ?: "",
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
                            if (lastMessage.type!! == "image") {
                                content = "Người lạ đã gửi 1 hình ảnh"
                            } else {
                                content = lastMessage.content!!
                            }
                            if (lastMessage.senderId != currentUserId) {
                                lastMessage.content?.let { showNotification("Chat ngẫu nhiên", content) }
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
        notificationService.showNotification(this, title, content)
    }

    private fun sendMessage(
        chatRoomId: String, senderId: String, receiverId: String, text: String
    ) {
        val secretKey = EncryptionUtils.generateKey()
        val encryptedMessage = EncryptionUtils.encrypt(text, secretKey)
        val encryptedKey = EncryptionUtils.getKeyAsString(secretKey)

        checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
            if (!isChatRoomEnded) {
                val timestamp = System.currentTimeMillis()
                val messageId = UUID.randomUUID().toString()
                val message = Message(
                    messageId,
                    senderId,
                    receiverId,
                    encryptedMessage,
                    "text",
                    encryptedKey,
                    timestamp
                )
                chatRoomsRef.child(chatRoomId).child("messages").push().setValue(message)
            } else {
                Toast.makeText(this, "Bạn cần tìm người chat trước!", Toast.LENGTH_SHORT).show()
            }
        }
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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
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
                if (user1Id != "" && user2Id != "" && !user1Id.isNullOrEmpty() && !user2Id.isNullOrEmpty()) receiverId =
                    if (user1Id == currentUserId) user2Id else user1Id
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun endChat(chatRoomId: String, callback: (Boolean) -> Unit) {
        sendMessage(
            this.chatRoomId, "system", currentUserId.toString(), "Cuộc trò chuyện đã kết thúc!"
        )
        sendMessage(
            this.chatRoomId, "system", receiverId, "Cuộc trò chuyện đã kết thúc!"
        )
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        chatRoomRef.child("status").setValue("ended").addOnCompleteListener { task ->
            callback.invoke(task.isSuccessful)
        }
    }

    private fun removeMessage(chatRoomId: String) {
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        chatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersRef.child(currentUserId.toString()).child("randomChatRoom").setValue("")
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

    private fun getSessionId(): String? {
        val sharedPref = getSharedPreferences("PreSession2", Context.MODE_PRIVATE)
        return sharedPref.getString("sessionID2", null)
    }

    private fun checkSession() {
        val sessionId = getSessionId()
        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        val user = auth.currentUser
        user?.let {
            databaseReferences.child(it.uid).child("session")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentSessionID = snapshot.value as String?
                        if (sessionId != currentSessionID) {
                            showConfirmationDialog()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                })
        }
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Thông báo!")
        builder.setMessage("Tài khoản này đang được đăng nhập ở thiết bị khác, vui lòng đăng nhập lại!")

        builder.setPositiveButton("OK") { _: DialogInterface, _: Int ->
            signOutAndStartSignInActivity()
            handleLogout()
            finish()
        }
        builder.show()
    }

    private fun signOutAndStartSignInActivity() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun handleLogout() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}