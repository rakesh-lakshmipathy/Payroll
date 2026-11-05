package com.payrollmanager.app.feature.payroll.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@ConsistentCopyVisibility
data class Money private constructor(val amount: BigDecimal, val currencyCode: String) {
    init {
        require(Currency.getInstance(currencyCode) != null)
    }

    operator fun plus(other: Money): Money =
        withSameCurrency(other) { copy(amount = amount + other.amount) }

    operator fun minus(other: Money): Money =
        withSameCurrency(other) {
            copy(amount = amount - other.amount)
        }

    fun percentage(rate: BigDecimal): Money =
        copy(amount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP))

    fun format(locale: Locale = Locale.getDefault()): String =
        NumberFormat.getCurrencyInstance(locale)
            .apply { currency = Currency.getInstance(currencyCode) }.format(amount)

    private inline fun withSameCurrency(other: Money, result: () -> Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot combine $currencyCode with ${other.currencyCode}" }
        return result()
    }

    companion object {
        const val PAYROLL_CURRENCY = "USD"
        fun of(amount: BigDecimal, currencyCode: String = PAYROLL_CURRENCY) =
            Money(amount.setScale(2, RoundingMode.HALF_UP), currencyCode)

        fun zero(currencyCode: String = PAYROLL_CURRENCY) = of(BigDecimal.ZERO, currencyCode)
    }
}
