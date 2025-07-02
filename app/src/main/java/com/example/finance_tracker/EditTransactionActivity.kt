package com.example.finance_tracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class EditTransactionActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var transactionId: Int = 0
    private lateinit var spCategory: Spinner
    private lateinit var tvSelectedDate: TextView
    private lateinit var etAmount: EditText
    private lateinit var rgTransactionType: RadioGroup

    private var selectedDate: Long = 0L
    private var selectedType: String = "Income"
    private val incomeCategories = listOf("Salary", "Business", "Investment", "Other")
    private val expenseCategories = listOf("Food", "Transport", "Shopping", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction) // Reuse Add Transaction layout

        db = AppDatabase.getDatabase(this)
        transactionId = intent.getIntExtra("transactionId", 0)

        spCategory = findViewById(R.id.spCategory)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        etAmount = findViewById(R.id.etAmount)
        rgTransactionType = findViewById(R.id.rgTransactionType)
        val btnAddTransaction = findViewById<Button>(R.id.btnAddTransaction)
        btnAddTransaction.text = "Update Transaction"

        loadTransactionDetails()

        btnAddTransaction.setOnClickListener {
            updateTransaction()
        }
    }

    private fun loadTransactionDetails() {
        CoroutineScope(Dispatchers.IO).launch {
            val transaction = db.transactionDao().getTransactionById(transactionId)
            runOnUiThread {
                selectedDate = transaction.date
                selectedType = transaction.type
                etAmount.setText(transaction.amount.toString())
                tvSelectedDate.text = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(transaction.date)
                if (transaction.type == "Income") {
                    rgTransactionType.check(R.id.rbIncome)
                    setupCategorySpinner(incomeCategories)
                } else {
                    rgTransactionType.check(R.id.rbExpense)
                    setupCategorySpinner(expenseCategories)
                }
                spCategory.setSelection((if (transaction.type == "Income") incomeCategories else expenseCategories).indexOf(transaction.category))
            }
        }
    }

    private fun updateTransaction() {
        val category = spCategory.selectedItem.toString()
        val amount = etAmount.text.toString().toDoubleOrNull()

        if (amount != null && selectedDate != 0L) {
            CoroutineScope(Dispatchers.IO).launch {
                val updatedTransaction = Transaction(
                    id = transactionId,
                    type = selectedType,
                    category = category,
                    date = selectedDate,
                    amount = amount,
                    userId = SessionManager(this@EditTransactionActivity).getUserId()
                )
                db.transactionDao().updateTransaction(updatedTransaction)
                runOnUiThread {
                    Toast.makeText(this@EditTransactionActivity, "Transaction Updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCategorySpinner(categories: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter
    }
}