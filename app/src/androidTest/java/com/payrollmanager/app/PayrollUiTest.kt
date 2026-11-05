package com.payrollmanager.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.payrollmanager.app.feature.payroll.ui.PayrollTestTags
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@HiltAndroidTest
class PayrollUiTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun create_edit_and_delete_payroll() {
        val updatedEmployeeName = "Alex Johnson"
        composeRule.onNodeWithTag(PayrollTestTags.CREATE_PAYROLL).performClick()
        addEmployee("Rakesh", "2000")
        composeRule.onNodeWithText("Rakesh").assertIsDisplayed()
        composeRule.onNodeWithTag(PayrollTestTags.REVIEW_PAYROLL).performClick()
        composeRule.onAllNodesWithText("100.00", substring = true).assertCountEquals(2)
        composeRule.onNodeWithTag(PayrollTestTags.SAVE_PAYROLL).performClick()

        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("1 employee").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("1 employee").performClick()
        composeRule.onNodeWithTag(PayrollTestTags.EDIT_PAYROLL).performClick()
        addEmployee(updatedEmployeeName, "1500")
        composeRule.onNodeWithTag(PayrollTestTags.REVIEW_PAYROLL).performClick()
        composeRule.onNodeWithTag(PayrollTestTags.SAVE_PAYROLL).performClick()

        // Saving an edit emits an event asynchronously; wait until the detail screen is shown.
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(PayrollTestTags.DELETE_PAYROLL).fetchSemanticsNodes().isNotEmpty()
        }
        
        composeRule.onNodeWithText(updatedEmployeeName).assertIsDisplayed()
        composeRule.onNodeWithTag(PayrollTestTags.DELETE_PAYROLL).performClick()
        composeRule.onNodeWithTag(PayrollTestTags.CONFIRM_DELETE).performClick()

        // Other payrolls may exist from a previous run. Verify this test's unique employee is gone.
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(updatedEmployeeName).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onAllNodesWithText(updatedEmployeeName).assertCountEquals(0)
    }

    private fun addEmployee(name: String, wages: String) {
        composeRule.onNodeWithTag(PayrollTestTags.EMPLOYEE_NAME).performTextInput(name)
        composeRule.onNodeWithTag(PayrollTestTags.EMPLOYEE_WAGES).performTextInput(wages)
        composeRule.onNodeWithTag(PayrollTestTags.ADD_EMPLOYEE).performClick()
    }
}
