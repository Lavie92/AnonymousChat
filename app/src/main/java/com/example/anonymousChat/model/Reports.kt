package com.example.anonymousChat.model

data class ReportsCustom(
    val UID_beReported: String,
    val UID_report: String,
    val message: String,
    val timestamp: Long

)

data class Reports(
    val UID_beReported: String,
    val UID_report: String
)


