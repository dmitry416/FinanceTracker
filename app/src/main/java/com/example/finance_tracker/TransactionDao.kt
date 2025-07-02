package com.example.finance_tracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Insert
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE userId = :userId AND type = 'Income' GROUP BY category")
    suspend fun getIncomeByCategory(userId: Int): List<CategoryTotal>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE userId = :userId AND type = 'Expense' GROUP BY category")
    suspend fun getExpenseByCategory(userId: Int): List<CategoryTotal>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllTransactions(userId: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY date DESC")
    suspend fun getTransactionsByType(userId: Int, type: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Int): Transaction

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}

data class CategoryTotal(
    val category: String,
    val total: Double
)