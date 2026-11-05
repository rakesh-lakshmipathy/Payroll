package com.payrollmanager.app.feature.payroll.domain.usecase

import com.payrollmanager.app.feature.payroll.domain.PayrollRepository
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import javax.inject.Inject

/** Application actions: UI depends on these, never on a data implementation. */
class ObservePayrolls @Inject constructor(private val repository: PayrollRepository) {
    operator fun invoke() = repository.observePayrolls()
}

class CreatePayroll @Inject constructor(private val repository: PayrollRepository) {
    suspend operator fun invoke(payroll: Payroll) = repository.createPayroll(payroll)
}

class UpdatePayroll @Inject constructor(private val repository: PayrollRepository) {
    suspend operator fun invoke(payroll: Payroll) = repository.updatePayroll(payroll)
}

class DeletePayroll @Inject constructor(private val repository: PayrollRepository) {
    suspend operator fun invoke(id: String) = repository.deletePayroll(id)
}

class RefreshPayrolls @Inject constructor(private val repository: PayrollRepository) {
    suspend operator fun invoke() = repository.refresh()
}

data class PayrollUseCases @Inject constructor(
    val observe: ObservePayrolls,
    val create: CreatePayroll,
    val update: UpdatePayroll,
    val delete: DeletePayroll,
    val refresh: RefreshPayrolls
)
