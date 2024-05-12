package com.example.anonymousChat.model

import com.google.firebase.auth.FirebaseUser

data class User(
    val id: String? = null,
    val email: String? = null,
    val username: String? = null,
    val age: String? = null,
    val active: Boolean = false,
    val gender: String? = null,
    val ready: Boolean = false,
    val filter: Boolean = true,
    val point: Int = 0,
    val coins: Long = 0,
//    var score: Int = 0,
    var randomChatRoom: String = "",
    var nearestChatRoom: String = "",
    var countryMatchingRoom: String = "",
    var location: MyLocation? = MyLocation(),
    var isFindByLocation: Boolean = false,
    var isCountryMatching: Boolean = false,
    var country: String? = null,
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
