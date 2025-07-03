package com.example.finance_tracker

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var tvBudget: TextView
    private lateinit var btnUpdateBudget: Button
    private lateinit var pieChartIncome: PieChart
    private lateinit var pieChartExpense: PieChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())
        sessionManager = SessionManager(requireContext())

        tvBudget = view.findViewById(R.id.tvBudget)
        pieChartIncome = view.findViewById(R.id.pieChartIncome)
        pieChartExpense = view.findViewById(R.id.pieChartExpense)
        btnUpdateBudget = view.findViewById(R.id.btnUpdateBudget)

        val userId = sessionManager.getUserId()

        loadBudget(userId)
        loadPieCharts(userId)

        btnUpdateBudget.setOnClickListener {
            showUpdateBudgetDialog(userId)
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

            val categories = db.categoryDao().getAllCategories(userId)
            val categoryColorMap = categories.associate { it.name to it.colorHex }

            withContext(Dispatchers.Main) {
                setupPieChart(pieChartIncome, incomeData, "Доходы", categoryColorMap)
                setupPieChart(pieChartExpense, expenseData, "Расходы", categoryColorMap)
            }
        }
    }

    private fun setupPieChart(
        pieChart: PieChart,
        data: List<CategoryTotal>,
        label: String,
        colorMap: Map<String, String>
    ) {
        if (data.isEmpty()) {
            pieChart.visibility = View.GONE
            return
        }
        pieChart.visibility = View.VISIBLE

        val entries = data.map { PieEntry(it.total.toFloat(), it.category) }

        val colors = data.map {
            try {
                Color.parseColor(colorMap[it.category] ?: "#808080")
            } catch (e: IllegalArgumentException) {
                Color.GRAY
            }
        }

        val dataSet = PieDataSet(entries, label)
        dataSet.colors = colors
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 14f

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.centerText = label
        pieChart.setUsePercentValues(true)
        pieChart.legend.isEnabled = false
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun loadBudget(userId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val budget = db.budgetDao().getLatestBudget(userId)
            withContext(Dispatchers.Main) {
                if (budget != null) {
                    tvBudget.text = "Бюджет: $${String.format("%.2f", budget.amount)}"
                } else {
                    tvBudget.text = "Не установлен"
                }
            }
        }
    }

    private fun showUpdateBudgetDialog(userId: Int) {
        val dialogView = View.inflate(requireContext(), R.layout.dialog_update_budget, null)
        val etBudgetAmount = dialogView.findViewById<android.widget.EditText>(R.id.etBudgetAmount)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnSubmitBudget).setOnClickListener {
            val budgetAmount = etBudgetAmount.text.toString().toDoubleOrNull()
            if (budgetAmount != null && budgetAmount > 0) {
                CoroutineScope(Dispatchers.IO).launch {
                    db.budgetDao().insertBudget(Budget(amount = budgetAmount, userId = userId))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Бюджет обновлен", Toast.LENGTH_SHORT)
                            .show()
                        loadBudget(userId)
                        dialog.dismiss()
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Пожалуйста, введите корректную сумму",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        dialog.show()
    }
}