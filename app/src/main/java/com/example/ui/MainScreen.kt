package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.live.LiveScreen
import com.example.ui.live.LiveViewModel
import com.example.ui.standings.StandingsScreen
import com.example.ui.standings.StandingsViewModel
import com.example.ui.theme.CarbonBackground
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.F1Red
import com.example.ui.theme.F1TextPrimary
import com.example.ui.theme.F1TextSecondary

enum class NavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    LIVE("Live", Icons.Filled.Speed, Icons.Outlined.Speed, "nav_live"),
    STANDINGS("Standings", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_standings"),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History, "nav_history")
}

@Composable
fun MainScreen() {
    var selectedTab by rememberSaveable { mutableStateOf(NavDestination.LIVE) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = CarbonCard,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavDestination.entries.forEach { destination ->
                    val isSelected = selectedTab == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = destination },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = F1TextPrimary,
                            selectedTextColor = F1Red,
                            indicatorColor = F1Red,
                            unselectedIconColor = F1TextSecondary,
                            unselectedTextColor = F1TextSecondary
                        ),
                        modifier = Modifier.testTag(destination.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().background(CarbonBackground).padding(innerPadding)
        ) {
            when (selectedTab) {
                NavDestination.LIVE -> {
                    val viewModel: LiveViewModel = viewModel()
                    LaunchedEffect(viewModel) { viewModel.setScreenActive(true) }
                    DisposableEffect(viewModel) {
                        onDispose { viewModel.setScreenActive(false) }
                    }
                    LiveScreen(viewModel = viewModel)
                }
                NavDestination.STANDINGS -> {
                    val viewModel: StandingsViewModel = viewModel()
                    StandingsScreen(viewModel = viewModel)
                }
                NavDestination.HISTORY -> {
                    val viewModel: HistoryViewModel = viewModel()
                    HistoryScreen(viewModel = viewModel)
                }
            }
        }
    }
}