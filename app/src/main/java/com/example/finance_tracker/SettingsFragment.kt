package com.example.finance_tracker

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import android.provider.OpenableColumns

class SettingsFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase
    private val gson = Gson()

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportDataAsJson(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            if (getFileName(it)?.endsWith(".json", ignoreCase = true) == true) {
                importDataFromJson(it)
            } else {
                Toast.makeText(context, "Неподдерживаемый тип файла. Выберите .json", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    if (result != null) {
                        result = result.substring(cut + 1)
                    }
                }
            }
        }
        return result
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        db = AppDatabase.getDatabase(requireContext())

        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val userId = sessionManager.getUserId()

        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUserById(userId)
            withContext(Dispatchers.Main) {
                user?.let {
                    tvUsername.text = "Username: ${it.username}"
                    tvEmail.text = "Email: ${it.email}"
                }
            }
        }

        setupPasswordReset(view, userId)
        setupDataManagement(view)
        setupThemeSwitch(view)
        setupLogout(view)

        view.findViewById<Button>(R.id.btnManageCategories).setOnClickListener {
            startActivity(Intent(activity, CategoriesActivity::class.java))
        }
    }

    private fun setupDataManagement(view: View) {
        view.findViewById<Button>(R.id.btnBackupData).setOnClickListener {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            exportLauncher.launch("FinanceTracker_Backup_$timestamp.json")
        }
        view.findViewById<Button>(R.id.btnRestoreData).setOnClickListener {
            importLauncher.launch("application/json")
        }

        view.findViewById<Button>(R.id.btnResetAppData).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Сбросить данные")
                .setMessage("Вы уверены, что хотите сбросить все данные? Это действие необратимо.")
                .setPositiveButton("Да") { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val userId = sessionManager.getUserId()
                        db.transactionDao().clearUserData(userId)
                        db.budgetDao().clearUserData(userId)
                        db.categoryDao().clearUserCategories(userId)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Данные аккаунта очищены", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Нет", null)
                .show()
        }
    }

    private fun exportDataAsJson(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()
                val transactions = db.transactionDao().getAllTransactions(userId)
                val userCategories = db.categoryDao().getAllCategories(userId).filter { !it.isDefault }
                val budget = db.budgetDao().getLatestBudget(userId)

                if (transactions.isEmpty() && userCategories.isEmpty() && budget == null) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Нет данных для экспорта", Toast.LENGTH_SHORT).show() }
                    return@launch
                }

                val backupData = BackupData(transactions, userCategories, budget)
                val jsonString = gson.toJson(backupData)

                requireContext().contentResolver.openOutputStream(uri)?.use { it.write(jsonString.toByteArray()) }
                withContext(Dispatchers.Main) { Toast.makeText(context, "Данные успешно экспортированы", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun importDataFromJson(uri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("Импорт данных")
            .setMessage("Внимание! Импорт ЗАМЕНИТ все ваши текущие транзакции, категории и бюджет. Продолжить?")
            .setPositiveButton("Да, заменить") { _, _ ->
                performJsonImport(uri)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performJsonImport(uri: Uri) {
        try {
            val json = requireContext().contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            if (json.isNullOrBlank()) {
                Toast.makeText(context, "Файл пуст", Toast.LENGTH_SHORT).show()
                return
            }
            val type = object : TypeToken<BackupData>() {}.type
            val backupData: BackupData = gson.fromJson(json, type)
            val currentUserId = sessionManager.getUserId()

            CoroutineScope(Dispatchers.IO).launch {
                db.transactionDao().clearUserData(currentUserId)
                db.categoryDao().clearUserCategories(currentUserId)
                db.budgetDao().clearUserData(currentUserId)

                db.transactionDao().insertTransactions(backupData.transactions.map { it.copy(id = 0, userId = currentUserId) })
                db.categoryDao().insertAll(backupData.categories.map { it.copy(id = 0, userId = currentUserId, isDefault = false) })
                backupData.budget?.let { db.budgetDao().insertBudget(it.copy(id = 0, userId = currentUserId)) }

                withContext(Dispatchers.Main) { Toast.makeText(context, "Данные успешно импортированы", Toast.LENGTH_SHORT).show() }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка импорта JSON: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupPasswordReset(view: View, userId: Int) {
        val etCurrentPassword: EditText = view.findViewById(R.id.etCurrentPassword)
        val etNewPassword: EditText = view.findViewById(R.id.etNewPassword)
        val etConfirmNewPassword: EditText = view.findViewById(R.id.etConfirmNewPassword)
        val btnResetPassword: Button = view.findViewById(R.id.btnResetPassword)

        btnResetPassword.setOnClickListener {
            val currentPassword = etCurrentPassword.text.toString()
            val newPassword = etNewPassword.text.toString()
            val confirmNewPassword = etConfirmNewPassword.text.toString()

            if (newPassword != confirmNewPassword) {
                Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val user = db.userDao().getUserById(userId)
                if (user != null && user.password == currentPassword) {
                    val updatedUser = user.copy(password = newPassword)
                    db.userDao().updateUser(updatedUser)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupThemeSwitch(view: View) {
        val switchTheme = view.findViewById<SwitchMaterial>(R.id.switchTheme)
        val isCurrentlyNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        switchTheme.isChecked = isCurrentlyNight

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            val newMode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            sessionManager.saveThemeMode(newMode)
            AppCompatDelegate.setDefaultNightMode(newMode)
        }
    }

    private fun setupLogout(view: View) {
        val btnLogout: Button = view.findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            sessionManager.clearSession()
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}