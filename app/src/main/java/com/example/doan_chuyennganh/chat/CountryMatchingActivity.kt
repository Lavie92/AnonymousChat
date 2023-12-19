package com.example.doan_chuyennganh.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan_chuyennganh.LoginActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.adapter.MessageAdapter
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.authentication.toUser
import com.example.doan_chuyennganh.databinding.ActivityCountryMatchingBinding
import com.example.doan_chuyennganh.encrypt.EncryptionUtils
import com.example.doan_chuyennganh.layout.SplashScreenActivity
import com.example.doan_chuyennganh.location.MyLocation
import com.example.doan_chuyennganh.notification.NotificationService
import com.example.doan_chuyennganh.report.Reports
import com.example.filterbadwodslibrary.filterBadwords
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
private const val STORAGE_PATH = "images/"

class CountryMatchingActivity : AppCompatActivity() {

    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var btnRandom: Button
    private lateinit var binding: ActivityCountryMatchingBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var usersRef: DatabaseReference
    private lateinit var countryMatchingChatRoomRef: DatabaseReference
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
    private  lateinit var databaseReferences: DatabaseReference
    private lateinit var btnSendImage: ImageView
    private  lateinit var btnHeart: Button
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountryMatchingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        checkSession()

        countryMatchingChatRoomRef = FirebaseDatabase.getInstance().getReference("CountryMatchingRooms")
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
        val btnBack = binding.btnBack
        btnBack.setOnClickListener{
            val splashIntent = Intent(this, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toMain")
            startActivity(splashIntent)        }
        checkChatRoomId()
        checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
            if (isChatRoomEnded) {
                readyToFind = false
                toggleFind()
            } else {
                readyToFind = true
                toggleFind()

            }
        }
//        getCountrySelected()
        sendButton.setOnClickListener {
            val messageText = messageBox.text.toString().trim()
            if (messageText.isNotEmpty() && currentUserId != null && chatRoomId.isNotEmpty()) {
                sendMessage(chatRoomId, currentUserId, receiverId, messageText)
                messageBox.text.clear()
            }
        }

        btnSendImage = binding.ivSendImage

        btnRandom.setOnClickListener{
            readyToFind = true
            toggleFind()
            findInternationalUser()
        }
        btnHeart.setOnClickListener{
            sendMessage(chatRoomId, "system", currentUserId.toString(), "Bạn đã nhấn yêu thích, nếu đối phương đồng ý thì bạn sẽ chia sẻ thông tin (tuổi, giới tính, username)")
            sendMessage(chatRoomId, "system", receiverId, "Đối phương thích bạn, nếu bạn cũng vậy hãy nhấn tim để chia sẻ thông tin gồm (username, tuổi, giới tính)")
            shareMoreInformation()
        }
        btnSendImage.setOnClickListener {
            showImagePickerDialog()
        }
        btnEndChat.setOnClickListener {

            endChat(chatRoomId) { success ->
                if (success) {
                    readyToFind = false
                    toggleFind()
                }
            }
        }
    }


    private fun showImagePickerDialog() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), ChatActivity.REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ChatActivity.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri: Uri = data.data ?: return
            uploadImageToFirebaseStorage(selectedImageUri)
        }
    }
    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val imageName = UUID.randomUUID().toString()
        val imageRef = storageRef.child("$STORAGE_PATH$imageName.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    handleImageUploadSuccess(imageUrl)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi tải ảnh lên: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun handleImageUploadSuccess(imageUrl: String) {
        val timestamp = System.currentTimeMillis()
        val messageId = UUID.randomUUID().toString()
        val secretKey = EncryptionUtils.generateKey()
        val encryptedMessage = EncryptionUtils.encrypt(imageUrl, secretKey)
        val encryptedKey = EncryptionUtils.getKeyAsString(secretKey)
        val type = "image"
        val message =
            Message(messageId, currentUserId.toString(), receiverId, encryptedMessage, type, encryptedKey, timestamp)
        countryMatchingChatRoomRef = FirebaseDatabase.getInstance().getReference("CountryMatchingRooms")
        countryMatchingChatRoomRef.child(chatRoomId).child("messages")
            .push().setValue(message)
        Toast.makeText(this, "Ảnh đã được gửi", Toast.LENGTH_SHORT).show()
    }

    private fun shareMoreInformation() {
        if (chatRoomId.isNotEmpty()) {
            val countryMatchingChatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId)
            val heartRef = countryMatchingChatRoomRef.child("heart")
            heartRef.child(currentUserId!!).setValue(true)
            heartRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    updatePoints(receiverId)
                    if (snapshot.childrenCount.toInt() == 2) {
                        sendMessage(chatRoomId, "system", currentUserId, "Thông tin đã được chia sẻ.")
                        sendMessage(chatRoomId, "system", receiverId, "Thông tin đã được chia sẻ.")
                        shareUserInfo(currentUserId, receiverId)
                        shareUserInfo(receiverId, currentUserId)
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                }
            })
        }
    }

    private fun shareUserInfo(shareFromUserId: String, shareToUserId: String) {
        usersRef.child(shareFromUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                val gender = snapshot.child("gender").getValue(String::class.java) ?: "Unknown"
                val age = snapshot.child("age").getValue(String::class.java) ?: "Unknown"
                val info = " Thông tin của người ấy: Username: $username, Giới tính: $gender, Tuổi: $age"
                sendMessage(chatRoomId, "system", shareToUserId, info)
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    private fun updatePoints(userId: String) {
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

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
            }
        })
    }


    private fun toggleFind(){
        val slideUp = TranslateAnimation(0f, 0f, binding.btnRandom.height.toFloat(), 0f)
        val slideDown = TranslateAnimation(0f, 0f, 0f, binding.btnRandom.height.toFloat())
        slideDown.duration = 50
        slideUp.duration = 500
        if(!readyToFind){
            binding.btnRandom.startAnimation(slideUp)
            binding.btnRandom.visibility = View.VISIBLE
            checkChatRoomStatus(chatRoomId) { isChatRoomchatting ->
                if (!isChatRoomchatting) {
                    binding.btnRandom.visibility = View.GONE
                }
            }
        }
        else{
            binding.btnRandom.startAnimation(slideDown)
            binding.btnRandom.visibility = View.GONE
        }
        readyToFind = !readyToFind


    }


    private fun toggleOptions() {
        val btnEndChat: Button = findViewById(R.id.btnEndChat)
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
            checkChatRoomStatus(chatRoomId) { isChatRoomchatting ->
                if (isChatRoomchatting) {
                    btnEndChat.startAnimation(slideDown)
                    btnHeart.startAnimation(slideDown)
                    btnHeart.visibility = View.GONE
                    btnEndChat.visibility = View.GONE
                    btnRandom.startAnimation(slideDown)
                    btnRandom.visibility = View.GONE
                    btnSendImage.startAnimation(slideDown)
                    btnSendImage.visibility = View.GONE
                }
            }
        }
        optionsVisible = !optionsVisible
    }

    private class FetchCountryNameTask : AsyncTask<Double, Void, String?>() {

        override fun doInBackground(vararg params: Double?): String? {
            val latitude = params[0]
            val longitude = params[1]

            val url =
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude"

            val request = Request.Builder().url(url).build()

            try {
                val response = OkHttpClient().newCall(request).execute()

                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val json = JSONObject(responseData)
                    return json.getJSONObject("address").getString("country_code")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }



    private val timeoutRunnable = Runnable {
        isCountryMatching(currentUserId.toString(), false)
        Toast.makeText(this, "No user found. Please try again.", Toast.LENGTH_SHORT).show()
    }


    private fun findInternationalUser() {
        chatRoomId = ""
        receiverId = ""
        checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
            if (isChatRoomEnded) {
                removeMessage(chatRoomId)
            }
        }

        if (currentUserId != null) {
            isCountryMatching(currentUserId, true)
        }

        showMessage("Đang tìm kiếm...")
        // Set timeout
        handler.postDelayed(timeoutRunnable, 30000)

        usersRef.get().addOnSuccessListener { snapshot ->
            setCurrentUserLocation()

            getCurrentUserLocationFromFirebase { currentUserLocation ->
                if (currentUserLocation != null) {
                    val fetchCountryNameTask = FetchCountryNameTask()
                    val lat = currentUserLocation.latitude
                    val lon = currentUserLocation.longitude

                    fetchCountryNameTask.execute(lat, lon)
                    val currentCountry: String? = fetchCountryNameTask.get()

                    if (currentCountry != null) {
                        usersRef.child(currentUserId.toString()).child("Country").setValue(currentCountry)
                    } else {
                        showMessage("Bạn không thể lấy được vị trí")
                    }
                    val matchingUsers = mutableListOf<User>()

                    for (userSnapshot in snapshot.children) {
                        try {
                            val user = userSnapshot.getValue(User::class.java)!!

                            user.id?.let { userId ->
                                getCountryFromFirebase(userId) { country ->
                                    if (country != currentCountry) {
                                        user.isCountryMatching =
                                            userSnapshot.child("isCountryMatching")
                                                .getValue(Boolean::class.java) ?: false
                                        if (user.isCountryMatching) {
                                            matchingUsers.add(user)
                                            val countryMatchingUser = matchingUsers.random()
                                            receiverId = countryMatchingUser.id.toString()
                                            currentUser?.toUser(currentUserLocation)
                                                ?.let { createChatRoom(it, countryMatchingUser) }
                                            country?.let {
                                                currentCountry?.let { it1 ->
                                                    sendInitialMessages(chatRoomId, currentUserId.toString(), receiverId,
                                                        it,
                                                        it1
                                                    )
                                                }
                                                isCountryMatching(currentUserId.toString(), false)
                                                isCountryMatching(receiverId, false)
                                            }
                                            handler.removeCallbacks(timeoutRunnable)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
    }
    private fun sendInitialMessages(chatRoomId: String, senderId: String, receiverId: String, user1Country: String, user2Country: String) {
        sendMessage(chatRoomId, "system", senderId, "người dùng ở $user1Country đã tham gia chat")
        sendMessage(chatRoomId, "system", receiverId, "người dùng ở $user2Country đã tham gia chat")
    }

    private fun getCountryFromFirebase(userId: String, callback: (String?) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val country = snapshot.child("Country").getValue(String::class.java)
                callback(country)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }


    private fun getCurrentUserLocationFromFirebase(callback: (MyLocation?) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            usersRef.child(currentUserId).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentUserLocation = snapshot.child("location").getValue(MyLocation::class.java)
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


    private fun isCountryMatching(userId: String, isCountryMatching: Boolean) {
        val userRef = usersRef.child(userId)
        userRef.child("isCountryMatching").setValue(isCountryMatching)
    }


    private fun createChatRoom(user1: User, user2: User): String {
        chatRoomId = UUID.randomUUID().toString()
        user1.countryMatchingRoom = chatRoomId
        user2.countryMatchingRoom = chatRoomId

        countryMatchingChatRoomRef = FirebaseDatabase.getInstance().getReference("CountryMatchingRooms").child(chatRoomId)
        countryMatchingChatRoomRef.child("user1Id").setValue(user1.id)
        countryMatchingChatRoomRef.child("user2Id").setValue(user2.id)
        countryMatchingChatRoomRef.child("status").setValue("chatting")

        val user1Reference = user1.id?.let { usersRef.child(it) }
        user1Reference?.child("countryMatchingRoom")?.setValue(chatRoomId)

        val user2Reference = user2.id?.let { usersRef.child(it) }
        user2Reference?.child("countryMatchingRoom")?.setValue(chatRoomId)
        return chatRoomId
    }

    private fun setCurrentUserLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        var userLocation: MyLocation
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation = MyLocation(latitude = location.latitude, longitude = location.longitude)
                        updateUserLocation(userLocation)
                    }
                }
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }


    private fun updateUserLocation(location: MyLocation) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(it.uid)
            userRef.child("location").setValue(location)
        }
    }


    private fun loadMessages(chatRoomId: String) {
        var filter : Boolean
        countryMatchingChatRoomRef = FirebaseDatabase.getInstance().getReference("CountryMatchingRooms")
        val messagesRef = countryMatchingChatRoomRef.child(chatRoomId).child("messages")
        if(currentUserId != null) {
            usersRef.child(currentUserId).get().addOnSuccessListener { it ->
                filter = it.child("filter").value as Boolean
                messagesRef.addValueEventListener(object : ValueEventListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val newMessages = snapshot.children.mapNotNull { msgSnapshot ->
                            val messageId = msgSnapshot.child("messageId").getValue(String::class.java)
                            val senderId = msgSnapshot.child("senderId").getValue(String::class.java)
                            val receiverId = msgSnapshot.child("receiverId").getValue(String::class.java)

                            // Check if the current user is the sender or receiver
                            if (senderId == currentUserId || receiverId == currentUserId) {
                                val encryptedMessage = msgSnapshot.child("content").getValue(String::class.java)
                                val type = msgSnapshot.child("type").getValue(String::class.java)
                                val encryptedKey = msgSnapshot.child("encryptKey").getValue(String::class.java)
                                val timestamp = msgSnapshot.child("timestamp").getValue(Long::class.java)

                                // Decrypt the message
                                var decryptedMessage = encryptedMessage?.let {
                                    encryptedKey?.let { key ->
                                        EncryptionUtils.decrypt(it, EncryptionUtils.getKeyFromString(key))
                                    }
                                }

                                // Apply the filter for bad words if needed
                                if (filter) {
                                    decryptedMessage = badwords.filterBadWords(decryptedMessage)
                                }

                                messageId?.let {
                                    senderId?.let { sid ->
                                        receiverId?.let { rid ->
                                            decryptedMessage?.let { msg ->
                                                timestamp?.let { time ->
                                                    type?.let { it1 ->
                                                        Message(it, sid, rid, msg,
                                                            it1, encryptedKey ?: "", time)
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
                        newMessages.let {messageList?.addAll(it) }

                        messageAdapter.notifyDataSetChanged()
                        messageRecyclerView.scrollToPosition(messageList!!.size - 1)

                        if (newMessages.isNotEmpty()) {
                            val lastMessage = newMessages.last()
                            if (lastMessage.senderId != currentUserId) {
                                lastMessage.content?.let { showNotification("Bạn có tin nhắn mới", it) }
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
            "",
            System.currentTimeMillis()
        )

        messageList?.add(systemMessage)
        messageAdapter.notifyDataSetChanged()

        messageRecyclerView.scrollToPosition(messageList!!.size - 1)
    }

    private fun showNotification(title: String, content: String) {
        val notificationService = NotificationService()
        notificationService.showNotification(this, title, content)
    }

    private fun sendMessage(
        chatRoomId: String,
        senderId: String,
        receiverId: String,
        text: String
    ) {
        val secretKey = EncryptionUtils.generateKey()
        val encryptedMessage = EncryptionUtils.encrypt(text, secretKey)
        val encryptedKey = EncryptionUtils.getKeyAsString(secretKey)

        checkChatRoomStatus(chatRoomId) { isChatRoomEnded ->
            if (!isChatRoomEnded) {
                val timestamp = System.currentTimeMillis()
                val messageId = UUID.randomUUID().toString()
                val message = Message(messageId, senderId, receiverId, encryptedMessage, "text",  encryptedKey, timestamp)
                countryMatchingChatRoomRef = FirebaseDatabase.getInstance().getReference("CountryMatchingRooms").child(chatRoomId)
                countryMatchingChatRoomRef.child("messages").push().setValue(message)
            } else {
                Toast.makeText(this, "Bạn cần tìm người chat trước!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun checkChatRoomStatus(chatRoomId: String, callback: (Boolean) -> Unit) {
        if (chatRoomId.isNotEmpty()) {
            val countryMatchingChatRoomRef = FirebaseDatabase.getInstance().getReference("CountryMatchingRooms").child(chatRoomId)
            countryMatchingChatRoomRef.child("status").addListenerForSingleValueEvent(object :
                ValueEventListener {
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
        var chatRoomValue: String
        if (currentUserId != null) {
            val userRef = usersRef.child(currentUserId)

            userRef.child("countryMatchingRoom").addValueEventListener(object : ValueEventListener {
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
        val countryMatchingcountryMatchingChatRoomRef = FirebaseDatabase.getInstance().getReference("CountryMatchingRooms").child(chatRoomId)

        countryMatchingcountryMatchingChatRoomRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user1Id = snapshot.child("user1Id").getValue(String::class.java)
                val user2Id = snapshot.child("user2Id").getValue(String::class.java)
                if (user1Id != "" && user2Id != "" && !user1Id.isNullOrEmpty() && !user2Id.isNullOrEmpty())
                    receiverId = if (user1Id == currentUserId) user2Id else user1Id

            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun endChat(chatRoomId: String, callback: (Boolean) -> Unit) {
        sendMessage(
            this.chatRoomId,
            "system",
            currentUserId.toString(),
            "Cuộc trò chuyện đã kết thúc!"
        )
        sendMessage(
            this.chatRoomId,
            "system",
            receiverId,
            "Cuộc trò chuyện đã kết thúc!"
        )
        if (chatRoomId.isNotEmpty()) {
            val countryMatchingChatRoomRef = FirebaseDatabase.getInstance().getReference("CountryMatchingRooms").child(chatRoomId)
            countryMatchingChatRoomRef.child("status").setValue("ended")
                .addOnCompleteListener { task ->
                    callback.invoke(task.isSuccessful)
                }
        }
    }

    private fun removeMessage(chatRoomId: String) {
        val countryMatchingChatRoomRef = FirebaseDatabase.getInstance().getReference("CountryMatchingRooms").child(chatRoomId)
        countryMatchingChatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersRef.child(currentUserId.toString()).child("CountryMatchingRoom").setValue("")
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

        val reportRef = FirebaseDatabase.getInstance().getReference("reports").child(chatRoomId).child(message.messageId!!)
        reportRef.setValue(report)

        reportRef.child("status").setValue("doing")
        reportRef.child("messageMap").setValue(messageMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Report added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error adding report: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun getSessionId(): String? {
        val sharedPref = getSharedPreferences("PreSession2", Context.MODE_PRIVATE)
        return sharedPref.getString("sessionID2", null)
    }
    private fun checkSession() {
        val sessionId = getSessionId()
        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        val user = auth.currentUser
        user?.let {
            databaseReferences.child(it.uid).child("session")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentSessionID = snapshot.value as String?
                        if(sessionId != currentSessionID){
                            showConfirmationDialog()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }
    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Thông báo!")
        builder.setMessage("Tài khoản này đang được đăng nhập ở thiết bị khác, vui lòng đăng nhập lại!")

        builder.setPositiveButton("OK") { _: DialogInterface, _: Int ->
            signOutAndStartSignInActivity()
            handleLogout()
            finish()
        }


        builder.show()
    }
    private fun signOutAndStartSignInActivity() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))


    }
    private fun handleLogout() {
        // Tạo Intent để chuyển hướng đến LoginActivity và xóa toàn bộ Activity đã mở trước đó
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // Kết thúc Activity hiện tại
        finish()
    }
}