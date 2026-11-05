package com.payrollmanager.app

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.payrollmanager.app.feature.payroll.sync.PayrollSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

/** Application composition root and owner of app-wide background work. */
@HiltAndroidApp
class PayrollApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<PayrollSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("payroll-sync", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
