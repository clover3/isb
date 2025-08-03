package com.postech.isb.ui.login

import android.content.Context
import android.content.SharedPreferences
import com.postech.isb.R
import java.security.MessageDigest

class NoticeManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("notice_prefs", Context.MODE_PRIVATE)
    private val LAST_NOTICE_HASH = "LAST_NOTICE_HASH"

    /**
     * Checks if the notice should be shown by comparing the hash of the
     * current notice content with the hash of the last notice shown.
     */
    fun shouldShowNotice(): Boolean {
        val savedNoticeHash = prefs.getString(LAST_NOTICE_HASH, "")
        val noticeContent = context.resources.getString(R.string.notice_content)
        return try {
            val md = MessageDigest.getInstance("MD5")
            val currentDigest = toHexString(md.digest(noticeContent.toByteArray(Charsets.UTF_8)))
            savedNoticeHash != currentDigest
        } catch (e: Exception) {
            false // Don't show notice if hashing fails for any reason
        }
    }

    /**
     * Saves the hash of the current notice content to SharedPreferences,
     * marking it as "shown".
     */
    fun markNoticeAsShown() {
        val noticeContent = context.resources.getString(R.string.notice_content)
        try {
            val md = MessageDigest.getInstance("MD5")
            val currentDigest = toHexString(md.digest(noticeContent.toByteArray(Charsets.UTF_8)))
            prefs.edit().putString(LAST_NOTICE_HASH, currentDigest).apply()
        } catch (e: Exception) {
            // If hashing fails, we can't save the state.
        }
    }

    // Helper function from original Login.java to convert byte array to hex string
    private fun toHexString(bytes: ByteArray): String {
        val hexString = StringBuilder()
        for (byte in bytes) {
            val hex = Integer.toHexString(0xFF and byte.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}
