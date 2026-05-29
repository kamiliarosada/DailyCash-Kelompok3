package com.example.dailycash.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dailycash.data.local.dao.BudgetDao
import com.example.dailycash.data.local.dao.FixedExpenseDao
import com.example.dailycash.data.local.dao.TransactionDao
import com.example.dailycash.data.local.entity.BudgetEntity
import com.example.dailycash.data.local.entity.FixedExpenseEntity
import com.example.dailycash.data.local.entity.TransactionEntity

@Database(entities = [TransactionEntity::class, FixedExpenseEntity::class, BudgetEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun fixedExpenseDao(): FixedExpenseDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_cash_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
