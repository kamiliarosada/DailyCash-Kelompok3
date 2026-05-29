package com.example.dailycash.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailycash.data.local.entity.TransactionEntity
import com.example.dailycash.databinding.FragmentTransactionBinding
import com.example.dailycash.ui.adapter.TransactionAdapter
import com.example.dailycash.viewmodel.CashViewModel
import com.google.firebase.auth.FirebaseAuth

import androidx.appcompat.app.AlertDialog
import com.example.dailycash.databinding.DialogAddTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CashViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: TransactionAdapter
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private val currentTransactions = MutableLiveData<List<TransactionEntity>>()
    private var activeFilterLiveData: androidx.lifecycle.LiveData<List<TransactionEntity>>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = auth.currentUser?.uid ?: ""
        setupRecyclerView(userId)
        setupCategoryClickListeners(userId)
        setupFilterListeners(userId)

        currentTransactions.observe(viewLifecycleOwner) { transactions ->
            adapter.updateData(transactions ?: emptyList())
            updateSummary(transactions ?: emptyList())
        }

        viewModel.syncTransactions(userId)
        loadTransactions(userId, "Semua")
    }

    private fun setupRecyclerView(userId: String) {
        adapter = TransactionAdapter(
            emptyList(),
            onItemClick = { transaction ->
                showTransactionDialog(userId, transaction)
            },
            onItemLongClick = { transaction ->
                showDeleteConfirmation(transaction)
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(context)
        binding.rvTransactions.adapter = adapter
    }

    private fun setupCategoryClickListeners(userId: String) {
        val listener = View.OnClickListener { v ->
            val category = when(v.id) {
                binding.catFood.id -> "Makan"
                binding.catTransport.id -> "Bensin"
                binding.catShopping.id -> "Belanja"
                binding.catEntertainment.id -> "Hiburan"
                binding.catHealth.id -> "Kesehatan"
                else -> "Lainnya"
            }
            showTransactionDialog(userId, category = category)
        }

        binding.catFood.setOnClickListener(listener)
        binding.catTransport.setOnClickListener(listener)
        binding.catShopping.setOnClickListener(listener)
        binding.catEntertainment.setOnClickListener(listener)
        binding.catHealth.setOnClickListener(listener)
        binding.catOthers.setOnClickListener(listener)
    }

    private fun setupFilterListeners(userId: String) {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val period = when(checkedIds[0]) {
                    binding.chipDaily.id -> "Harian"
                    binding.chipWeekly.id -> "Mingguan"
                    binding.chipMonthly.id -> "Bulanan"
                    binding.chipYearly.id -> "Tahunan"
                    binding.chipCustomDate.id -> "Pilih Tanggal"
                    else -> "Semua"
                }
                
                if (period == "Pilih Tanggal") {
                    showDatePickerFilter(userId)
                } else {
                    loadTransactions(userId, period)
                }
            }
        }
    }

    private fun showDatePickerFilter(userId: String) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            calendar.set(year, month, day)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val start = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val end = calendar.timeInMillis
            
            binding.tvPeriodTitle.text = "Filter: ${dateFormat.format(calendar.time)}"
            switchTransactionSource(viewModel.getTransactionsByDateRange(userId, start, end))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadTransactions(userId: String, period: String) {
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis
        
        when(period) {
            "Semua" -> {
                binding.tvPeriodTitle.text = "Semua Transaksi"
                switchTransactionSource(viewModel.getAllTransactions(userId))
            }
            "Harian" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                binding.tvPeriodTitle.text = "Hari Ini"
                switchTransactionSource(viewModel.getTransactionsByDateRange(userId, calendar.timeInMillis, end))
            }
            "Mingguan" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                binding.tvPeriodTitle.text = "7 Hari Terakhir"
                switchTransactionSource(viewModel.getTransactionsByDateRange(userId, calendar.timeInMillis, end))
            }
            "Bulanan" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                binding.tvPeriodTitle.text = "Bulan Ini"
                switchTransactionSource(viewModel.getTransactionsByDateRange(userId, calendar.timeInMillis, end))
            }
            "Tahunan" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                binding.tvPeriodTitle.text = "Tahun Ini"
                switchTransactionSource(viewModel.getTransactionsByDateRange(userId, calendar.timeInMillis, end))
            }
        }
    }

    private fun switchTransactionSource(newLiveData: androidx.lifecycle.LiveData<List<TransactionEntity>>) {
        activeFilterLiveData?.removeObservers(viewLifecycleOwner)
        activeFilterLiveData = newLiveData
        activeFilterLiveData?.observe(viewLifecycleOwner) {
            currentTransactions.value = it
        }
    }

    private fun updateSummary(transactions: List<TransactionEntity>) {
        val income = transactions.filter { it.type == "pemasukan" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "pengeluaran" }.sumOf { it.amount }
        
        binding.tvSummaryIncome.text = "Masuk: Rp $income"
        binding.tvSummaryExpense.text = "Keluar: Rp $expense"
    }

    private fun showTransactionDialog(userId: String, transaction: TransactionEntity? = null, category: String? = null) {
        val dialogBinding = DialogAddTransactionBinding.inflate(layoutInflater)
        val isEdit = transaction != null
        var selectedDate = transaction?.date ?: System.currentTimeMillis()

        dialogBinding.etDate.setText(dateFormat.format(Date(selectedDate)))

        if (isEdit) {
            dialogBinding.etTitle.setText(transaction?.title)
            dialogBinding.etAmount.setText(transaction?.amount.toString())
            dialogBinding.etCategory.setText(transaction?.category)
            if (transaction?.type == "pemasukan") {
                dialogBinding.rbIncome.isChecked = true
            } else {
                dialogBinding.rbExpense.isChecked = true
            }
        } else if (category != null) {
            dialogBinding.etCategory.setText(category)
        }

        dialogBinding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.timeInMillis
                dialogBinding.etDate.setText(dateFormat.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Ubah Transaksi" else "Tambah Transaksi")
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { _, _ ->
                val title = dialogBinding.etTitle.text.toString()
                val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
                val cat = dialogBinding.etCategory.text.toString()
                val type = if (dialogBinding.rbIncome.isChecked) "pemasukan" else "pengeluaran"

                if (title.isNotEmpty() && amount > 0) {
                    val updatedTransaction = transaction?.copy(
                        title = title,
                        amount = amount,
                        category = cat,
                        type = type,
                        date = selectedDate
                    ) ?: TransactionEntity(
                        userId = userId,
                        title = title,
                        amount = amount,
                        category = cat,
                        type = type,
                        date = selectedDate,
                        note = ""
                    )

                    if (isEdit) viewModel.updateTransaction(updatedTransaction)
                    else viewModel.insertTransaction(updatedTransaction)
                } else {
                    Toast.makeText(context, "Mohon isi data dengan benar", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteConfirmation(transaction: TransactionEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus")
            .setMessage("Hapus transaksi ini?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
