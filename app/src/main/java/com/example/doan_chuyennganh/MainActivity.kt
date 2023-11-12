package com.example.doan_chuyennganh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.doan_chuyennganh.authentication.LoginActivity
import com.example.doan_chuyennganh.authentication.UserData
import com.example.doan_chuyennganh.chat.ChatActivity
import com.example.doan_chuyennganh.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private var ivStartChat: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val textView = findViewById<TextView>(R.id.name)

        val auth = Firebase.auth
        val user = auth.currentUser
        ivStartChat = binding.ivStartChat
        ivStartChat?.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }
        mAuth = FirebaseAuth.getInstance()


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)





        if (user != null) {
            val userName = user.displayName
            textView.text = "Welcome, " + userName
        } else {
            // Handle the case where the user is not signed in
        }


// Inside onCreate() method
        val signout = findViewById<Button>(R.id.logout_button)
        signout.setOnClickListener {
            signOutAndStartSignInActivity()
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
                        val matchedUser = dataSnapshot.children.first().getValue(UserData::class.java)

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

    fun createChatRoom(user1Id: String?, user2Id: String?): String {
        val chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms")

        val chatRoomId = chatRoomsRef.push().key
        val chatRoom = HashMap<String, Any>()
        chatRoomsRef.child(chatRoomId!!).setValue(chatRoom)

        return chatRoomId
    }


    private fun signOutAndStartSignInActivity() {
        mAuth.signOut()
        Firebase.auth.signOut()
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            // Optional: Update UI or show a message to the user
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}