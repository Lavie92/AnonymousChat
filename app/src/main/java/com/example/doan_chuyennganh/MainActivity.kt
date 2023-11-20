package com.example.doan_chuyennganh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.chat.AgeActivity
import com.example.doan_chuyennganh.chat.ChatActivity
import com.example.doan_chuyennganh.chat.FillActivity
import com.example.doan_chuyennganh.chat.GameActivity
import com.example.doan_chuyennganh.chat.GenderActivity
import com.example.doan_chuyennganh.chat.LoveActivity
import com.example.doan_chuyennganh.chat.MapActivity
import com.example.doan_chuyennganh.chat.StudyActivity
import com.example.doan_chuyennganh.databinding.ActivityMainBinding
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
import com.google.firebase.ktx.Firebase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private  lateinit var firebaseDatabase: FirebaseDatabase
    private  lateinit var databaseReferences: DatabaseReference
    private var ivStartChat: ImageView? = null
    private var rectangle4: ImageView? = null
    private var imgGameChanel: ImageView? = null
    private var imgStudyChanel: ImageView? = null
    private var imgAgeChanel: ImageView? = null
    private var imgGenderChanel: ImageView? = null
    private var imgMapChanel: ImageView? = null
    private var imgLoveChanel: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val textView = findViewById<TextView>(R.id.name)

        val auth = Firebase.auth
        val user = auth.currentUser
        imgLoveChanel = binding.imgLoveChanel
        imgLoveChanel?.setOnClickListener {
            // Thay đổi Intent từ ChatActivity sang FillActivity
            val intent = Intent(this, LoveActivity::class.java)
            startActivity(intent)
        }
        imgMapChanel = binding.imgMapChanel
        imgMapChanel?.setOnClickListener {
            // Thay đổi Intent từ ChatActivity sang FillActivity
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
        imgGenderChanel = binding.imgGenderChanel
        imgGenderChanel?.setOnClickListener {
            // Thay đổi Intent từ ChatActivity sang FillActivity
            val intent = Intent(this, GenderActivity::class.java)
            startActivity(intent)
        }
        imgAgeChanel = binding.imgAgeChanel
        imgAgeChanel?.setOnClickListener {
            // Thay đổi Intent từ ChatActivity sang FillActivity
            val intent = Intent(this, AgeActivity::class.java)
            startActivity(intent)
        }
        imgStudyChanel = binding.imgStudyChanel
        imgStudyChanel?.setOnClickListener {
            // Thay đổi Intent từ ChatActivity sang FillActivity
            val intent = Intent(this, StudyActivity::class.java)
            startActivity(intent)
        }
        imgGameChanel = binding.imgGameChanel
        imgGameChanel?.setOnClickListener {
            // Thay đổi Intent từ ChatActivity sang FillActivity
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
        rectangle4 = binding.rectangle4
        rectangle4?.setOnClickListener {
            // Thay đổi Intent từ ChatActivity sang FillActivity
            val intent = Intent(this, FillActivity::class.java)
            startActivity(intent)
        }
        ivStartChat = binding.ivStartChat
        ivStartChat?.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }
        this.auth = FirebaseAuth.getInstance()



        //Load Screen to check Active
        checkActive()
        //

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        if (user != null) {
            val userName = user.displayName
            textView.text = "Welcome, " + userName
        } else {
            // Handle the case where the user is not signed in
        }
        binding.btnSetting.setOnClickListener{
            startActivity(Intent(this, ProfileActivity::class.java))
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

    fun createChatRoom(user1Id: String?, user2Id: String?): String {
        val chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms")

        val chatRoomId = chatRoomsRef.push().key
        val chatRoom = HashMap<String, Any>()
        chatRoomsRef.child(chatRoomId!!).setValue(chatRoom)

        return chatRoomId
    }


    private fun signOutAndStartSignInActivity() {
        auth.signOut()
        FirebaseAuth.getInstance().signOut();
        Firebase.auth.signOut()
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            // Optional: Update UI or show a message to the user
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkActive(){
        //mAuth = FirebaseAuth.getInstance()
        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        databaseReferences.child(auth.currentUser!!.uid).get().addOnSuccessListener {
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

}