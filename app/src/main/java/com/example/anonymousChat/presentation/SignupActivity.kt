package com.example.anonymousChat.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.example.anonymousChat.model.User
import com.example.anonymousChat.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import org.mindrot.jbcrypt.BCrypt
import java.util.regex.Pattern

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
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnSignup.setOnClickListener{
            val email = binding.edtEmail.text.toString()
            val password = binding.edtPassword.text.toString()
            checkEmailExists(email,password)
        }

        binding.textLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        val focusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.alpha = 1.0f
            } else {
                v.alpha = 0.5f
            }
        }
        binding.edtEmail.setOnFocusChangeListener(focusChangeListener)
        binding.edtPassword.setOnFocusChangeListener(focusChangeListener)
        binding.edtConfirmPassword.setOnFocusChangeListener(focusChangeListener)
        binding.btnSignup.setOnFocusChangeListener(focusChangeListener)

    }
    private fun signUp(email: String?, password: String?) {
        if (checkFields()) {
            auth.createUserWithEmailAndPassword(email!!, password!!).addOnCompleteListener {
                if (it.isSuccessful) {
                    val databaseReference = databaseReferences.database.reference.child("users")
                        .child(auth.currentUser!!.uid)
                    val users =
                        User(auth.currentUser!!.uid, email, "", "", false, null, false, true,100)
                    databaseReference.setValue(users).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()
                            auth.currentUser!!.sendEmailVerification()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(this, "Vui lòng xác thực email để đăng nhập!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            startActivity(Intent(this, LoginActivity::class.java))
                            auth.signOut()
                            finish()


                        } else {
                            Toast.makeText(this, "đã có lỗi khi tạo tài khoản!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun checkFields(): Boolean {
        val regex = Pattern.compile("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[a-zA-Z]).{8,}\$")
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
            if(!regex.matcher(signupPassword).matches()
            ){
                Toast.makeText(this,"Mật khẩu phải dài ít nhất 8 ký tự (chứa chữ hoa, chữ thường và  ký tự đặc biệt)", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        else{
            Toast.makeText(this,"All fields are mandatory", Toast.LENGTH_SHORT).show()
            return false
        }
        return  true
    }

    private fun checkEmailExists(email: String, password: String) {
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signinMethods = task.result?.signInMethods
                    if (!signinMethods.isNullOrEmpty()) {
                        Toast.makeText(this, "Email đã được sử dụng!!", Toast.LENGTH_SHORT).show()
                    } else {
                        signUp(email, password)
                    }
                }
            }
    }
    fun hashPassword(password: String): String {
        val salt = BCrypt.gensalt()
        return BCrypt.hashpw(password, salt)
    }

    fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }
}
