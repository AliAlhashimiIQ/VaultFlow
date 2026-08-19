package com.example.ui.components

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String,
    val hasDecimals: Boolean = true
)

object CurrencyHelper {
    val supportedCurrencies = listOf(
        CurrencyOption("IQD", "Iraqi Dinar", "IQD", hasDecimals = false),
        CurrencyOption("USD", "US Dollar", "$", hasDecimals = true),
        CurrencyOption("EUR", "Euro", "€", hasDecimals = true),
        CurrencyOption("GBP", "British Pound", "£", hasDecimals = true),
        CurrencyOption("SAR", "Saudi Riyal", "SAR", hasDecimals = true),
        CurrencyOption("AED", "UAE Dirham", "AED", hasDecimals = true),
        CurrencyOption("KWD", "Kuwaiti Dinar", "KWD", hasDecimals = true),
        CurrencyOption("TRY", "Turkish Lira", "₺", hasDecimals = true),
        CurrencyOption("CAD", "Canadian Dollar", "$", hasDecimals = true),
        CurrencyOption("AUD", "Australian Dollar", "$", hasDecimals = true),
        CurrencyOption("JPY", "Japanese Yen", "¥", hasDecimals = false),
        CurrencyOption("INR", "Indian Rupee", "₹", hasDecimals = true)
    )

    fun formatCurrency(amount: Double, currencyCode: String, currencySymbol: String = currencyCode): String {
        val isNoDecimal = currencyCode in listOf("IQD", "JPY", "KRW", "VND") || (amount % 1.0 == 0.0)
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = if (isNoDecimal) "#,##0" else "#,##0.00"
        val df = DecimalFormat(pattern, symbols)
        val formattedNumber = df.format(amount)
        
        return "$currencySymbol $formattedNumber"
    }

    fun formatDateHeader(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val date = Date(timestamp)
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val isToday = dayFormat.format(Date(now)) == dayFormat.format(date)
        val isYesterday = dayFormat.format(Date(now - 24 * 60 * 60 * 1000L)) == dayFormat.format(date)

        return when {
            isToday -> "TODAY"
            isYesterday -> "YESTERDAY"
            else -> {
                val formatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                formatter.format(date).uppercase()
            }
        }
    }

    fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}
