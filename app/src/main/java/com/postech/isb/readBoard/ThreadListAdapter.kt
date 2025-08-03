package com.postech.isb.readBoard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.postech.isb.R

class ThreadListAdapter(
    private val onThreadClicked: (IsbThread) -> Unit,
    // 1. ADDED: A parameter to receive the long-click handler from the Activity.
    private val onThreadLongClicked: (IsbThread, View) -> Unit
) : ListAdapter<IsbThread, ThreadListAdapter.ThreadViewHolder>(ThreadDiffCallback) {

    class ThreadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val writer: TextView = itemView.findViewById(R.id.readBoardWriter)
        private val date: TextView = itemView.findViewById(R.id.readBoardDate)
        private val title: TextView = itemView.findViewById(R.id.readBoardTitle)
        private val newIcon: ImageView = itemView.findViewById(R.id.readBoardNew)
        private val commentIcon: ImageView = itemView.findViewById(R.id.readBoardComment)
        private val pinIcon: ImageView = itemView.findViewById(R.id.notdel)

        fun bind(thread: IsbThread) {
            writer.text = thread.author
            date.text = thread.date
            title.text = thread.title
            newIcon.isVisible = thread.isNew
            commentIcon.isVisible = thread.hasComment
            pinIcon.isVisible = thread.isNotice
            itemView.isActivated = thread.isHighlighted
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thread, parent, false)
        return ThreadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
        val thread = getItem(position)
        holder.bind(thread)

        // Handle regular clicks
        holder.itemView.setOnClickListener {
            onThreadClicked(thread)
        }

        // 2. IMPLEMENTED: Set the long-click listener on the item view.
        holder.itemView.setOnLongClickListener { view ->
            // When a long-click happens, call the handler function we received.
            onThreadLongClicked(thread, view)
            // Return true to indicate that we have handled the event.
            true
        }
    }
}

object ThreadDiffCallback : DiffUtil.ItemCallback<IsbThread>() {
    override fun areItemsTheSame(oldItem: IsbThread, newItem: IsbThread): Boolean {
        return oldItem.key == newItem.key
    }

    override fun areContentsTheSame(oldItem: IsbThread, newItem: IsbThread): Boolean {
        return oldItem == newItem
    }
}
