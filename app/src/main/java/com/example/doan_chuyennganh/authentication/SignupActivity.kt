package com.example.doan_chuyennganh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.databinding.ActivityForgotPassBinding
import com.example.doan_chuyennganh.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {
    private lateinit var  binding: ActivitySignupBinding
    private  lateinit var firebaseDatabase: FirebaseDatabase
    private  lateinit var databaseReferences: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReferences = firebaseDatabase.reference.child("users")
        supportActionBar?.hide()
        //check User
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnSignup.setOnClickListener{
            val email = binding.edtEmail.text.toString()
            val password = binding.edtPassword.text.toString()

            if(checkFields()){
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
                    if(it.isSuccessful){
                        //
                        val databaseReference = databaseReferences.database.reference.child("users").child(auth.currentUser!!.uid)
                        val users : User = User(auth.currentUser!!.uid,email,null,null,"",false,null,false)
                        databaseReference.setValue(users).addOnCompleteListener{
                            if(it.isSuccessful){
                                auth.signOut()
                                Toast.makeText(this,"Account Created!",Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                            }else{
                                Toast.makeText(this,"Something is wrong!",Toast.LENGTH_SHORT).show()
                            }

                        }
                        //


                    }else{
                        Log.e("error: ",it.exception.toString())
                    }
                }
            }



        }

        binding.textLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }



    }

    private fun checkFields(): Boolean {
        val signupEmail = binding.edtEmail.text.toString()
        val signupPassword = binding.edtPassword.text.toString()
        val signupConfirm = binding.edtConfirmPassword.text.toString()
        if(signupEmail.isNotEmpty() && signupPassword.isNotEmpty()){
            if(!isValidEmail(signupEmail)){
                Toast.makeText(this,"Email wrong format!", Toast.LENGTH_SHORT).show()
                return false

            }
            if(signupPassword != signupConfirm){
                Toast.makeText(this,"Password and Confirm Password are not the same!", Toast.LENGTH_SHORT).show()
                return false

            }
            if(signupPassword.length <=6){
                Toast.makeText(this,"Password must least 6 characters", Toast.LENGTH_SHORT).show()
                return false
            }
            else
                return true
            //signupUser(signupEmail,signupPassword)
        }
        else{
            return false
            Toast.makeText(this,"All fields are mandatory", Toast.LENGTH_SHORT).show()

        }
    }


    fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }
}
