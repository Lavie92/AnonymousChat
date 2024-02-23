package com.example.anonymousChat.achievement

data class Achievements(
    val title: String,
    val description: String,
    val goal: Int,
    val currentCount: Int,
    var isRewardClaimed: Boolean = false  // Thêm trường này
)

