package com.example.finance_tracker

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException

class SettingsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase

    private val BACKUP_FILE_NAME = "transactions_backup.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)

        // Bottom Navigation
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Highlight the Settings icon
        bottomNavigation.selectedItemId = R.id.nav_settings

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
                    startActivity(Intent(this, ViewTransactionsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    // Already on Settings page
                    true
                }
                else -> false
            }
        }

        // User Details
        val tvUsername: TextView = findViewById(R.id.tvUsername)
        val tvEmail: TextView = findViewById(R.id.tvEmail)

        // Display Username and Email
        val userId = sessionManager.getUserId()
        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUserById(userId)
            runOnUiThread {
                if (user != null) {
                    tvUsername.text = "Username: ${user.username}"
                    tvEmail.text = "Email: ${user.email}"
                }
            }
        }

        // Password Reset
        val etCurrentPassword: EditText = findViewById(R.id.etCurrentPassword)
        val etNewPassword: EditText = findViewById(R.id.etNewPassword)
        val etConfirmNewPassword: EditText = findViewById(R.id.etConfirmNewPassword)
        val btnResetPassword: Button = findViewById(R.id.btnResetPassword)

        btnResetPassword.setOnClickListener {
            val currentPassword = etCurrentPassword.text.toString()
            val newPassword = etNewPassword.text.toString()
            val confirmNewPassword = etConfirmNewPassword.text.toString()

            if (newPassword != confirmNewPassword) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val user = db.userDao().getUserById(userId)
                if (user != null && user.password == currentPassword) {
                    val updatedUser = user.copy(password = newPassword)
                    db.userDao().updateUser(updatedUser)
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Backup and Restore
        val btnBackupData: Button = findViewById(R.id.btnBackupData)
        val btnRestoreData: Button = findViewById(R.id.btnRestoreData)

        btnBackupData.setOnClickListener {
            backupTransactionsToInternalStorage()
        }

        btnRestoreData.setOnClickListener {
            restoreTransactionsFromInternalStorage()
        }

        // Reset App Data
        val btnResetAppData: Button = findViewById(R.id.btnResetAppData)
        btnResetAppData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset App Data")
                .setMessage("Are you sure you want to reset all app data? This action cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        db.clearAllTables() // Clear all tables in the database
                        sessionManager.clearSession() // Clear user session
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "App data reset successfully", Toast.LENGTH_SHORT).show()
                            navigateToLogin()
                        }
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Logout
        val btnLogout: Button = findViewById(R.id.btnLogout)
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

                // Save JSON to internal storage
                try {
                    openFileOutput(BACKUP_FILE_NAME, MODE_PRIVATE).use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Backup saved to internal storage", Toast.LENGTH_LONG).show()
                    }
                } catch (e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Failed to save backup: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "No transactions to backup", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun restoreTransactionsFromInternalStorage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Read JSON from internal storage
                val json = openFileInput(BACKUP_FILE_NAME).bufferedReader().use { it.readText() }

                val gson = Gson()
                val type = object : TypeToken<List<Transaction>>() {}.type
                val transactions: List<Transaction> = gson.fromJson(json, type)

                db.transactionDao().insertTransactions(transactions)

                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Transactions restored successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: FileNotFoundException) {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "No backup file found", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Failed to restore transactions: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}