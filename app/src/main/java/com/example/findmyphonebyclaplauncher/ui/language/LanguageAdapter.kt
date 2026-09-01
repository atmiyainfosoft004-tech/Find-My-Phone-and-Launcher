package com.example.findmyphonebyclaplauncher.ui.language

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.findmyphonebyclaplauncher.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val languages: List<LanguageItem>,
    private val onLanguageSelected: (LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition: Int = languages.indexOfFirst { it.isSelected }

    fun getSelectedItem(): LanguageItem? {
        return if (selectedPosition in languages.indices) languages[selectedPosition] else null
    }

    fun setSelectedPosition(position: Int) {
        if (position in languages.indices) {
            val prev = selectedPosition
            selectedPosition = position
            if (prev != RecyclerView.NO_POSITION && prev in languages.indices) {
                notifyItemChanged(prev)
            }
            notifyItemChanged(selectedPosition)
            onLanguageSelected(languages[selectedPosition])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = languages.size

    inner class LanguageViewHolder(
        private val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LanguageItem, isSelected: Boolean) {
            binding.txtLanguageName.text = item.name

            val context = binding.root.context
            if (isSelected) {
                // Selected State: Light blue background fill (#EFF6FF), blue border (#2563EB), dark text (#0F172A)
                binding.cardLanguage.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
                binding.cardLanguage.strokeColor = Color.parseColor("#2563EB")
                binding.cardLanguage.strokeWidth = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)
                binding.txtLanguageName.setTextColor(Color.parseColor("#0F172A"))
            } else {
                // Unselected State: Plain white background (#FFFFFF), subtle gray border (#E2E8F0), text (#334155)
                binding.cardLanguage.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                binding.cardLanguage.strokeColor = Color.parseColor("#E2E8F0")
                binding.cardLanguage.strokeWidth = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)
                binding.txtLanguageName.setTextColor(Color.parseColor("#334155"))
            }

            binding.root.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION && currentPos != selectedPosition) {
                    val prev = selectedPosition
                    selectedPosition = currentPos
                    if (prev != RecyclerView.NO_POSITION && prev in languages.indices) {
                        notifyItemChanged(prev)
                    }
                    notifyItemChanged(selectedPosition)
                    onLanguageSelected(languages[selectedPosition])
                }
            }
        }
    }
}
