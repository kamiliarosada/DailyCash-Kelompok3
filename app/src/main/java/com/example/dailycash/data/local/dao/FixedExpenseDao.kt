package com.example.dailycash.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.dailycash.data.local.entity.FixedExpenseEntity

@Dao
interface FixedExpenseDao {
    @Query("SELECT * FROM fixed_expenses WHERE userId = :userId ORDER BY date DESC")
    fun getAllFixedExpenses(userId: String): LiveData<List<FixedExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedExpense(fixedExpense: FixedExpenseEntity)

    @Update
    suspend fun updateFixedExpense(fixedExpense: FixedExpenseEntity)

    @Delete
    suspend fun deleteFixedExpense(fixedExpense: FixedExpenseEntity)

    @Query("SELECT SUM(amount) FROM fixed_expenses WHERE userId = :userId")
    fun getTotalFixedExpense(userId: String): LiveData<Double>
}
