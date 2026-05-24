package com.example.dailycash.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.dailycash.data.local.entity.BudgetEntity

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget WHERE userId = :userId")
    fun getBudget(userId: String): LiveData<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity)
}
