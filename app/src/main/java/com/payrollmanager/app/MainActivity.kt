package com.payrollmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.payrollmanager.app.feature.payroll.ui.PayrollApp
import com.payrollmanager.app.feature.payroll.presentation.PayrollViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: PayrollViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme =
                    lightColorScheme(primary = Color(0xFF2457D6))
            ) {
                PayrollApp(viewModel)
            }
        }
    }
}
