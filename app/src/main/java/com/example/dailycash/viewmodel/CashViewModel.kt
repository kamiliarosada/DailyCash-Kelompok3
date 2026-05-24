package com.example.dailycash.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.dailycash.data.local.AppDatabase
import com.example.dailycash.data.local.entity.BudgetEntity
import com.example.dailycash.data.local.entity.FixedExpenseEntity
import com.example.dailycash.data.local.entity.TransactionEntity
import com.example.dailycash.data.remote.RetrofitClient
import com.example.dailycash.data.remote.QuoteResponse
import com.example.dailycash.data.repository.CashRepository
import kotlinx.coroutines.launch

class CashViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CashRepository
    val quote = MutableLiveData<QuoteResponse?>()
    val isLoading = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CashRepository(
            database.transactionDao(),
            database.fixedExpenseDao(),
            database.budgetDao(),
            RetrofitClient.instance
        )
    }

    fun getAllTransactions(userId: String) = repository.getAllTransactions(userId)
    fun insertTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.insertTransaction(transaction)
    }
    fun updateTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.updateTransaction(transaction)
    }
    fun deleteTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
    }
    fun getTotalIncome(userId: String) = repository.getTotalIncome(userId)
    fun getTotalExpense(userId: String) = repository.getTotalExpense(userId)

    fun getAllFixedExpenses(userId: String) = repository.getAllFixedExpenses(userId)
    fun insertFixedExpense(fixedExpense: FixedExpenseEntity) = viewModelScope.launch {
        repository.insertFixedExpense(fixedExpense)
    }
    fun updateFixedExpense(fixedExpense: FixedExpenseEntity) = viewModelScope.launch {
        repository.updateFixedExpense(fixedExpense)
    }
    fun deleteFixedExpense(fixedExpense: FixedExpenseEntity) = viewModelScope.launch {
        repository.deleteFixedExpense(fixedExpense)
    }
    fun getTotalFixedExpense(userId: String) = repository.getTotalFixedExpense(userId)

    fun getBudget(userId: String) = repository.getBudget(userId)
    fun insertOrUpdateBudget(budget: BudgetEntity) = viewModelScope.launch {
        repository.insertOrUpdateBudget(budget)
    }

    fun syncTransactions(userId: String) = viewModelScope.launch {
        repository.syncTransactionsFromFirestore(userId)
    }

    fun fetchQuote() = viewModelScope.launch {
        isLoading.postValue(true)
        try {
            val response = repository.getRandomQuote()
            if (response.isNotEmpty()) {
                quote.postValue(response[0])
            }
        } catch (e: Exception) {
            errorMessage.postValue("Failed to fetch quote: ${e.message}")
        } finally {
            isLoading.postValue(false)
        }
    }
}
