package com.example.doan_chuyennganh.exchanges

data class Payments(
    var paymentId: String = "",
    var transactionId: String = "",
    var method: String = "",
    var amount: Double = 0.0,
    var status: String = "",
    var createdAt: Long = System.currentTimeMillis()
)
