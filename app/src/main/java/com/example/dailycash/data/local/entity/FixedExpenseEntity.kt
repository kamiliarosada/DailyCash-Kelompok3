package com.example.dailycash.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fixed_expenses")
data class FixedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val originalAmount: Double = 0.0,
    val currency: String = "IDR",
    val category: String = "",
    val period: String = "Monthly", // "Daily", "Weekly", "Monthly", "Yearly"
    val date: Long = 0L
)
