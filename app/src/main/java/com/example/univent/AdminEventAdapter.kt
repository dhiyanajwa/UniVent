package com.example.univent

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.univent.databinding.ItemAdminEventBinding
import com.bumptech.glide.Glide


class AdminEventAdapter(
    private val onEditClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit
) : ListAdapter<Event, AdminEventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemAdminEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(private val binding: ItemAdminEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.tvEventTitle.text = event.title
            binding.tvEventDate.text = "${event.date} | ${event.time}"

            // Load Image using Glide for a "Professional UI"
            Glide.with(binding.ivEventImage.context)
                .load(event.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivEventImage)

            // Edit Action
            binding.btnEdit.setOnClickListener { onEditClick(event) }

            // Delete Action (Trash button)
            binding.btnDelete.setOnClickListener { onDeleteClick(event) }
        }
    }

    // DiffUtil handles item changes efficiently without refreshing the whole list
    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}