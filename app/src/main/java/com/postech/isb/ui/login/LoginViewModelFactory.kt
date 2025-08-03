package com.postech.isb.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.postech.isb.preference.SettingsManager
import com.postech.isb.util.IsbSession

class LoginViewModelFactory(
    private val isbSession: IsbSession,
    private val workManager: WorkManager,
    private val noticeManager: NoticeManager,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // CORRECTED: Changed LoginViewModel.class to LoginViewModel::class.java
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(isbSession, workManager, noticeManager, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
