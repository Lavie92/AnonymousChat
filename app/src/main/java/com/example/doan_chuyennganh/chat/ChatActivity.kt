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
    private var randomUserId: String = ""
    private val currentUser = FirebaseAuth.getInstance().currentUser

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

        if (currentUserId != null) {
            loadMessages(chatRoomId, currentUserId, randomUserId)
        }

        val startChat: TextView = binding.tvStartChat
        startChat.setOnClickListener {

            findRandomUserForChat()
        }
        sendButton.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            if (messageText.isNotEmpty() && currentUserId != null && chatRoomId.isNotEmpty()) {
                sendMessage(chatRoomId, currentUserId, messageText)
                messageBox.text.clear()
                if (currentUserId != null) {
                    chatRoomsRef.child(chatRoomId).child(currentUserId).child("messages")
                        .addValueEventListener(object: ValueEventListener {
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
                    Toast.makeText(this, "Welcome ${randomUser.username}", Toast.LENGTH_SHORT).show()
                    randomUserId = randomUser.id.toString()
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

        if (user1 != null) {
            roomRef.child(user1.uid).setValue(user1.uid)
        }
        user2.id?.let { roomRef.child(it).setValue(user2.id) }
        return roomId
    }

    private fun generateRoomId(userId1: String?, userId2: String?): String {
        // Sắp xếp ID để đảm bảo tính duy nhất không phụ thuộc vào thứ tự
        val sortedIds = listOfNotNull(userId1, userId2).sorted()
        return "${sortedIds[0]}_${sortedIds[1]}"
    }

    private fun loadMessages(chatRoomId: String, senderId: String, recipientId: String) {
        val messagesRef = chatRoomsRef.child(chatRoomId).child(senderId).child("messages")

        messagesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val newMessages = snapshot.children.mapNotNull {
                    try {
                        it.getValue(Message::class.java)
                    } catch (e: DatabaseException) {
                        null
                    }
                }
                Log.e("sizeList", "Failed to load messages: ${messageList!!.size}")

                messageList?.clear()
                messageList?.addAll(newMessages)

                messageAdapter.notifyDataSetChanged()
                messageRecyclerView.scrollToPosition(messageList!!.size - 1)
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

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun sendMessage(chatRoomId: String, senderId: String, text: String) {
        val timestamp = System.currentTimeMillis()
        val messageId = UUID.randomUUID().toString()

        val recipientId = if(senderId == currentUserId) randomUserId else currentUserId

        val message = Message(messageId, senderId, text, timestamp)

        if (recipientId != null) {
            chatRoomsRef.child(chatRoomId).child(senderId).child("messages")
                .push().setValue(message).addOnSuccessListener {
                    chatRoomsRef.child(chatRoomId).child(recipientId).child("messages")
                        .push().setValue(message)
                }
        }
    }
}
