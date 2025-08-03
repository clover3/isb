package com.postech.isb.readThread

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.postech.isb.R

class CommentAdapter : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback) {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Find the views by their IDs from the new item_comment.xml layout
        private val authorTextView: TextView = itemView.findViewById(R.id.commentAuthor)
        private val dateTextView: TextView = itemView.findViewById(R.id.commentDate)
        private val contentTextView: TextView = itemView.findViewById(R.id.commentContent)

        fun bind(comment: Comment) {
            // Set the text for each view from the Comment data object
            authorTextView.text = comment.author
            dateTextView.text = "(${comment.date})" // Formatting to match original
            contentTextView.text = "\"${comment.content}\"" // Formatting to match original
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        // Inflate the new item_comment.xml layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

object CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
    override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        // A more robust check assuming author + date is unique enough for a key
        return oldItem.author == newItem.author && oldItem.date == newItem.date
    }

    override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        return oldItem == newItem
    }
}
