package com.payrollmanager.app.feature.payroll.domain.model

import java.util.UUID

data class Employee(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val wages: Money,
    val isExempt: Boolean
)

data class Payroll(
    val id: String = UUID.randomUUID().toString(),
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val employees: List<Employee>
)

data class EmployeePay(val employee: Employee, val taxes:    Money) {
    val net: Money get() = employee.wages - taxes
}

data class PayrollSummary(val total: Money, val taxes: Money, val net: Money)
