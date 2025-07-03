package com.example.finance_tracker

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Budget::class, Transaction::class, Category::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addCallback(AppDatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback() : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    prePopulateDatabase(database.categoryDao())
                }
            }
        }

        suspend fun prePopulateDatabase(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                Category(name = "Еда", iconName = "ic_category_food", colorHex = "#FFC107", isDefault = true, userId = 0),
                Category(name = "Транспорт", iconName = "ic_category_transport", colorHex = "#2196F3", isDefault = true, userId = 0),
                Category(name = "Развлечения", iconName = "ic_category_entertainment", colorHex = "#9C27B0", isDefault = true, userId = 0),
                Category(name = "Здоровье", iconName = "ic_category_health", colorHex = "#E91E63", isDefault = true, userId = 0),
                Category(name = "Зарплата", iconName = "ic_category_salary", colorHex = "#4CAF50", isDefault = true, userId = 0),
                Category(name = "Другое", iconName = "ic_category_other", colorHex = "#795548", isDefault = true, userId = 0)
            )
            categoryDao.insertAll(defaultCategories)
        }
    }
}