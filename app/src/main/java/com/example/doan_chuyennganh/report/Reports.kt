package com.example.doan_chuyennganh.report

data class Reports(
    val UID_beReported: String,
    val UID_report: String,
    val Message: ArrayList<Map<String, Any?>>

)
