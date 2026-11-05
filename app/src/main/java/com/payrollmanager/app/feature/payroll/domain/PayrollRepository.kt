package com.payrollmanager.app.feature.payroll.domain

import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import kotlinx.coroutines.flow.Flow

/** Distinguishes a local write failure from a successful write that is awaiting sync. */
sealed interface WriteResult<out T> {
    data class Saved<out T>(val value: T, val syncPending: Boolean) : WriteResult<T>
    data class Failed(val cause: Exception) : WriteResult<Nothing>
}

interface PayrollRepository {
    fun observePayrolls(): Flow<List<Payroll>>
    suspend fun payroll(id: String): Payroll?
    suspend fun createPayroll(payroll: Payroll): WriteResult<Payroll>
    suspend fun updatePayroll(payroll: Payroll): WriteResult<Payroll>
    suspend fun deletePayroll(id: String): WriteResult<Unit>
    suspend fun refresh(): Result<Unit>
}
