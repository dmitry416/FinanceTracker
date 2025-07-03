package com.example.finance_tracker

import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AddTransactionFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var spCategory: Spinner
    private lateinit var tvSelectedDate: TextView
    private lateinit var etAmount: EditText

    private var selectedDate: Long = 0L
    private var selectedType: String = "Income"
    private val CHANNEL_ID = "finance_tracker_channel"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())
        sessionManager = SessionManager(requireContext())

        val rgTransactionType = view.findViewById<RadioGroup>(R.id.rgTransactionType)
        spCategory = view.findViewById(R.id.spCategory)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        etAmount = view.findViewById(R.id.etAmount)
        val btnAddTransaction = view.findViewById<Button>(R.id.btnAddTransaction)
        val btnSelectDate = view.findViewById<Button>(R.id.btnSelectDate)

        rgTransactionType.check(R.id.rbIncome)

        loadAndSetupCategories()

        rgTransactionType.setOnCheckedChangeListener { _, checkedId ->
            selectedType = if (checkedId == R.id.rbIncome) "Income" else "Expense"
        }

        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, dayOfMonth)
                    selectedDate = selectedCalendar.timeInMillis
                    tvSelectedDate.text = "Выбранная дата: ${dayOfMonth}/${month + 1}/${year}"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnAddTransaction.setOnClickListener {
            addTransaction()
        }
    }

    private fun addTransaction() {
        val selectedCategory = spCategory.selectedItem as? Category
        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "Пожалуйста, выберите категорию", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryName = selectedCategory.name
        val amount = etAmount.text.toString().toDoubleOrNull()
        val userId = sessionManager.getUserId()

        if (amount != null && amount > 0 && selectedDate != 0L) {
            CoroutineScope(Dispatchers.IO).launch {
                val transaction = Transaction(
                    type = selectedType,
                    category = categoryName,
                    date = selectedDate,
                    amount = amount,
                    userId = userId
                )
                db.transactionDao().insertTransaction(transaction)

                val budget = db.budgetDao().getLatestBudget(userId)
                if (selectedType == "Income") {
                    budget?.let {
                        db.budgetDao().insertBudget(Budget(amount = it.amount + amount, userId = userId))
                    }
                } else {
                    budget?.let {
                        db.budgetDao().insertBudget(Budget(amount = it.amount - amount, userId = userId))
                    }
                }

                withContext(Dispatchers.Main) {
                    showNotification(
                        "Новая транзакция",
                        "Добавлена запись: $selectedType на сумму $amount в категории $categoryName."
                    )
                    Toast.makeText(requireContext(), "Транзакция добавлена!", Toast.LENGTH_SHORT).show()

                    activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_home
                }
            }
        } else {
            Toast.makeText(requireContext(), "Пожалуйста, заполните все поля и введите корректную сумму", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAndSetupCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val userId = sessionManager.getUserId()
            val categories = db.categoryDao().getAllCategories(userId)
            withContext(Dispatchers.Main) {
                if (categories.isNotEmpty()) {
                    val adapter = CategorySpinnerAdapter(requireContext(), categories)
                    spCategory.adapter = adapter
                } else {
                    spCategory.adapter = null
                    Toast.makeText(requireContext(), "Нет доступных категорий. Добавьте их в настройках.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FinanceTracker Notifications"
            val descriptionText = "Notifications for new transactions"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            requireActivity().getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(requireContext())) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}