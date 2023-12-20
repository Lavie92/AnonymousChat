package com.example.doan_chuyennganh.authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan_chuyennganh.MainActivity
import com.example.doan_chuyennganh.ProfileActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.adapter.TransactionsAdapter
import com.example.doan_chuyennganh.databinding.ActivityNotificationBinding
import com.example.doan_chuyennganh.databinding.ActivityPayHistoryBinding
import com.example.doan_chuyennganh.databinding.ActivityPaymentsBinding
import com.example.doan_chuyennganh.exchanges.transaction
import com.example.doan_chuyennganh.report.ReportedMessagesAdapter
import com.example.doan_chuyennganh.report.ReportsCustom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PayHistoryActivity : AppCompatActivity() {
    private lateinit var  binding: ActivityPayHistoryBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionsAdapter
    val layoutManager = GridLayoutManager(this, 2)
    private val transactionList = mutableListOf<transaction>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        // Giả sử bạn có một danh sách các giao dịch

        recyclerView = binding.recyclerView
        recyclerView.layoutManager = layoutManager
        adapter = TransactionsAdapter(transactionList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val reportsRef = FirebaseDatabase.getInstance().getReference("transactions")

        reportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                transactionList.clear()
                for (transactionSnapshot in dataSnapshot.children) {
                    val transactionID = transactionSnapshot.key
                        val userID = transactionSnapshot.child("userId").value as String
                        val status = transactionSnapshot.child("status").value as String
                        val amount = transactionSnapshot.child("amount").value as Long
                        val completedTime = transactionSnapshot.child("completedAt").value as Long

                        // Kiểm tra xem UID có bằng với UIDfinding không
                        if (userID == auth.currentUser?.uid!!) {
                            if (status == "Success") {
                                val transaction = transaction(transactionID.toString(), userID, amount,status, completedTime)
                                transactionList.add(transaction)
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

    override fun onBackPressed() {
            startActivity(Intent(this, ProfileActivity::class.java))
            super.onBackPressed()

    }
}