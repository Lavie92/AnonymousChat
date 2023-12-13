package com.example.doan_chuyennganh.voice_chat

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.authentication.User
import com.google.firebase.auth.FirebaseAuth
import com.example.doan_chuyennganh.voice_chat.AudioRoom
import com.google.android.material.textfield.TextInputEditText
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
    private lateinit var tarGetUserIdTextView: TextInputEditText
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var waitingDialog: AlertDialog
    private var currentRoomId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        usersRef = FirebaseDatabase.getInstance().getReference("users")
        tarGetUserIdTextView = findViewById(R.id.target_user_id)
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
    private fun updateUserStatus(userId: String, isCall: Boolean) {
        val userRef = usersRef.child(userId)
        userRef.child("isCall").setValue(isCall)
    }
    private fun initVoiceButton() {
        val newVoiceCall = findViewById<ZegoSendCallInvitationButton>(R.id.new_voice_call)
        newVoiceCall.setIsVideoCall(false)
        val clickListener: (View) -> Unit = {
            if (currentUserId != null) {
                updateUserStatus(currentUserId, true)
                showWaitingDialog()
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
                showWaitingDialog()
                findRandomActiveUserAndCall(true)
            }
        }
        newVideoCall.setOnClickListener(clickListener)
    }
    private fun findRandomActiveUserAndCall(isVideoCall: Boolean) {
        currentUserId?.let { userId ->
            updateUserStatus(userId, true)
            usersRef.orderByChild("isCall").equalTo(true)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val activeUsers = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                        val eligibleUsers = activeUsers.filter { it.id != userId }
                        if (eligibleUsers.isNotEmpty()) {
                            val randomActiveUser = eligibleUsers.random()
                            tarGetUserIdTextView.text = Editable.Factory.getInstance().newEditable(randomActiveUser.id)

                            val roomId = createAudioRoom(User(userId, ""), randomActiveUser)
                            startCallWithUser(randomActiveUser, isVideoCall, roomId)
                        } else {
                            // No match found, show some UI indication or log
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error, update UI or log
                    }
                })
        }
    }


    private fun showWaitingDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Waiting")
        builder.setMessage("Waiting for another user to join the call...")
        builder.setCancelable(false)
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            if (currentUserId != null) {
                updateUserStatus(currentUserId, false)
            }
            currentRoomId?.let { endAudioRoom(it) }
        }
        waitingDialog = builder.create()
        waitingDialog.show()
    }


    private fun createAudioRoom(user1: User, user2: User): String {
        val roomId = UUID.randomUUID().toString()
        currentRoomId = roomId
        if (user1.id != user2.id) {
            val audioRoom = AudioRoom(user1.id!!, user2.id!!, "calling")
            val roomRef = FirebaseDatabase.getInstance().getReference("audioRooms").child(roomId)
            roomRef.setValue(audioRoom.toMap())
        }

        return roomId
    }





    private fun endAudioRoom(roomId: String) {
        if (roomId.isNullOrEmpty()) {
            return
        }
        val roomRef = FirebaseDatabase.getInstance().getReference("audioRooms").child(roomId)
        roomRef.child("status").setValue("ended")

        roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val audioRoom = snapshot.getValue(AudioRoom::class.java)
                audioRoom?.let {
                    updateUserStatus(it.user1Id, false)
                    updateUserStatus(it.user2Id, false)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }
    private fun startCallWithUser(user: User, isVideoCall: Boolean, roomId: String) {
        val users = mutableListOf(ZegoUIKitUser(user.id!!, user.username!!))
        val callButton = if (isVideoCall) findViewById<ZegoSendCallInvitationButton>(R.id.new_video_call)
        else findViewById<ZegoSendCallInvitationButton>(R.id.new_voice_call)
        callButton.setInvitees(users)

        updateUserStatus(user.id!!, false)
        FirebaseAuth.getInstance().currentUser?.uid?.let { currentUserId ->
            updateUserStatus(currentUserId, false)
        }
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