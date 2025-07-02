package com.example.finance_tracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val btnSignupLink: Button = findViewById(R.id.btnSignupLink)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validate inputs
            if (email.isEmpty()) {
                etEmail.error = "Please enter your email"
                return@setOnClickListener
            }
            if (!isValidEmail(email)) {
                etEmail.error = "Invalid email format"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Please enter your password"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            // Perform login
            CoroutineScope(Dispatchers.IO).launch {
                val user = db.userDao().getUser(email, password)
                if (user != null) {
                    sessionManager.saveUserId(user.id)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT)
                            .show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Invalid Credentials", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        btnSignupLink.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    // Helper function to validate email pattern
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}