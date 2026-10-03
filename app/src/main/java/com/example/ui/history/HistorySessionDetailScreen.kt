package com.example.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.repository.MergedHistoryClassification
import com.example.ui.components.PositionPill
import com.example.ui.components.TeamLiveryBar
import com.example.ui.theme.CarbonBackground
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardElevated
import com.example.ui.theme.CarbonDivider
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.F1Red
import com.example.ui.theme.F1TextPrimary
import com.example.ui.theme.F1TextSecondary
import com.example.ui.theme.F1TextTertiary
import com.example.ui.theme.FlagGreen
import com.example.ui.theme.FlagYellow
import com.example.ui.live.LiveTimeUtils

enum class QualiSessionTab {
    GRID, Q1, Q2, Q3
}

@Composable
fun HistorySessionDetailScreen(
    detailState: HistorySessionDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBackground)
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("history_detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = F1TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "HISTORICAL SESSION CLASSIFICATION",
                    color = F1Red,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = when (detailState) {
                        is HistorySessionDetailUiState.Success -> "${detailState.meetingName} • ${detailState.session.name ?: "Session"}"
                        else -> "Session Details"
                    },
                    color = F1TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        when (detailState) {
            is HistorySessionDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = F1Red)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "DOWNLOADING ARCHIVE TELEMETRY...",
                            color = F1TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            is HistorySessionDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = detailState.message, color = F1Red, fontSize = 14.sp)
                }
            }
            is HistorySessionDetailUiState.Success -> {
                val isQualifying = detailState.session.name?.contains("Qualifying", ignoreCase = true) == true ||
                        detailState.session.type?.contains("Qualifying", ignoreCase = true) == true

                HistoryClassificationList(
                    items = detailState.classification,
                    isQualifying = isQualifying,
                    sessionName = detailState.session.name ?: "Session"
                )
            }
            HistorySessionDetailUiState.Idle -> {}
        }
    }
}

@Composable
private fun HistoryClassificationList(
    items: List<MergedHistoryClassification>,
    isQualifying: Boolean,
    sessionName: String
) {
    var selectedQualiTab by rememberSaveable { mutableStateOf(QualiSessionTab.GRID) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Table info banner
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = CarbonCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sessionName.uppercase(),
                        color = F1Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Qualifying split session chips / tabs
        if (isQualifying) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        QualiSessionTab.GRID to "FINAL GRID",
                        QualiSessionTab.Q1 to "Q1 SESSION",
                        QualiSessionTab.Q2 to "Q2 SESSION",
                        QualiSessionTab.Q3 to "Q3 SHOOTOUT"
                    ).forEach { (tab, title) ->
                        val isSelected = selectedQualiTab == tab
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) F1Red else CarbonCard)
                                .border(1.dp, if (isSelected) F1Red else CarbonDivider, RoundedCornerShape(8.dp))
                                .clickable { selectedQualiTab = tab }
                                .padding(vertical = 8.dp)
                                .testTag("tab_quali_${tab.name}")
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else F1TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Table column header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CarbonCardElevated)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "POS", color = F1TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                Text(text = "DRIVER / CONSTRUCTOR", color = F1TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    text = if (isQualifying) {
                        when (selectedQualiTab) {
                            QualiSessionTab.GRID -> "BEST / GAP"
                            QualiSessionTab.Q1 -> "Q1 TIME"
                            QualiSessionTab.Q2 -> "Q2 TIME"
                            QualiSessionTab.Q3 -> "Q3 TIME"
                        }
                    } else "TIME / GAP",
                    color = F1TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (items.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CarbonCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No classification data recorded in the archive for this session.",
                            color = F1TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            // Filter and sort once per state change; the LazyColumn itself may recompose many times.
            val isRace = sessionName.contains("Race", ignoreCase = true)
            val isPractice = sessionName.contains("Practice", ignoreCase = true) || sessionName.contains("FP", ignoreCase = true)
            val displayItems: List<Pair<MergedHistoryClassification, String?>> = remember(items, selectedQualiTab, isQualifying, sessionName) {
                if (isQualifying) {
                    when (selectedQualiTab) {
                        QualiSessionTab.Q1 -> items.filter { !it.q1Time.isNullOrBlank() }
                            .sortedBy { LiveTimeUtils.parseLapTimeToMillis(it.q1Time) ?: Long.MAX_VALUE }
                            .mapIndexed { idx, it -> it to if (idx == 0) it.q1Time else (it.q1Diff ?: it.q1Time) }
                        QualiSessionTab.Q2 -> items.filter { !it.q2Time.isNullOrBlank() }
                            .sortedBy { LiveTimeUtils.parseLapTimeToMillis(it.q2Time) ?: Long.MAX_VALUE }
                            .mapIndexed { idx, it -> it to if (idx == 0) it.q2Time else (it.q2Diff ?: it.q2Time) }
                        QualiSessionTab.Q3 -> items.filter { !it.q3Time.isNullOrBlank() }
                            .sortedBy { LiveTimeUtils.parseLapTimeToMillis(it.q3Time) ?: Long.MAX_VALUE }
                            .mapIndexed { idx, it -> it to if (idx == 0) it.q3Time else (it.q3Diff ?: it.q3Time) }
                        QualiSessionTab.GRID -> items.map { it to (it.bestLapTime ?: it.q3Time ?: it.q2Time ?: it.q1Time) }
                    }
                } else if (isRace) {
                    items.mapIndexed { idx, it ->
                        it to if (idx == 0 || it.position == "1") it.totalRaceTime ?: it.bestLapTime ?: "WINNER"
                        else if (it.isRetired) "DNF" else (it.gapToLeader ?: it.intervalToAhead ?: "-")
                    }
                } else if (isPractice) {
                    items.mapIndexed { idx, it ->
                        it to if (idx == 0 || it.position == "1") it.bestLapTime ?: "-"
                        else it.timeDiffToFastest ?: it.gapToLeader ?: it.intervalToAhead ?: "-"
                    }
                } else {
                    items.map { it to it.bestLapTime }
                }
            }
            val displayRankByDriver = remember(displayItems, selectedQualiTab) {
                displayItems.mapIndexed { index, pair -> pair.first.driverNumber to (index + 1).toString() }.toMap()
            }
            items(displayItems, key = { "${it.first.driverNumber}:${selectedQualiTab.name}" }) { (item, sessionTime) ->
                val displayRank = when (selectedQualiTab) {
                    QualiSessionTab.GRID -> item.position
                    else -> displayRankByDriver[item.driverNumber] ?: "99"
                }

                HistoryDriverRow(
                    item = item,
                    displayRank = displayRank,
                    specificTime = sessionTime,
                    isQualifying = isQualifying,
                    isRace = isRace,
                    isPractice = isPractice,
                    currentQualiTab = selectedQualiTab,
                    totalInTab = displayItems.size
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryDriverRow(
    item: MergedHistoryClassification,
    displayRank: String,
    specificTime: String?,
    isQualifying: Boolean,
    isRace: Boolean,
    isPractice: Boolean,
    currentQualiTab: QualiSessionTab,
    totalInTab: Int
) {
    val rankInt = displayRank.toIntOrNull() ?: 99

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PositionPill(position = displayRank)
                Spacer(modifier = Modifier.width(8.dp))
                TeamLiveryBar(teamColour = item.teamColour, teamName = item.teamName)
                Spacer(modifier = Modifier.width(8.dp))

                // Driver Headshot Avatar (if available)
                if (!item.headshotUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.headshotUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CarbonCardElevated)
                            .border(1.dp, CarbonCardElevated, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${item.driverNumber}",
                            color = F1Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.fullName,
                            color = F1TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = item.teamName,
                            color = F1TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!item.countryCode.isNullOrBlank()) {
                            Text(
                                text = " • ${item.countryCode}",
                                color = F1TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Time / Status Column
                Column(horizontalAlignment = Alignment.End) {
                    val isP1 = displayRank == "1"
                    val timeToShow = specificTime ?: item.bestLapTime ?: item.gapToLeader ?: "-"

                    Text(
                        text = timeToShow,
                        color = if (isP1) FlagYellow else ElectricCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    if (isRace) {
                        if (isP1) {
                            if (item.numberOfLaps != null) {
                                Text(
                                    text = "${item.numberOfLaps} laps",
                                    color = FlagYellow,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else if (item.isRetired) {
                            Text(text = "DNF / RET", color = F1Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isPractice) {
                        if (item.numberOfLaps != null) {
                            Text(
                                text = "${item.numberOfLaps} laps",
                                color = if (isP1) FlagYellow else F1TextTertiary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (isQualifying) {
                        when (currentQualiTab) {
                            QualiSessionTab.Q1 -> {
                                if (rankInt <= 15) {
                                    Text(text = "ADVANCED", color = FlagGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text(text = "OUT IN Q1", color = F1Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            QualiSessionTab.Q2 -> {
                                if (rankInt <= 10) {
                                    Text(text = "ADVANCED TO Q3", color = FlagGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text(text = "OUT IN Q2", color = F1Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            QualiSessionTab.Q3 -> {
                                if (isP1) {
                                    Text(text = "POLE POSITION", color = FlagYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text(text = "TOP 10", color = F1TextSecondary, fontSize = 9.sp)
                                }
                            }
                            QualiSessionTab.GRID -> {
                                if (item.isRetired) {
                                    Text(text = "DNF / RET", color = F1Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else if (!item.gapToLeader.isNullOrBlank()) {
                                    Text(text = item.gapToLeader, color = F1TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    } else {
                        if (item.isRetired) {
                            Text(text = "DNF / RET", color = F1Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else if (item.numberOfLaps != null) {
                            Text(text = "${item.numberOfLaps} laps", color = F1TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // In GRID mode, show all 3 sessions (Q1, Q2, Q3) below the driver row
            if (isQualifying && currentQualiTab == QualiSessionTab.GRID && (item.q1Time != null || item.q2Time != null || item.q3Time != null)) {
                HorizontalDivider(color = CarbonDivider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QualifyingTimeSegment(label = "Q1", time = item.q1Time)
                    QualifyingTimeSegment(label = "Q2", time = item.q2Time)
                    QualifyingTimeSegment(label = "Q3", time = item.q3Time, isShootout = true)
                }
            }
        }
    }
}

@Composable
private fun QualifyingTimeSegment(
    label: String,
    time: String?,
    isShootout: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            color = if (isShootout) FlagYellow else F1TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = time ?: "--",
            color = if (!time.isNullOrBlank()) F1TextPrimary else F1TextTertiary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (!time.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
