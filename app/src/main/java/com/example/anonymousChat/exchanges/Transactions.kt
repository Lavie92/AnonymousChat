package com.example.anonymousChat.exchanges

data class Transactions(
    var transactionId: String = "",
    var userId: String = "",
    var amount: Long,
    var transactionType: String = "",
    var status: String = "",
    var completedAt: Long? = null,
    var createdAt:Long? = null
)

data class transaction(
    var transactionId: String = "",
    var userId: String = "",
    var amount: Long,
    var status: String = "",
    var completedAt: Long? = null,
)