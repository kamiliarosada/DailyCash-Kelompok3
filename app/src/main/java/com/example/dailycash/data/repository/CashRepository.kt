package com.example.dailycash.data.repository

import androidx.lifecycle.LiveData
import com.example.dailycash.data.local.dao.BudgetDao
import com.example.dailycash.data.local.dao.FixedExpenseDao
import com.example.dailycash.data.local.dao.TransactionDao
import com.example.dailycash.data.local.entity.BudgetEntity
import com.example.dailycash.data.local.entity.FixedExpenseEntity
import com.example.dailycash.data.local.entity.TransactionEntity
import com.example.dailycash.data.remote.QuoteApiService
import com.example.dailycash.data.remote.QuoteResponse

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CashRepository(
    private val transactionDao: TransactionDao,
    private val fixedExpenseDao: FixedExpenseDao,
    private val budgetDao: BudgetDao,
    private val quoteApiService: QuoteApiService
) {
    private val firestore = FirebaseFirestore.getInstance()

    fun getAllTransactions(userId: String) = transactionDao.getAllTransactions(userId)

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
        firestore.collection("users").document(transaction.userId)
            .collection("transactions").document(transaction.id.toString())
            .set(transaction)
    }

    private fun deleteTransactionFromFirestore(transaction: TransactionEntity) {
        firestore.collection("users").document(transaction.userId)
            .collection("transactions").document(transaction.id.toString())
            .delete()
    }

    suspend fun syncTransactionsFromFirestore(userId: String) {
        try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("transactions").get().await()
            val remoteTransactions = snapshot.toObjects(TransactionEntity::class.java)
            remoteTransactions.forEach { transactionDao.insertTransaction(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun getTotalIncome(userId: String) = transactionDao.getTotalIncome(userId)
    fun getTotalExpense(userId: String) = transactionDao.getTotalExpense(userId)

    fun getAllFixedExpenses(userId: String) = fixedExpenseDao.getAllFixedExpenses(userId)
    suspend fun insertFixedExpense(fixedExpense: FixedExpenseEntity) = fixedExpenseDao.insertFixedExpense(fixedExpense)
    suspend fun updateFixedExpense(fixedExpense: FixedExpenseEntity) = fixedExpenseDao.updateFixedExpense(fixedExpense)
    suspend fun deleteFixedExpense(fixedExpense: FixedExpenseEntity) = fixedExpenseDao.deleteFixedExpense(fixedExpense)
    fun getTotalFixedExpense(userId: String) = fixedExpenseDao.getTotalFixedExpense(userId)

    fun getBudget(userId: String) = budgetDao.getBudget(userId)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity) = budgetDao.insertOrUpdateBudget(budget)

    suspend fun getRandomQuote(): List<QuoteResponse> = quoteApiService.getRandomQuote()
}
