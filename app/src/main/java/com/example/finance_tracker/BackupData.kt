package com.example.finance_tracker

data class BackupData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budget: Budget?
)