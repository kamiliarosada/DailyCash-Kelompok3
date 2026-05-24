package com.example.dailycash.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey val userId: String = "",
    val amount: Double = 0.0,
    val period: String = "Monthly" // "Daily", "Weekly", "Monthly", "Yearly"
)
