package com.example.doan_chuyennganh.exchanges

import PaymentMethodDialogFragment
import android.app.AlertDialog
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import com.example.doan_chuyennganh.databinding.ActivityPaymentsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.OrderIntent
import com.paypal.checkout.createorder.UserAction
import com.paypal.checkout.order.Amount
import com.paypal.checkout.order.OrderRequest
import com.paypal.checkout.order.AppContext
import com.paypal.checkout.order.PurchaseUnit
import vn.zalopay.sdk.Environment
import vn.zalopay.sdk.ZaloPayError
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener
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
        ZaloPaySDK.init(2553, Environment.SANDBOX)

        //Paypal Init
        binding.paymentButtonContainer.setup(
            createOrder =
            CreateOrder { createOrderActions ->
                val order =
                    OrderRequest(
                        intent = OrderIntent.CAPTURE,
                        appContext = AppContext(userAction = UserAction.PAY_NOW),
                        purchaseUnitList =
                        listOf(
                            PurchaseUnit(
                                amount =
                                Amount(currencyCode = CurrencyCode.USD, value = "10.00")
                            )
                        )
                    )
                createOrderActions.create(order)
            },
            onApprove =
            OnApprove { approval ->
                approval.orderActions.capture { captureOrderResult ->
                }
            }
        )


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
    fun processOrder(method:String, amount: String, coin:Int) {
        when (method) {
            "ZaloPay" -> {
                val orderApi = CreateOrderZalo()
                try {
                    val data = orderApi.createOrder(amount)
                    val code = data.getString("return_code")
                    if (code == "1") {
                        val token = data.getString("zp_trans_token")
                        // Gọi hàm thanh toán của ZaloPay SDK
                        payWithZaloPay(token, amount, coin)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    private fun payWithZaloPay(token:String, amount:String, coin: Int){
        ZaloPaySDK.getInstance()
            .payOrder(this@PaymentsActivity, token, "demozpdk://app", object : PayOrderListener {
                override fun onPaymentSucceeded(
                    transactionId: String,
                    transToken: String,
                    appTransID: String
                ) {
                    runOnUiThread {
                        AlertDialog.Builder(this@PaymentsActivity)
                            .setTitle("Payment Success")
                            .setMessage("TransactionId: $transactionId - TransToken: $transToken")
                            .setPositiveButton("OK", null)
                            .setNegativeButton("Cancel", null).show()

                        // Tạo một instance của Transactions class với thông tin cần thiết
                        val transaction = Transactions(
                            transactionId = transactionId,
                            userId = currentUser!!.uid,
                            amount = amount.toDouble() ,
                            transactionType = "ZaloPay",
                            status = "Success",
                            completedAt = System.currentTimeMillis()
                        )

                        // Lưu transaction vào Realtime Database
                        saveTransactionToDatabase(transaction)
                        updateUsersCoin(currentUser.uid,coin)
                    }
                }

                override fun onPaymentCanceled(zpTransToken: String, appTransID: String) {
                    AlertDialog.Builder(this@PaymentsActivity)
                        .setTitle("User Cancel Payment")
                        .setMessage(String.format("zpTransToken: %s \n", zpTransToken))
                        .setPositiveButton("OK", object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface, which: Int) {}
                        })
                        .setNegativeButton("Cancel", null).show()
                }

                override fun onPaymentError(
                    zaloPayError: ZaloPayError,
                    zpTransToken: String,
                    appTransID: String
                ) {
                    AlertDialog.Builder(this@PaymentsActivity)
                        .setTitle("Payment Fail")
                        .setMessage(
                            String.format(
                                "ZaloPayErrorCode: %s \nTransToken: %s",
                                zaloPayError.toString(),
                                zpTransToken
                            )
                        )
                        .setPositiveButton("OK", object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface, which: Int) {}
                        })
                        .setNegativeButton("Cancel", null).show()
                }
            })
    }

    private fun payWithPayPal(){

    }

    private fun saveTransactionToDatabase(transaction: Transactions) {
        // Lấy instance của FirebaseDatabase
        val database = FirebaseDatabase.getInstance()
        // Tạo một tham chiếu đến nút "transactions" trong cơ sở dữ liệu
        val ref = database.getReference("transactions")

        // Lưu transaction sử dụng transactionId như là khóa
        ref.child(transaction.transactionId).setValue(transaction)
            .addOnSuccessListener {
                // Xử lý nếu lưu thành công
            }
            .addOnFailureListener {
                // Xử lý nếu có lỗi
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


}