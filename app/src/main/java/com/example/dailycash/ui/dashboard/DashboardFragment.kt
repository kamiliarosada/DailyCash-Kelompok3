package com.example.dailycash.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dailycash.R
import com.example.dailycash.databinding.FragmentDashboardBinding
import com.example.dailycash.viewmodel.CashViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import androidx.appcompat.app.AlertDialog
import com.example.dailycash.data.local.entity.BudgetEntity
import com.example.dailycash.databinding.DialogSetBudgetBinding
import com.example.dailycash.utils.PreferenceManager

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CashViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var preferenceManager: PreferenceManager
    private var actualBalance = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())

        val userId = auth.currentUser?.uid ?: ""
        
        setupObservers(userId)
        setupClickListeners()
        viewModel.fetchQuote()
    }

    private fun setupClickListeners() {
        binding.btnSetBudget.setOnClickListener {
            showSetBudgetDialog(auth.currentUser?.uid ?: "")
        }

        binding.btnToggleBalance.setOnClickListener {
            val isVisible = preferenceManager.isBalanceVisible()
            preferenceManager.setBalanceVisible(!isVisible)
            updateBalanceDisplay()
        }

        binding.cardIncome.setOnClickListener { findNavController().navigate(R.id.navigation_transactions) }
        binding.cardExpense.setOnClickListener { findNavController().navigate(R.id.navigation_transactions) }
        binding.cardFixed.setOnClickListener { findNavController().navigate(R.id.navigation_fixed_expenses) }
    }

    private fun setupObservers(userId: String) {
        viewModel.getBudget(userId).observe(viewLifecycleOwner) { budget ->
            val budgetAmount = budget?.amount ?: 0.0
            val period = budget?.period ?: "Monthly"
            binding.tvMonthlyBudget.text = "Rp ${String.format("%,.0f", budgetAmount)} ($period)"
            calculateFinance(budgetAmount, period, userId)
        }

        viewModel.quote.observe(viewLifecycleOwner) { quote ->
            if (quote != null) {
                binding.tvQuote.text = "\"${quote.text}\""
                binding.tvQuoteAuthor.text = "- ${quote.author}"
            }
        }
    }

    private fun calculateFinance(budget: Double, period: String, userId: String) {
        viewModel.getTotalIncome(userId).observe(viewLifecycleOwner) { income ->
            viewModel.getTotalExpense(userId).observe(viewLifecycleOwner) { expense ->
                viewModel.getTotalFixedExpense(userId).observe(viewLifecycleOwner) { fixed ->
                    
                    val incomeVal = income ?: 0.0
                    val expenseVal = expense ?: 0.0
                    val fixedVal = fixed ?: 0.0
                    
                    binding.tvTotalIncome.text = "Rp ${String.format("%,.0f", incomeVal)}"
                    binding.tvTotalExpense.text = "Rp ${String.format("%,.0f", expenseVal)}"
                    binding.tvTotalFixed.text = "Rp ${String.format("%,.0f", fixedVal)}"

                    actualBalance = budget + incomeVal - fixedVal - expenseVal
                    updateBalanceDisplay()

                    val remainingDays = when(period) {
                        "Daily" -> 1
                        "Weekly" -> 7
                        "Monthly" -> getRemainingDaysInMonth()
                        "Yearly" -> 365
                        else -> 1
                    }
                    
                    val dailyBudget = if (remainingDays > 0) actualBalance / remainingDays else 0.0
                    binding.tvDailyBudget.text = "Rp ${String.format("%,.0f", dailyBudget)} / hari"
                    binding.tvRemainingDays.text = "Sisa $remainingDays hari lagi"

                    updateStatus(actualBalance, dailyBudget)
                }
            }
        }
    }

    private fun updateBalanceDisplay() {
        if (preferenceManager.isBalanceVisible()) {
            binding.tvRemainingBalance.text = "Rp ${String.format("%,.0f", actualBalance)}"
            binding.btnToggleBalance.setImageResource(android.R.drawable.ic_menu_view)
        } else {
            binding.tvRemainingBalance.text = "Rp *********"
            binding.btnToggleBalance.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        }
    }

    private fun updateStatus(sisa: Double, daily: Double) {
        val (status, bgRes) = when {
            sisa < 0 -> "BOROS" to R.drawable.bg_status_danger
            sisa < (daily * 3) -> "WASPADA" to R.drawable.bg_status_warning
            else -> "AMAN" to R.drawable.bg_status_safe
        }
        binding.tvFinancialStatus.text = "Kondisi: $status"
        binding.tvFinancialStatus.setBackgroundResource(bgRes)
    }

    private fun getRemainingDaysInMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_MONTH) + 1
    }

    private fun showSetBudgetDialog(userId: String) {
        val dialogBinding = DialogSetBudgetBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle("Atur Anggaran")
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { _, _ ->
                val amount = dialogBinding.etBudgetAmount.text.toString().toDoubleOrNull() ?: 0.0
                val period = when (dialogBinding.rgBudgetPeriod.checkedRadioButtonId) {
                    dialogBinding.rbBudgetDaily.id -> "Daily"
                    dialogBinding.rbBudgetWeekly.id -> "Weekly"
                    dialogBinding.rbBudgetMonthly.id -> "Monthly"
                    dialogBinding.rbBudgetYearly.id -> "Yearly"
                    else -> "Monthly"
                }
                if (amount > 0) viewModel.insertOrUpdateBudget(BudgetEntity(userId, amount, period))
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
