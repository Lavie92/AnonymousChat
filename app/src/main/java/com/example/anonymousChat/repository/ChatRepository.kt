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
        senderId: String,
        receiverId: String,
        content: String
    ) {
    }

    override suspend fun loadMessageSync(
        chatRoomId: String, callback: (List<Message>) -> Unit
    ): List<Message>? {
        return suspendCoroutine { continuation ->
            var isResumed = false
            chatService.loadMessage(chatRoomId, onSuccess = { messages ->
                callback(messages)
            }, onError = { error ->
                // Xử lý lỗi nếu cần
            })
        }
//            chatService.loadMessage(
//                chatRoomId = chatRoomId,
//                onSuccess = {
//                    if (!isResumed) {
//                        continuation.resume(it)
//                        isResumed = true
//                    }
//                },
//                onError = {
//                    continuation.resume(listOf())
//                    isResumed = true
//                }
//            )
        }


    override suspend fun checkChatRoomStatus(chatRoomId: String): Boolean {
//        return try {
//            val snapshot = chatRoomsRefProvider.getReference(chatType).child(chatRoomId).child("status").get().await()
//            val status = snapshot.getValue(String::class.java)
//            status == "ended"
//        } catch (e: Exception) {
//            false
//        }
        return true
    }

}