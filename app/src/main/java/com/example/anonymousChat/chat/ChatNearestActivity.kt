package com.example.anonymousChat.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.anonymousChat.R
import com.example.anonymousChat.adapter.MessageAdapter
import com.example.anonymousChat.authentication.User
import com.example.anonymousChat.authentication.toUser
import com.example.anonymousChat.databinding.ActivityChatNearestBinding
import com.example.anonymousChat.encrypt.EncryptionUtils
import com.example.anonymousChat.layout.SplashScreenActivity
import com.example.anonymousChat.location.MyLocation
import com.example.anonymousChat.notification.NotificationService
import com.example.anonymousChat.report.Reports
import com.example.filterbadwodslibrary.filterBadwords
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ChatNearestActivity : AppCompatActivity() {

    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var btnRandom: Button
    private lateinit var binding: ActivityChatNearestBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var usersRef: DatabaseReference
    private lateinit var nearestChatRoomRef: DatabaseReference
    private var chatRoomId: String = ""
    private var messageList: ArrayList<Message>? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var receiverId: String = ""
    private val currentUser = FirebaseAuth.getInstance().currentUser
    val badwords = filterBadwords()
    private val handler = Handler()
    private var readyToFind = false
    private var optionsVisible = false
    private lateinit var auth: FirebaseAuth
    private lateinit var btnSendImage: ImageView
    private lateinit var btnHeart: Button
    private lateinit var imageUtils: ImageUtils
    private lateinit var messageUtils: MessageUtils

    companion object {
        const val REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatNearestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        imageUtils = ImageUtils(this)
        messageUtils = MessageUtils(this, "NearestChatRoom")
        nearestChatRoomRef = FirebaseDatabase.getInstance().getReference("NearestChatRoom")
        usersRef = FirebaseDatabase.getInstance().getReference("users")
        messageRecyclerView = binding.rcMessage
        messageBox = binding.messageBox
        sendButton = binding.ivSend
        btnRandom = binding.btnRandom
        messageList = ArrayList()
        btnHeart = binding.btnHeart
        messageAdapter = MessageAdapter(this, messageList!!)
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        val btnEndChat: Button = binding.btnEndChat
        val btnShowOptions: FloatingActionButton = findViewById(R.id.btnShowOptions)
        btnShowOptions.setOnClickListener {
            toggleOptions()
        }
        binding.btnBack.setOnClickListener {
            val splashIntent = Intent(this@ChatNearestActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toMain")
            startActivity(splashIntent)
        }
        checkChatRoomId()
        checkChatRoomStatus(chatRoomId) {
        }
        btnSendImage = binding.ivSendImage
        btnRandom.setOnClickListener {
            checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
                if (!isChatRoomEnded) {
                    Toast.makeText(
                        this,
                        "Bạn cần kết thúc cuộc trò chuyện hiện tại trước!!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    findNearestUserForChat()
                }
            }
        }
        btnEndChat.setOnClickListener {
            messageUtils.endChat(chatRoomId, currentUserId.toString(), receiverId, )
        }

        sendButton.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            if (messageText.isNotEmpty() && currentUserId != null && chatRoomId.isNotEmpty()) {
                messageUtils.sendMessage(chatRoomId, currentUserId, receiverId, messageText)
                messageBox.text.clear()
            }
        }
        btnHeart.setOnClickListener {
            messageUtils.sendMessage(
                chatRoomId,
                "system",
                currentUserId.toString(),
                "Bạn đã nhấn yêu thích, nếu đối phương đồng ý thì bạn sẽ chia sẻ thông tin (tuổi, giới tính, username)"
            )
            messageUtils.sendMessage(
                chatRoomId,
                "system",
                receiverId,
                "Đối phương thích bạn, nếu bạn cũng vậy hãy nhấn tim để chia sẻ thông tin gồm (username, tuổi, giới tính)"
            )
            messageUtils.shareMoreInformation(chatRoomId, currentUserId.toString(), receiverId)
        }
        btnSendImage.setOnClickListener {
            checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
                if (!isChatRoomEnded) {
                    imageUtils.showImagePickerDialog()
                } else {
                    Toast.makeText(this, "Bạn cần tìm người chat trước!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri: Uri = data.data ?: return
            if (currentUserId != null) {
                imageUtils.uploadImageToFirebaseStorage(
                    selectedImageUri, chatRoomId,
                    "NearestChatRoom", currentUserId, receiverId
                )
            }
        }
    }

    private fun toggleOptions() {
        val btnEndChat: Button = binding.btnEndChat
        val slideUp = TranslateAnimation(0f, 0f, btnHeart.height.toFloat(), 0f)
        slideUp.duration = 500

        val slideDown = TranslateAnimation(0f, 0f, 0f, btnHeart.height.toFloat())
        slideDown.duration = 50
        if (!optionsVisible) {
            btnEndChat.startAnimation(slideUp)
            btnSendImage.startAnimation(slideUp)
            btnHeart.startAnimation((slideUp))
            btnHeart.visibility = View.VISIBLE
            btnSendImage.visibility = View.VISIBLE
            btnRandom.startAnimation(slideUp)
            btnRandom.visibility = View.VISIBLE
            btnEndChat.visibility = View.VISIBLE
            checkChatRoomStatus(chatRoomId) {
            }
        } else {
            btnEndChat.startAnimation(slideDown)
            btnHeart.startAnimation(slideDown)
            btnHeart.visibility = View.GONE
            btnEndChat.visibility = View.GONE
            btnRandom.startAnimation(slideDown)
            btnRandom.visibility = View.GONE
            btnSendImage.startAnimation(slideDown)
            btnSendImage.visibility = View.GONE
        }
        optionsVisible = !optionsVisible
    }

    private val timeoutRunnable = Runnable {
        isFindByLocation(currentUserId.toString(), false)
        Toast.makeText(this, "No user found. Please try again.", Toast.LENGTH_SHORT).show()
    }

    private fun findNearestUserForChat() {
        chatRoomId = ""
        receiverId = ""
        checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
            if (isChatRoomEnded) {
                removeMessage(chatRoomId) {
                    if (currentUserId != null) {
                        isFindByLocation(currentUserId, true)
                    }
                    showMessage("Đang tìm kiếm...")
                    handler.postDelayed(timeoutRunnable, 30000)
                    var distanceUser = 0.0
                    usersRef.get().addOnSuccessListener { snapshot ->
                        setCurrentUserLocation()!!
                        getCurrentUserLocationFromFirebase { currentUserLocation ->
                            if (currentUserLocation != null) {
                                val allUsers = snapshot.children.mapNotNull {
                                    try {
                                        val user = it.getValue(User::class.java)!!
                                        user.isFindByLocation =
                                            it.child("isFindByLocation").getValue(Boolean::class.java) ?: false
                                        user
                                    } catch (e: Exception) {
                                        null
                                    }
                                }.filter { it.isFindByLocation && it.id != currentUserId }

                                var nearestUser: User? = null
                                for (user in allUsers) {
                                    val userLocation = user.location
                                    if (userLocation != null) {
                                        val distance = calculateDistance(
                                            currentUserLocation.latitude,
                                            currentUserLocation.longitude,
                                            userLocation.latitude,
                                            userLocation.longitude
                                        )
                                        if (distance >= 0) {
                                            nearestUser = user
                                            distanceUser =
                                                BigDecimal(distance).setScale(2, RoundingMode.HALF_EVEN)
                                                    .toDouble()
                                        }
                                    }
                                }

                                if (nearestUser != null) {
                                    handler.removeCallbacks(timeoutRunnable)
                                    val nearestUserLocation = nearestUser.location
                                    if (nearestUserLocation != null) {
                                        receiverId = nearestUser.id.toString()

                                        chatRoomId = currentUser?.toUser()?.let {
                                            createChatRoom(
                                                it,
                                                nearestUser
                                            )
                                        }.toString()
                                        sendInitialMessages(
                                            chatRoomId,
                                            currentUserId.toString(),
                                            receiverId,
                                            distanceUser
                                        )
                                        currentUserId?.let { isFindByLocation(it, false) }
                                        isFindByLocation(receiverId, false)
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Không thể lấy vị trí của người dùng gần nhất.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendInitialMessages(
        chatRoomId: String,
        senderId: String,
        receiverId: String,
        distanceUser: Double
    ) {
        messageUtils.sendMessage(
            chatRoomId,
            "system",
            senderId,
            "người dùng gần nhất đã tham gia chat với khoảng cách $distanceUser km"
        )
        messageUtils.sendMessage(
            chatRoomId,
            "system",
            receiverId,
            "người dùng gần nhất đã tham gia chat với khoảng cách $distanceUser km"
        )
    }

    private fun getCurrentUserLocationFromFirebase(callback: (MyLocation?) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            usersRef.child(currentUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentUserLocation =
                            snapshot.child("location").getValue(MyLocation::class.java)
                        callback(currentUserLocation)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback(null)
                    }
                })
        } else {
            callback(null)
        }
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371 // Radius of the earth in km
        val dLat = deg2rad(lat2 - lat1)  // deg2rad below
        val dLon = deg2rad(lon2 - lon1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                    cos(deg2rad(lat1)) * cos(deg2rad(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun deg2rad(deg: Double): Double {
        return deg * (Math.PI / 180)
    }

    private fun isFindByLocation(userId: String, isFindByLocation: Boolean) {
        val userRef = usersRef.child(userId)
        userRef.child("isFindByLocation").setValue(isFindByLocation)
    }


    private fun createChatRoom(user1: User, user2: User): String {
        chatRoomId = UUID.randomUUID().toString()
        user1.nearestChatRoom = chatRoomId
        user2.nearestChatRoom = chatRoomId
        nearestChatRoomRef =
            FirebaseDatabase.getInstance().getReference("NearestChatRoom").child(chatRoomId)
        nearestChatRoomRef.child("user1Id").setValue(user1.id)
        nearestChatRoomRef.child("user2Id").setValue(user2.id)
        nearestChatRoomRef.child("status").setValue("chatting")

        val user1Reference = user1.id?.let { usersRef.child(it) }
        user1Reference?.child("nearestChatRoom")?.setValue(chatRoomId)

        val user2Reference = user2.id?.let { usersRef.child(it) }
        user2Reference?.child("nearestChatRoom")?.setValue(chatRoomId)
        return chatRoomId
    }

    private fun setCurrentUserLocation(): MyLocation? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        var userLocation = MyLocation()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation =
                            MyLocation(latitude = location.latitude, longitude = location.longitude)
                        updateUserLocation(userLocation)
                    }
                }
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
        return userLocation
    }


    private fun updateUserLocation(location: MyLocation) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(it.uid)
            userRef.child("location").setValue(location)
        }
    }


    private fun loadMessages(chatRoomId: String) {
        var filter: Boolean
        val messagesRef = nearestChatRoomRef.child(chatRoomId).child("messages")
        if (currentUserId != null) {
            usersRef.child(currentUserId).get().addOnSuccessListener {
                filter = it.child("filter").value as Boolean
                messagesRef.addValueEventListener(object : ValueEventListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val newMessages = snapshot.children.mapNotNull { msgSnapshot ->
                            val messageId =
                                msgSnapshot.child("messageId").getValue(String::class.java)
                            val senderId =
                                msgSnapshot.child("senderId").getValue(String::class.java)
                            val receiverId =
                                msgSnapshot.child("receiverId").getValue(String::class.java)
                            if (senderId == currentUserId || receiverId == currentUserId) {
                                var content =
                                    msgSnapshot.child("content").getValue(String::class.java)
                                val type = msgSnapshot.child("type").getValue(String::class.java)
                                val timestamp =
                                    msgSnapshot.child("timestamp").getValue(Long::class.java)

                                if (filter) {
                                    content = badwords.filterBadWords(content)
                                }

                                messageId?.let {
                                    senderId?.let { sid ->
                                        receiverId?.let { rid ->
                                            content?.let { msg ->
                                                timestamp?.let { time ->
                                                    type?.let { it1 ->
                                                        Message(
                                                            it,
                                                            sid,
                                                            rid,
                                                            msg,
                                                            it1,
                                                            time
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                null
                            }
                        }

                        messageList?.clear()
                        newMessages.let { messageList?.addAll(it) }

                        messageAdapter.notifyDataSetChanged()
                        messageRecyclerView.scrollToPosition(messageList!!.size - 1)

                        if (newMessages.isNotEmpty()) {
                            var content = "";
                            val lastMessage = newMessages.last()
                            content = if (lastMessage.type!! == "image") {
                                "Người lạ đã gửi 1 hình ảnh"
                            } else {
                                lastMessage.content!!
                            }
                            if (lastMessage.senderId != currentUserId) {
                                lastMessage.content?.let {
                                    showNotification(
                                        "Chat ngẫu nhiên",
                                        content
                                    )
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showMessage(message: String) {
        val systemMessage = Message(
            UUID.randomUUID().toString(),
            "system",
            currentUserId ?: "",
            message,
            "text",
            System.currentTimeMillis()
        )

        messageList?.add(systemMessage)
        messageAdapter.notifyDataSetChanged()

        messageRecyclerView.scrollToPosition(messageList!!.size - 1)
    }

    private fun showNotification(title: String, content: String) {
        val notificationService = NotificationService()
        notificationService.showNotification(this, title, content, "NearestChatRoom")
    }
    private fun checkChatRoomStatus(chatRoomId: String, callback: (Boolean) -> Unit) {
        if (chatRoomId.isNotEmpty()) {
            val nearestChatRoomRef =
                FirebaseDatabase.getInstance().getReference("NearestChatRoom").child(chatRoomId)
            nearestChatRoomRef.child("status")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val status = snapshot.getValue(String::class.java)
                        callback.invoke(status == "ended")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback.invoke(false)
                    }
                })
        }
    }

    private fun checkChatRoomId() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        var chatRoomValue = ""
        if (currentUserId != null) {
            val userRef = usersRef.child(currentUserId)

            userRef.child("nearestChatRoom").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatRoomValue = snapshot.getValue(String::class.java).toString()
                    checkUsersInChatRoom(chatRoomValue)
                    chatRoomId = chatRoomValue
                    loadMessages(chatRoomId)
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    private fun checkUsersInChatRoom(chatRoomId: String) {
        val nearestChatRoomRef =
            FirebaseDatabase.getInstance().getReference("NearestChatRoom").child(chatRoomId)

        nearestChatRoomRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user1Id = snapshot.child("user1Id").getValue(String::class.java)
                val user2Id = snapshot.child("user2Id").getValue(String::class.java)
                if (user1Id != "" && user2Id != "" && !user1Id.isNullOrEmpty() && !user2Id.isNullOrEmpty())
                    receiverId = if (user1Id == currentUserId) user2Id ?: "" else user1Id ?: ""
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
    private fun removeMessage(chatRoomId: String, callback: () -> Unit) {
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("NearestChatRoom").child(chatRoomId)
        chatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                usersRef.child(currentUserId.toString()).child("nearestChatRoom").setValue("")
                callback.invoke()
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    fun reportMessage(message: Message) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val receiverUserId = currentUserId
        val senderUserId = message.senderId
        val messageMap = mapOf(
            "timestamp" to message.timestamp,
            "content" to message.content
        )
        val report = Reports(senderUserId!!, receiverUserId!!)
        val reportRef = FirebaseDatabase.getInstance().getReference("reports").child(chatRoomId)
            .child(message.messageId!!)
        reportRef.setValue(report)
        reportRef.child("status").setValue("doing")
        reportRef.child("messageMap").setValue(messageMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Report added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error adding report: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}
