package com.example.doan_chuyennganh.chat

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

interface MessageHandler {
//    fun toggleOptions(){}
//    fun showMessage(){}
//    fun sendMessage(){}
//    fun shareMoreInformation(){}
    fun updatePoints(userId: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val userPointRef = usersRef.child(userId).child("point")
        userPointRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                var points = mutableData.getValue(Int::class.java)
                if (points == null) {
                    points = 0
                }
                if (points < 100) {
                    mutableData.value = points + 5
                }
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                databaseError: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                Log.d("updatePoints", "Points updated: $databaseError")
            }
        })
    }
}