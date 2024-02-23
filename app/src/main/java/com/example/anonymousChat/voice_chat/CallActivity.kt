//package com.example.anonymousChat.voice_chat
//
//import android.annotation.SuppressLint
//import android.os.Bundle
//import android.os.Handler
//import android.view.View
//import android.webkit.PermissionRequest
//import android.webkit.WebChromeClient
//import android.webkit.WebView
//import android.webkit.WebViewClient
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.bumptech.glide.Glide
//import com.example.anonymousChat.R
//import com.example.anonymousChat.authentication.InterfaceJava
//import com.example.anonymousChat.authentication.User
//import com.example.anonymousChat.databinding.ActivityCallBinding
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.ValueEventListener
//import java.util.UUID
//
//class CallActivity : AppCompatActivity() {
//    var binding: ActivityCallBinding? = null
//    private var uniqueId = ""
//    var auth: FirebaseAuth? = null
//    var username: String? = ""
//    private var friendsUsername: String? = ""
//    private var isPeerConnected = false
//    private var firebaseRef: DatabaseReference? = null
//    private var isAudio = true
//    private var isVideo = true
//    private var createdBy: String? = null
//    private var pageExit = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityCallBinding.inflate(layoutInflater)
//        setContentView(binding!!.root)
//        auth = FirebaseAuth.getInstance()
//        firebaseRef = FirebaseDatabase.getInstance().reference.child("audiorooms")
//        username = intent.getStringExtra("username")
//        Toast.makeText(this, "calling with $username", Toast.LENGTH_SHORT).show()
//        val incoming = intent.getStringExtra("incoming")
//        createdBy = intent.getStringExtra("createdBy")
//        friendsUsername = incoming
//        setupWebView()
//        binding!!.micBtn.setOnClickListener {
//            isAudio = !isAudio
//            callJavaScriptFunction("javascript:toggleAudio(\"$isAudio\")")
//            if (isAudio) {
//                binding!!.micBtn.setImageResource(R.drawable.btn_unmute_normal)
//            } else {
//                binding!!.micBtn.setImageResource(R.drawable.btn_mute_normal)
//            }
//        }
//        binding!!.videoBtn.setOnClickListener {
//            isVideo = !isVideo
//            callJavaScriptFunction("javascript:toggleVideo(\"$isVideo\")")
//            if (isVideo) {
//                binding!!.videoBtn.setImageResource(R.drawable.btn_video_normal)
//            } else {
//                binding!!.videoBtn.setImageResource(R.drawable.btn_video_muted)
//            }
//        }
//        binding!!.endCall.setOnClickListener { finish() }
//    }
//
//    @SuppressLint("SetJavaScriptEnabled")
//    private fun setupWebView() {
//        binding?.webView?.webChromeClient = object : WebChromeClient() {
//            override fun onPermissionRequest(request: PermissionRequest) {
//                request.grant(request.resources)
//            }
//        }
//        binding?.webView?.settings?.javaScriptEnabled = true
//        binding?.webView?.settings?.mediaPlaybackRequiresUserGesture = false
//        binding?.webView?.addJavascriptInterface(InterfaceJava(this), "Android")
//        loadVideoCall()
//    }
//
//    private fun loadVideoCall() {
//        val filePath = "file:android_asset/call.html"
//        binding?.webView?.loadUrl(filePath)
//        binding?.webView?.webViewClient = object : WebViewClient() {
//            override fun onPageFinished(view: WebView, url: String) {
//                super.onPageFinished(view, url)
//                initializePeer()
//            }
//        }
//    }
//
//
//    fun initializePeer() {
//        uniqueId = getUniqueId()
//        callJavaScriptFunction("javascript:init(\"$uniqueId\")")
//        if (createdBy.equals(username, ignoreCase = true)) {
//            if (pageExit) return
//            firebaseRef!!.child(username!!).child("connId").setValue(uniqueId)
//            firebaseRef!!.child(username!!).child("isAvailable").setValue(true)
//            binding?.loadingGroup?.visibility = View.GONE
//            binding?.controls?.visibility = View.VISIBLE
//            FirebaseDatabase.getInstance().reference
//                .child("users")
//                .child(friendsUsername!!)
//                .addListenerForSingleValueEvent(object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        val user: User? = snapshot.getValue(User::class.java)
//                        if (user != null) {
//                            Glide.with(this@CallActivity).load(user.username)
//
//                        }
//
//                    }
//
//                    override fun onCancelled(error: DatabaseError) {}
//                })
//        } else {
//            Handler().postDelayed({
//                friendsUsername = createdBy
//                FirebaseDatabase.getInstance().reference
//                    .child("users")
//                    .child(friendsUsername!!)
//                    .addListenerForSingleValueEvent(object : ValueEventListener {
//                        override fun onDataChange(snapshot: DataSnapshot) {
//                            val user: User? = snapshot.getValue(User::class.java)
//                            if (user != null) {
//                                Glide.with(this@CallActivity).load(user.username)
//                            }
//
////
//                        }
//
//                        override fun onCancelled(error: DatabaseError) {}
//                    })
//                FirebaseDatabase.getInstance().reference
//                    .child("audiorooms")
//                    .child(friendsUsername!!)
//                    .child("connId")
//                    .addListenerForSingleValueEvent(object : ValueEventListener {
//                        override fun onDataChange(snapshot: DataSnapshot) {
//                            if (snapshot.value != null) {
//                                sendCallRequest()
//                            }
//                        }
//
//                        override fun onCancelled(error: DatabaseError) {}
//                    })
//            }, 3000)
//        }
//    }
//
//    fun onPeerConnected() {
//        isPeerConnected = true
//    }
//
//    fun sendCallRequest() {
//        if (!isPeerConnected) {
//            Toast.makeText(
//                this,
//                "You are not connected. Please check your internet.",
//                Toast.LENGTH_SHORT
//            ).show()
//            return
//        }
//        listenConnId()
//    }
//
//    private fun listenConnId() {
//        firebaseRef!!.child(friendsUsername!!).child("connId")
//            .addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.value == null) return
//                    binding?.loadingGroup?.visibility = View.GONE
//                    binding?.controls?.visibility = View.VISIBLE
//                    val connId = snapshot.getValue(String::class.java)
//                    callJavaScriptFunction("javascript:startCall(\"$connId\")")
//                }
//
//                override fun onCancelled(error: DatabaseError) {}
//            })
//    }
//
//    fun callJavaScriptFunction(function: String?) {
//        binding?.webView?.post {
//            if (function != null) {
//                binding?.webView!!.evaluateJavascript(function, null)
//            }
//        }
//    }
//
//    private fun getUniqueId(): String {
//        return UUID.randomUUID().toString()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        pageExit = true
//        firebaseRef!!.child(createdBy!!).setValue(null)
//        finish()
//    }
//}