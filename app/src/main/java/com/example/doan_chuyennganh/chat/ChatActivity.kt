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
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID
import kotlin.math.log

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms")
        usersRef = FirebaseDatabase.getInstance().getReference("users")
        Log.d("room id", "User is in room: $chatRoomId")

        messageRecyclerView = binding.rcMessage
        messageBox = binding.messageBox
        sendButton = binding.ivSend
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList!!)
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        checkChatRoomStatus()
        val startChat: TextView = binding.tvStartChat
        loadMessages(chatRoomId, currentUserId.toString(), receiverId)
        startChat.setOnClickListener {

            findRandomUserForChat()
        }
        sendButton.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            if (messageText.isNotEmpty() && currentUserId != null && chatRoomId.isNotEmpty()) {
                sendMessage(chatRoomId, currentUserId, receiverId, messageText)
                messageBox.text.clear()
                if (currentUserId != null) {
                    chatRoomsRef.child(chatRoomId).child(currentUserId).child("messages")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                Log.d("chatRoomId", "Id: $chatRoomId")

                                messageList!!.clear()
                                for (postSnapshot in snapshot.children) {
                                    val message = postSnapshot.getValue(Message::class.java)
                                    if (message != null) {
                                        messageList?.add(message)
                                        Log.d("messList", "Id: ${messageList!!.size}")

                                    }
                                    messageAdapter.notifyDataSetChanged()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }

                        })
                }
            }
        }
    }

    private fun findRandomUserForChat() {
        Toast.makeText(this, "Dang tim kiem", Toast.LENGTH_SHORT).show()
        if (currentUserId != null) {
            updateUserStatus(currentUserId, true)
        }
        usersRef.get().addOnSuccessListener { snapshot ->
            val allUsers = snapshot.children.map { it.getValue(User::class.java)!! }
                .filter { it.ready }
            if (allUsers.size > 1) {
                val randomUser = allUsers.random()
                if (randomUser.id != currentUserId) {
                    Toast.makeText(this, "Welcome ${randomUser.username}", Toast.LENGTH_SHORT)
                        .show()
                    receiverId = randomUser.id.toString()
                    chatRoomId = createChatRoom(currentUser, randomUser)
                    currentUserId?.let { updateUserStatus(it, false) }
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


    private fun createChatRoom(user1: FirebaseUser?, user2: User): String {
        val roomId = generateRoomId(user1?.uid, user2.id)
        val roomRef = chatRoomsRef.child(roomId)
        return roomId
    }

    private fun generateRoomId(userId1: String?, userId2: String?): String {
        // Sắp xếp ID để đảm bảo tính duy nhất không phụ thuộc vào thứ tự
        val sortedIds = listOfNotNull(userId1, userId2).sorted()
        return "${sortedIds[0]}_${sortedIds[1]}"
    }

    private fun loadMessages(chatRoomId: String, currentUserId: String, randomUserId: String) {
        val messagesRef = chatRoomsRef.child(chatRoomId).child("messages")

        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newMessages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                Log.d("loadMessages", "newMessages: $newMessages")
                messageList?.clear()
                messageList?.addAll(newMessages)

                messageAdapter.notifyDataSetChanged()
                Log.d("loadMessages", "Adapter updated with new messages")
                messageRecyclerView.scrollToPosition(messageList!!.size - 1)

                if (messageList?.isNotEmpty() == true) {
                    val lastMessageSenderId = messageList!![messageList!!.size - 1].senderId
                    val recipientId = if (lastMessageSenderId == currentUserId) randomUserId else currentUserId
                    // Perform any additional actions based on recipientId if needed
                }
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
        chatRoomsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (roomSnapshot in snapshot.children) {
                    val roomId = roomSnapshot.key
                    val messages = roomSnapshot.child("messages").children

                    for (messageSnapshot in messages) {
                        val senderId =
                            messageSnapshot.child("senderId").getValue(String::class.java)
                        var receiverUser =
                            messageSnapshot.child("receiverId").getValue(String::class.java)

                        if (currentUserId == senderId || currentUserId == receiverId) {
                            var roomName = roomId ?: ""
                            Log.d("ChatRoomStatus", "User is in room: $roomName")
                            loadMessages(roomName, currentUserId!!, receiverId)
                            chatRoomId = roomName
                            receiverId = receiverUser.toString()
                            Log.d("room id", "User is in room: $chatRoomId")
                            return
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRoomStatus", "Error checking chat room status: ${error.message}")
            }
        })
    }

}
