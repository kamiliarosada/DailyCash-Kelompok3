package com.example.dailycash.ui.fixedexpense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailycash.R
import com.example.dailycash.data.local.entity.FixedExpenseEntity
import com.example.dailycash.databinding.FragmentFixedExpenseBinding
import com.example.dailycash.ui.adapter.FixedExpenseAdapter
import com.example.dailycash.viewmodel.CashViewModel
import com.google.firebase.auth.FirebaseAuth

import androidx.appcompat.app.AlertDialog
import com.example.dailycash.databinding.DialogAddFixedExpenseBinding
import kotlinx.coroutines.launch

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
            binding.tvTotalFixedHeader.text = getString(R.string.rp_format, String.format("%,.0f", total))
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
        var selectedCurrency = expense?.currency ?: "IDR"

        if (isEdit) {
            dialogBinding.etName.setText(expense?.name)
            dialogBinding.etCategory.setText(expense?.category)
            dialogBinding.etAmount.setText(expense?.originalAmount?.toString() ?: expense?.amount.toString())
            dialogBinding.btnFixedCurrency.text = "$selectedCurrency ▾"
            when (expense?.period) {
                "Daily" -> dialogBinding.rbDaily.isChecked = true
                "Weekly" -> dialogBinding.rbWeekly.isChecked = true
                "Monthly" -> dialogBinding.rbMonthly.isChecked = true
                "Yearly" -> dialogBinding.rbYearly.isChecked = true
            }
        }

        dialogBinding.btnFixedCurrency.setOnClickListener {
            val currencies = arrayOf("IDR", "USD", "EUR", "JPY", "SGD")
            AlertDialog.Builder(requireContext())
                .setTitle("Pilih Mata Uang")
                .setItems(currencies) { _, which ->
                    selectedCurrency = currencies[which]
                    dialogBinding.btnFixedCurrency.text = "$selectedCurrency ▾"
                }
                .show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) getString(R.string.edit_fixed_title) else getString(R.string.add_fixed_title))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = dialogBinding.etName.text.toString()
                val category = dialogBinding.etCategory.text.toString()
                val originalAmount = dialogBinding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
                val period = when (dialogBinding.rgPeriod.checkedRadioButtonId) {
                    dialogBinding.rbDaily.id -> "Daily"
                    dialogBinding.rbWeekly.id -> "Weekly"
                    dialogBinding.rbMonthly.id -> "Monthly"
                    dialogBinding.rbYearly.id -> "Yearly"
                    else -> "Monthly"
                }

                if (name.isNotEmpty() && originalAmount > 0) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val convertedAmount = if (selectedCurrency != "IDR") {
                            viewModel.convertCurrency(selectedCurrency, "IDR", originalAmount)
                        } else {
                            originalAmount
                        }

                        val newExpense = expense?.copy(
                            name = name,
                            category = category,
                            amount = convertedAmount,
                            originalAmount = originalAmount,
                            currency = selectedCurrency,
                            period = period
                        ) ?: FixedExpenseEntity(
                            userId = userId,
                            name = name,
                            category = category,
                            amount = convertedAmount,
                            originalAmount = originalAmount,
                            currency = selectedCurrency,
                            period = period,
                            date = System.currentTimeMillis()
                        )

                        if (isEdit) {
                            viewModel.updateFixedExpense(newExpense)
                        } else {
                            viewModel.insertFixedExpense(newExpense)
                        }
                    }
                } else {
                    Toast.makeText(context, getString(R.string.input_correctly), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteConfirmation(expense: FixedExpenseEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_title))
            .setMessage(getString(R.string.delete_fixed_confirm))
            .setPositiveButton(getString(R.string.delete_title)) { _, _ ->
                viewModel.deleteFixedExpense(expense)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
