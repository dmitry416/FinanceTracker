package com.example.finance_tracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var tvBudget: TextView
    private lateinit var btnUpdateBudget: Button
    private lateinit var btnAddTransaction: Button
    private lateinit var pieChartIncome: PieChart
    private lateinit var pieChartExpense: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        // Schedule daily notification
        scheduleDailyNotification()

        // Bottom Navigation
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Highlight Home by default
        bottomNavigation.selectedItemId = R.id.nav_home

        // Handle Navigation Item Selection
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on Home
                    true
                }

                R.id.nav_add_transaction -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    finish()
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

        tvBudget = findViewById(R.id.tvBudget)
        pieChartIncome = findViewById(R.id.pieChartIncome)
        pieChartExpense = findViewById(R.id.pieChartExpense)
        btnUpdateBudget = findViewById(R.id.btnUpdateBudget)
        btnAddTransaction = findViewById(R.id.btnAddTransaction)

        val userId = sessionManager.getUserId()

        // Load data
        loadBudget(userId)
        loadPieCharts(userId)

        // Set up buttons
        btnUpdateBudget.setOnClickListener {
            showUpdateBudgetDialog(userId)
        }

        btnAddTransaction.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = sessionManager.getUserId()
        loadBudget(userId)
        loadPieCharts(userId)
    }

    private fun loadPieCharts(userId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val incomeData = db.transactionDao().getIncomeByCategory(userId)
            val expenseData = db.transactionDao().getExpenseByCategory(userId)

            runOnUiThread {
                setupPieChart(pieChartIncome, incomeData, "Income Categories")
                setupPieChart(pieChartExpense, expenseData, "Expense Categories")
            }
        }
    }

    private fun setupPieChart(pieChart: PieChart, data: List<CategoryTotal>, label: String) {
        val entries = data.map {
            PieEntry(it.total.toFloat(), it.category)
        }

        val dataSet = PieDataSet(entries, label)
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        dataSet.valueTextSize = 14f

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.centerText = label
        pieChart.animateY(1000)
        pieChart.invalidate() // refresh chart
    }

    private fun loadBudget(userId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val budget = db.budgetDao().getLatestBudget(userId)
            runOnUiThread {
                if (budget != null) {
                    tvBudget.text = "Budget: $${String.format("%.2f", budget.amount)}"
                } else {
                    tvBudget.text = "No budget set"
                }
            }
        }
    }

    private fun showUpdateBudgetDialog(userId: Int) {
        val dialogView = View.inflate(this, R.layout.dialog_update_budget, null)
        val etBudgetAmount = dialogView.findViewById<android.widget.EditText>(R.id.etBudgetAmount)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnSubmitBudget).setOnClickListener {
            val budgetAmount = etBudgetAmount.text.toString().toDoubleOrNull()
            if (budgetAmount != null && budgetAmount > 0) {
                CoroutineScope(Dispatchers.IO).launch {
                    db.budgetDao().insertBudget(Budget(amount = budgetAmount, userId = userId))
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Budget updated successfully", Toast.LENGTH_SHORT).show()
                        loadBudget(userId)
                        dialog.dismiss()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun scheduleDailyNotification() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val notificationWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "transaction_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            notificationWorkRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        val currentTime = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 45)
            set(java.util.Calendar.SECOND, 0)
        }
        
        var targetTime = calendar.timeInMillis
        if (targetTime <= currentTime) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            targetTime = calendar.timeInMillis
        }
        
        return targetTime - currentTime
    }
}