package com.example.doan_chuyennganh

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan_chuyennganh.databinding.ActivityProfileBinding
import com.google.firebase.FirebaseError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await


class ProfileActivity : AppCompatActivity() {
    private lateinit var  binding: ActivityProfileBinding
    private  lateinit var firebaseDatabase: FirebaseDatabase
    private  lateinit var databaseReferences: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()


        val userId = auth.currentUser?.uid
        val currentUser = auth.currentUser


        //Read data
        readData(userId)
        //

        binding.btnSave.setOnClickListener{
            val username = binding.txtChangeName.text.toString()
            val age = binding.txtChangeAge.text.toString()
            val gender = binding.genderSpinner.selectedItem.toString()
            if (userId != null) {
                if (currentUser != null) {
                    updateData(currentUser,username,age,gender)
                }
            } else {
                Toast.makeText(this,"No user signed in!",Toast.LENGTH_SHORT).show()
            }

        }
    }


    private fun updateData(user: FirebaseUser,username: String, age: String, gender: String) {
        val userId = user.uid

        val profileUpdates = userProfileChangeRequest {
            displayName = username
        }


        val userUpdate = mapOf(
            "username" to username,
            "age" to age,
            "gender" to gender,
            "active" to true
        )

        if(checkValue()){
            user!!.updateProfile(profileUpdates)
            databaseReferences.child(userId).updateChildren(userUpdate)
                .addOnSuccessListener {
                    Toast.makeText(this, "User data updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

    }

    private fun readData(id:String?){
        val spinner = binding.genderSpinner

        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        if (id != null) {
            databaseReferences.child(id).get().addOnSuccessListener {
                if(it.exists()){
                    //get value
                    val username = it.child("username").value
                    val email = it.child("email").value
                    val gender = it.child("gender").value
                    val active = it.child("active").value
                    val age = it.child("age").value
                    //end get value

                    //binding
                    binding.txtChangeName.setText(username.toString())
                    binding.txtChangeEmail.setText(email.toString())
                    val position = getIndexFromValue(spinner, gender.toString())
                    spinner.setSelection(position)
                    binding.txtChangeAge.setText(age.toString())
                    //end binding

                    //binding.switchFilter.
                }else{
                    startActivity(Intent(this, LoginActivity::class.java))
                    Toast.makeText(this,"No user Signed in!",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun getIndexFromValue(spinner: Spinner, value: String?): Int {
        val adapter = spinner.adapter as ArrayAdapter<String>
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i) == value) {
                return i
            }
        }
        return 0
    }



    private suspend fun checkUsernameExistence(username: String): Boolean {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        return try {
            val dataSnapshot = databaseReference.orderByChild("username").equalTo(username).get().await()
            dataSnapshot.exists()
        } catch (e: Exception) {

            false
        }
    }



    private fun checkValue() :Boolean{
        val username = binding.txtChangeName.text.toString()
        val age = binding.txtChangeAge.text.toString()

        if (username.isNullOrBlank() || username.length < 3) {
            showError(binding.txtChangeName, "Username must be at least 3 characters")
            return false
        } else if (age.isNullOrBlank() || age.toIntOrNull() == null || age.toInt() < 0) {
            showError(binding.txtChangeAge, "Age is not valid!")
            return false
        } else {
            val usernameExists = runBlocking { checkUsernameExistence(username) }

            return if (usernameExists) {
                showError(binding.txtChangeName, "Username is already taken")
                false
            } else {
                true
            }
        }
    }




    private fun showError(input :EditText, err: String){
        input.error = err
        input.requestFocus()
    }


}