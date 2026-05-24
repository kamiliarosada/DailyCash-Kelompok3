package com.example.dailycash.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dailycash.data.local.entity.TransactionEntity
import com.example.dailycash.databinding.ItemTransactionBinding

class TransactionAdapter(
    private var transactions: List<TransactionEntity>,
    private val onItemClick: (TransactionEntity) -> Unit,
    private val onItemLongClick: (TransactionEntity) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = transactions[position]
        holder.binding.tvItemTitle.text = item.title
        holder.binding.tvItemAmount.text = if (item.type == "pemasukan") "+ Rp ${item.amount}" else "- Rp ${item.amount}"
        holder.binding.tvItemCategory.text = item.category
        holder.binding.root.setOnClickListener {
            onItemClick(item)
        }
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
