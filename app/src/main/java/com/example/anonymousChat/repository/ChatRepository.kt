package com.example.anonymousChat.repository

import com.example.anonymousChat.model.Message
import com.example.anonymousChat.service.IChatService
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatRepository(
    private val chatService: IChatService
) : IChatRepository {
    override suspend fun sendMessage(
        chatRoomId: String,
        chatType: String,
        senderId: String,
        receiverId: String,
        content: String
    ) {
        chatService.sendMessage(chatRoomId, chatType, senderId, receiverId, content)
    }

    override suspend fun loadMessageSync(
        chatRoomId: String, chatType: String, callback: (List<Message>) -> Unit
    ): List<Message>? {
        return suspendCoroutine {
            chatService.loadMessage(chatRoomId, chatType = chatType, onSuccess = { messages ->
                callback(messages)
            }, onError = {
            })
        }
        }


//    override suspend fun checkChatRoomStatus(chatRoomId: String): Boolean {
//        return try {
//            val snapshot = chatRoomsRefProvider.getReference(chatType).child(chatRoomId).child("status").get().await()
//            val status = snapshot.getValue(String::class.java)
//            status == "ended"
//        } catch (e: Exception) {
//            false
//        }
//        return true
//    }

}