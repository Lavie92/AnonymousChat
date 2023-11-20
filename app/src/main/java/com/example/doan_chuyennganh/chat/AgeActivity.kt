package com.example.doan_chuyennganh.chat

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.authentication.toUser
import com.example.doan_chuyennganh.databinding.ActivityAgeBinding

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class AgeActivity : AppCompatActivity() {

    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var binding: ActivityAgeBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var usersRef: DatabaseReference
    private lateinit var chatRoomsRef: DatabaseReference
    private var chatRoomId: String = ""
    private var messageList: ArrayList<Message>? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var receiverId: String = ""
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgeBinding.inflate(layoutInflater)
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
        checkChatRoomStatus()
        val btnStartChat: Button = binding.btnStartChat
        val btnEndChat: Button = binding.btnEndChat
        loadMessages(chatRoomId)
        btnStartChat.setOnClickListener {
            findRandomUserForChat()
        }
        btnEndChat.setOnClickListener {
            endChat(chatRoomId)
        }
        sendButton.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            if (messageText.isNotEmpty() && currentUserId != null && chatRoomId.isNotEmpty()) {
                sendMessage(chatRoomId, currentUserId, receiverId, messageText)
                messageBox.text.clear()
            }
        }
    }

    private fun findRandomUserForChat() {
        chatRoomId = ""
        receiverId = ""
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
        chatRoomId = generateRoomId(user1.id, user2.id)
        user1.chatRoom = chatRoomId
        user2.chatRoom = chatRoomId

        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        chatRoomRef.child("user1Id").setValue(user1.id)
        chatRoomRef.child("user2Id").setValue(user2.id)

        val user1Reference = user1.id?.let { usersRef.child(it) }
        user1Reference?.child("chatRoom")?.setValue(chatRoomId)

        val user2Reference = user2.id?.let { usersRef.child(it) }
        user2Reference?.child("chatRoom")?.setValue(chatRoomId)
        return chatRoomId
    }

    private fun generateRoomId(userId1: String?, userId2: String?): String {
        val sortedIds = listOfNotNull(userId1, userId2).sorted()
        return "${sortedIds[0]}_${sortedIds[1]}"
    }

    private fun loadMessages(chatRoomId: String) {
        val messagesRef = chatRoomsRef.child(chatRoomId).child("messages")

        messagesRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val newMessages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                Log.d("loadMessages", "newMessages: $newMessages")
                messageList?.clear()
                messageList?.addAll(newMessages)

                messageAdapter.notifyDataSetChanged()
                Log.d("loadMessages", "Adapter updated with new messages")
                messageRecyclerView.scrollToPosition(messageList!!.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("loadMessages", "Error loading messages: ${error.message}")
            }
        })
    }

    private fun sendMessage(
        chatRoomId: String,
        senderId: String,
        receiverId: String,
        text: String
    ) {
        val timestamp = System.currentTimeMillis()
        val messageId = UUID.randomUUID().toString()
        val message = Message(messageId, senderId, receiverId, text, timestamp)

        chatRoomsRef.child(chatRoomId).child("messages")
            .push().setValue(message)
    }

    private fun checkChatRoomStatus() {
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
//        chatRoomsRef.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                for (roomSnapshot in snapshot.children) {
//                    val roomId = roomSnapshot.key
//                    val userId1 = roomSnapshot.child("user1Id").value as? String
//                    val userId2 = roomSnapshot.child("user2Id").value as? String
//
//                    if (userId1 != null && userId2 != null) {
//                        Log.d("userId", "User is in room: $userId1")
//                        Log.d("UserId", "User is in room: $userId2")
//                        receiverId = if (userId1 == currentUserId) userId2 else userId1
//                        Log.d("userId", "User is in room: $userId1")
//                        Log.d("UserId", "User is in room: $userId2")
//                    }
//                    Log.d("userId", "User is in room: $userId1")
//                    Log.d("UserId", "User is in room: $userId2")
//                    val messages = roomSnapshot.child("messages").children
//                    for (messageSnapshot in messages) {
//                        val senderId =
//                            messageSnapshot.child("senderId").getValue(String::class.java)
//                        val receiverUser =
//                            messageSnapshot.child("receiverId").getValue(String::class.java)
//                        if (currentUserId == senderId || currentUserId == receiverId) {
//                            val roomName = roomId ?: ""
//                            loadMessages(roomName)
//                            chatRoomId = roomName
//                            Log.d("room id", "User is in room: $chatRoomId")
//                            return
//                        }
//                    }
//                    Log.d("room id", "User is in room: $chatRoomId")
//                    Log.d("userId", "User is in room: $userId1")
//                    Log.d("UserId", "User is in room: $userId2")
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("ChatRoomStatus", "Error checking chat room status: ${error.message}")
//            }
//        })
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
    private fun endChat(chatRoomId: String) {
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
        chatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user1Id = snapshot.child("user1Id").getValue(String::class.java)
                val user2Id = snapshot.child("user2Id").getValue(String::class.java)
                val otherUserId = if (user1Id == currentUserId) user2Id else user1Id
                usersRef.child(otherUserId.toString()).child("chatRoom").setValue("")
                usersRef.child(currentUserId.toString()).child("chatRoom").setValue("")
                val messagesRef = chatRoomsRef.child(chatRoomId).child("messages")
                messagesRef.removeValue()
                val roomRef = chatRoomsRef.child(chatRoomId)
                roomRef.removeValue()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EndChat", "Error getting chatRoom info: ${error.message}")
            }
        })
    }

}
