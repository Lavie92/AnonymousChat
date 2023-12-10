package com.example.doan_chuyennganh.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuInflater
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan_chuyennganh.LoginActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.authentication.toUser
import com.example.doan_chuyennganh.databinding.ActivityChatBinding
import com.example.doan_chuyennganh.encrypt.EncryptionUtils
import com.example.doan_chuyennganh.layout.SplashScreenActivity
import com.example.doan_chuyennganh.location.MyLocation
import com.example.doan_chuyennganh.notification.NotificationService
import com.example.doan_chuyennganh.report.Reports
import com.example.filterbadwodslibrary.filterBadwords
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ChatActivity : AppCompatActivity() {
    private lateinit var menuLayout: LinearLayout

    private var optionsVisible = false
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var usersRef: DatabaseReference
    private lateinit var chatRoomsRef: DatabaseReference
    private var chatRoomId: String = ""
    private var messageList: ArrayList<Message>? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var receiverId: String = ""
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val badwords = filterBadwords()
    private lateinit var popupMenu: PopupMenu
    private val handler = Handler()
    private var readyToFind = false
    private  lateinit var databaseReferences: DatabaseReference
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        checkSession()

        chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms")
        usersRef = FirebaseDatabase.getInstance().getReference("users")
        messageRecyclerView = binding.rcMessage
        messageBox = binding.messageBox
        sendButton = binding.ivSend
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList!!)
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        val btnEndChat: Button = binding.btnEndChat

        val btnShowOptions: FloatingActionButton = findViewById(R.id.btnShowOptions)
        btnShowOptions.setOnClickListener {
            toggleOptions()
        }

        binding.btnBack.setOnClickListener{
            val splashIntent = Intent(this@ChatActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toMain")
            startActivity(splashIntent)
        }


        checkChatRoomId()
        checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
            if (isChatRoomEnded) {
                readyToFind = false
                toggleFind()
            } else {
                readyToFind = true
                toggleFind()

            }
        }

        loadMessages(chatRoomId)


        binding.btnRandom.setOnClickListener{
            if(currentUserId!=null){
                updateUserStatus(currentUserId,true)
                isFindByLocation(currentUserId,false)
            }
            readyToFind = true
            toggleFind()
            showMessage("Đang tìm kiếm...")
            findRandomUserForChat()
        }
        btnEndChat.setOnClickListener {

            endChat(chatRoomId) { success ->
                if (success) {
                    readyToFind = false
                    toggleFind()
                } else {
                    Log.e("EndChat", "Failed to end chat.")
                }
            }
        }

        sendButton.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            if (messageText.isNotEmpty() && currentUserId != null && chatRoomId.isNotEmpty()) {
                sendMessage(chatRoomId, currentUserId, receiverId, messageText)
                messageBox.text.clear()
            }
        }
    }

    private fun toggleFind(){
        val slideUp = TranslateAnimation(0f, 0f, binding.btnRandom.height.toFloat(), 0f)
        val slideDown = TranslateAnimation(0f, 0f, 0f, binding.btnRandom.height.toFloat())
        slideDown.duration = 50
        slideUp.duration = 500
        if(!readyToFind){
            binding.btnRandom.startAnimation(slideUp)
            binding.btnRandom.visibility = View.VISIBLE
            checkChatRoomStatus(chatRoomId) { isChatRoomchatting ->
                if (!isChatRoomchatting) {
                    // If chatting, hide the "Tìm" button
                    binding.btnRandom.visibility = View.GONE
                }
            }
        }
        else{
            binding.btnRandom.startAnimation(slideDown)
            binding.btnRandom.visibility = View.GONE
        }
        readyToFind = !readyToFind


    }


    private fun toggleOptions() {
        val btnEndChat: Button = findViewById(R.id.btnEndChat)
        val btnheart: Button = findViewById(R.id.btnheart)
        val slideUp = TranslateAnimation(0f, 0f, btnheart.height.toFloat(), 0f)
        slideUp.duration = 500

        val slideDown = TranslateAnimation(0f, 0f, 0f, btnheart.height.toFloat())
        slideDown.duration = 50

        if (!optionsVisible) {
            btnEndChat.startAnimation(slideUp)
            btnheart.startAnimation((slideUp))
            btnheart.visibility = View.VISIBLE
            btnEndChat.visibility = View.VISIBLE
            checkChatRoomStatus(chatRoomId) { isChatRoomchatting ->
                if (!isChatRoomchatting) {
                }
            }
        } else {
            btnEndChat.startAnimation(slideDown)
            btnheart.startAnimation(slideDown)
            btnheart.visibility = View.GONE
            btnEndChat.visibility = View.GONE
        }

        optionsVisible = !optionsVisible
    }
    private val timeoutRunnable = object : Runnable {
        override fun run() {
            updateUserStatus(currentUserId.toString(), false)
            isFindByLocation(currentUserId.toString(), false)
                showMessage("No user found, please try again!")
                if (!readyToFind) {
                    toggleFind()
            }
            handler.removeCallbacks(this)
        }
    }



    private fun showMessage(message: String) {
        val systemMessage = Message(
            UUID.randomUUID().toString(),
            "system",
            currentUserId ?: "",
            message,
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
        handler.postDelayed(timeoutRunnable, 15000)
        usersRef.get().addOnSuccessListener { snapshot ->
            val allUsers = snapshot.children.map {
                it.getValue(User::class.java)!!
            }
                .filter { it.ready && it.id != currentUserId }
            if (allUsers.isNotEmpty()) {
                val randomUser = allUsers.random()
                if (randomUser.id != currentUserId) {
                    handler.removeCallbacks(timeoutRunnable)
                    receiverId = randomUser.id.toString()
                    chatRoomId =
                        currentUser?.toUser()?.let { createChatRoom(it, randomUser) }.toString()
                    sendMessage(
                        chatRoomId,
                        "system",
                        currentUserId.toString(),
                        "Bạn đã tham gia chat!!"
                    )
                    currentUserId?.let { updateUserStatus(it, false) }
                    receiverId?.let { updateUserStatus(it, false) }
                } else {
                }
            }
            else {
            }
        }
    }
    private fun deg2rad(deg: Double): Double {
        return deg * (Math.PI / 180)
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
        user1.chatRoom = chatRoomId
        user2.chatRoom = chatRoomId
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        chatRoomRef.child("user1Id").setValue(user1.id)
        chatRoomRef.child("user2Id").setValue(user2.id)
        chatRoomRef.child("status").setValue("chatting")

        val user1Reference = user1.id?.let { usersRef.child(it) }
        user1Reference?.child("chatRoom")?.setValue(chatRoomId)

        val user2Reference = user2.id?.let { usersRef.child(it) }
        user2Reference?.child("chatRoom")?.setValue(chatRoomId)
        return chatRoomId
    }

    private fun loadMessages(chatRoomId: String) {
        var filter : Boolean

        val messagesRef = chatRoomsRef.child(chatRoomId).child("messages")



        if(currentUserId != null) {
            usersRef.child(currentUserId!!).get().addOnSuccessListener {
                filter = it.child("filter").value as Boolean

                //loadMessage
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
                            val encryptedMessage =
                                msgSnapshot.child("content").getValue(String::class.java)
                            val encryptedKey =
                                msgSnapshot.child("encryptKey").getValue(String::class.java)
                            val timestamp =
                                msgSnapshot.child("timestamp").getValue(Long::class.java)

                            // Decrypt the message using the key
                            var decryptedMessage =
                                encryptedMessage?.let {
                                    encryptedKey?.let { it1 ->
                                        EncryptionUtils.decrypt(
                                            it,
                                            EncryptionUtils.getKeyFromString(it1)
                                        )
                                    }
                                }
                            var message123 = decryptedMessage
                            if (filter == true) {
                                message123 = badwords.filterBadWords(decryptedMessage)
                            }

                            // Create a new Message object with the decrypted content
                            messageId?.let {
                                senderId?.let { it1 ->
                                    receiverId?.let { it2 ->
                                        message123?.let { it3 ->
                                            timestamp?.let { it4 ->
                                                Message(
                                                    it,
                                                    it1,
                                                    it2,
                                                    it3,
                                                    encryptedKey ?: "",
                                                    it4
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        messageList?.clear()
                        newMessages?.let { messageList?.addAll(it) }

                        messageAdapter.notifyDataSetChanged()
                        messageRecyclerView.scrollToPosition(messageList!!.size - 1)

                        if (newMessages.isNotEmpty()) {
                            checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
                                if (isChatRoomEnded) {
                                    readyToFind = false
                                    toggleFind()
                                } else {
                                    readyToFind = true
                                    toggleFind()
                                }
                            }
                            val lastMessage = newMessages.last()
                            if (lastMessage.senderId != currentUserId) {
                                lastMessage.content?.let { showNotification("New Message", it) }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("loadMessages", "Error loading messages: ${error.message}")
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
        chatRoomId: String,
        senderId: String,
        receiverId: String,
        text: String
    ) {
        val secretKey = EncryptionUtils.generateKey()
        val encryptedMessage = EncryptionUtils.encrypt(text, secretKey)
        val encryptedKey = EncryptionUtils.getKeyAsString(secretKey)

        checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
            if (!isChatRoomEnded) {
                val timestamp = System.currentTimeMillis()
                val messageId = UUID.randomUUID().toString()
                val message = Message(messageId, senderId, receiverId, encryptedMessage, encryptedKey, timestamp)
                chatRoomsRef.child(chatRoomId).child("messages")
                    .push().setValue(message)
            } else {
                Toast.makeText(this, "Bạn cần tìm người chat trước!", Toast.LENGTH_SHORT).show()
                Log.d("SendMessage", "Cannot send message, ChatRoom has ended.")
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
                Log.e("CheckChatRoomStatus", "Error checking ChatRoom status: ${error.message}")
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

            userRef.child("chatRoom").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatRoomValue = snapshot.getValue(String::class.java).toString()
                    if (chatRoomValue != null) {
                        checkUsersInChatRoom(chatRoomValue)
                        chatRoomId = chatRoomValue
                        loadMessages(chatRoomId)
                    } else {
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatRoomValue", "Error getting chatRoom value: ${error.message}")
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
                    receiverId = if (user1Id == currentUserId) ({
                        user2Id
                    }).toString() else user1Id.toString()

                Log.d("receiver", "User2Id: $receiverId")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UsersInChatRoom", "Error checking users in chatRoom: ${error.message}")
            }
        })
    }

    private fun endChat(chatRoomId: String, callback: (Boolean) -> Unit) {
        sendMessage(
            this.chatRoomId,
            "system",
            currentUserId.toString(),
            "Cuộc trò chuyện đã kết thúc!"
        )
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        chatRoomRef.child("status").setValue("ended")
            .addOnCompleteListener { task ->
                callback.invoke(task.isSuccessful)
            }
    }

    private fun removeMessage(chatRoomId: String) {
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        chatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersRef.child(currentUserId.toString()).child("chatRoom").setValue("")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EndChat", "Error getting chatRoom info: ${error.message}")
            }
        })
    }

    fun reportMessage(message: Message) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val receiverUserId = currentUserId
        val senderUserId = message.senderId

        Log.d("reportMessage", "currentUserId: $currentUserId, senderUserId: $senderUserId, receiverUserId: $receiverUserId")


        // Extract only the necessary properties from the Message object
        val messageMap = mapOf(
            "timestamp" to message.timestamp,
            "content" to message.content
        )

        // Set UID_beReported to the ID of the user being reported
        val report = Reports(senderUserId!!, receiverUserId!!)

        val reportRef = FirebaseDatabase.getInstance().getReference("reports").child(chatRoomId).child(message.messageId!!)
        reportRef.setValue(report)

        // Push the messageMap to a different child node under the messageID
        reportRef.child("status").setValue("doing")
        reportRef.child("messageMap").setValue(messageMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Report added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error adding report: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.e("reportMessage", "Error adding report: ${it.message}")
            }
    }
    //Sessions check
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
                        if(sessionId != currentSessionID){
                            showConfirmationDialog()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
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
        // Tạo Intent để chuyển hướng đến LoginActivity và xóa toàn bộ Activity đã mở trước đó
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // Kết thúc Activity hiện tại
        finish()
    }

}
