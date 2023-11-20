package com.example.doan_chuyennganh.authentication

import com.example.doan_chuyennganh.location.MyLocation
import com.google.firebase.auth.FirebaseUser

data class User(
    val id: String? = null,
    val email: String? = null,
    val password: String? = null,
    val username: String? = null,
    val active: Boolean = false,
    val gender: String? = null,
    val ready: Boolean = false,
    var chatRoom: String = "",
    var location: MyLocation? = MyLocation(),

    )
fun FirebaseUser.toUser(location: MyLocation? = null): User? {
    return this?.let {
        User(
            id = uid,
            email = email,
            location = location

        )
    }
}
