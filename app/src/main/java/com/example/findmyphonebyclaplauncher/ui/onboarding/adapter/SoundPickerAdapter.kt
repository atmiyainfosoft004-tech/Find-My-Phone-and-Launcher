package com.example.findmyphonebyclaplauncher.ui.onboarding.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.findmyphonebyclaplauncher.R
import com.example.findmyphonebyclaplauncher.databinding.ItemSoundPickerBinding

data class SoundItem(
    val id: String,
    val name: String,
    @param:DrawableRes val iconRes: Int
)

class SoundPickerAdapter(
    private val sounds: List<SoundItem>,
    private var selectedId: String,
    private val onSoundSelected: (SoundItem) -> Unit
) : RecyclerView.Adapter<SoundPickerAdapter.SoundViewHolder>() {

    inner class SoundViewHolder(val binding: ItemSoundPickerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val binding = ItemSoundPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SoundViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        val sound = sounds[position]
        val context = holder.itemView.context
        val isSelected = sound.id == selectedId

        holder.binding.imgSoundIcon.setImageResource(sound.iconRes)
        holder.binding.txtSoundName.text = sound.name

        if (isSelected) {
            holder.binding.cardSound.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.color_sound_item_selected_bg)
            )
            holder.binding.cardSound.strokeColor =
                ContextCompat.getColor(context, R.color.color_primary)
            holder.binding.cardSound.strokeWidth = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)

            holder.binding.cardCheckmark.visibility = View.VISIBLE

            holder.binding.cardIconCircle.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.color_primary)
            )
            holder.binding.imgSoundIcon.setColorFilter(
                ContextCompat.getColor(context, R.color.white)
            )
            holder.binding.txtSoundName.setTextColor(
                ContextCompat.getColor(context, R.color.color_primary)
            )
        } else {
            holder.binding.cardSound.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.white)
            )
            holder.binding.cardSound.strokeColor =
                ContextCompat.getColor(context, R.color.color_onboarding_stroke)
            holder.binding.cardSound.strokeWidth = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)

            holder.binding.cardCheckmark.visibility = View.GONE

            holder.binding.cardIconCircle.setCardBackgroundColor(
                Color.parseColor("#FFFFFF")
            )
            holder.binding.imgSoundIcon.setColorFilter(
                Color.parseColor("#475569")
            )
            holder.binding.txtSoundName.setTextColor(
                ContextCompat.getColor(context, R.color.color_onboarding_text_primary)
            )
        }

        holder.itemView.setOnClickListener {
            selectedId = sound.id
            notifyDataSetChanged()
            onSoundSelected(sound)
        }
    }

    override fun getItemCount(): Int = sounds.size
}
