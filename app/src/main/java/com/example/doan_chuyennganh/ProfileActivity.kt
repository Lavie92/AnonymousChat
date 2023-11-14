package com.example.doan_chuyennganh

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.doan_chuyennganh.databinding.ActivityProfileBinding
import com.example.doan_chuyennganh.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {
    private lateinit var  binding: ActivityProfileBinding
    private  lateinit var firebaseDatabase: FirebaseDatabase
    private  lateinit var databaseReferences: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_profile)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        binding.btnSave.setOnClickListener{
            Toast.makeText(this,"No user signed in!",Toast.LENGTH_SHORT).show()
            val username = binding.txtUsername.text.toString()
            val age = binding.txtAge.text.toString()
            val gender = binding.txtGender.text.toString()
            if (userId != null) {
                updateData(userId,username,age,gender)
            } else {
                Toast.makeText(this,"No user signed in!",Toast.LENGTH_SHORT).show()
            }

        }
    }
    private fun updateData(id: String,username: String, age: String, gender: String){
        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        val user = mapOf(
            "username" to username,
            "age" to age,
            "gender" to gender,
            "active" to true
        )
        databaseReferences.child(id).updateChildren(user).addOnSuccessListener {
            Toast.makeText(this,"Saved!",Toast.LENGTH_SHORT).show()
        }.addOnFailureListener{
            Toast.makeText(this,"Failed!",Toast.LENGTH_SHORT).show()
        }
    }
}