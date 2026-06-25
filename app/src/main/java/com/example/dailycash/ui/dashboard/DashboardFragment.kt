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
import com.example.dailycash.data.local.entity.TransactionEntity
import com.example.dailycash.databinding.DialogSetBudgetBinding
import com.example.dailycash.utils.PreferenceManager

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CashViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: com.example.dailycash.ui.adapter.TransactionAdapter
    private var actualBalance = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())

        setupHeader()
        setupRecyclerView()
        val userId = auth.currentUser?.uid ?: ""
        
        setupObservers(userId)
        setupClickListeners()
        viewModel.fetchQuote()
    }

    private fun setupHeader() {
        val user = auth.currentUser
        val name = user?.displayName ?: user?.email?.split("@")?.get(0) ?: "User"
        binding.tvHelloUser.text = "Halo, $name! 👋"
        
        // Load Profile Image from path safely
        preferenceManager.getProfileImageUri()?.let { path ->
            try {
                val file = java.io.File(path)
                if (file.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(path)
                    binding.ivDashboardProfile.setImageBitmap(bitmap)
                    binding.ivDashboardProfile.clearColorFilter() // Clear tint to show photo
                    binding.ivDashboardProfile.setPadding(0, 0, 0, 0)
                    binding.ivDashboardProfile.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = com.example.dailycash.ui.adapter.TransactionAdapter(
            emptyList(),
            onItemClick = { transaction -> 
                showEditTransactionDialog(transaction)
            },
            onItemLongClick = { transaction ->
                showDeleteConfirmation(transaction)
            }
        )
        binding.rvRecentTransactions.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvRecentTransactions.adapter = adapter
    }

    private fun showDeleteConfirmation(transaction: TransactionEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Transaksi")
            .setMessage("Apakah Anda yakin ingin menghapus transaksi ini?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showEditTransactionDialog(transaction: TransactionEntity) {
        val dialogBinding = com.example.dailycash.databinding.DialogEditTransactionBinding.inflate(layoutInflater)
        dialogBinding.etEditAmount.setText(transaction.amount.toString())
        dialogBinding.etEditNote.setText(transaction.title)

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { _, _ ->
                val newAmount = dialogBinding.etEditAmount.text.toString().toDoubleOrNull() ?: transaction.amount
                val newTitle = dialogBinding.etEditNote.text.toString()
                
                val updatedTransaction = transaction.copy(
                    amount = newAmount,
                    title = if (newTitle.isNotEmpty()) newTitle else transaction.title,
                    category = if (newTitle.isNotEmpty()) newTitle else transaction.category
                )
                viewModel.updateTransaction(updatedTransaction)
            }
            .setNegativeButton("Hapus") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNeutralButton("Batal", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnSetBudget.setOnClickListener {
            showSetBudgetDialog()
        }

        binding.btnToggleBalance.setOnClickListener {
            val isVisible = preferenceManager.isBalanceVisible()
            preferenceManager.setBalanceVisible(!isVisible)
            updateBalanceDisplay()
        }

        binding.cardIncome.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isIncome", true) }
            findNavController().navigate(R.id.navigation_add_transaction, bundle)
        }
        binding.cardExpense.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isIncome", false) }
            findNavController().navigate(R.id.navigation_add_transaction, bundle)
        }
        binding.btnViewAll.setOnClickListener {
            findNavController().navigate(R.id.navigation_statistics)
        }
        
        binding.cardProfileIcon.setOnClickListener {
            findNavController().navigate(R.id.navigation_profile)
        }
    }

    private var currentBudget = 0.0
    private var currentIncome = 0.0
    private var currentExpense = 0.0
    private var currentFixed = 0.0
    private var currentPeriod = "Monthly"

    private fun setupObservers(userId: String) {
        val (monthStart, monthEnd) = getCurrentMonthRange()

        viewModel.getBudget(userId).observe(viewLifecycleOwner) { budget ->
            currentBudget = budget?.amount ?: 0.0
            currentPeriod = budget?.period ?: "Monthly"
            
            binding.tvMonthlyBudget.text = getString(R.string.rp_format, String.format("%,.0f", currentBudget))
            binding.tvBudgetTitle.text = when(currentPeriod) {
                "Daily" -> "Anggaran Harian"
                "Weekly" -> "Anggaran Mingguan"
                "Yearly" -> "Anggaran Tahunan"
                else -> "Anggaran Bulanan"
            }
            updateCalculations()
        }

        // Use Monthly totals for dashboard display
        viewModel.getIncomeByDateRange(userId, monthStart, monthEnd).observe(viewLifecycleOwner) { income ->
            currentIncome = income ?: 0.0
            updateCalculations()
        }
        
        viewModel.getExpenseByDateRange(userId, monthStart, monthEnd).observe(viewLifecycleOwner) { expense ->
            currentExpense = expense ?: 0.0
            updateCalculations()
        }
        
        viewModel.getTotalFixedExpense(userId).observe(viewLifecycleOwner) { fixed ->
            currentFixed = fixed ?: 0.0
            updateCalculations()
        }

        viewModel.getAllTransactions(userId).observe(viewLifecycleOwner) { transactions ->
            val recent = transactions.sortedByDescending { it.date }.take(5)
            adapter.updateData(recent)
        }

        viewModel.quote.observe(viewLifecycleOwner) { quote ->
            if (quote != null) {
                binding.tvQuote.text = getString(R.string.quote_format, quote.text)
            }
        }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        
        return Pair(start, end)
    }

    private fun updateCalculations() {
        // Balance = Initial Budget + Month Income - Bills - Month Expenses
        actualBalance = currentBudget + currentIncome - currentFixed - currentExpense
        updateBalanceDisplay()

        val remainingDays = when(currentPeriod) {
            "Daily" -> 1
            "Weekly" -> 7
            "Monthly" -> getRemainingDaysInMonth()
            "Yearly" -> 365
            else -> getRemainingDaysInMonth()
        }
        
        val dailyBudget = if (remainingDays > 0) actualBalance / remainingDays else 0.0
        binding.tvDailyBudget.text = getString(R.string.rp_format, String.format("%,.0f", dailyBudget))
        binding.tvRemainingDays.text = "$remainingDays Hari lagi"

        val totalSpent = currentExpense + currentFixed
        updateStatus(currentBudget, totalSpent, actualBalance)
    }

    private fun updateBalanceDisplay() {
        if (preferenceManager.isBalanceVisible()) {
            binding.tvRemainingBalance.text = getString(R.string.rp_format, String.format("%,.0f", actualBalance))
            binding.btnToggleBalance.setImageResource(android.R.drawable.ic_menu_view)
            
            // Visual warning for negative balance
            if (actualBalance < 0) {
                binding.tvRemainingBalance.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_danger))
            } else {
                binding.tvRemainingBalance.setTextColor(android.graphics.Color.WHITE)
            }
        } else {
            binding.tvRemainingBalance.text = getString(R.string.balance_hidden)
            binding.tvRemainingBalance.setTextColor(android.graphics.Color.WHITE)
            binding.btnToggleBalance.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        }
    }

    private fun updateStatus(budget: Double, spent: Double, sisa: Double) {
        val totalAvailable = budget + currentIncome
        val percentage = if (totalAvailable > 0) (spent / totalAvailable) * 100 else if (spent > 0) 100.0 else 0.0

        binding.budgetProgress.progress = percentage.toInt().coerceIn(0, 100)

        val statusStr = when {
            sisa < 0 -> "KRISIS / MINUS"
            percentage > 90 -> getString(R.string.status_danger)
            percentage > 70 -> getString(R.string.status_warning)
            else -> getString(R.string.status_safe)
        }

        binding.tvFinancialStatus.text = statusStr
        
        // Progress color based on status
        val colorRes = when {
            sisa < 0 || percentage > 90 -> R.color.expense
            percentage > 70 -> R.color.accent_orange
            else -> R.color.income
        }
        binding.budgetProgress.setIndicatorColor(androidx.core.content.ContextCompat.getColor(requireContext(), colorRes))
        binding.tvFinancialStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun getRemainingDaysInMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_MONTH) + 1
    }

    private fun showSetBudgetDialog() {
        val dialogBinding = DialogSetBudgetBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.set_budget_title))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val amount = dialogBinding.etBudgetAmount.text.toString().toDoubleOrNull() ?: 0.0
                val period = when (dialogBinding.rgBudgetPeriod.checkedRadioButtonId) {
                    dialogBinding.rbBudgetDaily.id -> "Daily"
                    dialogBinding.rbBudgetWeekly.id -> "Weekly"
                    dialogBinding.rbBudgetMonthly.id -> "Monthly"
                    dialogBinding.rbBudgetYearly.id -> "Yearly"
                    else -> "Monthly"
                }
                if (amount > 0) {
                    viewModel.insertOrUpdateBudget(BudgetEntity(auth.currentUser?.uid ?: "", amount, period))
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
