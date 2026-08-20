package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplashScreenOn = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreenOn }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Allow the splash animation and obsidian backdrop to show smoothly for 600ms on cold start
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            keepSplashScreenOn = false
        }, 650)
        setContent {
            val viewModel: FinanceViewModel = viewModel()
            val settings by viewModel.userSettings.collectAsStateWithLifecycle()

            FinanceTrackerTheme(
                themeMode = settings.themeMode,
                accentTheme = settings.accentTheme
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
