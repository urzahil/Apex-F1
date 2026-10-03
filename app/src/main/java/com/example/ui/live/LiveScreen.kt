package com.example.ui.live

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceControlMessage
import com.example.data.model.TimingDriverLine
import com.example.ui.components.LivePulsingBadge
import com.example.ui.components.PositionPill
import com.example.ui.components.TeamLiveryBar
import com.example.ui.components.TrackStatusIndicator
import com.example.ui.components.WeatherCardView
import com.example.ui.theme.CarbonBackground
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardElevated
import com.example.ui.theme.CarbonDivider
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.F1Red
import com.example.ui.theme.F1TextPrimary
import com.example.ui.theme.F1TextSecondary
import com.example.ui.theme.F1TextTertiary
import com.example.ui.theme.FlagYellow

@Composable
fun LiveScreen(
    viewModel: LiveViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBackground)
    ) {
        // App / Live Header Bar
        LiveTopBar(
            isLive = (uiState as? LiveUiState.Success)?.isLive ?: false
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
                LiveContentList(
                    state = state,
                    onRefresh = { viewModel.refresh() }
                )
            }
        }
    }
}

@Composable
private fun LiveTopBar(
    isLive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "APEX",
                color = F1Red,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "LIVE",
                color = F1TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            LivePulsingBadge(isLive = isLive)
        }
    }
}

@Composable
private fun LiveContentList(
    state: LiveUiState.Success,
    onRefresh: () -> Unit
) {
    val session = state.status.session
    val weather = state.snapshot?.weather
    val raceControlMessages = state.snapshot?.raceControl?.messages ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Session Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
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
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (session?.meeting?.name ?: "GRAND PRIX").uppercase(),
                            color = F1Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                        state.status.trackStatus?.message?.let { trackMsg ->
                            TrackStatusIndicator(statusMessage = trackMsg)
                        }
                    }

                    val sessionPart = state.status.topThree?.sessionPart ?: state.snapshot?.topThree?.sessionPart
                    val detailedSessionName = LiveTimeUtils.getDetailedSessionName(
                        sessionName = session?.name,
                        sessionType = session?.type,
                        sessionPart = sessionPart,
                        raceControlMessages = raceControlMessages.mapNotNull { it.message }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detailedSessionName,
                        color = F1TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = session?.meeting?.circuit?.shortName ?: session?.meeting?.location ?: "Circuit",
                            color = F1TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        session?.meeting?.country?.name?.let { country ->
                            Text(
                                text = " • $country",
                                color = F1TextTertiary,
                                fontSize = 13.sp
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
                    windSpeed = weather.windSpeed
                )
            }
        }

        // Leaderboard Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE CLASSIFICATION",
                    color = F1TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${state.leaderboard.size} DRIVERS",
                    color = F1TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Table Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CarbonCardElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "POS", color = F1TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                Text(text = "DRIVER", color = F1TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = "TIME / GAP", color = F1TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            items(state.leaderboard, key = { it.getDisplayNumber() }) { driver ->
                DriverTimingRow(driver = driver)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DriverTimingRow(driver: TimingDriverLine) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("driver_row_${driver.getDisplayNumber()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PositionPill(position = driver.getDisplayPosition())
            Spacer(modifier = Modifier.width(6.dp))
            TeamLiveryBar(teamColour = driver.teamColour)
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = driver.team ?: "",
                    color = F1TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val isP1 = driver.getDisplayPosition() == "1" || driver.getDisplayPosition() == "P1"
                val effectiveLap = driver.bestLapTime ?: driver.lapTime ?: driver.lastLapTime
                val effectiveGap = driver.gapToLeader ?: driver.gap

                // Primary time: lap time for the top driver, and the GAP for the others!
                val primaryTime = if (isP1) {
                    effectiveLap ?: "-"
                } else {
                    effectiveGap ?: "-"
                }

                Text(
                    text = primaryTime,
                    color = if (isP1) FlagYellow else ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Current driver status from API (in pit, pit out, out, stopped, knocked out)
                val statusText = driver.getStatusText()
                if (!statusText.isNullOrBlank()) {
                    val statusColor = when (statusText) {
                        "OUT", "STOPPED", "KNOCKED OUT" -> F1Red
                        "PIT OUT" -> ElectricCyan
                        else -> F1TextTertiary
                    }
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
