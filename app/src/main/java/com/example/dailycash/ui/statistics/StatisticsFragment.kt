package com.example.dailycash.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailycash.R
import com.example.dailycash.data.local.entity.TransactionEntity
import com.example.dailycash.databinding.FragmentStatisticsBinding
import com.example.dailycash.ui.adapter.TransactionAdapter
import com.example.dailycash.viewmodel.CashViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CashViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: TransactionAdapter
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var allTransactions: List<TransactionEntity> = emptyList()
    private var allFixedExpenses: List<com.example.dailycash.data.local.entity.FixedExpenseEntity> = emptyList()
    private var lastSelectedMonth: CalendarDay = CalendarDay.today()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCalendar()
        
        val userId = auth.currentUser?.uid ?: ""
        
        viewModel.getAllTransactions(userId).observe(viewLifecycleOwner) { transactions ->
            allTransactions = transactions ?: emptyList()
            updateChartForSelectedMonth(lastSelectedMonth)
            updateCalendarDots(allTransactions)
            filterTransactionsByDate(CalendarDay.today())
        }

        viewModel.getAllFixedExpenses(userId).observe(viewLifecycleOwner) { fixed ->
            allFixedExpenses = fixed ?: emptyList()
            updateChartForSelectedMonth(lastSelectedMonth)
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            emptyList(),
            onItemClick = { transaction -> 
                showEditTransactionDialog(transaction)
            },
            onItemLongClick = { transaction ->
                showDeleteConfirmation(transaction)
            }
        )
        binding.rvDateTransactions.layoutManager = LinearLayoutManager(context)
        binding.rvDateTransactions.adapter = adapter
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

    private fun setupCalendar() {
        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            filterTransactionsByDate(date)
        }
        binding.calendarView.setOnMonthChangedListener { _, date ->
            lastSelectedMonth = date
            updateChartForSelectedMonth(date)
        }
        binding.calendarView.setSelectedDate(CalendarDay.today())
    }

    private fun updateChartForSelectedMonth(date: CalendarDay) {
        val monthTransactions = allTransactions.filter {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            cal.get(Calendar.YEAR) == date.year && (cal.get(Calendar.MONTH) + 1) == date.month
        }
        
        // Sum expenses and income
        val income = monthTransactions.filter { it.type == "pemasukan" }.sumOf { it.amount }.toFloat()
        var expense = monthTransactions.filter { it.type == "pengeluaran" }.sumOf { it.amount }.toFloat()
        
        // Add fixed expenses (tagihan rutin) to the monthly expense chart
        val totalFixed = allFixedExpenses.sumOf { it.amount }.toFloat()
        expense += totalFixed

        val entries = mutableListOf<PieEntry>()
        if (income > 0) entries.add(PieEntry(income, "Pemasukan"))
        if (expense > 0) entries.add(PieEntry(expense, "Pengeluaran"))

        if (entries.isEmpty()) {
            binding.pieChart.clear()
            binding.pieChart.centerText = "Data Kosong"
            binding.pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.income_green),
            ContextCompat.getColor(requireContext(), R.color.expense_red)
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.description.isEnabled = false
        binding.pieChart.centerText = "Bulan ${date.month}/${date.year}"
        binding.pieChart.animateY(1000)
        binding.pieChart.invalidate()
    }

    private fun updateChart(transactions: List<TransactionEntity>) {
        // This method is now replaced by updateChartForSelectedMonth logic internally
    }

    private fun updateCalendarDots(transactions: List<TransactionEntity>) {
        val datesWithTransactions = transactions.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        }.toSet()

        binding.calendarView.addDecorator(EventDecorator(Color.RED, datesWithTransactions))
    }

    private fun filterTransactionsByDate(date: CalendarDay) {
        val calendar = Calendar.getInstance()
        calendar.set(date.year, date.month - 1, date.day)
        val selectedDateStr = dateFormat.format(calendar.time)
        binding.tvSelectedDate.text = "Transaksi Tanggal: $selectedDateStr"

        val filtered = allTransactions.filter {
            val transCal = Calendar.getInstance()
            transCal.timeInMillis = it.date
            transCal.get(Calendar.YEAR) == date.year &&
            transCal.get(Calendar.MONTH) == date.month - 1 &&
            transCal.get(Calendar.DAY_OF_MONTH) == date.day
        }
        adapter.updateData(filtered)
    }

    class EventDecorator(private val color: Int, private val dates: Collection<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(5f, color))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Helper for Float conversion (14sp)
    private val Int.sp: Float get() = this * resources.displayMetrics.scaledDensity
}
