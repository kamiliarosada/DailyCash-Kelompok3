package com.example.dailycash.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dailycash.data.local.entity.FixedExpenseEntity
import com.example.dailycash.databinding.ItemFixedExpenseBinding

class FixedExpenseAdapter(
    private var expenses: List<FixedExpenseEntity>,
    private val onItemClick: (FixedExpenseEntity) -> Unit,
    private val onItemLongClick: (FixedExpenseEntity) -> Unit
) : RecyclerView.Adapter<FixedExpenseAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemFixedExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFixedExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = expenses[position]
        holder.binding.tvFixedName.text = item.name
        
        val formattedAmount = String.format("%,.0f", item.amount)
        if (item.currency != "IDR") {
            val original = String.format("%,.2f", item.originalAmount)
            holder.binding.tvFixedAmount.text = "Rp $formattedAmount ($original ${item.currency})"
        } else {
            holder.binding.tvFixedAmount.text = "Rp $formattedAmount"
        }
        
        holder.binding.tvFixedCategory.text = "${item.category} • ${item.period}"

        holder.binding.root.setOnClickListener {
            onItemClick(item)
        }
        holder.binding.root.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount() = expenses.size

    fun updateData(newData: List<FixedExpenseEntity>) {
        expenses = newData
        notifyDataSetChanged()
    }
}
