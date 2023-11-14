package com.example.doan_chuyennganh.authentication

data class User(
    val id:String? = null,
    val email: String? = null,
    val password: String? = null,
    val username: String? = null,
    val active: Boolean = false,
    val gender: String? = null,
    val ready: Boolean = false
)
