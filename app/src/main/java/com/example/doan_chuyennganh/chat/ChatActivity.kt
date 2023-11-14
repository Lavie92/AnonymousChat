package com.example.doan_chuyennganh.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
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
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms")
        usersRef = FirebaseDatabase.getInstance().getReference("users")
        val senderId = FirebaseAuth.getInstance().currentUser?.uid

        messageRecyclerView = binding.rcMessage
        messageBox = binding.messageBox
        sendButton = binding.ivSend
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList!!)
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this)

        val startChat: TextView = binding.tvStartChat
        startChat.setOnClickListener {
            if (currentUserId != null) {
                updateUserStatus(currentUserId, true)
            }
            findRandomUserForChat()
        }
        sendButton.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            if (messageText.isNotEmpty() && currentUserId != null && chatRoomId.isNotEmpty()) {
                sendMessage(chatRoomId, currentUserId, messageText)
                messageBox.text.clear()
            }
        }
    }

    private fun findRandomUserForChat() {
        usersRef.get().addOnSuccessListener { snapshot ->
            val allUsers = snapshot.children.map { it.getValue(User::class.java)!! }
                .filter { it.ready }
            Log.d("findRandomUserForChat", "Number of users: ${allUsers.size}")

            if (allUsers.isNotEmpty()) {
                val randomUser = allUsers.random()
                if (randomUser.id != currentUserId) {
                    Toast.makeText(this, "welcome ${randomUser.username}", Toast.LENGTH_SHORT).show()
                    val chatRoomId = randomUser.id?.let {
                        if (currentUserId != null) {
                            createChatRoom(currentUser, randomUser)
                            updateUserStatus(currentUserId, false)
                        }
                    }
                    Log.d("chatRoomId", "Id: $chatRoomId")

                } else {
                    findRandomUserForChat()
                }
            } else {
                Toast.makeText(this, "not found!!", Toast.LENGTH_SHORT).show()

            }
        }

    }
    private fun updateUserStatus(userId: String, ready: Boolean) {
        val userRef = usersRef.child(userId)
        userRef.child("ready").setValue(ready)
    }

    private fun createChatRoom(user1: FirebaseUser?, user2: User): String {

        val roomId = UUID.randomUUID().toString()

        val roomRef = chatRoomsRef.child(roomId)

        if (user1 != null) {
            roomRef.child("user1Id").setValue(user1.uid)
        }
        roomRef.child("user2Id").setValue(user2.id)

        roomRef.child("messages").setValue(null)
        val welcomeMessage = Message("Welcome to the chat!", "system")
        roomRef.push().setValue(welcomeMessage)
        return roomId

    }

    private fun loadMessages(chatRoomId: String) {
        val messagesRef = chatRoomsRef.child(chatRoomId).child("messages")

        messagesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    messageList?.add(message)
                    messageAdapter.notifyDataSetChanged()
                    messageRecyclerView.scrollToPosition(messageList!!.size - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            // Other overridden methods of ChildEventListener

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Failed to load messages: ${error.message}")
            }
        })
    }
    private fun sendMessage(chatRoomId: String, senderId: String, text: String) {
        val messagesRef = chatRoomsRef.child(chatRoomId).child("messages")
        val messageId = messagesRef.push().key
        val timestamp = System.currentTimeMillis()

        if (messageId != null) {
            val message = Message(messageId, senderId, text, timestamp)
            messagesRef.child(messageId).setValue(message)
            val welcomeMessage = Message("Welcome to the chat!", "system")
            messagesRef.push().setValue(welcomeMessage)
        }
    }

}