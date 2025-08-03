package com.postech.isb.boardList

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

class BoardListAdapter(
    private val onFavoriteClicked: (Board) -> Unit,
    private val onBoardClicked: (Board) -> Unit // ADDED: Click listener for the whole item
) : ListAdapter<Board, BoardListAdapter.BoardViewHolder>(BoardDiffCallback) {

    class BoardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Use original IDs from boardlist_item.xml
        private val nameTextView: TextView = itemView.findViewById(R.id.BoardListTitle)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favorite)
        private val newPostIcon: ImageView = itemView.findViewById(R.id.BoardListNew)
        private val newCommentIcon: ImageView = itemView.findViewById(R.id.BoardListComment)

        fun bind(board: Board, onFavoriteClicked: (Board) -> Unit, onBoardClicked: (Board) -> Unit) {
            nameTextView.text = board.name

            favoriteIcon.setImageResource(
                if (board.favorite) R.drawable.btn_star_big_on else R.drawable.btn_star_big_off
            )

            favoriteIcon.setOnClickListener {
                onFavoriteClicked(board)
            }

            newPostIcon.isVisible = board.hasNewPost
            newCommentIcon.isVisible = board.hasNewComment

            // ADDED: Set the click listener for the entire row
            itemView.setOnClickListener {
                onBoardClicked(board)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_board, parent, false)
        return BoardViewHolder(view)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val board = getItem(position)
        holder.bind(board, onFavoriteClicked, onBoardClicked)
    }
}

object BoardDiffCallback : DiffUtil.ItemCallback<Board>() {
    override fun areItemsTheSame(oldItem: Board, newItem: Board): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: Board, newItem: Board): Boolean {
        return oldItem == newItem
    }
}
