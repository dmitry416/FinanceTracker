package com.example.finance_tracker

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [User::class, Budget::class, Transaction::class], version = 3, exportSchema = false) // Increment version to 3
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration() // Handle migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}