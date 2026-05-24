package com.example.dailycash.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailycash.data.local.entity.TransactionEntity
import com.example.dailycash.databinding.FragmentTransactionBinding
import com.example.dailycash.ui.adapter.TransactionAdapter
import com.example.dailycash.viewmodel.CashViewModel
import com.google.firebase.auth.FirebaseAuth

import androidx.appcompat.app.AlertDialog
import com.example.dailycash.databinding.DialogAddTransactionBinding

import com.example.dailycash.databinding.DialogCategoryPickerBinding

class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CashViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = auth.currentUser?.uid ?: ""
        setupRecyclerView(userId)
        setupCategoryClickListeners(userId)

        viewModel.syncTransactions(userId)

        viewModel.getAllTransactions(userId).observe(viewLifecycleOwner) { transactions ->
            adapter.updateData(transactions)
        }
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

    private fun showTransactionDialog(userId: String, transaction: TransactionEntity? = null, category: String? = null) {
        val dialogBinding = DialogAddTransactionBinding.inflate(layoutInflater)
        val isEdit = transaction != null

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

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Ubah Transaksi" else "Tambah Transaksi")
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { _, _ ->
                val title = dialogBinding.etTitle.text.toString()
                val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
                val cat = dialogBinding.etCategory.text.toString()
                val type = if (dialogBinding.rbIncome.isChecked) "pemasukan" else "pengeluaran"

                if (title.isNotEmpty() && amount > 0) {
                    val newTransaction = transaction?.copy(
                        title = title,
                        amount = amount,
                        category = cat,
                        type = type
                    ) ?: TransactionEntity(
                        userId = userId,
                        title = title,
                        amount = amount,
                        category = cat,
                        type = type,
                        date = System.currentTimeMillis(),
                        note = ""
                    )

                    if (isEdit) {
                        viewModel.updateTransaction(newTransaction)
                    } else {
                        viewModel.insertTransaction(newTransaction)
                    }
                } else {
                    Toast.makeText(context, "Mohon isi data dengan benar", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteConfirmation(transaction: TransactionEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTransaction(transaction)
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
