package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    TAP("tap", "Tap", Icons.Rounded.Dialpad),
    HISTORY("history", "History", Icons.Rounded.ReceiptLong),
    ANALYTICS("analytics", "Analytics", Icons.AutoMirrored.Rounded.TrendingUp),
    GOALS("goals", "Goals", Icons.Rounded.Savings),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings)
}
