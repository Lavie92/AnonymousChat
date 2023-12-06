package com.example.doan_chuyennganh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.os.Handler
import android.widget.ImageView
import android.widget.Toast
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.chat.ChatActivity
import com.example.doan_chuyennganh.databinding.ActivityMainBinding
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import org.mindrot.jbcrypt.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private  lateinit var firebaseDatabase: FirebaseDatabase
    private  lateinit var databaseReferences: DatabaseReference

    private lateinit var auth: FirebaseAuth
    private  lateinit var firebaseDatabase: FirebaseDatabase
    private  lateinit var databaseReferences: DatabaseReference
    private var ivStartChat: ImageView? = null
    private var ivFindNearUser: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userID = Firebase.auth.currentUser
        userID?.let{
            val uid = it.uid
        }
        mAuth = FirebaseAuth.getInstance()

        //Load Screen to check Active
        checkActive()
        //
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)



        val textView = findViewById<TextView>(R.id.name)

        val auth = Firebase.auth
        val user = auth.currentUser
        ivStartChat = binding.ivStartChat
        ivFindNearUser = binding.ivFindNearUser
        ivStartChat?.setOnClickListener {
            val splashIntent = Intent(this@MainActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toChat")
            startActivity(splashIntent)
        }

        this.auth = FirebaseAuth.getInstance()


        //Load Screen to check Active
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
        binding.btnSetting.setOnClickListener{
            startActivity(Intent(this, ProfileActivity::class.java))
        }



    }
// Inside onCreate() method
        val signout = findViewById<Button>(R.id.logout_button)
        signout.setOnClickListener {
            FirebaseAuth.getInstance().signOut();
            Firebase.auth.signOut()
            signOutAndStartSignInActivity()
        }

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
    private fun signOutAndStartSignInActivity() {
        mAuth.signOut()
        FirebaseAuth.getInstance().signOut();
        Firebase.auth.signOut()
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            // Optional: Update UI or show a message to the user
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    fun createChatRoom(user1Id: String?, user2Id: String?): String {
        val chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms")

        val chatRoomId = chatRoomsRef.push().key
        val chatRoom = HashMap<String, Any>()
        chatRoomsRef.child(chatRoomId!!).setValue(chatRoom)

        return chatRoomId
    }

    private fun checkActive(){
        //mAuth = FirebaseAuth.getInstance()
        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        databaseReferences.child(mAuth.currentUser!!.uid).get().addOnSuccessListener {
            if(it.exists()){
                val active = it.child("active").value
                if(active == false){
                    Toast.makeText(this,"Please complete some Information!",Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
            }else{
                Toast.makeText(this,"Error!",Toast.LENGTH_SHORT).show()

            }

        }

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