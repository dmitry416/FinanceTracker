package com.example.finance_tracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Ensure it's auto-generated
    val username: String,
    val email: String,
    val password: String
)