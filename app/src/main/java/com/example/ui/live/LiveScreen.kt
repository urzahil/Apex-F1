package com.example.ui.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TimingDriverLine
import com.example.data.model.getBadgeStatus
import com.example.ui.components.DriverHeadshotAvatar
import com.example.ui.components.LiveBadgeStatus
import com.example.ui.components.LivePulsingBadge
import com.example.ui.components.TeamLiveryBar
import com.example.ui.components.TrackStatusIndicator
import com.example.ui.components.TyreCompoundBadge
import com.example.ui.components.WeatherCardView
import com.example.ui.theme.CarbonBackground
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardElevated
import com.example.ui.theme.CarbonDivider
import com.example.ui.theme.F1Red
import com.example.ui.theme.F1TextPrimary
import com.example.ui.theme.F1TextSecondary
import com.example.ui.theme.F1TextTertiary
import com.example.ui.theme.FlagYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
    viewModel: LiveViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val autoRefreshEnabled by viewModel.autoRefreshEnabled.collectAsStateWithLifecycle()
    var isManualRefresh by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) isManualRefresh = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBackground)
    ) {
        // App / Live Header Bar
        LiveTopBar(
            status = (uiState as? LiveUiState.Success)?.status.getBadgeStatus(),
            autoRefreshEnabled = autoRefreshEnabled,
            onAutoRefreshChange = { viewModel.toggleAutoRefresh() }
        )

        when (val state = uiState) {
            is LiveUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = F1Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "CONNECTING TO F1 LIVE TIMING...",
                            color = F1TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            is LiveUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Connection Error",
                            tint = F1Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Live Timing Stream Unavailable",
                            color = F1TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = F1TextSecondary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = F1Red),
                            modifier = Modifier.testTag("retry_live_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Connection")
                        }
                    }
                }
            }

            is LiveUiState.Success -> {
                PullToRefreshBox(
                    // Automatic refreshes (including returning to this tab) should update
                    // the data without displaying the pull-to-refresh indicator.
                    isRefreshing = isRefreshing && isManualRefresh,
                    onRefresh = {
                        isManualRefresh = true
                        viewModel.refresh()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    LiveContentList(state = state)
                }
            }
        }
    }
}

@Composable
private fun LiveTopBar(
    status: LiveBadgeStatus,
    autoRefreshEnabled: Boolean,
    onAutoRefreshChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
//            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "LIVE",
                color = F1TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            LivePulsingBadge(status = status)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AUTO",
                color = if (autoRefreshEnabled) F1TextPrimary else F1TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Switch(
                checked = autoRefreshEnabled,
                onCheckedChange = { onAutoRefreshChange() },
                thumbContent = if (autoRefreshEnabled) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else null,
                modifier = Modifier.testTag("auto_refresh_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = F1Red,
                    checkedIconColor = F1Red,
                    checkedBorderColor = F1Red,
                    uncheckedThumbColor = F1TextSecondary,
                    uncheckedTrackColor = CarbonCardElevated,
                    uncheckedBorderColor = CarbonDivider
                )
            )
        }
    }
}

@Composable
private fun LiveContentList(
    state: LiveUiState.Success
) {
    val session = state.status.session
    val weather = state.snapshot?.weather
    val raceControlMessages = state.snapshot?.raceControl?.messages ?: emptyList()
    var isRaceControlExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedDriver by remember { mutableStateOf<TimingDriverLine?>(null) }
    val lastMessages = remember(raceControlMessages) {
        raceControlMessages
            .filterNot { it.message.isNullOrBlank() }
            .takeLast(5)
            .reversed()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hero Session Banner
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CarbonCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    F1Red.copy(alpha = 0.15f),
                                    CarbonCard
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (session?.meeting?.name ?: "GRAND PRIX").uppercase(),
                            color = F1Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        val sessionEnded = session?.sessionStatus?.let {
                            it.equals("Finalised", true) || it.equals("Finished", true) ||
                                it.equals("Ended", true) || it.equals("Completed", true)
                        } == true
                        if (!sessionEnded) {
                            state.status.trackStatus?.message?.let { trackMsg ->
                                TrackStatusIndicator(statusMessage = trackMsg)
                            }
                        }
                    }

                    val sessionPart =
                        state.status.topThree?.sessionPart ?: state.snapshot?.topThree?.sessionPart
                    val detailedSessionName = LiveTimeUtils.getDetailedSessionName(
                        sessionName = session?.name,
                        sessionType = session?.type,
                        sessionPart = sessionPart,
                        raceControlMessages = raceControlMessages.mapNotNull { it.message }
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = detailedSessionName,
                        color = F1TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = session?.meeting?.circuit?.shortName
                                ?: session?.meeting?.location ?: "Circuit",
                            color = F1TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        session?.meeting?.country?.name?.let { country ->
                            Text(
                                text = " • $country",
                                color = F1TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Weather
        if (weather != null) {
            item {
                WeatherCardView(
                    airTemp = weather.airTemp,
                    trackTemp = weather.trackTemp,
                    humidity = weather.humidity,
                    rainfall = weather.rainfall,
                    windSpeed = weather.windSpeed
                )
            }
        }

        // Leaderboard & Table Header
        item {
            val lapCount = state.lapCount ?: state.snapshot?.lapCount ?: state.status.lapCount
            val isRaceEvent = session?.type?.contains("Race", true) == true ||
                    session?.name?.contains("Race", true) == true ||
                    session?.type?.contains("Sprint", true) == true ||
                    session?.name?.contains("Sprint", true) == true
            val lapText = if (isRaceEvent && lapCount?.getCurrentLapInt() != null) {
                val current = lapCount.getCurrentLapInt()
                val total = lapCount.getTotalLapsInt()
                if (total != null && total > 0) "LAP $current/$total" else "LAP $current"
            } else null

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (lapText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lapText,
                            color = F1TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CarbonCardElevated)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "POS",
                        color = F1TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        text = "DRIVER",
                        color = F1TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "TIME / GAP",
                        color = F1TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (state.leaderboard.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CarbonCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Waiting for Session",
                            tint = F1TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Awaiting Session Timing Feed",
                            color = F1TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cars will appear here once the pit exit opens.",
                            color = F1TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            val sessionMetadata = "${state.status.session?.type.orEmpty()} ${state.status.session?.name.orEmpty()}"
            val useBestLapTime = listOf("Practice", "Qualifying", "Shootout").any {
                sessionMetadata.contains(it, ignoreCase = true)
            }
            items(state.leaderboard, key = { it.getDisplayNumber() }) { driver ->
                LiveDriverRow(driver = driver, useBestLapTime = useBestLapTime, onClick = {
                    if (driver.getSortedStints().isNotEmpty()) {
                        selectedDriver = driver
                    }
                })
            }
        }

        // Race Control Messages Section (Last 5 messages, newest at top, collapsible)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CarbonCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FlagYellow.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .testTag("race_control_section")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    FlagYellow.copy(alpha = 0.12f),
                                    CarbonCard
                                )
                            )
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRaceControlExpanded = !isRaceControlExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Race Control",
                                tint = FlagYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RACE CONTROL",
                                color = FlagYellow,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Icon(
                            imageVector = if (isRaceControlExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isRaceControlExpanded) "Collapse" else "Expand",
                            tint = FlagYellow,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isRaceControlExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))

                            if (lastMessages.isEmpty()) {
                                Text(
                                    text = "No recent messages from Race Control.",
                                    color = F1TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    lastMessages.forEachIndexed { index, msg ->
                                        if (index > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(FlagYellow.copy(alpha = 0.15f))
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(FlagYellow)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = msg.message.orEmpty(),
                                                    color = F1TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (!msg.category.isNullOrBlank() || !msg.flag.isNullOrBlank()) {
                                                    val categoryText =
                                                        listOfNotNull(msg.category, msg.flag)
                                                            .filter { it.isNotBlank() }
                                                            .joinToString(" • ")
                                                    Text(
                                                        text = categoryText,
                                                        color = FlagYellow.copy(alpha = 0.9f),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    selectedDriver?.let { driver ->
        val stints = driver.getSortedStints()
        if (stints.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { selectedDriver = null },
                containerColor = CarbonCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .border(1.dp, CarbonDivider, RoundedCornerShape(16.dp)),
                title = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamLiveryBar(
                                    teamColour = driver.teamColour,
                                    teamName = driver.team
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                DriverHeadshotAvatar(
                                    headshotUrl = driver.getEffectiveHeadshotUrl(),
                                    driverName = driver.name ?: driver.getDisplayTla(),
                                    size = 36.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#${driver.getDisplayNumber()}",
                                            color = F1Red,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = driver.name ?: driver.getDisplayTla(),
                                            color = F1TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = driver.team ?: "F1 Team",
                                        color = F1TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CarbonDivider)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "TYRE STINT HISTORY",
                            color = F1Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stints.forEachIndexed { index, stint ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = CarbonCardElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TyreCompoundBadge(compound = stint.compound)
                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "STINT ${index + 1}",
                                                color = F1TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${(stint.compound ?: "UNKNOWN").uppercase()}",
                                                color = F1TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        val details = listOfNotNull(
                                            stint.totalLaps?.let { "$it LAPS" },
                                            stint.isNew?.let {
                                                if (it.equals(
                                                        "true",
                                                        true
                                                    )
                                                ) "NEW SET" else "USED SET"
                                            },
                                            stint.tyresNotChanged?.let { if (it != "0") "REUSED" else null }
                                        ).filter { it.isNotBlank() }

                                        if (details.isNotEmpty()) {
                                            Text(
                                                text = details.joinToString(" • "),
                                                color = F1TextTertiary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedDriver = null },
                        colors = ButtonDefaults.buttonColors(containerColor = F1Red),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CLOSE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            )
        }
    }
}
