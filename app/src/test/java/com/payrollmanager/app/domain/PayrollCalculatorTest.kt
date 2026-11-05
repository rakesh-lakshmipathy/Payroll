package com.payrollmanager.app.domain

import com.payrollmanager.app.feature.payroll.domain.PayrollCalculator
import com.payrollmanager.app.feature.payroll.domain.model.Employee
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import com.payrollmanager.app.feature.payroll.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PayrollCalculatorTest {
    @Test fun `applies five percent only above threshold for non exempt employees`() {
        val payroll = Payroll(employees = listOf(
            Employee(name = "Rakesh", wages = Money.of(BigDecimal("900")), isExempt = false),
            Employee(name = "John", wages = Money.of(BigDecimal("1900")), isExempt = true),
            Employee(name = "Richard", wages = Money.of(BigDecimal("2000")), isExempt = false)
        ))
        val summary = PayrollCalculator.summary(payroll)
        assertEquals(BigDecimal("4800.00"), summary.total.amount)
        assertEquals(BigDecimal("100.00"), summary.taxes.amount)
        assertEquals(BigDecimal("4700.00"), summary.net.amount)
    }

    @Test fun `does not tax wages at the threshold`() {
        val employee = Employee(name = "Ada", wages = Money.of(BigDecimal("1000")), isExempt = false)
        assertEquals(BigDecimal("0.00"), PayrollCalculator.employeePay(employee).taxes.amount)
    }

    @Test fun `does not tax an exempt employee above the threshold`() {
        val employee = Employee(name = "Ada", wages = Money.of(BigDecimal("5000")), isExempt = true)
        assertEquals(BigDecimal("0.00"), PayrollCalculator.employeePay(employee).taxes.amount)
    }
}
