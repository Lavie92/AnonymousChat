package com.example.doan_chuyennganh.chat

class Message {

    var messageId: String? = null
    var senderId: String? = null
    var content: String? = null
    var timestamp: Long = 0

    constructor() {}
    constructor(messageId: String, senderId: String, text: String, timestamp: Long) {
        this.messageId = messageId
        this.senderId = senderId
        this.content = text
        this.timestamp = timestamp
    }
}