package com.example.AnonymousChat.authentication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.AnonymousChat.databinding.ActivityNotificationBinding
import com.example.AnonymousChat.report.ReportedMessagesAdapter
import com.example.AnonymousChat.report.ReportsCustom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationActivity : AppCompatActivity() {
    private lateinit var  binding: ActivityNotificationBinding
    private  lateinit var databaseReferences: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReportedMessagesAdapter
    val layoutManager = GridLayoutManager(this, 2)

    private val reportedMessagesList = mutableListOf<ReportsCustom>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        recyclerView = binding.recyclerView
        recyclerView.layoutManager = layoutManager
        adapter = ReportedMessagesAdapter(reportedMessagesList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        binding.btnBack.setOnClickListener{
            onBackPressed()
        }

        // Initialize Firebase Realtime Database reference
        val reportsRef = FirebaseDatabase.getInstance().getReference("reports")

        reportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                reportedMessagesList.clear()
                for (roomSnapshot in dataSnapshot.children) {
                    val roomID = roomSnapshot.key
                    for (messageSnapshot in roomSnapshot.children) {
                        val messageID = messageSnapshot.key
                        val reportedByUid = messageSnapshot.child("uid_report").value as String?
                        val reportedUserUid = messageSnapshot.child("uid_beReported").value as String?
                        val status = messageSnapshot.child("status").value as String?

                        val messageMapSnapshot = messageSnapshot.child("messageMap")
                        val messageMap = messageMapSnapshot.getValue() as Map<String, Any?>
                        // Sử dụng dữ liệu messageMap ở đây
                        val timestamp = messageMap["timestamp"] as Long
                        val content = messageMap["content"] as String

                        // Kiểm tra xem UID có bằng với UIDfinding không
                        if (reportedUserUid == auth.currentUser?.uid!!) {
                            if (status == "reported") {
                                val reportedMessage = ReportsCustom(reportedByUid!!, reportedUserUid, content, timestamp)
                                reportedMessagesList.add(reportedMessage)
                                if (status == "restored") {
                                    // Xử lý khi status là "restored"
                                }
                            }
                        }

                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
            }
        })
    }
}