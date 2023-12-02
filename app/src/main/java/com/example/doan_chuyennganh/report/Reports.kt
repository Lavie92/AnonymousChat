package com.example.doan_chuyennganh.report

import com.example.doan_chuyennganh.chat.Message

data class Reports(
    val UID_beReported: String,
    val UID_report: String,
    val listOf: ArrayList<Message>

)

