package com.postech.isb.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.postech.isb.preference.SettingsManager
import com.postech.isb.util.HeartbeatWorker
import com.postech.isb.util.IsbSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class LoginUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val userId: String? = null,
    val hasNewMail: Boolean = false,
    val error: String? = null,
    val showNoticeDialog: Boolean = false,
)

class LoginViewModel(
    private val isbSession: IsbSession,
    private val workManager: WorkManager, // Inject WorkManager
    private val noticeManager: NoticeManager,
    private val settingsManager: SettingsManager // New dependency

) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkInitialLoginState()
    }

    private fun checkInitialLoginState() {
        // No need for viewModelScope here as it's a quick, non-blocking check
        _uiState.update {
            it.copy(
                isLoggedIn = isbSession.isConnected,
                // CORRECTED: Access static fields via the class name
                userId = if (isbSession.isConnected) IsbSession.userId else null,
                hasNewMail = IsbSession.new_mail
            )
        }
    }
    fun attemptAutoLogin(credentials: UserCredentials) {
        // Only attempt to auto-login if the user has opted-in and is not already connected.
        if (credentials.rememberMe && !isbSession.isConnected) {
            login(credentials.id, credentials.pass)
        }
    }
    fun login(id: String, pw: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                withContext(Dispatchers.IO) {
                    isbSession.connect()
                }

                if (!isbSession.isConnected) {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to connect to server") }
                    return@launch
                }

                val loginSuccess = withContext(Dispatchers.IO) {
                    isbSession.login(id, pw, true)
                }
                Log.i("LoginViewModel", "loginSuccess: $loginSuccess")

                if (loginSuccess) {
                    settingsManager.saveUserId(IsbSession.userId)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userId = IsbSession.userId,
                            hasNewMail = IsbSession.new_mail,
                            showNoticeDialog = noticeManager.shouldShowNotice()
                        )
                    }
                    startHeartbeat()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Login failed") }
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Exception during login", e)
                _uiState.update { it.copy(isLoading = false, error = "Connection error: ${e.message}") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                isbSession.disconnect()
            }
            settingsManager.saveUserId(null)
            _uiState.update {
                it.copy(
                    isLoggedIn = false,
                    userId = null,
                    hasNewMail = false
                )
            }
            stopHeartbeat()
        }
    }


    fun onNoticeShown() {
        noticeManager.markNoticeAsShown()
        _uiState.update { it.copy(showNoticeDialog = false) }
    }

    private fun startHeartbeat() {
        // Create a periodic work request that runs approximately every 10 minutes
        val heartbeatRequest = PeriodicWorkRequestBuilder<HeartbeatWorker>(10, TimeUnit.MINUTES)
            .build()

        // Enqueue the work as a unique periodic task
        workManager.enqueueUniquePeriodicWork(
            HeartbeatWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep the existing work if it's already running
            heartbeatRequest
        )
    }

    private fun stopHeartbeat() {
        workManager.cancelUniqueWork(HeartbeatWorker.WORK_NAME)
    }
}
