package com.example.doan_chuyennganh.voice_chat

class AudioRoom(var user1Id: String = "", var user2Id: String = "", var status: String = "") {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "user1Id" to user1Id,
            "user2Id" to user2Id,
            "status" to status
        )
    }
}
