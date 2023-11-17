package com.example.doan_chuyennganh.authentication

import com.google.firebase.auth.FirebaseUser

data class User(
    val id: String? = null,
    val email: String? = null,
    val username: String? = null,
    val age: String? = null,
    val active: Boolean = false,
    val gender: String? = null,
    val ready: Boolean = false,
    var chatRoom: String = ""

)
fun FirebaseUser.toUser(): User? {
    return this?.let {
        User(
            id = uid,
            email = email
        )
    }
}
