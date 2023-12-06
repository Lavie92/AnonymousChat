package com.example.doan_chuyennganh.voice_chat

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doan_chuyennganh.R
import com.google.android.material.textfield.TextInputLayout
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val yourUserID = findViewById<TextView>(R.id.your_user_id)
        val yourUserName = findViewById<TextView>(R.id.your_user_name)
        val userID = intent.getStringExtra("userID")
        val userName = intent.getStringExtra("userName")
        yourUserID.text = "Your User ID :$userID"
        yourUserName.text = "Your User Name :$userName"
        val appID: Long = 1311438322
        val appSign = "c3f756e989f25bf53f56760a4224840ea082ea8a34667bdf9e100049e4418258"
        initCallInviteService(appID, appSign, userID, userName)
        initVoiceButton()
        initVideoButton()
        findViewById<View>(R.id.user_logout).setOnClickListener { v: View? ->
            val builder =
                AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Sign Out")
            builder.setMessage("Are you sure to Sign Out?")
            builder.setNegativeButton(
                "Cancel"
            ) { dialog, which -> dialog.dismiss() }
            builder.setPositiveButton(
                "OK"
            ) { dialog, which ->
                dialog.dismiss()
                ZegoUIKitPrebuiltCallInvitationService.unInit()
                finish()
            }
            builder.create().show()
        }
    }

    fun initCallInviteService(appID: Long, appSign: String?, userID: String?, userName: String?) {
        val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()
        ZegoUIKitPrebuiltCallInvitationService.init(
            application, appID, appSign, userID, userName,
            callInvitationConfig
        )
    }

    private fun initVideoButton() {
        val newVideoCall = findViewById<ZegoSendCallInvitationButton>(R.id.new_video_call)
        newVideoCall.setIsVideoCall(true)
        newVideoCall.setOnClickListener { v: View? ->
            val inputLayout =
                findViewById<TextInputLayout>(R.id.target_user_id)
            val targetUserID = inputLayout.editText!!.text.toString()
            val split =
                targetUserID.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val users: MutableList<ZegoUIKitUser> =
                ArrayList()
            for (userID in split) {
                val userName = userID + "_name"
                users.add(ZegoUIKitUser(userID, userName))
            }
            newVideoCall.setInvitees(users)
        }
    }

    private fun initVoiceButton() {
        val newVoiceCall = findViewById<ZegoSendCallInvitationButton>(R.id.new_voice_call)
        newVoiceCall.setIsVideoCall(false)
        newVoiceCall.setOnClickListener { v: View? ->
            val inputLayout =
                findViewById<TextInputLayout>(R.id.target_user_id)
            val targetUserID = inputLayout.editText!!.text.toString()
            val split =
                targetUserID.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val users: MutableList<ZegoUIKitUser> =
                ArrayList()
            for (userID in split) {
                val userName = userID + "_name"
                users.add(ZegoUIKitUser(userID, userName))
            }
            newVoiceCall.setInvitees(users)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ZegoUIKitPrebuiltCallInvitationService.unInit()
    }
}