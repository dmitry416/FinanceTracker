package com.example.finance_tracker

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val onDeleteClicked: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var transactions: List<Transaction> = emptyList()
    private var categoryMap: Map<String, Category> = emptyMap()

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
        val ivCategoryIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val context = holder.itemView.context

        holder.tvCategory.text = transaction.category
        val amountText = if (transaction.type == "Income") {
            "+ ${String.format("%.2f", transaction.amount)} \$"
        } else {
            "- ${String.format("%.2f", transaction.amount)} \$"
        }
        holder.tvAmount.text = amountText

        val incomeColor = context.theme.resolveColor(R.attr.incomeColor)
        val expenseColor = context.theme.resolveColor(R.attr.expenseColor)

        holder.tvAmount.setTextColor(
            if (transaction.type == "Income") incomeColor else expenseColor
        )

        holder.tvDate.text =
            SimpleDateFormat("dd MMMM yyyy", Locale("ru")).format(transaction.date)

        val category = categoryMap[transaction.category]
        if (category != null) {
            val iconResId = getIconResourceId(context, category.iconName)
            holder.ivCategoryIcon.setImageResource(iconResId)
            (holder.ivCategoryIcon.background as GradientDrawable).setColor(
                try {
                    Color.parseColor(category.colorHex)
                } catch (e: Exception) {
                    Color.GRAY
                }
            )
        } else {
            holder.ivCategoryIcon.setImageResource(R.drawable.ic_category_other)
            (holder.ivCategoryIcon.background as GradientDrawable).setColor(Color.GRAY)
        }

        holder.ivDelete.setOnClickListener { onDeleteClicked(transaction) }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateData(newTransactions: List<Transaction>, newCategoryMap: Map<String, Category>) {
        this.transactions = newTransactions
        this.categoryMap = newCategoryMap
        notifyDataSetChanged()
    }

    private fun getIconResourceId(context: Context, iconName: String): Int {
        return context.resources.getIdentifier(iconName, "drawable", context.packageName).let {
            if (it == 0) R.drawable.ic_category_other else it
        }
    }

    fun android.content.res.Resources.Theme.resolveColor(@androidx.annotation.AttrRes attr: Int): Int {
        val a = obtainStyledAttributes(intArrayOf(attr))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }
}