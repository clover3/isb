package com.postech.isb.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.postech.isb.PostechIsb

class HeartbeatWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "com.postech.isb.util.HeartbeatWorker"
    }

    override suspend fun doWork(): Result {
        Log.i("HeartbeatWorker", "Sending heartbeat...")

        // Get the IsbSession from the Application context
        val application = applicationContext as PostechIsb
        val isbSession = application.isb

        return try {
            // Call the existing sendHeartbeat method
            val success = isbSession.sendHeartbeat()
            if (success) {
                Log.i("HeartbeatWorker", "Heartbeat successful.")
                Result.success()
            } else {
                Log.w("HeartbeatWorker", "Heartbeat failed.")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("HeartbeatWorker", "Heartbeat threw an exception.", e)
            Result.failure()
        }
    }
}
