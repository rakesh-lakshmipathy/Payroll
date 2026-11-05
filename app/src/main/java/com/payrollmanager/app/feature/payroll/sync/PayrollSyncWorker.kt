package com.payrollmanager.app.feature.payroll.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import com.payrollmanager.app.di.PayrollSyncEntryPoint

/** Retries the persistent outbox with OS-managed connectivity and exponential backoff. */
class PayrollSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    private val repository by lazy {
        EntryPointAccessors.fromApplication(applicationContext, PayrollSyncEntryPoint::class.java)
            .payrollRepository()
    }

    override suspend fun doWork(): Result = repository.refresh()
        .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}
