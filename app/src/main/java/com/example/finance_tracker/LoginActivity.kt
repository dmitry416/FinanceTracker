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
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignupLink: Button
    private lateinit var btnContinueAsGuest: Button

    companion object {
        const val GUEST_EMAIL = "guest@local.user"
        const val GUEST_USERNAME = "Guest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        if (sessionManager.getUserId() != -1) {
            navigateToMain()
            return
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignupLink = findViewById(R.id.btnSignupLink)
        btnContinueAsGuest = findViewById(R.id.btnContinueAsGuest)

        btnLogin.setOnClickListener {
            handleLogin(etEmail.text.toString().trim(), etPassword.text.toString().trim())
        }

        btnSignupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        btnContinueAsGuest.setOnClickListener {
            handleGuestLogin()
        }
    }

    private fun handleGuestLogin() {
        CoroutineScope(Dispatchers.IO).launch {
            var guestUser = db.userDao().getUserByEmail(GUEST_EMAIL)
            if (guestUser == null) {
                val newGuest = User(username = GUEST_USERNAME, email = GUEST_EMAIL, password = "")
                db.userDao().insert(newGuest)
                guestUser = db.userDao().getUserByEmail(GUEST_EMAIL)
            }

            guestUser?.let {
                sessionManager.saveUserId(it.id)
                withContext(Dispatchers.Main) {
                    navigateToMain()
                }
            }
        }
    }

    private fun handleLogin(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isValidEmail(email)) {
            etEmail.error = "Неверный формат почты"
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUser(email, password)
            withContext(Dispatchers.Main) {
                if (user != null) {
                    sessionManager.saveUserId(user.id)
                    Toast.makeText(this@LoginActivity, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this@LoginActivity, "Неверные учетные данные", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}