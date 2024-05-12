package com.example.anonymousChat.service

import android.widget.Toast
import com.example.anonymousChat.model.Message
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class ChatService(private val chatType: String) : IChatService {
    override fun sendMessage(
        chatRoomId: String,
        senderId: String,
        receiverId: String,
        content: String
    ) {
        val chatRoomsRef = FirebaseDatabase.getInstance().getReference(chatType)
        checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
            if (!isChatRoomEnded) {
                val timestamp = System.currentTimeMillis()
                val messageId = UUID.randomUUID().toString()
                val message = Message(
                    messageId,
                    senderId,
                    receiverId,
                    content,
                    "text",
                    timestamp
                )
                chatRoomsRef.child(chatRoomId).child("messages").push().setValue(message)
            }
        }
    }

    override fun loadMessage(
        chatRoomId: String,
        onSuccess: (data: List<Message>) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val chatRoomsRef = FirebaseDatabase.getInstance().getReference(chatType)
        val messagesRef = chatRoomsRef.child(chatRoomId).child("messages")
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                for (msgSnapshot in snapshot.children) {
                    val messageId = msgSnapshot.child("messageId").getValue(String::class.java)
                    val senderId = msgSnapshot.child("senderId").getValue(String::class.java)
                    val receiverId = msgSnapshot.child("receiverId").getValue(String::class.java)
                    val content = msgSnapshot.child("content").getValue(String::class.java)
                    val type = msgSnapshot.child("type").getValue(String::class.java)
                    val timestamp = msgSnapshot.child("timestamp").getValue(Long::class.java)

                    messageId?.let {
                        senderId?.let { sid ->
                            receiverId?.let { rid ->
                                content?.let { msg ->
                                    timestamp?.let { time ->
                                        type?.let { it1 ->
                                            val message = Message(it, sid, rid, msg, it1, time)
                                            messages.add(message)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                onSuccess(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }


    private fun checkChatRoomStatus(chatRoomId: String, callback: (Boolean) -> Unit) {
        val chatRoomsRef = FirebaseDatabase.getInstance().getReference(chatType)
        chatRoomsRef.child("status").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                callback.invoke(status == "ended")
            }
            override fun onCancelled(error: DatabaseError) {
                callback.invoke(false)
            }
        })
    }
}