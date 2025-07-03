package com.example.finance_tracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val category: String,
    val date: Long,
    val amount: Double,
    val userId: Int
)