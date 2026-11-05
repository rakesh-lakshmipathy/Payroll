package com.payrollmanager.app.feature.payroll.presentation

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.payrollmanager.app.feature.payroll.domain.WriteResult
import com.payrollmanager.app.feature.payroll.domain.model.Payroll
import com.payrollmanager.app.feature.payroll.domain.usecase.PayrollUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-off UI outcomes. Navigation is performed by the UI, not by this ViewModel. */
sealed interface PayrollEvent {
    data object Created : PayrollEvent
    data class Updated(val payroll: Payroll) : PayrollEvent
    data object Deleted : PayrollEvent
}

@HiltViewModel
class PayrollViewModel @Inject constructor(
    private val useCases: PayrollUseCases,
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics
) : ViewModel() {
    val payrolls: StateFlow<List<Payroll>> = useCases.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _events = Channel<PayrollEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun create(payroll: Payroll) = viewModelScope.launch {
        when (val result = useCases.create(payroll)) {
            is WriteResult.Saved -> {
                logEvent(PayrollAnalytics.EVENT_PAYROLL_CREATED, result.syncPending)
                _events.send(PayrollEvent.Created)
                if (result.syncPending) _error.value =
                    "Saved locally, but sync failed. You can retry when online."
            }

            is WriteResult.Failed -> {
                recordFailure(PayrollAnalytics.OPERATION_CREATE, result.cause)
                _error.value = "Could not save payroll. Please try again."
            }
        }
    }

    fun update(payroll: Payroll) = viewModelScope.launch {
        when (val result = useCases.update(payroll)) {
            is WriteResult.Saved -> {
                logEvent(PayrollAnalytics.EVENT_PAYROLL_UPDATED, result.syncPending)
                _events.send(PayrollEvent.Updated(payroll))
                if (result.syncPending) _error.value =
                    "Changes saved locally, but sync failed. You can retry when online."
            }

            is WriteResult.Failed -> {
                recordFailure(PayrollAnalytics.OPERATION_UPDATE, result.cause)
                _error.value = "Could not save changes. Please try again."
            }
        }
    }

    fun delete(id: String) = viewModelScope.launch {
        when (val result = useCases.delete(id)) {
            is WriteResult.Saved -> {
                logEvent(PayrollAnalytics.EVENT_PAYROLL_DELETED, result.syncPending)
                _events.send(PayrollEvent.Deleted)
                if (result.syncPending) _error.value =
                    "Deletion saved locally, but sync failed. You can retry when online."
            }

            is WriteResult.Failed -> {
                recordFailure(PayrollAnalytics.OPERATION_DELETE, result.cause)
                _error.value = "Could not delete payroll. Please try again."
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        useCases.refresh().onFailure {
            _error.value = "Could not sync right now. Your saved payrolls are still available."
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun logEvent(name: String, syncPending: Boolean) {
        analytics.logEvent(
            name,
            Bundle().apply {
                putBoolean(PayrollAnalytics.PARAM_SYNC_PENDING, syncPending)
            }
        )
    }

    private fun recordFailure(operation: String, error: Exception) {
        crashlytics.setCustomKey(PayrollAnalytics.CRASHLYTICS_KEY_OPERATION, operation)
        crashlytics.recordException(error)
    }
}

private object PayrollAnalytics {
    const val EVENT_PAYROLL_CREATED = "payroll_created"
    const val EVENT_PAYROLL_UPDATED = "payroll_updated"
    const val EVENT_PAYROLL_DELETED = "payroll_deleted"
    const val PARAM_SYNC_PENDING = "sync_pending"
    const val CRASHLYTICS_KEY_OPERATION = "operation"
    const val OPERATION_CREATE = "create_payroll"
    const val OPERATION_UPDATE = "update_payroll"
    const val OPERATION_DELETE = "delete_payroll"
}
