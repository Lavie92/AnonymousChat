package com.example.anonymousChat.service

import com.example.anonymousChat.model.Message

interface IChatService {
    fun sendMessage(chatRoomId: String, chatType: String, senderId: String, receiverId: String, content: String)
    fun loadMessage(chatRoomId: String, chatType: String, onSuccess:(data: List<Message>) -> Unit, onError: (message: String) -> Unit)
}