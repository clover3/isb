package com.postech.isb.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val KEY_MY_BOARD = "myboard_name"
        // Key from original Login.java
        private const val KEY_SAVED_ID = "saved_id"
    }

    fun setMyBoard(boardName: String) {
        prefs.edit().putString(KEY_MY_BOARD, boardName).apply()
    }

    fun getMyBoard(): String {
        return prefs.getString(KEY_MY_BOARD, "") ?: ""
    }

    // ADDED: Replicates the original prefSaveId() method
    fun saveUserId(userId: String?) {
        prefs.edit().putString(KEY_SAVED_ID, userId).apply()
    }
}
