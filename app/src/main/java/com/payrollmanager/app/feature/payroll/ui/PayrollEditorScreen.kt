package com.payrollmanager.app.feature.payroll.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import com.payrollmanager.app.R
import com.payrollmanager.app.feature.payroll.domain.model.Employee
import com.payrollmanager.app.feature.payroll.domain.model.Money
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import java.math.BigDecimal
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayrollEditorScreen(
    existing: Payroll?,
    onBack: () -> Unit,
    onReview: (Payroll) -> Unit
) {
    val context = LocalContext.current
    var employees by rememberSaveable(existing?.id, stateSaver = EmployeeListSaver) {
        mutableStateOf(existing?.employees ?: emptyList())
    }
    var name by rememberSaveable(existing?.id) { mutableStateOf("") }
    var wages by rememberSaveable(existing?.id) { mutableStateOf("") }
    var exempt by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var error by rememberSaveable(existing?.id) { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.create_payroll else R.string.edit_payroll)) },
                colors = payrollTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_description))
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                Button(
                    onClick = {
                        if (employees.isEmpty()) {
                            error = context.getString(R.string.add_employee_error)
                        } else {
                            onReview(
                                Payroll(
                                    id = existing?.id ?: UUID.randomUUID().toString(),
                                    createdAtEpochMillis = existing?.createdAtEpochMillis
                                        ?: System.currentTimeMillis(),
                                    employees = employees
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag(PayrollTestTags.REVIEW_PAYROLL)
                ) { Text(stringResource(R.string.review_payroll)) }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.employees), style = MaterialTheme.typography.titleLarge
                )
                Text(stringResource(R.string.tax_rule), style = MaterialTheme.typography.bodySmall)
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PayrollTestTags.EMPLOYEE_NAME),
                    label = { Text(stringResource(R.string.full_name)) },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = wages,
                    onValueChange = { wages = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PayrollTestTags.EMPLOYEE_WAGES),
                    label = { Text(stringResource(R.string.wages)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    singleLine = true
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = exempt, onCheckedChange = { exempt = it })
                    Text(stringResource(R.string.tax_exempt))
                }
            }
            item {
                Button(
                    onClick = {
                        val amount = wages.toBigDecimalOrNull()
                        if (name.isBlank() || amount == null || amount < BigDecimal.ZERO) {
                            error = context.getString(R.string.employee_input_error)
                        } else {
                            employees = employees + Employee(
                                name = name.trim(), wages = Money.of(amount), isExempt = exempt
                            )
                            name = ""
                            wages = ""
                            exempt = false
                            error = null
                        }
                    },
                    modifier = Modifier.testTag(PayrollTestTags.ADD_EMPLOYEE)
                ) { Text(stringResource(R.string.add_employee)) }
            }
            error?.let { message ->
                item {
                    Text(
                        message, color = MaterialTheme.colorScheme.error
                    )
                }
            }
            items(employees, key = { it.id }) { employee ->
                ElevatedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(employee.name, fontWeight = FontWeight.SemiBold)
                            val amount = money(employee.wages)
                            Text(
                                if (employee.isExempt) stringResource(
                                    R.string.employee_exempt, amount
                                ) else amount
                            )
                        }
                        TextButton(
                            onClick = { employees = employees - employee },
                            modifier = Modifier.semantics {
                                contentDescription =
                                    context.getString(R.string.remove_employee, employee.name)
                            }
                        ) { Text(stringResource(R.string.remove)) }
                    }
                }
            }
        }
    }
}

private val EmployeeListSaver: Saver<List<Employee>, Any> = listSaver(
    save = { employees ->
        employees.map {
            listOf(
                it.id, it.name, it.wages.amount.toPlainString(), it.wages.currencyCode, it.isExempt
            )
        }
    },
    restore = { saved ->
        saved.map { raw ->
            val employee = raw as List<*>
            Employee(
                id = employee[0] as String,
                name = employee[1] as String,
                wages = Money.of((employee[2] as String).toBigDecimal(), employee[3] as String),
                isExempt = employee[4] as Boolean
            )
        }
    }
)
