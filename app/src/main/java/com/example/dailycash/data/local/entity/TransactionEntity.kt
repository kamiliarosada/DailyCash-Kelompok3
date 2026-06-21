package com.example.dailycash.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey 
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0, // Ini dalam Rupiah (hasil konversi)
    val originalAmount: Double = 0.0, // Ini jumlah asli (misal $5)
    val currency: String = "IDR", // "IDR", "USD", dll
    val category: String = "",
    val type: String = "", // "pemasukan" or "pengeluaran"
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)
