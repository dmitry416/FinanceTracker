package com.example.finance_tracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double, // Store the budget amount
    val userId: Int // Foreign key to associate with a user
)