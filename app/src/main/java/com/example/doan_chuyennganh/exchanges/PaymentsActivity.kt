package com.example.doan_chuyennganh.exchanges

import PaymentMethodDialogFragment
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan_chuyennganh.MainActivity
import com.example.doan_chuyennganh.databinding.ActivityPaymentsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import vn.zalopay.sdk.Environment
import vn.zalopay.sdk.ZaloPayError
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener
import java.util.UUID


class PaymentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentsBinding
    private val currentUser = FirebaseAuth.getInstance().currentUser


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ZaloPay SDK Init

        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        ZaloPaySDK.init(2554, Environment.SANDBOX)



        binding.pay10k.setOnClickListener{
            PaymentMethodDialogFragment("10000", 10).show(supportFragmentManager, "paymentMethodDialog")
        }
        binding.pay20k.setOnClickListener{
            PaymentMethodDialogFragment("20000", 20).show(supportFragmentManager, "paymentMethodDialog")
        }
        binding.pay50k.setOnClickListener{
            PaymentMethodDialogFragment("50000", 50).show(supportFragmentManager, "paymentMethodDialog")
        }
        binding.pay100k.setOnClickListener{
            PaymentMethodDialogFragment("100000", 100).show(supportFragmentManager, "paymentMethodDialog")
        }

    }

    // Hàm thực hiện tạo order và xử lý kết quả
    fun processOrder(method: String, amount: String, coin: Int) {
        when (method) {
            "ZaloPay" -> {
                // Tạo transactionId
                val transactionId = generateTransactionId()

                // Tạo đơn hàng với trạng thái 'pending'
                createPendingOrder(transactionId, amount)

                // Tiếp tục với quá trình thanh toán
                val orderApi = CreateOrderZalo()
                try {
                    val data = orderApi.createOrder(amount)
                    val code = data.getString("return_code")
                    if (code == "1") {
                        val token = data.getString("zp_trans_token")

                        // Gọi hàm thanh toán của ZaloPay SDK
                        payWithZaloPay(token, amount, coin, transactionId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createPendingOrder(transactionId: String, amount: String) {
        val transaction = Transactions(
            transactionId = transactionId,
            userId = currentUser!!.uid,
            amount = amount.toLong(),
            transactionType = "ZaloPay",
            status = "Pending",
            createdAt = System.currentTimeMillis()
        )
        saveTransactionToDatabase(transaction)
    }



    private fun payWithZaloPay(token: String, amount: String, coin: Int, transactionId: String) {
        updateOrderStatus(transactionId, "Success")
        updateUsersCoin(currentUser!!.uid, coin)
        ZaloPaySDK.getInstance()
            .payOrder(this@PaymentsActivity, token, "demozpdk://app", object : PayOrderListener {
                override fun onPaymentSucceeded(s: String, transToken: String, appTransID: String) {
                }

                override fun onPaymentCanceled(zpTransToken: String, appTransID: String) {
                    showAlertDialog("User Cancel Payment", "zpTransToken: $zpTransToken")
                }

                override fun onPaymentError(zaloPayError: ZaloPayError, zpTransToken: String, appTransID: String) {
                    showAlertDialog("Payment Fail", "ZaloPayErrorCode: ${zaloPayError.toString()} \nTransToken: $zpTransToken")
                }
            })
    }

    private fun showAlertDialog(title: String, message: String) {
        runOnUiThread {
            AlertDialog.Builder(this@PaymentsActivity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateOrderStatus(transactionId: String, status: String) {
        val orderRef = FirebaseDatabase.getInstance().getReference("transactions/$transactionId")
        orderRef.child("status").setValue(status)
        orderRef.child("completedAt").setValue(System.currentTimeMillis())
    }


    private fun saveTransactionToDatabase(transaction: Transactions) {
        if (transaction.transactionId.isBlank()) {
            Toast.makeText(this, "Transaction ID không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        val ref = FirebaseDatabase.getInstance().getReference("transactions")
        ref.child(transaction.transactionId).setValue(transaction)
            .addOnSuccessListener {
            }
            .addOnFailureListener {
            }
    }


    private fun updateUsersCoin(userId: String, coin: Int) {
        val userRef = FirebaseDatabase.getInstance().getReference("users/$userId")

        // Lấy thông tin hiện tại của người dùng
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Giả sử bạn có trường 'coins' trong object của người dùng
                val currentCoins = dataSnapshot.child("coins").getValue(Double::class.java) ?: 0.0
                val newCoinValue = currentCoins + coin

                // Cập nhật số dư mới
                userRef.child("coins").setValue(newCoinValue)
                    .addOnSuccessListener {
                    }
                    .addOnFailureListener {
                    }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        ZaloPaySDK.getInstance().onResult(intent)
    }
    fun generateTransactionId(): String {
        return UUID.randomUUID().toString()
    }
    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java))
        super.onBackPressed()

    }
}