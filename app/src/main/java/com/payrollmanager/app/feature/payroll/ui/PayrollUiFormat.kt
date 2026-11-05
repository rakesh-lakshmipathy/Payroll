package com.payrollmanager.app.feature.payroll.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payrollmanager.app.feature.payroll.domain.model.Money
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PayrollAmountLine(label: String, amount: String, bold: Boolean = false) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    Text(label, fontWeight = if (bold) FontWeight.Bold else null)
    Text(amount, fontWeight = if (bold) FontWeight.Bold else null)
}

internal fun money(value: Money): String = value.format()

internal fun formatDate(payroll: Payroll): String = DateFormat.getDateTimeInstance(
    DateFormat.MEDIUM,
    DateFormat.SHORT,
    Locale.getDefault()
).format(Date(payroll.createdAtEpochMillis))

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun payrollTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primary,
    scrolledContainerColor = MaterialTheme.colorScheme.primary,
    titleContentColor = MaterialTheme.colorScheme.onPrimary,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
)
