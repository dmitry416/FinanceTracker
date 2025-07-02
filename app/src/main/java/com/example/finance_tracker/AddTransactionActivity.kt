package com.example.finance_tracker

import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var spCategory: Spinner
    private lateinit var tvSelectedDate: TextView
    private lateinit var etAmount: EditText

    private var selectedDate: Long = 0L
    private var selectedType: String = "Income"
    private val incomeCategories = listOf("Salary", "Business", "Investment", "Other")
    private val expenseCategories = listOf("Food", "Transport", "Shopping", "Other")
    private val CHANNEL_ID = "cash_tracker_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        val rgTransactionType = findViewById<RadioGroup>(R.id.rgTransactionType)
        spCategory = findViewById(R.id.spCategory)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        etAmount = findViewById(R.id.etAmount)
        val btnAddTransaction = findViewById<Button>(R.id.btnAddTransaction)
        val btnSelectDate = findViewById<Button>(R.id.btnSelectDate)
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Highlight Add icon
        bottomNavigation.selectedItemId = R.id.nav_add_transaction

        // Handle Bottom Navigation Item Selection
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_add_transaction -> {
                    // Already on Add Transaction page
                    true
                }
                R.id.nav_view_transactions -> {
                    startActivity(Intent(this, ViewTransactionsActivity::class.java))
                    finish()
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

        // Set initial spinner values for income
        setupCategorySpinner(incomeCategories)

        // Handle transaction type selection
        rgTransactionType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbIncome) {
                selectedType = "Income"
                setupCategorySpinner(incomeCategories)
            } else if (checkedId == R.id.rbExpense) {
                selectedType = "Expense"
                setupCategorySpinner(expenseCategories)
            }
        }

        // Handle date selection
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, dayOfMonth)
                    selectedDate = selectedCalendar.timeInMillis
                    tvSelectedDate.text = "Selected Date: ${dayOfMonth}/${month + 1}/${year}"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // Handle adding transaction
        btnAddTransaction.setOnClickListener {
            val category = spCategory.selectedItem.toString()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val userId = sessionManager.getUserId()

            if (amount != null && selectedDate != 0L) {
                CoroutineScope(Dispatchers.IO).launch {
                    val transaction = Transaction(
                        type = selectedType,
                        category = category,
                        date = selectedDate,
                        amount = amount,
                        userId = userId
                    )
                    db.transactionDao().insertTransaction(transaction)

                    // Update budget
                    val budget = db.budgetDao().getLatestBudget(userId)
                    if (selectedType == "Income") {
                        budget?.let {
                            db.budgetDao().insertBudget(
                                Budget(amount = it.amount + amount, userId = userId)
                            )
                        }
                    } else {
                        budget?.let {
                            db.budgetDao().insertBudget(
                                Budget(amount = it.amount - amount, userId = userId)
                            )
                        }
                    }

                    // Show notification
                    runOnUiThread {
                        showNotification(
                            title = "New Transaction Added",
                            message = "You added a $selectedType of $amount in category $category."
                        )
                        Toast.makeText(this@AddTransactionActivity, "Transaction Added!", Toast.LENGTH_SHORT).show()

                        // Redirect to MainActivity or another activity
                        val intent = Intent(this@AddTransactionActivity, MainActivity::class.java)
                        startActivity(intent)

                        // Close the current activity
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCategorySpinner(categories: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter
    }

    private fun showNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "CashTracker Notifications"
            val descriptionText = "Notifications for new transactions"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your app's icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}