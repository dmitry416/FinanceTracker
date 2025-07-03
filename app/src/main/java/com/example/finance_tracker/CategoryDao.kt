package com.example.finance_tracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories WHERE userId = 0 OR userId = :userId ORDER BY name ASC")
    suspend fun getAllCategories(userId: Int): List<Category>


    @Query("DELETE FROM categories WHERE userId = :userId AND isDefault = 0")
    suspend fun clearAndInsert(userId: Int, categories: List<Category>) {
        clearUserCategories(userId)
        insertAll(categories)
    }

    @Query("DELETE FROM categories WHERE userId = :userId AND isDefault = 0")
    suspend fun clearUserCategories(userId: Int)
}
