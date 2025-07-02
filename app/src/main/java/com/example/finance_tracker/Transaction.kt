package com.example.finance_tracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Income" or "Expense"
    val category: String, // Category of the transaction
    val date: Long, // Timestamp for the date
    val amount: Double, // Transaction amount
    val userId: Int // Foreign key to associate with a user
)