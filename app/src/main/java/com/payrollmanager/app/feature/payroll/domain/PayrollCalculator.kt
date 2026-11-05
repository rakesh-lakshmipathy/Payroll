package com.payrollmanager.app.feature.payroll.domain

import com.payrollmanager.app.feature.payroll.domain.model.Employee
import com.payrollmanager.app.feature.payroll.domain.model.EmployeePay
import com.payrollmanager.app.feature.payroll.domain.model.Money
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import com.payrollmanager.app.feature.payroll.domain.model.PayrollSummary
import java.math.BigDecimal

/** Single source of truth for assessment business rules. */
object PayrollCalculator {
    private val threshold = BigDecimal("1000")
    private val rate = BigDecimal("0.05")

    fun employeePay(employee: Employee): EmployeePay {
        val taxes =
            if (!employee.isExempt && employee.wages.amount > threshold) employee.wages.percentage(
                rate
            ) else Money.zero(employee.wages.currencyCode)
        return EmployeePay(employee, taxes)
    }

    fun summary(payroll: Payroll): PayrollSummary {
        val payments = payroll.employees.map(::employeePay)
        val currency =
            payroll.employees.firstOrNull()?.wages?.currencyCode ?: Money.PAYROLL_CURRENCY
        val total = payments.fold(Money.zero(currency)) { sum, item -> sum + item.employee.wages }
        val taxes = payments.fold(Money.zero(currency)) { sum, item -> sum + item.taxes }
        return PayrollSummary(total, taxes, total - taxes)
    }
}
