package com.payrollmanager.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.payrollmanager.app.feature.payroll.data.DefaultPayrollRepository
import com.payrollmanager.app.feature.payroll.data.InMemoryPayrollApi
import com.payrollmanager.app.feature.payroll.data.local.PayrollDatabase
import com.payrollmanager.app.feature.payroll.domain.model.Employee
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import com.payrollmanager.app.feature.payroll.domain.model.Money
import com.payrollmanager.app.feature.payroll.domain.WriteResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class DefaultPayrollRepositoryTest {
    private lateinit var database: PayrollDatabase
    private lateinit var repository: DefaultPayrollRepository

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PayrollDatabase::class.java).allowMainThreadQueries().build()
        repository = DefaultPayrollRepository(InMemoryPayrollApi(), database.payrollDao())
    }

    @After fun tearDown() = database.close()

    @Test fun creates_and_reads_a_persisted_payroll() = runBlocking {
        val payroll = Payroll(employees = listOf(Employee(name = "Rakesh", wages = Money.of(BigDecimal("2000")), isExempt = false)))
        repository.createPayroll(payroll)
        val saved = repository.observePayrolls().first()
        assertEquals(payroll.id, saved.single().id)
        assertEquals("Rakesh", saved.single().employees.single().name)
    }

    @Test fun update_replaces_existing_employees() = runBlocking {
        val payroll = Payroll(employees = listOf(Employee(name = "Alexander", wages = Money.of(BigDecimal("1000")), isExempt = false)))
        repository.createPayroll(payroll)
        val updated = payroll.copy(employees = listOf(Employee(name = "Rakesh", wages = Money.of(BigDecimal("2000")), isExempt = false)))
        assertEquals(true, repository.updatePayroll(updated) is WriteResult.Saved)
        assertEquals("Rakesh", repository.observePayrolls().first().single().employees.single().name)
    }

    @Test fun delete_removes_the_payroll_from_room() = runBlocking {
        val payroll = Payroll(employees = listOf(Employee(name = "Rakesh", wages = Money.of(BigDecimal("1000")), isExempt = false)))
        repository.createPayroll(payroll)
        repository.deletePayroll(payroll.id)
        assertEquals(emptyList<Payroll>(), repository.observePayrolls().first())
    }
}
