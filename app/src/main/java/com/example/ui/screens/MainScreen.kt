package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.NavTab
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.savings.SavingsGoalsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tap.TapScreen
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun MainScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by rememberSaveable { mutableStateOf(NavTab.TAP) }
    val extendedColors = LocalExtendedColors.current

    Scaffold(
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = extendedColors.navBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_navigation_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTab.entries.forEach { tab ->
                        val isSelected = tab == currentTab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { currentTab = tab }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .testTag("nav_tab_${tab.route}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .padding(horizontal = if (isSelected) 18.dp else 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(durationMillis = 120),
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    NavTab.TAP -> TapScreen(viewModel = viewModel)
                    NavTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                    NavTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                    NavTab.GOALS -> SavingsGoalsScreen(viewModel = viewModel)
                    NavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
