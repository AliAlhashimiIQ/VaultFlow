package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconMapper {
    private val iconMap = mapOf<String, ImageVector>(
        "subscriptions" to Icons.Rounded.Subscriptions,
        "shopping_cart" to Icons.Rounded.ShoppingCart,
        "fastfood" to Icons.Rounded.Fastfood,
        "directions_car" to Icons.Rounded.DirectionsCar,
        "person" to Icons.Rounded.Person,
        "receipt_long" to Icons.AutoMirrored.Rounded.ReceiptLong,
        "movie" to Icons.Rounded.Movie,
        "medical_services" to Icons.Rounded.LocalHospital,
        "shopping_bag" to Icons.Rounded.ShoppingCart,
        "home" to Icons.Rounded.Home,
        "school" to Icons.Rounded.School,
        "payments" to Icons.Rounded.Payments,
        "laptop" to Icons.Rounded.Laptop,
        "trending_up" to Icons.AutoMirrored.Rounded.TrendingUp,
        "redeem" to Icons.Rounded.Redeem,
        "shield" to Icons.Rounded.Shield,
        "flight" to Icons.Rounded.Flight,
        "pets" to Icons.Rounded.Pets,
        "fitness_center" to Icons.Rounded.FitnessCenter,
        "local_cafe" to Icons.Rounded.LocalCafe,
        "build" to Icons.Rounded.Build,
        "savings" to Icons.Rounded.Savings,
        "category" to Icons.Rounded.Category,
        "account_balance_wallet" to Icons.Rounded.AccountBalanceWallet,
        "sports_esports" to Icons.Rounded.SportsEsports,
        "celebration" to Icons.Rounded.Celebration,
        "auto_awesome" to Icons.Rounded.AutoAwesome
    )

    fun getIcon(name: String): ImageVector {
        return iconMap[name] ?: Icons.Rounded.Category
    }

    val allAvailableIcons = listOf(
        "subscriptions" to "Subscription",
        "shopping_cart" to "Market / Cart",
        "fastfood" to "Food & Dining",
        "directions_car" to "Transport / Car",
        "person" to "Personal Care",
        "receipt_long" to "Bills & Utilities",
        "movie" to "Entertainment",
        "medical_services" to "Health & Medical",
        "home" to "Home & Rent",
        "school" to "Education",
        "payments" to "Salary / Cash",
        "laptop" to "Tech / Freelance",
        "trending_up" to "Investment",
        "redeem" to "Gift / Bonus",
        "shield" to "Emergency Fund",
        "flight" to "Travel / Vacation",
        "pets" to "Pets",
        "fitness_center" to "Gym & Fitness",
        "local_cafe" to "Coffee & Drinks",
        "savings" to "Savings & Vault",
        "build" to "Maintenance / Tools",
        "category" to "Other"
    )

    val availableColors = listOf(
        "#4F46E5", // Indigo
        "#2563EB", // Blue
        "#0284C7", // Sky Blue
        "#0D9488", // Teal
        "#10B981", // Emerald
        "#F59E0B", // Amber
        "#FB923C", // Orange
        "#EF4444", // Red
        "#E11D48", // Rose
        "#EC4899", // Pink
        "#8B5CF6", // Violet
        "#6366F1", // Slate Indigo
        "#64748B"  // Slate
    )

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF4F46E5)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorLong = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color((0xFF000000 or colorLong).toInt())
            } else if (cleanHex.length == 8) {
                Color(colorLong.toInt())
            } else {
                defaultColor
            }
        } catch (e: Exception) {
            defaultColor
        }
    }
}
