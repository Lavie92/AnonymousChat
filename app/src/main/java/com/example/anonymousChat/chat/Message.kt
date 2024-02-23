package com.example.anonymousChat.chat

class Message {
    var messageId: String? = null
    var senderId: String? = null
    var receiverId: String? = null
    var content: String? = null
    var type: String? = null
    var timestamp: Long = 0


    constructor() {}
    constructor(messageId: String, senderId: String, receiverId: String, text: String, type: String, timestamp: Long) {
        this.messageId = messageId
        this.senderId = senderId
        this.receiverId = receiverId
        this.content = text
        this.type = type
        this.timestamp = timestamp
    }
    fun getMessageText(): String {
        return content.toString()
    }


}