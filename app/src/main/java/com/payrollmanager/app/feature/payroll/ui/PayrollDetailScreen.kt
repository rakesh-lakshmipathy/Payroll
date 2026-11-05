package com.payrollmanager.app.feature.payroll.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.payrollmanager.app.R
import com.payrollmanager.app.feature.payroll.domain.PayrollCalculator
import com.payrollmanager.app.feature.payroll.domain.model.Payroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayrollDetailScreen(
    payroll: Payroll,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var askDelete by remember { mutableStateOf(false) }
    val payments = remember(payroll) { payroll.employees.map(PayrollCalculator::employeePay) }
    val summary = remember(payroll) { PayrollCalculator.summary(payroll) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payroll_details)) },
                colors = payrollTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_description))
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag(PayrollTestTags.EDIT_PAYROLL)
                    ) { Icon(Icons.Filled.Edit, stringResource(R.string.edit_description)) }
                    IconButton(
                        onClick = { askDelete = true },
                        modifier = Modifier.testTag(PayrollTestTags.DELETE_PAYROLL)
                    ) { Icon(Icons.Filled.Delete, stringResource(R.string.delete_description)) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(formatDate(payroll), style = MaterialTheme.typography.bodyMedium) }
            items(payments, key = { it.employee.id }) { pay ->
                ElevatedCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            pay.employee.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        PayrollAmountLine(
                            stringResource(R.string.total_wages),
                            money(pay.employee.wages)
                        )
                        if (pay.taxes.amount.signum() > 0) PayrollAmountLine(
                            stringResource(R.string.taxes),
                            money(pay.taxes)
                        )
                        PayrollAmountLine(stringResource(R.string.net), money(pay.net), bold = true)
                    }
                }
            }
            item {
                HorizontalDivider()
                Text(
                    stringResource(R.string.payroll_summary),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
                PayrollAmountLine(stringResource(R.string.total), money(summary.total))
                if (summary.taxes.amount.signum() > 0) PayrollAmountLine(
                    stringResource(R.string.total_taxes),
                    money(summary.taxes)
                )
                PayrollAmountLine(
                    stringResource(R.string.total_net),
                    money(summary.net),
                    bold = true
                )
            }
        }
    }
    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text(stringResource(R.string.delete_payroll_question)) },
            text = { Text(stringResource(R.string.delete_payroll_message)) },
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag(PayrollTestTags.CONFIRM_DELETE)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { askDelete = false },
                    modifier = Modifier.testTag(PayrollTestTags.CANCEL_DELETE)
                ) { Text(stringResource(R.string.dismiss)) }
            }
        )
    }
}
