package com.hourly.app.feature.payroll.ui

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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.hourly.app.feature.payroll.domain.model.Employee
import com.hourly.app.feature.payroll.domain.model.Money
import com.hourly.app.feature.payroll.domain.model.Payroll
import com.hourly.app.R
import com.hourly.app.feature.payroll.presentation.PayrollEvent
import com.hourly.app.feature.payroll.presentation.PayrollViewModel
import androidx.activity.compose.BackHandler

private object PayrollRoute {
    const val LIST = "payrolls"
    const val CREATE = "create"
    const val EDIT = "edit/{payrollId}"
    const val REVIEW = "review/{isUpdate}"
    const val DETAIL = "detail/{payrollId}"

    fun edit(payrollId: String) = "edit/$payrollId"
    fun review(isUpdate: Boolean) = "review/$isUpdate"
    fun detail(payrollId: String) = "detail/$payrollId"
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
    BackHandler(enabled = currentEntry?.destination?.route == PayrollRoute.LIST) {
        activity?.finish()
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                PayrollEvent.Created, PayrollEvent.Deleted -> navController.navigateToList()
                is PayrollEvent.Updated -> navController.navigate(
                    PayrollRoute.detail(event.payroll.id),
                    navOptions {
                        popUpTo(PayrollRoute.LIST) { inclusive = false }
                        launchSingleTop = true
                    }
                )
            }
        }
    }

    NavHost(navController = navController, startDestination = PayrollRoute.LIST) {
        composable(PayrollRoute.LIST) {
            PayrollListScreen(
                payrolls = payrolls,
                onCreate = {
                    pendingDraft = null
                    navController.navigate(PayrollRoute.CREATE)
                },
                onOpen = { navController.navigate(PayrollRoute.detail(it.id)) },
                onRetry = vm::refresh
            )
        }
        composable(PayrollRoute.CREATE) {
            PayrollEditorScreen(
                existing = null,
                onBack = navController::popBackStack,
                onReview = { draft ->
                    pendingDraft = draft
                    navController.navigate(PayrollRoute.review(isUpdate = false))
                }
            )
        }
        composable(
            route = PayrollRoute.EDIT,
            arguments = listOf(navArgument("payrollId") { type = NavType.StringType })
        ) { entry ->
            val payrollId = entry.arguments?.getString("payrollId") ?: return@composable
            val existing = payrolls.firstOrNull { it.id == payrollId }
            if (existing == null) {
                ReturningToList { navController.popBackStack() }
            } else {
                PayrollEditorScreen(
                    existing = existing,
                    onBack = navController::popBackStack,
                    onReview = { draft ->
                        pendingDraft = draft
                        navController.navigate(PayrollRoute.review(isUpdate = true))
                    }
                )
            }
        }
        composable(
            route = PayrollRoute.REVIEW,
            arguments = listOf(navArgument("isUpdate") { type = NavType.BoolType })
        ) { entry ->
            val draft = pendingDraft
            if (draft == null) {
                ReturningToList { navController.navigateToList() }
            } else {
                val isUpdate = entry.arguments?.getBoolean("isUpdate") == true
                PayrollReviewScreen(
                    payroll = draft,
                    isUpdate = isUpdate,
                    onEdit = navController::popBackStack,
                    onConfirm = { if (isUpdate) vm.update(draft) else vm.create(draft) }
                )
            }
        }
        composable(
            route = PayrollRoute.DETAIL,
            arguments = listOf(navArgument("payrollId") { type = NavType.StringType })
        ) { entry ->
            val payrollId = entry.arguments?.getString("payrollId") ?: return@composable
            val payroll = payrolls.firstOrNull { it.id == payrollId }
            if (payroll == null) {
                ReturningToList { navController.navigateToList() }
            } else {
                PayrollDetailScreen(
                    payroll = payroll,
                    onBack = navController::popBackStack,
                    onEdit = {
                        pendingDraft = null
                        navController.navigate(PayrollRoute.edit(payroll.id))
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

private fun androidx.navigation.NavHostController.navigateToList() {
    navigate(PayrollRoute.LIST) {
        popUpTo(PayrollRoute.LIST) { inclusive = false }
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
