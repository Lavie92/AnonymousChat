package com.example.doan_chuyennganh.voice_chat

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.authentication.User
import com.google.firebase.auth.FirebaseAuth
import com.example.doan_chuyennganh.voice_chat.AudioRoom
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.ChildEventListener
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
    private var selectedUserId: String? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var waitingDialog: AlertDialog
    private var currentRoomId: String? = null
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
        listenForRoomUpdates()  // Add this line to start listening for room updates
        findViewById<View>(R.id.user_logout).setOnClickListener {
            showLogoutDialog()
        }
        findViewById<Button>(R.id.btn_find_user).setOnClickListener {
            findUserForCall()
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
        newVoiceCall.setOnClickListener(View.OnClickListener { startCallBasedOnTargetUserId(true) })
    }

    private fun initVideoButton() {
        val newVideoCall = findViewById<ZegoSendCallInvitationButton>(R.id.new_video_call)
        newVideoCall.setIsVideoCall(true)
        newVideoCall.setOnClickListener(View.OnClickListener { startCallBasedOnTargetUserId(true) })
    }

    private fun findUserForCall() {
        showWaitingDialog()
        selectedUserId = null // Reset selected user ID

        currentUserId?.let { userId ->
            updateUserStatus(userId, true)
            usersRef.orderByChild("isCall").equalTo(true)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val activeUsers = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                        val eligibleUsers = activeUsers.filter { it.id != userId }
                        if (eligibleUsers.isNotEmpty()) {
                            val randomActiveUser = eligibleUsers.random()
                            val targetUserIdTextView = findViewById<TextInputEditText>(R.id.target_user_id)
                            targetUserIdTextView.text = Editable.Factory.getInstance().newEditable(randomActiveUser.id)
                            selectedUserId = randomActiveUser.id

                            // Lấy thông tin đầy đủ của người dùng hiện tại
                            usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(currentUserSnapshot: DataSnapshot) {
                                    val currentUser = currentUserSnapshot.getValue(User::class.java)
                                    if (currentUser != null) {
                                        val roomId = createAudioRoom(currentUser, randomActiveUser)
                                        currentRoomId = roomId
                                        Toast.makeText(this@MainActivity, "Found user and created room: $roomId", Toast.LENGTH_LONG).show()

                                        updateUserStatus(userId, false)
                                        randomActiveUser.id?.let { updateUserStatus(it, false) }
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    // Handle error
                                }
                            })
                        } else {
                            Toast.makeText(this@MainActivity, "No available users for call", Toast.LENGTH_SHORT).show()
                        }

                        waitingDialog.dismiss()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        waitingDialog.dismiss()
                    }
                })
        }
    }
    private fun listenForRoomUpdates() {
        val roomRef = FirebaseDatabase.getInstance().getReference("audioRooms")
        roomRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val room = snapshot.getValue(AudioRoom::class.java)
                room?.let {
                    if (it.user1Id == currentUserId || it.user2Id == currentUserId) {
                        val otherUserId = if (it.user1Id == currentUserId) it.user2Id else it.user1Id
                        updateUIWithOtherUserId(otherUserId)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Tương tự như onChildAdded
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Xử lý khi một child bị xoá
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Xử lý khi một child di chuyển vị trí trong list
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi lắng nghe bị hủy do lỗi cơ sở dữ liệu
            }
            // Các phương thức khác của ChildEventListener
        })
    }
    private fun updateUIWithOtherUserId(otherUserId: String?) {
        val targetUserIdTextView = findViewById<TextInputEditText>(R.id.target_user_id)
        targetUserIdTextView.text = Editable.Factory.getInstance().newEditable(otherUserId)
    }

    private fun startCallWithUser(user: User, isVideoCall: Boolean) {
        val users = mutableListOf(ZegoUIKitUser(user.id, user.username))
        val callButton = if (isVideoCall) findViewById<ZegoSendCallInvitationButton>(R.id.new_video_call)
        else findViewById(R.id.new_voice_call)
        callButton.setInvitees(users)

        user.id?.let { updateUserStatus(it, false) }
        FirebaseAuth.getInstance().currentUser?.uid?.let { currentUserId ->
            updateUserStatus(currentUserId, false)
        }
    }

    private fun startCallBasedOnTargetUserId(isVideoCall: Boolean) {
        val targetUserIdTextView = findViewById<TextInputEditText>(R.id.target_user_id)
        val targetUserId = targetUserIdTextView.text.toString()

        if (targetUserId.isNotEmpty()) {
            usersRef.child(targetUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        val invitees = mutableListOf(ZegoUIKitUser(it.id, it.username))
                        val callButton = if (isVideoCall) findViewById<ZegoSendCallInvitationButton>(R.id.new_video_call)
                        else findViewById<ZegoSendCallInvitationButton>(R.id.new_voice_call)
                        callButton.setInvitees(invitees)
                        startCallWithUser(it, isVideoCall)
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    // Xử lý lỗi
                }
            })
        } else {
            Toast.makeText(this, "No target user ID specified", Toast.LENGTH_SHORT).show()
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