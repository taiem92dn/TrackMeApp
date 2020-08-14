package com.tngdev.trackmeapp.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tngdev.trackmeapp.R
import com.tngdev.trackmeapp.data.model.Session
import com.tngdev.trackmeapp.databinding.ItemHistoryBinding
import com.tngdev.trackmeapp.util.Utils
import kotlinx.android.synthetic.main.item_history.view.*

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    var data : List<Session> ?=null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return data ?.size ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = data?.get(position)
        holder.binding.tvAvgSpeed.text = String.format("%.1f km/h", Utils.convertMpsToKmh(session?.avgSpeed!!))
        holder.binding.tvDistance.text = String.format("%.1f km", session.distance / 1000)
        holder.binding.tvDuration.text = Utils.convertDurationToString(session.duration)
        holder.binding.tvSessionStartTime.text = Utils.dateToString(session.startTime)

        Glide.with(holder.itemView.context).load(session.thumbnailPath).into(holder.itemView.ivSession)
    }

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}