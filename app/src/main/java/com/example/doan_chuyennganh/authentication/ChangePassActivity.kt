package com.example.doan_chuyennganh.authentication

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.databinding.ActivityChangePassBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ChangePassActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangePassBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.btnConfirm.setOnClickListener{
            val oldPass = binding.oldPass.text.toString()
            val newPass = binding.newPass.text.toString()
            val confirmPass = binding.confirmPass.text.toString()

            if(checkPass()){
                changePass(oldPass,newPass)
            }
        }
        binding.btnBack.setOnClickListener{
            onBackPressed()
        }

    }

    private fun changePass(oldpassword:String?,password:String?){
        val user = Firebase.auth.currentUser!!
        val credential = EmailAuthProvider
            .getCredential(user.email!!, oldpassword!!)

// Prompt the user to re-provide their sign-in credentials
        user.reauthenticate(credential)
            .addOnSuccessListener{
                if (password != null) {
                    user!!.updatePassword(password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Password Updated!!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, SettingActivity::class.java))
                            }
                        }
                }
            }.addOnFailureListener{
                binding.oldPass.error = "Wrong Password!"
            }

    }
    private fun checkPass() : Boolean{
        var check = false
        val oldPass = binding.oldPass.text.toString()
        val newPass = binding.newPass.text.toString()
        val confirmPass = binding.confirmPass.text.toString()
        if(binding.oldPass.text.toString().length < 6){
            binding.oldPass.error = "Password must least 6 characters"
        }
        if(binding.newPass.text.toString().length < 6){
            binding.newPass.error = "Password must least 6 characters"
        }
        if(binding.confirmPass.text.toString().length<6){
            binding.confirmPass.error = "Password must least 6 characters"
        }
        else if(newPass != confirmPass){
            binding.confirmPass.error = "Confirm Password is not match New Password!"

        }
        else if(newPass == oldPass){
            binding.newPass.error = "New Password must not be Old Password!"
        }
        else{
            check = true
        }
        return check
    }


}