package com.example.dailycash.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.dailycash.databinding.FragmentDashboardBinding
import com.example.dailycash.viewmodel.CashViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.*

import androidx.appcompat.app.AlertDialog
import com.example.dailycash.data.local.entity.BudgetEntity
import com.example.dailycash.databinding.DialogSetBudgetBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CashViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = auth.currentUser?.uid ?: ""
        
        setupObservers(userId)
        viewModel.fetchQuote()

        binding.btnSetBudget.setOnClickListener {
            showSetBudgetDialog(userId)
        }
    }

    private fun setupObservers(userId: String) {
        viewModel.getBudget(userId).observe(viewLifecycleOwner) { budget ->
            val budgetAmount = budget?.amount ?: 0.0
            val period = budget?.period ?: "Monthly"
            binding.tvMonthlyBudget.text = "Rp $budgetAmount ($period)"
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
                    
                    binding.tvTotalIncome.text = "Rp $incomeVal"
                    binding.tvTotalExpense.text = "Rp $expenseVal"
                    binding.tvTotalFixed.text = "Rp $fixedVal"

                    val sisaBudget = budget + incomeVal - fixedVal - expenseVal
                    binding.tvRemainingBalance.text = "Rp $sisaBudget"

                    val remainingDays = when(period) {
                        "Daily" -> 1
                        "Weekly" -> 7
                        "Monthly" -> getRemainingDaysInMonth()
                        "Yearly" -> 365
                        else -> 1
                    }
                    
                    val dailyBudget = if (remainingDays > 0) sisaBudget / remainingDays else 0.0
                    binding.tvDailyBudget.text = "Rp ${String.format("%.2f", dailyBudget)} / hari"
                    binding.tvRemainingDays.text = "Sisa $remainingDays hari lagi (Estimasi)"

                    updateStatus(sisaBudget, expenseVal, dailyBudget)
                }
            }
        }
    }

    private fun updateStatus(sisa: Double, expense: Double, daily: Double) {
        val status = when {
            sisa < 0 -> "BOROS"
            sisa < (daily * 3) -> "WASPADA"
            else -> "AMAN"
        }
        binding.tvFinancialStatus.text = status
    }

    private fun getRemainingDaysInMonth(): Int {
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        return daysInMonth - currentDay + 1
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
                
                if (amount > 0) {
                    viewModel.insertOrUpdateBudget(BudgetEntity(userId, amount, period))
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
