package com.example.doan_chuyennganh

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.chat.ChatActivity
import com.example.doan_chuyennganh.databinding.ActivityMainBinding
import com.example.doan_chuyennganh.exchanges.PaymentsActivity
import com.example.doan_chuyennganh.layout.SplashScreenActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.values
import com.google.firebase.ktx.Firebase
import org.mindrot.jbcrypt.*
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private  lateinit var databaseReferences: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val auth = Firebase.auth
        val user = auth.currentUser

        binding.layoutRandomChat.setOnClickListener {
            val splashIntent = Intent(this@MainActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toChatRandom")
            startActivity(splashIntent)
        }
        binding.layoutNearestChat.setOnClickListener {
            val splashIntent = Intent(this@MainActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toChatNearest")
            startActivity(splashIntent)
        }

        binding.layoutAIChat.setOnClickListener{
            val splashIntent = Intent(this@MainActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toChatAI")
            startActivity(splashIntent)
        }
        binding.layoutCountryMatch.setOnClickListener{
            val splashIntent = Intent(this@MainActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toCountryMatch")
            startActivity(splashIntent)
        }
        binding.layoutVoiceChat.setOnClickListener{
            val splashIntent = Intent(this@MainActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toVoiceChat")
            startActivity(splashIntent)
        }
        binding.layoutTicTacToe.setOnClickListener{
            val splashIntent = Intent(this@MainActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toGame")
            startActivity(splashIntent)
        }




        this.auth = FirebaseAuth.getInstance()

        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        databaseReferences.child(auth.currentUser!!.uid).get().addOnSuccessListener {
            if(it.exists()){
                val userCoins = it.child("coins").value
                binding.txtCoins.text = userCoins.toString()

            }else{
                Toast.makeText(this,"Error!",Toast.LENGTH_SHORT).show()

            }

        }

        binding.paymentBox.setOnClickListener{
            startActivity(Intent(this@MainActivity, PaymentsActivity::class.java))
        }
        //Load Screen to check Active
        checkSession()
        checkActive()
        //



        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        if (user != null) {


        } else {
        }
        binding.btnSetting.setOnClickListener{
            startActivity(Intent(this, ProfileActivity::class.java))
        }



    }
    private fun updateUsersCoin(userId: String, coin: Int) {
        val userRef = FirebaseDatabase.getInstance().getReference("users/$userId")

        // Lấy thông tin hiện tại của người dùng
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Giả sử bạn có trường 'coins' trong object của người dùng
                val currentCoins = dataSnapshot.child("coins").getValue(Double::class.java) ?: 0.0
                val newCoinValue = currentCoins - coin

                // Cập nhật số dư mới
                userRef.child("coins").setValue(newCoinValue)
                    .addOnSuccessListener {
                    }
                    .addOnFailureListener {
                    }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    //Sessions check
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

    //end Sessions check



    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Optional: Finish the current activity to prevent going back to it
        }
    }
    fun findRandomUserForChat() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val context = this@MainActivity

        usersRef.orderByChild("available").equalTo(true).limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val matchedUser = dataSnapshot.children.first().getValue(User::class.java)

                        val chatRoomId = createChatRoom(currentUser?.uid, matchedUser?.id)
                        val intent = Intent(context, ChatActivity::class.java)
                        intent.putExtra("chatRoomId", chatRoomId)
                        startActivity(intent)
                    } else {
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                }
            })
    }
    override fun onBackPressed() {
        // Kiểm tra xem người dùng đang ở MainActivity hay không
        if (isTaskRoot) {
            // Nếu là MainActivity, tắt ứng dụng khi ấn nút Back lần 2
            if (backPressedOnce) {
                super.onBackPressed()
                return
            }

            backPressedOnce = true
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

            // Thiết lập một đồng hồ đếm để đặt lại trạng thái backPressedOnce sau một khoảng thời gian
            Handler().postDelayed({ backPressedOnce = false }, BACK_PRESS_INTERVAL.toLong())
        } else {
            // Nếu không phải MainActivity, thực hiện hành động mặc định khi ấn nút Back
            super.onBackPressed()
        }
    }
    fun createChatRoom(user1Id: String?, user2Id: String?): String {
        val chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms")

        val chatRoomId = chatRoomsRef.push().key
        val chatRoom = HashMap<String, Any>()
        chatRoomsRef.child(chatRoomId!!).setValue(chatRoom)

        return chatRoomId
    }





    fun checkPassword(plainPassword: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(plainPassword, hashedPassword)
    }
    private fun checkActive(){
        //mAuth = FirebaseAuth.getInstance()
        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        databaseReferences.child(auth.currentUser!!.uid).get().addOnSuccessListener {
            if(it.exists()){
                val active = it.child("active").value
                if(active == false){
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
            }else{
                Toast.makeText(this,"Error!",Toast.LENGTH_SHORT).show()

            }

        }

    }

    companion object {
        private const val BACK_PRESS_INTERVAL = 2000 // Thời gian giữa hai lần ấn nút Back để thoát (2 giây)
        private var backPressedOnce = false
    }
}