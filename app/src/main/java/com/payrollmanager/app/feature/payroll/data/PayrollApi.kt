package com.payrollmanager.app.feature.payroll.data

import com.payrollmanager.app.feature.payroll.domain.model.Payroll

/** Boundary for a future HTTP implementation; current implementation is deliberately local/offline. */
interface PayrollApi {
    suspend fun fetchPayrolls(): List<Payroll>
    suspend fun upsertPayroll(payroll: Payroll): Payroll
    suspend fun deletePayroll(id: String)
}
