import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import com.example.anonymousChat.R
import com.example.anonymousChat.exchanges.PaymentsActivity

class PaymentMethodDialogFragment(private val amount: String, private val coin: Int) : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.payment_method, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<LinearLayout>(R.id.btnZaloPay).setOnClickListener {
            processPayment("ZaloPay")
        }

        view.findViewById<LinearLayout>(R.id.btnPaypal).setOnClickListener {
            processPayment("Paypal")
        }

    }

    private fun processPayment(method: String) {
        // Gửi thông tin phương thức thanh toán đã chọn về Activity
        (activity as PaymentsActivity).processOrder(method, amount, coin)
        dismiss() // Đóng dialog sau khi chọn
    }
}
