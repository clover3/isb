package com.postech.isb.readThread

// Represents a single comment on a thread.
data class Comment(
    val author: String,
    val date: String,
    val content: String
)
