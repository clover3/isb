package com.postech.isb.boardList

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey val name: String,
    val isFavorite: Boolean,
    val isMyBoard: Boolean,
    // ADDED: Fields to store the new content status
    val hasNewPost: Boolean = false,
    val hasNewComment: Boolean = false
)
