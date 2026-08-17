package com.example.findmyphonebyclaplauncher.ui.aftercall

import android.graphics.Color
import android.provider.CallLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.findmyphonebyclaplauncher.R

class CallHistoryAdapter(
    private val subtitleFormatter: (CallLogItem) -> String
) : ListAdapter<CallLogItem, CallHistoryAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivType: ImageView = view.findViewById(R.id.iv_call_type)
        val tvName: TextView = view.findViewById(R.id.tv_caller_name)
        val tvTime: TextView = view.findViewById(R.id.tv_call_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_call_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.tvName.text = if (!item.name.isNullOrEmpty()) item.name else item.number
        holder.tvTime.text = subtitleFormatter(item)

        when (item.type) {
            CallLog.Calls.INCOMING_TYPE -> {
                holder.ivType.setImageResource(R.drawable.ic_incoming)
                holder.ivType.setColorFilter(Color.parseColor("#4CAF50"))
            }
            CallLog.Calls.OUTGOING_TYPE -> {
                holder.ivType.setImageResource(R.drawable.ic_outgoing)
                holder.ivType.clearColorFilter()
            }
            CallLog.Calls.MISSED_TYPE -> {
                holder.ivType.setImageResource(R.drawable.ic_incoming)
                holder.ivType.setColorFilter(Color.parseColor("#F44336"))
            }
            else -> {
                holder.ivType.setImageResource(R.drawable.ic_incoming)
                holder.ivType.clearColorFilter()
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CallLogItem>() {
            override fun areItemsTheSame(oldItem: CallLogItem, newItem: CallLogItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CallLogItem, newItem: CallLogItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
