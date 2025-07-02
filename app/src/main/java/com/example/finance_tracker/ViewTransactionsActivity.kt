package com.example.finance_tracker

import android.content.Intent
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewTransactionsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: TransactionAdapter
    private lateinit var rgFilterTransactions: RadioGroup
    private lateinit var rvTransactions: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_transactions)

        db = AppDatabase.getDatabase(this)

        rgFilterTransactions = findViewById(R.id.rgFilterTransactions)
        rvTransactions = findViewById(R.id.rvTransactions)

        adapter = TransactionAdapter(
            emptyList(),
            onEditClicked = { transaction -> navigateToEditTransaction(transaction) },
            onDeleteClicked = { transaction -> deleteTransaction(transaction) }
        )
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        // Load all transactions initially
        loadTransactions(filter = "All")

        // Handle filter changes
        rgFilterTransactions.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbAll -> loadTransactions(filter = "All")
                R.id.rbIncome -> loadTransactions(filter = "Income")
                R.id.rbExpense -> loadTransactions(filter = "Expense")
            }
        }

        // Bottom Navigation
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Highlight the View Transactions icon
        bottomNavigation.selectedItemId = R.id.nav_view_transactions

        // Handle Bottom Navigation Item Selection
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_add_transaction -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_view_transactions -> {
                    // Already on View Transactions page
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadTransactions(filter: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val userId = SessionManager(this@ViewTransactionsActivity).getUserId()
            val transactions = when (filter) {
                "Income" -> db.transactionDao().getTransactionsByType(userId, "Income")
                "Expense" -> db.transactionDao().getTransactionsByType(userId, "Expense")
                else -> db.transactionDao().getAllTransactions(userId)
            }
            runOnUiThread {
                adapter.updateTransactions(transactions)
            }
        }
    }

    private fun navigateToEditTransaction(transaction: Transaction) {
        val intent = Intent(this, EditTransactionActivity::class.java)
        intent.putExtra("transactionId", transaction.id)
        startActivity(intent)
    }

    private fun deleteTransaction(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Yes") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    db.transactionDao().deleteTransaction(transaction)
                    loadTransactions(filter = "All")
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}