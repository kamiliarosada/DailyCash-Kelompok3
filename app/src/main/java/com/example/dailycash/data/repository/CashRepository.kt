package com.example.dailycash.data.repository

import androidx.lifecycle.LiveData
import com.example.dailycash.data.local.dao.BudgetDao
import com.example.dailycash.data.local.dao.FixedExpenseDao
import com.example.dailycash.data.local.dao.TransactionDao
import com.example.dailycash.data.local.entity.BudgetEntity
import com.example.dailycash.data.local.entity.FixedExpenseEntity
import com.example.dailycash.data.local.entity.TransactionEntity
import com.example.dailycash.data.remote.CurrencyApiService
import com.example.dailycash.data.remote.QuoteApiService
import com.example.dailycash.data.remote.QuoteResponse

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CashRepository(
    private val transactionDao: TransactionDao,
    private val fixedExpenseDao: FixedExpenseDao,
    private val budgetDao: BudgetDao,
    private val quoteApiService: QuoteApiService,
    private val currencyApiService: CurrencyApiService
) {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun convertCurrency(from: String, to: String, amount: Double): Double {
        if (from == to) return amount
        return try {
            val apiKey = "006d649987823f6630f9a2e1" 
            val response = currencyApiService.convertCurrency(apiKey, from, to, amount)
            android.util.Log.d("CurrencyConvert", "API Success: $amount $from -> ${response.conversion_result} $to")
            response.conversion_result
        } catch (e: Exception) {
            android.util.Log.e("CurrencyConvert", "API Error: ${e.message}")
            // Hardcoded Fallback rates if API fails
            val rate = when (from) {
                "USD" -> 15750.0
                "EUR" -> 16800.0
                "JPY" -> 105.0
                "SGD" -> 11600.0
                else -> 1.0
            }
            val result = amount * rate
            android.util.Log.d("CurrencyConvert", "Using Fallback: $amount $from -> $result $to")
            result
        }
    }

    fun getAllTransactions(userId: String) = transactionDao.getAllTransactions(userId)

    fun getTransactionsByDateRange(userId: String, startDate: Long, endDate: Long) =
        transactionDao.getTransactionsByDateRange(userId, startDate, endDate)

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
        backupTransactionToFirestore(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
        backupTransactionToFirestore(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        deleteTransactionFromFirestore(transaction)
    }

    private fun backupTransactionToFirestore(transaction: TransactionEntity) {
        if (transaction.userId.isEmpty()) return
        firestore.collection("users").document(transaction.userId)
            .collection("transactions").document(transaction.id)
            .set(transaction)
    }

    private fun deleteTransactionFromFirestore(transaction: TransactionEntity) {
        if (transaction.userId.isEmpty()) return
        firestore.collection("users").document(transaction.userId)
            .collection("transactions").document(transaction.id)
            .delete()
    }

    suspend fun syncTransactionsFromFirestore(userId: String) {
        if (userId.isEmpty()) return
        try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("transactions").get().await()
            val remoteTransactions = snapshot.toObjects(TransactionEntity::class.java)
            remoteTransactions.forEach { transaction ->
                transactionDao.insertTransaction(transaction)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTotalIncome(userId: String) = transactionDao.getTotalIncome(userId)
    fun getTotalExpense(userId: String) = transactionDao.getTotalExpense(userId)

    fun getIncomeByDateRange(userId: String, startDate: Long, endDate: Long) =
        transactionDao.getIncomeByDateRange(userId, startDate, endDate)

    fun getExpenseByDateRange(userId: String, startDate: Long, endDate: Long) =
        transactionDao.getExpenseByDateRange(userId, startDate, endDate)

    fun getAllFixedExpenses(userId: String) = fixedExpenseDao.getAllFixedExpenses(userId)
    suspend fun insertFixedExpense(fixedExpense: FixedExpenseEntity) = fixedExpenseDao.insertFixedExpense(fixedExpense)
    suspend fun updateFixedExpense(fixedExpense: FixedExpenseEntity) = fixedExpenseDao.updateFixedExpense(fixedExpense)
    suspend fun deleteFixedExpense(fixedExpense: FixedExpenseEntity) = fixedExpenseDao.deleteFixedExpense(fixedExpense)
    fun getTotalFixedExpense(userId: String) = fixedExpenseDao.getTotalFixedExpense(userId)

    fun getBudget(userId: String) = budgetDao.getBudget(userId)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity) = budgetDao.insertOrUpdateBudget(budget)

    suspend fun getRandomQuote(): List<QuoteResponse> = quoteApiService.getRandomQuote()
}
