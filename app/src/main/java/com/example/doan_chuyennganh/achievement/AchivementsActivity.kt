package com.example.doan_chuyennganh.achievement

import AchievementAdapter
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan_chuyennganh.databinding.ActivityAchivementsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.doan_chuyennganh.achievement.Achievements
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
class AchievementsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAchivementsBinding
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchivementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.child(userID).child("achievements").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val achievements = arrayListOf<Achievements>()
                val countChats = snapshot.child("countChats").getValue(Int::class.java) ?: 0
                val countLogins = snapshot.child("countLogin").getValue(Int::class.java) ?: 0

                achievements.add(Achievements("10 lần chat", "Hoàn thành 10 lần chat", 10, countChats))
                achievements.add(Achievements("10 lần login", "Hoàn thành 10 lần đăng nhập", 10, countLogins))
                // Thêm các thành tựu khác...

                setupRecyclerView(achievements)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý lỗi
            }
        })
    }

    private fun setupRecyclerView(achievements: List<Achievements>) {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = AchievementAdapter(achievements) { achievement ->
            handleAchievementClick(achievement)
        }
    }

    private fun handleAchievementClick(achievement: Achievements) {
        if (achievement.currentCount >= achievement.goal && !achievement.isRewardClaimed) {
            awardUserWithCoins(achievement)
            achievement.isRewardClaimed = true
            binding.recyclerView.adapter?.notifyDataSetChanged()
        } else if (achievement.isRewardClaimed) {
            Toast.makeText(this, "Bạn đã nhận thưởng cho thành tựu này!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Thành tựu chưa hoàn thành!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun awardUserWithCoins(achievement: Achievements) {
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val coinsRef = FirebaseDatabase.getInstance().getReference("users/$userID/coins")

        coinsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentCoins = mutableData.getValue(Int::class.java) ?: 0
                mutableData.value = currentCoins + calculateRewardCoins(achievement)
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                if (committed) {
                    Toast.makeText(this@AchievementsActivity, "Phần thưởng đã được cập nhật!", Toast.LENGTH_SHORT).show()
                } else {
                    databaseError?.let {
                        Log.e("Firebase", "Cập nhật coin thất bại", it.toException())
                        Toast.makeText(this@AchievementsActivity, "Cập nhật coin thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun calculateRewardCoins(achievement: Achievements): Int {
        // Tính toán số coin thưởng (ví dụ: mỗi thành tựu đạt được thưởng 50 coins)
        return 50
    }
}
