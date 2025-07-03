package com.example.finance_tracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import java.io.FileNotFoundException
import java.io.IOException

class SettingsFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase

    private val BACKUP_FILE_NAME = "transactions_backup.json"

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
                if (user != null) {
                    tvUsername.text = "Username: ${user.username}"
                    tvEmail.text = "Email: ${user.email}"
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

    private fun setupDataManagement(view: View) {
        val btnBackupData: Button = view.findViewById(R.id.btnBackupData)
        val btnRestoreData: Button = view.findViewById(R.id.btnRestoreData)
        val btnResetAppData: Button = view.findViewById(R.id.btnResetAppData)

        btnBackupData.setOnClickListener { backupTransactionsToInternalStorage() }
        btnRestoreData.setOnClickListener { restoreTransactionsFromInternalStorage() }

        btnResetAppData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reset App Data")
                .setMessage("Are you sure you want to reset all app data? This action cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        sessionManager.clearSession()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "App data reset successfully", Toast.LENGTH_SHORT).show()
                            navigateToLogin()
                        }
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun setupThemeSwitch(view: View) {
        val switchTheme = view.findViewById<SwitchMaterial>(R.id.switchTheme)
        switchTheme.isChecked = sessionManager.getThemeMode() == AppCompatDelegate.MODE_NIGHT_YES

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            val newMode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(newMode)
            sessionManager.saveThemeMode(newMode)
        }
    }

    private fun setupLogout(view: View) {
        val btnLogout: Button = view.findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            sessionManager.clearSession()
            navigateToLogin()
        }
    }

    private fun backupTransactionsToInternalStorage() {
        CoroutineScope(Dispatchers.IO).launch {
            val userId = sessionManager.getUserId()
            val transactions = db.transactionDao().getAllTransactions(userId)

            if (transactions.isNotEmpty()) {
                val gson = Gson()
                val json = gson.toJson(transactions)
                try {
                    requireContext().openFileOutput(BACKUP_FILE_NAME, Context.MODE_PRIVATE).use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Backup saved", Toast.LENGTH_LONG).show()
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save backup: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No transactions to backup", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun restoreTransactionsFromInternalStorage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = requireContext().openFileInput(BACKUP_FILE_NAME).bufferedReader().use { it.readText() }
                val gson = Gson()
                val type = object : TypeToken<List<Transaction>>() {}.type
                val transactions: List<Transaction> = gson.fromJson(json, type)
                db.transactionDao().insertTransactions(transactions)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Transactions restored successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: FileNotFoundException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No backup file found", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to restore: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}