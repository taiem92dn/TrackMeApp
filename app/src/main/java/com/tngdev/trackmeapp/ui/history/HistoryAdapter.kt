package com.tngdev.trackmeapp.ui.history

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tngdev.trackmeapp.R
import com.tngdev.trackmeapp.data.model.Session
import com.tngdev.trackmeapp.databinding.ItemHistoryBinding
import com.tngdev.trackmeapp.util.Utils
import kotlinx.android.synthetic.main.item_history.view.*

class HistoryAdapter : PagingDataAdapter<Session, HistoryAdapter.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = getItem(position)
        session ?: return
        holder.binding.tvAvgSpeed.text = String.format("%.1f km/h", Utils.convertMpsToKmh(session?.avgSpeed!!))
        holder.binding.tvDistance.text = String.format("%.1f km", session.distance / 1000)
        holder.binding.tvDuration.text = Utils.convertDurationToString(session.duration)
        holder.binding.tvSessionStartTime.text = Utils.dateToString(session.startTime)

        Glide.with(holder.itemView.context).load(session.thumbnailPath).into(holder.itemView.ivSession)
    }

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        /**
         * This diff callback informs the PagedListAdapter how to compute list differences when new
         * PagedLists arrive.
         * <p>
         * When you add a Session with the 'Add' button, the PagedListAdapter uses diffCallback to
         * detect there's only a single item difference from before, so it only needs to animate and
         * rebind a single view.
         *
         * @see DiffUtil
         */
        private val diffCallback = object : DiffUtil.ItemCallback<Session>() {
            override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean =
                oldItem == newItem

        }
    }
}