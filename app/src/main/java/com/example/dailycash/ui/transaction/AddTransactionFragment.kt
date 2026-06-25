package com.example.dailycash.ui.transaction

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailycash.R
import com.example.dailycash.data.local.entity.TransactionEntity
import com.example.dailycash.databinding.DialogCustomCategoryBinding
import com.example.dailycash.databinding.FragmentAddTransactionBinding
import com.example.dailycash.databinding.ItemCategoryBinding
import com.example.dailycash.viewmodel.CashViewModel
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CashViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var selectedDate = System.currentTimeMillis()
    private var selectedCategory: String = ""
    private var currentType: String = "pengeluaran"
    private var amountString: String = ""
    private var customCategoryName: String = ""
    private var selectedCurrency: String = "IDR"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupCategories()
        setupKeypad()
        setupDate()
        
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.btnCurrency.setOnClickListener {
            showCurrencyDialog()
        }
        
        // Default selection
        val isIncome = arguments?.getBoolean("isIncome") ?: false
        if (isIncome) {
            binding.tabLayout.getTabAt(1)?.select()
            updateType("pemasukan")
        } else {
            updateType("pengeluaran")
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> updateType("pengeluaran")
                    1 -> updateType("pemasukan")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateType(type: String) {
        currentType = type
        selectedCategory = ""
        customCategoryName = ""
        setupCategories()
    }

    private fun setupCategories() {
        val categories = when (currentType) {
            "pemasukan" -> getIncomeCategories()
            else -> getExpenseCategories()
        }
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvCategories.adapter = CategoryAdapter(categories) { category ->
            selectedCategory = category.name
            if (category.name == "Lainnya") {
                showCustomCategoryDialog()
            }
        }
    }

    private fun showCustomCategoryDialog() {
        val dialogBinding = DialogCustomCategoryBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val input = dialogBinding.etDialogCustomCategory.text.toString()
                if (input.isNotEmpty()) {
                    customCategoryName = input
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showCurrencyDialog() {
        val currencies = arrayOf("IDR", "USD", "EUR", "JPY", "SGD")
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Mata Uang")
            .setItems(currencies) { _, which ->
                selectedCurrency = currencies[which]
                binding.btnCurrency.text = "$selectedCurrency ▾"
            }
            .show()
    }

    private fun setupKeypad() {
        val digitButtons = listOf(
            binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6,
            binding.btn7, binding.btn8, binding.btn9,
            binding.btn0, binding.btnDot
        )

        digitButtons.forEach { btn ->
            btn.setOnClickListener {
                val text = btn.text.toString()
                if (text == "." && amountString.contains(".")) return@setOnClickListener
                amountString += text
                updateAmountDisplay()
            }
        }

        binding.btnDelete.setOnClickListener {
            if (amountString.isNotEmpty()) {
                amountString = amountString.substring(0, amountString.length - 1)
                updateAmountDisplay()
            }
        }

        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun updateAmountDisplay() {
        binding.etAmount.setText(amountString)
    }

    private fun setupDate() {
        binding.btnDate.text = dateFormat.format(Date(selectedDate))
        binding.btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.timeInMillis
                binding.btnDate.text = dateFormat.format(calendar.time)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun saveTransaction() {
        val amount = amountString.toDoubleOrNull() ?: 0.0
        val comment = binding.etComment.text.toString()
        val userId = auth.currentUser?.uid ?: ""
        
        var finalCategory = selectedCategory
        if (selectedCategory == "Lainnya") {
            finalCategory = if (customCategoryName.isNotEmpty()) customCategoryName else "Lainnya"
        }

        if (amount > 0 && finalCategory.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                val convertedAmount = if (selectedCurrency != "IDR") {
                    viewModel.convertCurrency(selectedCurrency, "IDR", amount)
                } else {
                    amount
                }

                val transaction = TransactionEntity(
                    userId = userId,
                    title = finalCategory,
                    amount = convertedAmount,
                    originalAmount = amount,
                    currency = selectedCurrency,
                    category = finalCategory,
                    type = currentType,
                    date = selectedDate,
                    note = comment
                )
                viewModel.insertTransaction(transaction)
                findNavController().popBackStack()
            }
        }
    }

    private fun getExpenseCategories(): List<Category> {
        return listOf(
            Category("Makanan", R.drawable.ic_food, R.color.cat_food),
            Category("Obat", R.drawable.ic_health, R.color.cat_veg),
            Category("Kuliah", android.R.drawable.ic_menu_agenda, R.color.cat_plane),
            Category("Belanja", R.drawable.ic_shopping_cart, R.color.cat_fruit),
            Category("Hiburan", R.drawable.ic_entertainment, R.color.cat_cake),
            Category("Lainnya", android.R.drawable.ic_input_add, R.color.cat_shoes)
        )
    }

    private fun getIncomeCategories(): List<Category> {
        return listOf(
            Category("Tunai", R.drawable.ic_app_logo, R.color.cat_income_cash),
            Category("Gaji", android.R.drawable.ic_menu_recent_history, R.color.cat_income_salary),
            Category("Bonus", android.R.drawable.btn_star_big_on, R.color.cat_cake),
            Category("E-Wallet", android.R.drawable.ic_menu_send, R.color.cat_drink),
            Category("Investasi", android.R.drawable.ic_menu_compass, R.color.cat_plane),
            Category("Lainnya", android.R.drawable.ic_input_add, R.color.cat_shoes)
        )
    }

    data class Category(val name: String, val iconRes: Int, val colorRes: Int)

    inner class CategoryAdapter(
        private val categories: List<Category>,
        private val onClick: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        private var selectedPosition = -1

        inner class ViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]
            holder.binding.tvCategoryName.text = category.name
            holder.binding.ivCategoryIcon.setImageResource(category.iconRes)
            
            val isSelected = selectedPosition == position
            
            if (isSelected) {
                val highlightColor = Color.parseColor("#FFD54F")
                holder.binding.cardCategory.setCardBackgroundColor(highlightColor)
                holder.binding.ivCategoryIcon.setColorFilter(Color.WHITE)
                holder.binding.tvCategoryName.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                holder.binding.cardCategory.scaleX = 1.1f
                holder.binding.cardCategory.scaleY = 1.1f
                holder.binding.cardCategory.elevation = 6f
            } else {
                holder.binding.cardCategory.setCardBackgroundColor(Color.WHITE)
                holder.binding.ivCategoryIcon.setColorFilter(ContextCompat.getColor(requireContext(), category.colorRes))
                holder.binding.tvCategoryName.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                holder.binding.cardCategory.scaleX = 1.0f
                holder.binding.cardCategory.scaleY = 1.0f
                holder.binding.cardCategory.elevation = 2f
            }

            holder.binding.root.setOnClickListener {
                if (selectedPosition != holder.bindingAdapterPosition) {
                    val oldPosition = selectedPosition
                    selectedPosition = holder.bindingAdapterPosition
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onClick(category)
                }
            }
        }

        override fun getItemCount() = categories.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
