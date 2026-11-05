package com.payrollmanager.app.feature.payroll.data

import com.payrollmanager.app.feature.payroll.domain.PayrollRepository
import com.payrollmanager.app.feature.payroll.domain.WriteResult
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import com.payrollmanager.app.feature.payroll.data.local.PayrollDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultPayrollRepository @Inject constructor(
    private val api: PayrollApi,
    private val dao: PayrollDao
) : PayrollRepository {
    override fun observePayrolls(): Flow<List<Payroll>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun payroll(id: String): Payroll? = dao.find(id)?.toDomain()
    override suspend fun createPayroll(payroll: Payroll): WriteResult<Payroll> = try {
        dao.saveAndQueue(payroll)
        WriteResult.Saved(payroll, syncPendingAfterLocalWrite())
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        WriteResult.Failed(error)
    }

    override suspend fun updatePayroll(payroll: Payroll): WriteResult<Payroll> =
        createPayroll(payroll)

    override suspend fun deletePayroll(id: String): WriteResult<Unit> = try {
        dao.deleteAndQueue(id)
        WriteResult.Saved(Unit, syncPendingAfterLocalWrite())
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        WriteResult.Failed(error)
    }

    override suspend fun refresh(): Result<Unit> = runCatching {
        syncPending()
        api.fetchPayrolls().forEach { dao.upsert(it) }
    }

    private suspend fun syncPending() {
        dao.pendingOperations().forEach { operation ->
            when (operation.type) {
                UPSERT -> {
                    val payroll = dao.find(operation.payrollId)?.toDomain()
                    if (payroll != null) api.upsertPayroll(payroll)
                }

                DELETE -> api.deletePayroll(operation.payrollId)
            }
            dao.completeOperation(operation.id)
        }
    }

    private suspend fun syncPendingAfterLocalWrite(): Boolean = try {
        syncPending()
        false
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        true
    }

    private companion object {
        const val UPSERT = "UPSERT"
        const val DELETE = "DELETE"
    }
}
