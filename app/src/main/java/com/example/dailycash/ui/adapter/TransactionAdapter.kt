package com.example.dailycash.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dailycash.R
import com.example.dailycash.data.local.entity.TransactionEntity
import com.example.dailycash.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var transactions: List<TransactionEntity>,
    private val onItemClick: (TransactionEntity) -> Unit,
    private val onItemLongClick: (TransactionEntity) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    inner class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = transactions[position]
        holder.binding.tvItemTitle.text = item.title
        
        val prefix = if (item.type == "pemasukan") "+" else "-"
        holder.binding.tvItemAmount.text = "$prefix Rp ${item.amount}"
        
        val dateStr = dateFormat.format(Date(item.date))
        holder.binding.tvItemCategory.text = "${item.category} • $dateStr"

        // Set icon based on category
        val iconRes = when (item.category.lowercase()) {
            "makan" -> R.drawable.ic_food
            "bensin" -> R.drawable.ic_gas_station
            "belanja" -> R.drawable.ic_shopping_cart
            "hiburan" -> R.drawable.ic_entertainment
            "kesehatan" -> R.drawable.ic_health
            else -> R.drawable.ic_others
        }
        holder.binding.ivItemIcon.setImageResource(iconRes)

        holder.binding.root.setOnClickListener { onItemClick(item) }
        holder.binding.root.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newData: List<TransactionEntity>) {
        transactions = newData
        notifyDataSetChanged()
    }
}
