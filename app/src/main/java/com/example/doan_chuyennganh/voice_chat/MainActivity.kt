package com.example.doan_chuyennganh.voice_chat

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.authentication.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var usersRef: DatabaseReference
    private lateinit var yourUserIDTextView: TextView
    private lateinit var yourUserNameTextView: TextView
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        usersRef = FirebaseDatabase.getInstance().getReference("users")

        yourUserIDTextView = findViewById(R.id.your_user_id)
        yourUserNameTextView = findViewById(R.id.your_user_name)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        currentUserId?.let { userId ->
            fetchAndDisplayUserInfo(userId)
        }
        initVoiceButton()
        initVideoButton()
        findViewById<View>(R.id.user_logout).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun fetchAndDisplayUserInfo(userId: String) {
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    yourUserIDTextView.text = "Your User ID: ${user.id}"
                    yourUserNameTextView.text = "Your User Name: ${user.username}"
                    initCallInviteService(user.id, user.username)
                } else {
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    private fun initCallInviteService(userID: String?, userName: String?) {
        val appID: Long = 1311438322
        val appSign = "c3f756e989f25bf53f56760a4224840ea082ea8a34667bdf9e100049e4418258"
        val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()
        ZegoUIKitPrebuiltCallInvitationService.init(
            application, appID, appSign, userID, userName,
            callInvitationConfig
        )
    }
    private fun updateUserStatus(userId: String, ready: Boolean) {
        val userRef = usersRef.child(userId)
        userRef.child("ready").setValue(ready)
    }
    private fun initVoiceButton() {
        val newVoiceCall = findViewById<ZegoSendCallInvitationButton>(R.id.new_voice_call)
        newVoiceCall.setIsVideoCall(false)
        val clickListener: (View) -> Unit = {
            if (currentUserId != null) {
                updateUserStatus(currentUserId, true)
                findRandomActiveUserAndCall(false)
            }
        }
        newVoiceCall.setOnClickListener(clickListener)
    }

    private fun initVideoButton() {
        val newVideoCall = findViewById<ZegoSendCallInvitationButton>(R.id.new_video_call)
        newVideoCall.setIsVideoCall(true)
        val clickListener: (View) -> Unit = {
            if (currentUserId != null) {
                updateUserStatus(currentUserId, true)
                findRandomActiveUserAndCall(true)
            }
        }
        newVideoCall.setOnClickListener(clickListener)
    }


    private fun findRandomActiveUserAndCall(isVideoCall: Boolean) {
        usersRef.orderByChild("ready").equalTo(true).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val readyUsers = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                    .filter { it.id != FirebaseAuth.getInstance().currentUser?.uid && it.ready }

                if (readyUsers.isNotEmpty()) {
                    val randomReadyUser = readyUsers.random()
                    val currentUser = FirebaseAuth.getInstance().currentUser?.let { firebaseUser ->
                        User(firebaseUser.uid, firebaseUser.displayName ?: "")
                    }

                    if (currentUser != null) {
                        val roomId = createAudioRoom(currentUser, randomReadyUser)
                        startCallWithUser(randomReadyUser, isVideoCall, roomId)
                    }
                } else {
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý lỗi
            }
        })
    }
    private fun createAudioRoom(user1: User, user2: User): String {
        val roomId = UUID.randomUUID().toString()
        val audioRoom = user1.id?.let { user2.id?.let { it1 -> AudioRoom(it, it1, "calling") } }
        val roomRef = FirebaseDatabase.getInstance().getReference("audioRooms").child(roomId)
        roomRef.setValue(audioRoom)
        return roomId
    }
    class AudioRoom(var user1Id: String, var user2Id: String, var status: String)

    private fun endAudioRoom(roomId: String) {
        val roomRef = FirebaseDatabase.getInstance().getReference("audioRooms").child(roomId)
        roomRef.child("status").setValue("ended")
    }
    private fun startCallWithUser(user: User, isVideoCall: Boolean, roomId: String) {
        val users: MutableList<ZegoUIKitUser> = mutableListOf(ZegoUIKitUser(user.id!!, user.username!!))
        val callButton = if (isVideoCall) findViewById<ZegoSendCallInvitationButton>(R.id.new_video_call)
        else findViewById<ZegoSendCallInvitationButton>(R.id.new_voice_call)
        callButton.setInvitees(users)
        updateUserStatus(user.id!!, false)
        FirebaseAuth.getInstance().currentUser?.uid?.let { updateUserStatus(it, false) }

    }

    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sign Out")
        builder.setMessage("Are you sure to Sign Out?")
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            ZegoUIKitPrebuiltCallInvitationService.unInit()
            finish()
        }
        builder.create().show()
    }

    override fun onDestroy() {
        super.onDestroy()
        ZegoUIKitPrebuiltCallInvitationService.unInit()
    }
}