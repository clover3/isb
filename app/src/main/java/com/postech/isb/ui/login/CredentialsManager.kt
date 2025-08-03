package com.postech.isb.ui.login


import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

// A data class to hold the loaded credentials
data class UserCredentials(
    val id: String,
    val pass: String,
    val rememberMe: Boolean
)

class CredentialsManager(context: Context) {

    // Create a file name for the encrypted preferences
    private val prefsFile = "secure_user_prefs"

    // Get the master key from the Android Keystore
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    // Create the EncryptedSharedPreferences instance
    private val sharedPreferences = EncryptedSharedPreferences.create(
        prefsFile,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(id: String, pass: String, rememberMe: Boolean) {
        if (rememberMe) {
            sharedPreferences.edit()
                .putString("KEY_USER_ID", id)
                .putString("KEY_USER_PASS", pass)
                .putBoolean("KEY_REMEMBER_ME", true)
                .apply()
        } else {
            // If the user unchecks "remember me", clear the saved credentials
            clearCredentials()
        }
    }

    fun loadCredentials(): UserCredentials {
        val id = sharedPreferences.getString("KEY_USER_ID", "") ?: ""
        val pass = sharedPreferences.getString("KEY_USER_PASS", "") ?: ""
        val rememberMe = sharedPreferences.getBoolean("KEY_REMEMBER_ME", false)
        return UserCredentials(id, pass, rememberMe)
    }

    fun clearCredentials() {
        sharedPreferences.edit()
            .remove("KEY_USER_ID")
            .remove("KEY_USER_PASS")
            .putBoolean("KEY_REMEMBER_ME", false)
            .apply()
    }
}
