package com.example.univent

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.univent.databinding.ItemStudentEventBinding

class StudentEventAdapter(
    private val onBookmarkClick: (Event) -> Unit,
    private val onItemClick: (Event) -> Unit
) : ListAdapter<Event, StudentEventAdapter.EventViewHolder>(EventDiffCallback()) {

    // List of IDs that the current user has bookmarked
    private var bookmarkedIds: List<String> = emptyList()

    fun updateBookmarks(newBookmarks: List<String>) {
        bookmarkedIds = newBookmarks
        notifyDataSetChanged() // Refresh UI to show correct star status
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemStudentEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(private val binding: ItemStudentEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.tvTitle.text = event.title
            binding.tvDate.text = "${event.date} • ${event.time}"
            binding.tvLocation.text = event.location

            // Image Loading
            if (event.imageUrl.isNotEmpty()) {
                Glide.with(binding.ivEventImage.context)
                    .load(event.imageUrl)
                    .placeholder(R.drawable.event_seminar) // Make sure this drawable exists
                    .into(binding.ivEventImage)
            } else {
                binding.ivEventImage.setImageResource(R.drawable.event_seminar)
            }

            // Click Listeners
            binding.root.setOnClickListener {
                onItemClick(event)
            }

            // Assuming there is a bookmark button in the item layout, if not, this part is just placeholder logic based on constructor
            // binding.btnBookmark.setOnClickListener { onBookmarkClick(event) } 
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
    }
}