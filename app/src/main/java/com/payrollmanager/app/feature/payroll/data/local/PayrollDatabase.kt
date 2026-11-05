package com.payrollmanager.app.feature.payroll.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.ColumnInfo
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.payrollmanager.app.feature.payroll.domain.model.Employee
import com.payrollmanager.app.feature.payroll.domain.model.Money
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "payrolls")
data class PayrollEntity(@PrimaryKey val id: String, val createdAtEpochMillis: Long)

@Entity(
    tableName = "employees",
    foreignKeys = [ForeignKey(
        entity = PayrollEntity::class,
        parentColumns = ["id"],
        childColumns = ["payrollId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("payrollId")]
)
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val payrollId: String,
    val name: String,
    val wages: String,
    @ColumnInfo(defaultValue = "'USD'") val currencyCode: String = Money.PAYROLL_CURRENCY,
    val isExempt: Boolean
)

@Entity(tableName = "sync_operations", indices = [Index("payrollId")])
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payrollId: String,
    val type: String
)

data class PayrollWithEmployees(
    @Embedded val payroll: PayrollEntity,
    @Relation(parentColumn = "id", entityColumn = "payrollId") val employees: List<EmployeeEntity>
) {
    fun toDomain() = Payroll(
        payroll.id,
        payroll.createdAtEpochMillis,
        employees.map {
            Employee(
                it.id,
                it.name,
                Money.of(it.wages.toBigDecimal(), it.currencyCode),
                it.isExempt
            )
        })
}

@Dao
interface PayrollDao {
    @Transaction
    @Query("SELECT * FROM payrolls ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<PayrollWithEmployees>>

    @Transaction
    @Query("SELECT * FROM payrolls WHERE id = :id")
    suspend fun find(id: String): PayrollWithEmployees?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayroll(payroll: PayrollEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<EmployeeEntity>)

    @Query("DELETE FROM employees WHERE payrollId = :payrollId")
    suspend fun deleteEmployees(payrollId: String)

    @Query("DELETE FROM payrolls WHERE id = :id")
    suspend fun delete(id: String)

    @Insert
    suspend fun enqueue(operation: SyncOperationEntity)

    @Query("SELECT * FROM sync_operations ORDER BY id")
    suspend fun pendingOperations(): List<SyncOperationEntity>

    @Query("DELETE FROM sync_operations WHERE id = :id")
    suspend fun completeOperation(id: Long)

    @Transaction
    suspend fun upsert(domain: Payroll) {
        insertPayroll(PayrollEntity(domain.id, domain.createdAtEpochMillis))
        deleteEmployees(domain.id)
        insertEmployees(domain.employees.map {
            EmployeeEntity(
                it.id,
                domain.id,
                it.name,
                it.wages.amount.toPlainString(),
                it.wages.currencyCode,
                it.isExempt
            )
        })
    }

    @Transaction
    suspend fun saveAndQueue(domain: Payroll) {
        upsert(domain)
        enqueue(SyncOperationEntity(payrollId = domain.id, type = "UPSERT"))
    }

    @Transaction
    suspend fun deleteAndQueue(payrollId: String) {
        delete(payrollId)
        enqueue(SyncOperationEntity(payrollId = payrollId, type = "DELETE"))
    }
}

@Database(
    entities = [PayrollEntity::class, EmployeeEntity::class, SyncOperationEntity::class],
    version = 3,
    exportSchema = true
)
abstract class PayrollDatabase : RoomDatabase() {
    abstract fun payrollDao(): PayrollDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_operations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, payrollId TEXT NOT NULL, type TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_operations_payrollId ON sync_operations (payrollId)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE employees ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'USD'")
            }
        }
    }
}
