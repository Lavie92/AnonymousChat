package com.example.anonymousChat.chat

import android.app.Activity
import android.widget.Toast
import com.example.anonymousChat.model.Message
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class MessageUtils (private val activity: Activity, chatType: String) {
    private val usersRef = FirebaseDatabase.getInstance().getReference("users")
    private val chatRoomsRef: DatabaseReference = FirebaseDatabase.getInstance().getReference(chatType)
    fun sendMessage(
        chatRoomId: String, senderId: String, receiverId: String, content: String
    ) {

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
            } else {
                Toast.makeText(activity, "Bạn cần tìm người chat trước!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkChatRoomStatus(chatRoomId: String, callback: (Boolean) -> Unit) {
        val chatRoomRef = chatRoomsRef.child(chatRoomId)
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
    fun checkAndDeleteOldChatRooms() {
        chatRoomsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (chatRoomSnapshot in snapshot.children) {
                    val chatRoomId = chatRoomSnapshot.key
                    chatRoomId?.let {
                        val lastMessageRef = chatRoomSnapshot.child("messages").children.lastOrNull()
                        lastMessageRef?.let { lastMessageSnapshot ->
                            val timestamp = lastMessageSnapshot.child("timestamp").getValue(Long::class.java)
                            if (timestamp != null && isOlderThan7Days(timestamp)) {
                                deleteChatRoom(chatRoomId)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun isOlderThan7Days(timestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000
        return currentTime - timestamp > sevenDaysInMillis
    }

    private fun deleteChatRoom(chatRoomId: String) {
        chatRoomsRef.child(chatRoomId).removeValue()
    }

    fun endChat(chatRoomId: String, currentUserId: String, receiverId: String) {
        sendMessage(chatRoomId, "system", currentUserId, "Cuộc trò chuyện đã kết thúc!")
        sendMessage(chatRoomId, "system", receiverId, "Cuộc trò chuyện đã kết thúc!")
        chatRoomsRef.child(chatRoomId).child("status").setValue("ended")
    }

    fun shareMoreInformation(chatRoomId: String, currentUserId: String, receiverId: String) {
        val heartRef = chatRoomsRef.child(chatRoomId).child("heart")
        heartRef.child(currentUserId).setValue(true)
        heartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updatePoints(receiverId)
                if (snapshot.childrenCount.toInt() == 2) {
                    sendMessage(
                        chatRoomId,
                        "system",
                        currentUserId,
                        "Thông tin đã được chia sẻ."
                    )
                    sendMessage(
                        chatRoomId,
                        "system",
                        receiverId,
                        "Thông tin đã được chia sẻ."
                    )
                    shareUserInfo(chatRoomId,currentUserId, receiverId)
                    shareUserInfo(chatRoomId, receiverId, currentUserId)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    fun updatePoints(userId: String) {
        val userPointRef = usersRef.child(userId).child("point")
        userPointRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                var points = mutableData.getValue(Int::class.java)
                if (points == null) {
                    points = 0
                }
                if (points < 100) {
                    mutableData.value = points + 5
                }
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                databaseError: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
            }
        })
    }

    fun shareUserInfo(chatRoomId: String ,shareFromUserId: String, shareToUserId: String) {
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
}