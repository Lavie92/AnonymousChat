package com.example.doan_chuyennganh.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.authentication.toUser
import com.example.doan_chuyennganh.databinding.ActivityChatBinding
import com.example.doan_chuyennganh.encrypt.EncryptionUtils
import com.example.doan_chuyennganh.location.MyLocation
import com.example.doan_chuyennganh.notification.NotificationService
import com.example.filterbadwodslibrary.filterBadwords
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class ChatActivity : AppCompatActivity() {

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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms")
        usersRef = FirebaseDatabase.getInstance().getReference("users")
        messageRecyclerView = binding.rcMessage
        messageBox = binding.messageBox
        sendButton = binding.ivSend
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList!!)
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        val btnStartChat: Button = binding.btnStartChat
        val btnEndChat: Button = binding.btnEndChat

        checkChatRoomId()
        loadMessages(chatRoomId)

        btnStartChat.setOnClickListener {
            findRandomUserForChat()
        }

        btnEndChat.setOnClickListener {
            val lastChatRoom = chatRoomId
            endChat(chatRoomId) { success ->
                if (success) {
                } else {
                    // Handle the case where ending the chat was not successful
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

    private fun loadMessagesForCurrentUser(chatRoomId: String) {
        // Load messages for the current user
        loadMessages(chatRoomId)
    }

    private fun loadMessagesForOtherUser(chatRoomId: String) {
        val otherUserId = if (currentUserId == receiverId) currentUserId else receiverId
        val otherUserRef = usersRef.child(otherUserId)

        otherUserRef.child("chatRoom").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val otherChatRoom = snapshot.getValue(String::class.java)
                if (otherChatRoom != null) {
                    loadMessages(otherChatRoom)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "LoadMessagesForOtherUser",
                    "Error loading messages for the other user: ${error.message}"
                )
            }
        })
    }

    private fun findRandomUserForChat() {
        chatRoomId = ""
        receiverId = ""
        checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
            if (isChatRoomEnded) {
                removeMessage(chatRoomId)
            } else {
                Log.d("SendMessage", "Cannot send message, ChatRoom has ended.")
            }
        }
        Toast.makeText(this, "Dang tim kiem", Toast.LENGTH_SHORT).show()
        if (currentUserId != null) {
            updateUserStatus(currentUserId, true)
        }
        usersRef.get().addOnSuccessListener { snapshot ->

            val allUsers = snapshot.children.map {
                it.getValue(User::class.java)!!
            }
                .filter { it.ready }
            if (allUsers.size > 1) {
                val randomUser = allUsers.random()
                if (randomUser.id != currentUserId) {
                    Toast.makeText(this, "Welcome ${randomUser.username}", Toast.LENGTH_SHORT)
                        .show()
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
                    findRandomUserForChat()
                }
            }
        }
    }

    private fun updateUserStatus(userId: String, ready: Boolean) {
        val userRef = usersRef.child(userId)
        userRef.child("ready").setValue(ready)
    }


    private fun createChatRoom(user1: User, user2: User): String {
        chatRoomId = UUID.randomUUID().toString()
        user1.chatRoom = chatRoomId
        user2.chatRoom = chatRoomId

        user1.location = getCurrentUserLocation()
        user2.location = getCurrentUserLocation()

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

    private fun getCurrentUserLocation(): MyLocation? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        var userLocation = MyLocation()
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation =
                            MyLocation(latitude = location.latitude, longitude = location.longitude)
                        updateUserLocation(userLocation)
                    } else {
                    }
                }
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
        return userLocation
    }

    private fun updateUserLocation(location: MyLocation) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(it.uid)
            userRef.child("location").setValue(location)
        }
    }


    private fun loadMessages(chatRoomId: String) {
        val messagesRef = chatRoomsRef.child(chatRoomId).child("messages")

        messagesRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val newMessages = snapshot.children.mapNotNull { msgSnapshot ->
                    val messageId = msgSnapshot.child("messageId").getValue(String::class.java)
                    val senderId = msgSnapshot.child("senderId").getValue(String::class.java)
                    val receiverId = msgSnapshot.child("receiverId").getValue(String::class.java)
                    val encryptedMessage = msgSnapshot.child("content").getValue(String::class.java)
                    val encryptedKey = msgSnapshot.child("encryptKey").getValue(String::class.java)
                    val timestamp = msgSnapshot.child("timestamp").getValue(Long::class.java)

                    // Decrypt the message using the key
                    val decryptedMessage =
                        encryptedMessage?.let {
                            encryptedKey?.let { it1 ->
                                EncryptionUtils.decrypt(it, EncryptionUtils.getKeyFromString(it1))
                            }
                        }
                    val filterWords = badwords.filterBadWords(decryptedMessage)

                    // Create a new Message object with the decrypted content
                    messageId?.let {
                        senderId?.let { it1 ->
                            receiverId?.let { it2 ->
                                filterWords?.let { it3 ->
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
                        Log.d("ChatRoomValue", "Chat Room: $chatRoomValue")
                    } else {
                        Log.d("ChatRoomValue", "Chat Room is null")
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
                val user1Id = snapshot.child("user1Id").getValue(String::class.java)
                val user2Id = snapshot.child("user2Id").getValue(String::class.java)
                val otherUserId = if (user1Id == currentUserId) user2Id else user1Id
                usersRef.child(otherUserId.toString()).child("chatRoom").setValue("")
                usersRef.child(currentUserId.toString()).child("chatRoom").setValue("")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EndChat", "Error getting chatRoom info: ${error.message}")
            }
        })
    }

}
