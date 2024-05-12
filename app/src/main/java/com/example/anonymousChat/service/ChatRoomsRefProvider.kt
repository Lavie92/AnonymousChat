//package com.example.anonymousChat.service
//
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
//
//class ChatRoomsRefProvider: IChatRoomsRef {
//    override fun getReference(chatType: String): DatabaseReference {
//        return FirebaseDatabase.getInstance().getReference(chatType)
//    }
//}