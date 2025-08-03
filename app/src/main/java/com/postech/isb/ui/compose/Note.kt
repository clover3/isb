package com.postech.isb.ui.compose

import androidx.room.Entity
import androidx.room.PrimaryKey

// This class represents a single note in the database
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val lastModified: Long = System.currentTimeMillis()
)
