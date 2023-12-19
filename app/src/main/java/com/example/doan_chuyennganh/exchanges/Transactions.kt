package com.example.doan_chuyennganh.exchanges

data class Transactions(
    var transactionId: String = "",
    var userId: String = "",
    var amount: Double = 0.0,
    var transactionType: String = "",
    var status: String = "",
    var completedAt: Long? = null
)