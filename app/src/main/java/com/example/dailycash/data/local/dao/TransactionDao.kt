package com.example.dailycash.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.dailycash.data.local.entity.TransactionEntity

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllTransactions(userId: String): LiveData<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'pemasukan'")
    fun getTotalIncome(userId: String): LiveData<Double>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'pengeluaran'")
    fun getTotalExpense(userId: String): LiveData<Double>
}
