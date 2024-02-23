package com.example.anonymousChat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.anonymousChat.R
import com.example.anonymousChat.exchanges.transaction
import java.text.DecimalFormat
import java.text.SimpleDateFormat

var dateFormat: SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss") // Customize the format as needed


class TransactionsAdapter(private val transactions: List<transaction>) : RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val time: TextView = view.findViewById(R.id.tvTime)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvSTT: TextView = view.findViewById(R.id.tvSTT)
        val coins: TextView = view.findViewById(R.id.tvCoin)
        // Thêm các tham chiếu đến các TextView khác nếu cần
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payments, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        val formatter = DecimalFormat("#,###")

        holder.time.text =  dateFormat.format(transaction.completedAt!!.toLong())
        holder.tvAmount.text = "-${formatter.format(transaction.amount)}VND"
        holder.tvSTT.text = "${position+1}"
        val amount = transaction.amount.toString()
        when (amount) {
            "10000" -> {
                holder.coins.text = "+10"
            }
            "20000" -> {
                holder.coins.text = "+20"
            }
            "50000" -> {
                holder.coins.text = "+50"
            }
            "100000" -> {
                holder.coins.text = "+100"
            }

        }
        // Cập nhật các TextView khác với dữ liệu từ transaction
    }

    override fun getItemCount() = transactions.size
}
