package com.teamvault.budgetvault

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bumptech.glide.Glide
import com.teamvault.budgetvault.data.model.Expense
import com.teamvault.budgetvault.databinding.ItemExpenseBinding
import java.text.DecimalFormat

class ExpenseAdapter(private val expenses: List<Expense>) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expense) {
            binding.descriptionText.text = expense.description ?: "No description"
            binding.dateText.text = expense.date ?: "No date"
            binding.categoryText.text = expense.category ?: "Uncategorized"

            val formattedAmount = "R${DecimalFormat("#,##0.00").format(expense.amount)}"
            binding.amountText.text = formattedAmount

            if (!expense.photoUri.isNullOrEmpty()) {
                binding.imagePhoto.visibility = View.VISIBLE
                binding.viewImageButton.visibility = View.VISIBLE

                Glide.with(binding.root.context)
                    .load(expense.photoUri)
                    .into(binding.imagePhoto)

                binding.viewImageButton.setOnClickListener {
                    val dialog = android.app.AlertDialog.Builder(binding.root.context).create()
                    val imageView = ImageView(binding.root.context)
                    imageView.load(expense.photoUri)
                    dialog.setView(imageView)
                    dialog.setButton(android.app.AlertDialog.BUTTON_NEGATIVE, "Close") { d, _ -> d.dismiss() }
                    dialog.show()
                }
            } else {
                binding.imagePhoto.visibility = View.GONE
                binding.viewImageButton.visibility = View.GONE
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount(): Int = expenses.size
}
