package com.payrollmanager.app.feature.payroll.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payrollmanager.app.feature.payroll.domain.PayrollCalculator
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import com.payrollmanager.app.R

/** Payroll list feature screen. The implementation remains package-private while it is split further. */
@Composable
internal fun PayrollListScreen(
    payrolls: List<Payroll>, onCreate: () -> Unit, onOpen: (Payroll) -> Unit, onRetry: () -> Unit
) = PayrollListContent(payrolls, onCreate, onOpen, onRetry)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayrollListContent(
    payrolls: List<Payroll>, onCreate: () -> Unit, onOpen: (Payroll) -> Unit, onRetry: () -> Unit
) {
    val syncDescription = stringResource(R.string.sync_payrolls_description)
    val createDescription = stringResource(R.string.create_payroll_description)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payroll_list_title)) },
                colors = payrollTopAppBarColors(),
                actions = {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .semantics { contentDescription = syncDescription }
                        .testTag(PayrollTestTags.SYNC_PAYROLL)
                ) { Icon(Icons.Filled.Sync, contentDescription = null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .semantics { contentDescription = createDescription }
                    .testTag(PayrollTestTags.CREATE_PAYROLL)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { padding ->
        if (payrolls.isEmpty()) Box(
            Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.no_payrolls)) }
        else LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(payrolls, key = { it.id }) { payroll ->
                val summary = PayrollCalculator.summary(payroll)
                val employeeCount = payroll.employees.size
                val description = stringResource(
                    R.string.payroll_list_item,
                    formatDate(payroll),
                    employeeCount,
                    money(summary.total)
                )
                ElevatedCard(Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(payroll) }
                    .semantics {
                        contentDescription = description
                    }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            formatDate(payroll),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            pluralStringResource(
                                R.plurals.employee_count, employeeCount, employeeCount
                            )
                        )
                        Text(
                            stringResource(R.string.total_payroll, money(summary.total)),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
