package com.example.finance_tracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BudgetDao {
    @Insert
    suspend fun insertBudget(budget: Budget)

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    suspend fun getLatestBudget(userId: Int): Budget?

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun clearUserData(userId: Int)

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun clearAndInsert(userId: Int, budget: Budget?) {
        clearUserData(userId)
        budget?.let { insertBudget(it) }
    }
}