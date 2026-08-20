package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class ExtendedColors(
    val cardBackground: Color,
    val cardHero: Color,
    val cardHeroBorder: Color,
    val navBackground: Color,
    val navActivePill: Color,
    val keypadKey: Color,
    val keypadKeyActive: Color,
    val borderSubtle: Color,
    val incomeGreen: Color,
    val incomeGreenSubtle: Color,
    val expenseRed: Color,
    val expenseRedSubtle: Color,
    val vaultViolet: Color,
    val vaultVioletSubtle: Color,
    val textMuted: Color,
    val textTertiary: Color,
    val isDark: Boolean
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        cardBackground = LightCardBackground,
        cardHero = LightCardHero,
        cardHeroBorder = LightCardHeroBorder,
        navBackground = LightNavBackground,
        navActivePill = LightNavActivePill,
        keypadKey = LightKeypadKey,
        keypadKeyActive = LightKeypadKeyActive,
        borderSubtle = LightBorderSubtle,
        incomeGreen = IncomeGreen,
        incomeGreenSubtle = IncomeGreenSubtle,
        expenseRed = ExpenseRed,
        expenseRedSubtle = ExpenseRedSubtle,
        vaultViolet = VaultViolet,
        vaultVioletSubtle = VaultVioletSubtle,
        textMuted = LightTextMuted,
        textTertiary = LightTextTertiary,
        isDark = false
    )
}

fun getAccentColors(accent: String, isDark: Boolean): Pair<Color, Color> {
    return when (accent.uppercase()) {
        "INDIGO" -> if (isDark) Pair(IndigoPrimaryDark, IndigoContainerDark) else Pair(IndigoPrimaryLight, IndigoContainerLight)
        "BLUE" -> if (isDark) Pair(BluePrimaryDark, BlueContainerDark) else Pair(BluePrimaryLight, BlueContainerLight)
        "VIOLET" -> if (isDark) Pair(VioletPrimaryDark, VioletContainerDark) else Pair(VioletPrimaryLight, VioletContainerLight)
        "ROSE" -> if (isDark) Pair(RosePrimaryDark, RoseContainerDark) else Pair(RosePrimaryLight, RoseContainerLight)
        "EMERALD", "TEAL" -> if (isDark) Pair(EmeraldPrimaryDark, EmeraldContainerDark) else Pair(EmeraldPrimaryLight, EmeraldContainerLight)
        else -> if (isDark) Pair(EmeraldPrimaryDark, EmeraldContainerDark) else Pair(EmeraldPrimaryLight, EmeraldContainerLight)
    }
}

fun createAppColorScheme(isDark: Boolean, accent: String): ColorScheme {
    val (primaryColor, containerColor) = getAccentColors(accent, isDark)

    return if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = containerColor,
            onPrimaryContainer = Color.White,
            secondary = DarkTextMuted,
            onSecondary = Color.White,
            secondaryContainer = DarkSurfaceVariant,
            onSecondaryContainer = DarkTextPrimary,
            tertiary = VaultViolet,
            onTertiary = Color.White,
            tertiaryContainer = VaultVioletDarkSubtle,
            onTertiaryContainer = Color.White,
            background = DarkBackground,
            onBackground = DarkTextPrimary,
            surface = DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkBorder,
            outlineVariant = DarkBorderSubtle,
            error = ExpenseRed,
            onError = Color.White,
            errorContainer = ExpenseRedDarkSubtle,
            onErrorContainer = ExpenseRed
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = containerColor,
            onPrimaryContainer = primaryColor,
            secondary = LightTextMuted,
            onSecondary = Color.White,
            secondaryContainer = LightSurfaceVariant,
            onSecondaryContainer = LightTextPrimary,
            tertiary = VaultViolet,
            onTertiary = Color.White,
            tertiaryContainer = VaultVioletSubtle,
            onTertiaryContainer = VaultViolet,
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightTextSecondary,
            outline = LightBorder,
            outlineVariant = LightBorderSubtle,
            error = ExpenseRed,
            onError = Color.White,
            errorContainer = ExpenseRedSubtle,
            onErrorContainer = ExpenseRed
        )
    }
}

fun createExtendedColors(isDark: Boolean): ExtendedColors {
    return if (isDark) {
        ExtendedColors(
            cardBackground = DarkCardBackground,
            cardHero = DarkCardHero,
            cardHeroBorder = DarkCardHeroBorder,
            navBackground = DarkNavBackground,
            navActivePill = DarkNavActivePill,
            keypadKey = DarkKeypadKey,
            keypadKeyActive = DarkKeypadKeyActive,
            borderSubtle = DarkBorderSubtle,
            incomeGreen = IncomeGreen,
            incomeGreenSubtle = IncomeGreenDarkSubtle,
            expenseRed = ExpenseRed,
            expenseRedSubtle = ExpenseRedDarkSubtle,
            vaultViolet = VaultViolet,
            vaultVioletSubtle = VaultVioletDarkSubtle,
            textMuted = DarkTextMuted,
            textTertiary = DarkTextTertiary,
            isDark = true
        )
    } else {
        ExtendedColors(
            cardBackground = LightCardBackground,
            cardHero = LightCardHero,
            cardHeroBorder = LightCardHeroBorder,
            navBackground = LightNavBackground,
            navActivePill = LightNavActivePill,
            keypadKey = LightKeypadKey,
            keypadKeyActive = LightKeypadKeyActive,
            borderSubtle = LightBorderSubtle,
            incomeGreen = IncomeGreen,
            incomeGreenSubtle = IncomeGreenSubtle,
            expenseRed = ExpenseRed,
            expenseRedSubtle = ExpenseRedSubtle,
            vaultViolet = VaultViolet,
            vaultVioletSubtle = VaultVioletSubtle,
            textMuted = LightTextMuted,
            textTertiary = LightTextTertiary,
            isDark = false
        )
    }
}

@Composable
fun FinanceTrackerTheme(
    themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    accentTheme: String = "EMERALD",
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
    }

    val colorScheme = createAppColorScheme(isDark, accentTheme)
    val extendedColors = createExtendedColors(isDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = extendedColors.navBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
