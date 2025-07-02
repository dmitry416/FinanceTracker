package com.example.finance_tracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onEditClicked: (Transaction) -> Unit,
    private val onDeleteClicked: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val ivEdit: ImageView = view.findViewById(R.id.ivEdit)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.tvCategory.text = transaction.category
        holder.tvAmount.text = "${transaction.type}: $${String.format("%.2f", transaction.amount)}"
        holder.tvDate.text = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(transaction.date)

        // Handle Edit Click
        holder.ivEdit.setOnClickListener {
            onEditClicked(transaction)
        }

        // Handle Delete Click
        holder.ivDelete.setOnClickListener {
            onDeleteClicked(transaction)
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}