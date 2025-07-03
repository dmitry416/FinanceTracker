package com.example.finance_tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewTransactionsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: TransactionAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var rgFilterTransactions: RadioGroup
    private lateinit var rvTransactions: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())
        sessionManager = SessionManager(requireContext())

        rgFilterTransactions = view.findViewById(R.id.rgFilterTransactions)
        rvTransactions = view.findViewById(R.id.rvTransactions)

        adapter = TransactionAdapter { transaction -> showDeleteConfirmationDialog(transaction) }
        rvTransactions.layoutManager = LinearLayoutManager(context)
        rvTransactions.adapter = adapter

        loadTransactions("All")

        rgFilterTransactions.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbAll -> loadTransactions("All")
                R.id.rbIncome -> loadTransactions("Income")
                R.id.rbExpense -> loadTransactions("Expense")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val checkedId = rgFilterTransactions.checkedRadioButtonId
        val filter = when (checkedId) {
            R.id.rbIncome -> "Income"
            R.id.rbExpense -> "Expense"
            else -> "All"
        }
        loadTransactions(filter)
    }

    private fun loadTransactions(filter: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val userId = sessionManager.getUserId()

            val transactions = when (filter) {
                "Income" -> db.transactionDao().getTransactionsByType(userId, "Income")
                "Expense" -> db.transactionDao().getTransactionsByType(userId, "Expense")
                else -> db.transactionDao().getAllTransactions(userId)
            }

            val categories = db.categoryDao().getAllCategories(userId)
            val categoryMap = categories.associateBy { it.name }

            withContext(Dispatchers.Main) {
                adapter.updateData(transactions, categoryMap)
            }
        }
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить транзакцию")
            .setMessage("Вы уверены, что хотите удалить эту транзакцию?")
            .setPositiveButton("Да") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    db.transactionDao().deleteTransaction(transaction)
                    withContext(Dispatchers.Main) {
                        val checkedId = rgFilterTransactions.checkedRadioButtonId
                        val filter = when (checkedId) {
                            R.id.rbIncome -> "Income"
                            R.id.rbExpense -> "Expense"
                            else -> "All"
                        }
                        loadTransactions(filter)
                    }
                }
            }
            .setNegativeButton("Нет", null)
            .show()
    }
}