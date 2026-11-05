package com.payrollmanager.app.feature.payroll.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import androidx.navigation.NavHostController
import com.payrollmanager.app.feature.payroll.domain.model.Employee
import com.payrollmanager.app.feature.payroll.domain.model.Money
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import com.payrollmanager.app.R
import com.payrollmanager.app.feature.payroll.presentation.PayrollEvent
import com.payrollmanager.app.feature.payroll.presentation.PayrollViewModel
import androidx.activity.compose.BackHandler
import kotlinx.serialization.Serializable

sealed interface PayrollRoute {
    @Serializable
    data object List : PayrollRoute

    @Serializable
    data object Create : PayrollRoute

    @Serializable
    data class Edit(val payrollId: String) : PayrollRoute

    @Serializable
    data class Review(val isUpdate: Boolean) : PayrollRoute

    @Serializable
    data class Detail(val payrollId: String) : PayrollRoute
}

/** Feature coordinator: owns routes and navigation outcomes, not individual screen layouts. */
@Composable
internal fun PayrollApp(vm: PayrollViewModel) {
    val navController = rememberNavController()
    val activity = LocalActivity.current
    val currentEntry by navController.currentBackStackEntryAsState()
    val payrolls by vm.payrolls.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var pendingDraft by rememberSaveable(stateSaver = PayrollSaver) { mutableStateOf<Payroll?>(null) }

    // The list is the root destination. Back here must close the activity, not empty the NavHost.
    BackHandler(enabled = currentEntry?.
    destination?.route?.
    startsWith(PayrollRoute.List::class.qualifiedName ?: "") == true) {
        activity?.finish()
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                PayrollEvent.Created, PayrollEvent.Deleted -> navController.navigateToList()
                is PayrollEvent.Updated -> navController.navigate(
                    PayrollRoute.Detail(event.payroll.id),
                    navOptions {
                        popUpTo<PayrollRoute.List> { inclusive = false }
                        launchSingleTop = true
                    }
                )
            }
        }
    }

    NavHost(navController = navController, startDestination = PayrollRoute.List) {
        composable<PayrollRoute.List> {
            PayrollListScreen(
                payrolls = payrolls,
                onCreate = {
                    pendingDraft = null
                    navController.navigate(PayrollRoute.Create)
                },
                onOpen = { navController.navigate(PayrollRoute.Detail(it.id)) },
                onRetry = vm::refresh
            )
        }
        composable<PayrollRoute.Create> {
            PayrollEditorScreen(
                existing = null,
                onBack = navController::popBackStack,
                onReview = { draft ->
                    pendingDraft = draft
                    navController.navigate(PayrollRoute.Review(isUpdate = false))
                }
            )
        }
        composable<PayrollRoute.Edit> { entry ->
            val route = entry.toRoute<PayrollRoute.Edit>()
            val payrollId = route.payrollId
            val existing = payrolls.firstOrNull { it.id == payrollId }
            if (existing == null) {
                ReturningToList { navController.popBackStack() }
            } else {
                PayrollEditorScreen(
                    existing = existing,
                    onBack = navController::popBackStack,
                    onReview = { draft ->
                        pendingDraft = draft
                        navController.navigate(PayrollRoute.Review(isUpdate = true))
                    }
                )
            }
        }
        composable<PayrollRoute.Review> { entry ->
            val route = entry.toRoute<PayrollRoute.Review>()
            val isUpdate = route.isUpdate
            val draft = pendingDraft
            if (draft == null) {
                ReturningToList { navController.navigateToList() }
            } else {
                PayrollReviewScreen(
                    payroll = draft,
                    isUpdate = isUpdate,
                    onEdit = navController::popBackStack,
                    onConfirm = { if (isUpdate) vm.update(draft) else vm.create(draft) }
                )
            }
        }
        composable<PayrollRoute.Detail> { entry ->
            val route = entry.toRoute<PayrollRoute.Detail>()
            val payrollId = route.payrollId
            val payroll = payrolls.firstOrNull { it.id == payrollId }
            if (payroll == null) {
                ReturningToList { navController.navigateToList() }
            } else {
                PayrollDetailScreen(
                    payroll = payroll,
                    onBack = navController::popBackStack,
                    onEdit = {
                        pendingDraft = null
                        navController.navigate(PayrollRoute.Edit(payroll.id))
                    },
                    onDelete = { vm.delete(payroll.id) }
                )
            }
        }
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text(stringResource(R.string.sync_notice)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearError()
                    vm.refresh()
                }) {
                    Text(stringResource(R.string.retry))
                }
            },
            dismissButton = { TextButton(onClick = vm::clearError) { Text(stringResource(R.string.dismiss)) } }
        )
    }
}

@Composable
private fun ReturningToList(onReturn: () -> Unit) {
    Text(stringResource(R.string.returning_to_payrolls))
    LaunchedEffect(Unit) { onReturn() }
}

private fun NavHostController.navigateToList() {
    navigate(PayrollRoute.List) {
        popUpTo<PayrollRoute.List> { inclusive = false }
        launchSingleTop = true
    }
}

private val PayrollSaver: Saver<Payroll?, Any> = listSaver(
    save = { payroll -> payroll?.let(::savePayroll) ?: emptyList() },
    restore = { saved -> saved.takeIf { it.isNotEmpty() }?.let(::restorePayroll) }
)

private fun savePayroll(payroll: Payroll): List<Any> = listOf(
    payroll.id,
    payroll.createdAtEpochMillis,
    payroll.employees.map { employee ->
        listOf(
            employee.id,
            employee.name,
            employee.wages.amount.toPlainString(),
            employee.wages.currencyCode,
            employee.isExempt
        )
    }
)

private fun restorePayroll(saved: List<*>): Payroll = Payroll(
    id = saved[0] as String,
    createdAtEpochMillis = saved[1] as Long,
    employees = (saved[2] as List<*>).map { raw ->
        val employee = raw as List<*>
        Employee(
            id = employee[0] as String,
            name = employee[1] as String,
            wages = Money.of((employee[2] as String).toBigDecimal(), employee[3] as String),
            isExempt = employee[4] as Boolean
        )
    }
)
