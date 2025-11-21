package com.teamvault.budgetvault

import android.content.Context

/**
 * Manager class for handling currency preferences
 */
object CurrencyManager {
    // Currency codes
    const val CURRENCY_INR = "INR"
    const val CURRENCY_ZAR = "ZAR"
    const val CURRENCY_GBP = "GBP"
    const val CURRENCY_EUR = "EUR"
    const val CURRENCY_CNY = "CNY"

    private const val PREF_NAME = "app_preferences"
    private const val PREF_CURRENCY = "selected_currency"

    // Default currency
    private const val DEFAULT_CURRENCY = CURRENCY_ZAR

    /**
     * Get the currently selected currency code
     */
    fun getSelectedCurrency(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(PREF_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY
    }

    /**
     * Update the selected currency
     */
    fun setSelectedCurrency(context: Context, currencyCode: String) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(PREF_CURRENCY, currencyCode).apply()
    }

    /**
     * Get currency symbol for the given currency code
     */
    fun getCurrencySymbol(currencyCode: String): String {
        return when (currencyCode) {
            CURRENCY_INR -> "₹"
            CURRENCY_ZAR -> "R"
            CURRENCY_GBP -> "£"
            CURRENCY_EUR -> "€"
            CURRENCY_CNY -> "¥"
            else -> "R" // Default to ZAR
        }
    }

    /**
     * Format amount with currency symbol
     */
    fun formatAmount(amount: Double, currencyCode: String): String {
        val symbol = getCurrencySymbol(currencyCode)
        return "$symbol ${String.format("%.2f", amount)}"
    }
}