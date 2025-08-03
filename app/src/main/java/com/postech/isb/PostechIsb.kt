package com.postech.isb

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.postech.isb.boardList.BoardDatabase
import com.postech.isb.boardList.BoardRepository
import com.postech.isb.preference.SettingsManager
import com.postech.isb.util.IsbSession


class PostechIsb : Application() {

    // This retains the original IsbSession instance
    val isb: IsbSession by lazy {
        Log.i("PostechIsb", "Create new isb session.")
        IsbSession()
    }
    private val database by lazy {
        Room.databaseBuilder(
            this,
            BoardDatabase::class.java, "board-database"
        ).fallbackToDestructiveMigration().build()
    }
    val boardRepository: BoardRepository by lazy {
        BoardRepository(
            boardDao = database.boardDao(),
            noteDao = database.noteDao(),
            isbSession = isb
        )
    }

    val settingsManager: SettingsManager by lazy {
        SettingsManager(this)
    }
}
