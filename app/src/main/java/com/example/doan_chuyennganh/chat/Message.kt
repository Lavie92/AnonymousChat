package com.example.doan_chuyennganh.chat

class Message {

    var messageId: String? = null
    var senderId: String? = null
    var text: String? = null
    var timestamp: Long = 0

    constructor() {}
    constructor(messageId: String, senderId: String, text: String, timestamp: Long) {
        this.messageId = messageId
        this.senderId = senderId
        this.text = text
        this.timestamp = timestamp
    }

    constructor(s: String, s1: String) {

        this.text = s
    }
}