package com.example.doan_chuyennganh

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan_chuyennganh.authentication.User

import com.example.doan_chuyennganh.databinding.ActivityLoginBinding

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


public class LoginActivity : AppCompatActivity() {

    private lateinit var  binding: ActivityLoginBinding
    private lateinit var firebaseDatabases: FirebaseDatabase
    private lateinit var databaseReferences: DatabaseReference

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private lateinit var auth: FirebaseAuth
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }
    fun updateUI(account: FirebaseUser?) {
        if (account != null) {
            // User is signed in
            Toast.makeText(this, "Signed in!", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        firebaseDatabases = FirebaseDatabase.getInstance()
        databaseReferences = firebaseDatabases.reference.child("users")

        supportActionBar?.hide()
        binding.btnLogin.setOnClickListener{
            val loginEmail = binding.loginEmail.text.toString()
            val loginPassword = binding.loginPassword.text.toString()
            if(checkFields()){
                auth.signInWithEmailAndPassword(loginEmail,loginPassword).addOnCompleteListener() {
                    if(it.isSuccessful){
                        val user = auth.currentUser
                        updateUI(user)

                    }else{
                        Toast.makeText(this,"Wrong Email or Password!",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        //google
        binding.textSignup.setOnClickListener{
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // The user is already signed in, navigate to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish() // finish the current activity to prevent the user from coming back to the SignInActivity using the back button
        }




        val signInButton = findViewById<ImageView>(R.id.signInButton)
        signInButton.setOnClickListener {
            signIn()
        }




        //




    }

    //Google
    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Welcome back, ${user?.displayName}!", Toast.LENGTH_SHORT).show()
                    checkIfEmailExists(user?.email)


                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
    //End Google




    private fun checkFields(): Boolean {
        val signupEmail = binding.loginEmail.text.toString()
        val signupPassword = binding.loginPassword.text.toString()
        if(signupEmail.isNotEmpty() && signupPassword.isNotEmpty()){
            if(!isValidEmail(signupEmail)){
                Toast.makeText(this,"Email wrong format!", Toast.LENGTH_SHORT).show()
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

    private fun checkIfEmailExists(email: String?) {
        if (email != null) {
            val currentUser = auth.currentUser

            if (currentUser != null) {
                // Người dùng đã đăng nhập
                val databaseReference = databaseReferences.database.reference.child("users")

                databaseReference.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Email đã tồn tại trong cơ sở dữ liệu
                            } else {
                                // Email chưa tồn tại trong cơ sở dữ liệu
                                val users: User = User(currentUser.uid, currentUser.email,"", currentUser.displayName, null, false, null)
                                databaseReference.child(currentUser.uid).setValue(users)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(this@LoginActivity, "Welcome, ${currentUser.displayName}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(this@LoginActivity, "Failed to create user data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Toast.makeText(this@LoginActivity, "Error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            } else {

                Toast.makeText(this@LoginActivity, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }




    fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }
}