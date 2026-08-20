package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val currencyCode: String = "IQD",
    val currencySymbol: String = "IQD",
    val monthlyBudget: Double = 0.0,
    val isSetupCompleted: Boolean = true,
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val accentTheme: String = "EMERALD" // "EMERALD", "INDIGO", "BLUE", "VIOLET", "ROSE"
)
