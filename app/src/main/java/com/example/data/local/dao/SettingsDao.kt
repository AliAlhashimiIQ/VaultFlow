package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getUserSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getUserSettingsDirect(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSettings(settings: UserSettingsEntity)

    @Update
    suspend fun updateUserSettings(settings: UserSettingsEntity)

    @Query("UPDATE user_settings SET currencyCode = :code, currencySymbol = :symbol WHERE id = 1")
    suspend fun updateCurrency(code: String, symbol: String)

    @Query("UPDATE user_settings SET monthlyBudget = :budget WHERE id = 1")
    suspend fun updateMonthlyBudget(budget: Double)

    @Query("UPDATE user_settings SET themeMode = :mode WHERE id = 1")
    suspend fun updateThemeMode(mode: String)

    @Query("UPDATE user_settings SET accentTheme = :theme WHERE id = 1")
    suspend fun updateAccentTheme(theme: String)
}
