package com.example.dailycash.ui.fixedexpense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailycash.data.local.entity.FixedExpenseEntity
import com.example.dailycash.databinding.FragmentFixedExpenseBinding
import com.example.dailycash.ui.adapter.FixedExpenseAdapter
import com.example.dailycash.viewmodel.CashViewModel
import com.google.firebase.auth.FirebaseAuth

import androidx.appcompat.app.AlertDialog
import com.example.dailycash.databinding.DialogAddFixedExpenseBinding

class FixedExpenseFragment : Fragment() {

    private var _binding: FragmentFixedExpenseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CashViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: FixedExpenseAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFixedExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = auth.currentUser?.uid ?: ""
        setupRecyclerView(userId)

        viewModel.getAllFixedExpenses(userId).observe(viewLifecycleOwner) { expenses ->
            adapter.updateData(expenses)
            val total = expenses.sumOf { it.amount }
            binding.tvTotalFixedHeader.text = "Rp $total"
        }

        binding.fabAddFixedExpense.setOnClickListener {
            showFixedExpenseDialog(userId)
        }
    }

    private fun setupRecyclerView(userId: String) {
        adapter = FixedExpenseAdapter(
            emptyList(),
            onItemClick = { expense ->
                showFixedExpenseDialog(userId, expense)
            },
            onItemLongClick = { expense ->
                showDeleteConfirmation(expense)
            }
        )
        binding.rvFixedExpenses.layoutManager = LinearLayoutManager(context)
        binding.rvFixedExpenses.adapter = adapter
    }

    private fun showFixedExpenseDialog(userId: String, expense: FixedExpenseEntity? = null) {
        val dialogBinding = DialogAddFixedExpenseBinding.inflate(layoutInflater)
        val isEdit = expense != null

        if (isEdit) {
            dialogBinding.etName.setText(expense?.name)
            dialogBinding.etAmount.setText(expense?.amount.toString())
            when (expense?.period) {
                "Daily" -> dialogBinding.rbDaily.isChecked = true
                "Weekly" -> dialogBinding.rbWeekly.isChecked = true
                "Monthly" -> dialogBinding.rbMonthly.isChecked = true
                "Yearly" -> dialogBinding.rbYearly.isChecked = true
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Ubah Pengeluaran Tetap" else "Tambah Pengeluaran Tetap")
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { _, _ ->
                val name = dialogBinding.etName.text.toString()
                val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
                val period = when (dialogBinding.rgPeriod.checkedRadioButtonId) {
                    dialogBinding.rbDaily.id -> "Daily"
                    dialogBinding.rbWeekly.id -> "Weekly"
                    dialogBinding.rbMonthly.id -> "Monthly"
                    dialogBinding.rbYearly.id -> "Yearly"
                    else -> "Monthly"
                }

                if (name.isNotEmpty() && amount > 0) {
                    val newExpense = expense?.copy(
                        name = name,
                        amount = amount,
                        period = period
                    ) ?: FixedExpenseEntity(
                        userId = userId,
                        name = name,
                        amount = amount,
                        period = period,
                        date = System.currentTimeMillis()
                    )

                    if (isEdit) {
                        viewModel.updateFixedExpense(newExpense)
                    } else {
                        viewModel.insertFixedExpense(newExpense)
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteConfirmation(expense: FixedExpenseEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Pengeluaran")
            .setMessage("Hapus pengeluaran ini?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteFixedExpense(expense)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
