package com.example.anonymousChat.layout

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.example.anonymousChat.MainActivity
import com.example.anonymousChat.R
import com.example.anonymousChat.authentication.BannedActivity
import com.example.anonymousChat.chat.ChatActivity
import com.example.anonymousChat.chat.ChatNearestActivity
import com.example.anonymousChat.databinding.ActivityChatBinding
import com.example.anonymousChat.databinding.ActivitySplashScreenBinding
import com.example.anonymousChat.tictactoe.TictactoeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var databaseReferences: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySplashScreenBinding
    private val tips = listOf(
        "nếu thấy đối phương quá đáng yêu và tin tưởng, hãy thả tim <3",
        "ngoài chia sẻ thông tin, thả tim còn giúp người ấy tăng điểm uy tín và katcoin trong app",
        "katcoin là đơn vị tiền tệ trong app, có thể dùng để gửi ảnh",
        "nghiêm cấm các hành vi quấy rối, gạ chat ếch, nếu phát hiện sẽ bị ban vĩnh viễn",
        "nếu bạn thấy bị xúc phạm,hãy nhấn giữ tin nhắn đó và chọn \"report\"",
        "bạn có thể bật/tắt tuỳ chọn lọc ngôn ngữ trong cài đặt",
        "phòng chat sau 7 ngày không hoạt động sẽ bị xoá, hãy chat thường xuyên nhé",
        "nếu nút \"Lẹt Gô\" không hoạt động, hãy thử bấm \"Kết Thúc\" và thử lại nhé",
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val tip = tips.random()
        binding.tvTip.text = tip
        auth = FirebaseAuth.getInstance()
        Handler().postDelayed({
            val intent = intent
            if (intent != null) {
                val sourceActivity = intent.getStringExtra("source_activity")
                if (sourceActivity != null && sourceActivity == "toMain") {
                    databaseReferences = FirebaseDatabase.getInstance().getReference("users")
                    if (auth.currentUser != null) {
                        databaseReferences.child(auth.currentUser?.uid!!).get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    val point = snapshot.child("point").value
                                    if(point.toString().toInt() <= 0){
                                        startActivity(Intent(this@SplashScreenActivity,BannedActivity::class.java))
                                        finish()
                                    }else{
                                        val loginIntent = Intent(this@SplashScreenActivity, MainActivity::class.java)
                                        startActivity(loginIntent)
                                        finish()
                                    }
                                }
                            }
                    }


                } else if (sourceActivity != null && sourceActivity == "toChatRandom") {

                    val chatIntent = Intent(this@SplashScreenActivity, ChatActivity::class.java)
                    startActivity(chatIntent)
                    finish()
                }  else if (sourceActivity != null && sourceActivity == "toChatNearest") {

                    val chatIntent = Intent(this@SplashScreenActivity, ChatNearestActivity::class.java)
                    startActivity(chatIntent)
                    finish()
                }else if (sourceActivity != null && sourceActivity == "toGame") {

                    val chatIntent = Intent(this@SplashScreenActivity, TictactoeActivity::class.java)
                    startActivity(chatIntent)
                    finish()
                }
            }
        }, 2000)
    }




}