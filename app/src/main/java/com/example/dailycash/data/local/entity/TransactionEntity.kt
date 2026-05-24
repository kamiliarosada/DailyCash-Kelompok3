package com.example.dailycash.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "", // "pemasukan" or "pengeluaran"
    val date: Long = 0L,
    val note: String = ""
)
