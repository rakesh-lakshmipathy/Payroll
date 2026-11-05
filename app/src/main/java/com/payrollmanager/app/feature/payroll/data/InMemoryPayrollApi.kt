package com.payrollmanager.app.feature.payroll.data

import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class InMemoryPayrollApi @Inject constructor() : PayrollApi {
    private val payrolls = linkedMapOf<String, Payroll>()
    override suspend fun fetchPayrolls(): List<Payroll> = payrolls.values.toList()
    override suspend fun upsertPayroll(payroll: Payroll): Payroll {
        delay(120.milliseconds) // Simulates a transport boundary without requiring connectivity.
        payrolls[payroll.id] = payroll
        return payroll
    }

    override suspend fun deletePayroll(id: String) {
        payrolls.remove(id)
    }
}
