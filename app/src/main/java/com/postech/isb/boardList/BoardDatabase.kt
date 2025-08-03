package com.postech.isb.boardList
import androidx.room.Database
import androidx.room.RoomDatabase
import com.postech.isb.ui.compose.Note
import com.postech.isb.ui.compose.NoteDao

// Add Note to the entities array and increment the version number
@Database(entities = [BoardEntity::class, Note::class], version = 2)
abstract class BoardDatabase : RoomDatabase() {
    abstract fun boardDao(): BoardDao
    abstract fun noteDao(): NoteDao // Add the new DAO
}
