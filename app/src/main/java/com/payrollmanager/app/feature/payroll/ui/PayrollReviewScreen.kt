package com.payrollmanager.app.feature.payroll.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.payrollmanager.app.feature.payroll.domain.PayrollCalculator
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import com.payrollmanager.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayrollReviewScreen(
    payroll: Payroll,
    isUpdate: Boolean,
    onEdit: () -> Unit,
    onConfirm: () -> Unit
) {
    val summary = remember(payroll) { PayrollCalculator.summary(payroll) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_payroll)) },
                colors = payrollTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_description))
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag(PayrollTestTags.SAVE_PAYROLL)
                ) { Text(stringResource(if (isUpdate) R.string.save_changes else R.string.create_payroll)) }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.review_instruction),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            items(payroll.employees, key = { it.id }) { employee ->
                val pay = PayrollCalculator.employeePay(employee)
                ElevatedCard {
                    Column(Modifier.padding(14.dp)) {
                        Text(employee.name, fontWeight = FontWeight.SemiBold)
                        PayrollAmountLine(stringResource(R.string.wages), money(employee.wages))
                        PayrollAmountLine(stringResource(R.string.taxes), money(pay.taxes))
                        PayrollAmountLine(stringResource(R.string.net), money(pay.net), bold = true)
                    }
                }
            }
            item {
                HorizontalDivider()
                Text(stringResource(R.string.total), fontWeight = FontWeight.Bold)
                PayrollAmountLine(stringResource(R.string.gross), money(summary.total))
                if (summary.taxes.amount.signum() > 0) PayrollAmountLine(
                    stringResource(R.string.taxes), money(summary.taxes)
                )
                PayrollAmountLine(stringResource(R.string.net), money(summary.net), bold = true)
            }
        }
    }
}
