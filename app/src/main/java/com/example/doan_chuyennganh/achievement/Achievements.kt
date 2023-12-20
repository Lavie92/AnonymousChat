package com.example.doan_chuyennganh.achievement

data class Achievements(
    val title: String,
    val description: String,
    val goal: Int,
    val currentCount: Int,
    var isRewardClaimed: Boolean = false  // Thêm trường này
)

