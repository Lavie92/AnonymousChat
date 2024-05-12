package com.example.anonymousChat.repository

import com.example.anonymousChat.model.Message

interface IChatRepository {
    suspend fun sendMessage(chatRoomId: String, senderId: String, receiverId: String, content: String)
    suspend fun loadMessageSync(chatRoomId: String, callback: (List<Message>) -> Unit): List<Message>?
    suspend fun checkChatRoomStatus(chatRoomId: String): Boolean
}