package com.example.doan_chuyennganh.chat

class Message {

    var messageId: String? = null
    var senderId: String? = null
    var receiverId: String? = null
    var content: String? = null
    var timestamp: Long = 0

    constructor() {}
    constructor(messageId: String, senderId: String, receiverId: String, text: String, timestamp: Long) {
        this.messageId = messageId
        this.senderId = senderId
        this.receiverId = receiverId
        this.content = text
        this.timestamp = timestamp
    }
    fun getMessageText(): String {
        return content.toString()
    }
}