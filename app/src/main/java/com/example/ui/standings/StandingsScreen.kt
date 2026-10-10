package com.example.ui.standings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ConstructorStanding
import com.example.data.model.DriverStanding
import com.example.ui.components.DriverHeadshotAvatar
import com.example.ui.components.PositionPill
import com.example.ui.components.TeamLiveryBar
import com.example.ui.theme.CarbonBackground
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonDivider
import com.example.ui.theme.F1Red
import com.example.ui.theme.F1TextPrimary
import com.example.ui.theme.F1TextSecondary
import com.example.ui.theme.F1TextTertiary
import com.example.ui.theme.FlagYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(
    viewModel: StandingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBackground)
    ) {
        // Standings Header without top refresh button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${java.time.Year.now().value} WORLD CHAMPIONSHIP",
                    color = F1Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "STANDINGS",
                    color = F1TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        when (val state = uiState) {
            is StandingsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = F1Red)
                }
            }

            is StandingsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error Loading Standings",
                            color = F1TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = F1TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = F1Red)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is StandingsUiState.Success -> {
                // Category Tabs (Drivers vs Constructors)
                CategoryTabs(
                    selectedCategory = state.category,
                    onSelectCategory = { viewModel.selectCategory(it) }
                )

                // Standings List
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.category == StandingsCategory.DRIVERS) {
                        DriversStandingsList(drivers = state.drivers)
                    } else {
                        ConstructorsStandingsList(constructors = state.constructors)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTabs(
    selectedCategory: StandingsCategory,
    onSelectCategory: (StandingsCategory) -> Unit
) {
    val selectedIndex = if (selectedCategory == StandingsCategory.DRIVERS) 0 else 1
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = CarbonBackground,
        contentColor = F1Red,
        divider = { HorizontalDivider(color = CarbonDivider, thickness = 1.dp) },
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex),
                color = F1Red,
                height = 3.dp
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Tab(
            selected = selectedCategory == StandingsCategory.DRIVERS,
            onClick = { onSelectCategory(StandingsCategory.DRIVERS) },
            text = {
                Text(
                    text = "DRIVERS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (selectedCategory == StandingsCategory.DRIVERS) F1TextPrimary else F1TextSecondary
                )
            },
            modifier = Modifier.testTag("tab_drivers")
        )
        Tab(
            selected = selectedCategory == StandingsCategory.CONSTRUCTORS,
            onClick = { onSelectCategory(StandingsCategory.CONSTRUCTORS) },
            text = {
                Text(
                    text = "CONSTRUCTORS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (selectedCategory == StandingsCategory.CONSTRUCTORS) F1TextPrimary else F1TextSecondary
                )
            },
            modifier = Modifier.testTag("tab_constructors")
        )
    }
}

private fun formatStandingsDriverName(name: String?): String {
    val value = name?.trim().orEmpty()
    if (value.isBlank()) return "Unknown Driver"

    val parts = value.split(Regex("\\s+"))
    return if (parts.size < 2) {
        value
    } else {
        parts.dropLast(1).joinToString(" ") + " " + parts.last().uppercase()
    }
}

@Composable
private fun DriversStandingsList(drivers: List<DriverStanding>) {
    val leaderPoints = drivers.firstOrNull()?.points ?: 0.0
    val maxPoints = leaderPoints.coerceAtLeast(1.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            drivers,
            key = { it.driverNumber ?: it.name ?: it.position?.toString() ?: "driver" }) { driver ->
            DriverStandingCard(driver = driver, maxPoints = maxPoints, leaderPoints = leaderPoints)
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DriverStandingCard(driver: DriverStanding, maxPoints: Double, leaderPoints: Double) {
    val progress = ((driver.points ?: 0.0) / maxPoints).coerceIn(0.0, 1.0).toFloat()
    val teamColor = com.example.ui.theme.getTeamColor(driver.team, driver.teamColour)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("driver_standing_${driver.position}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PositionPill(position = driver.position?.toString() ?: "-")
                Spacer(modifier = Modifier.width(8.dp))
                TeamLiveryBar(teamColour = driver.teamColour, teamName = driver.team)
                Spacer(modifier = Modifier.width(10.dp))
                DriverHeadshotAvatar(
                    headshotUrl = driver.getEffectiveHeadshotUrl(),
                    driverName = driver.name
                )
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!driver.driverNumber.isNullOrBlank()) {
                            Text(
                                text = "#${driver.driverNumber}",
                                color = F1Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = formatStandingsDriverName(driver.name),
                            color = F1TextPrimary,
                            fontSize = 12.sp,
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
                            text = driver.team ?: "",
                            color = F1TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!driver.nationality.isNullOrBlank()) {
                            Text(
                                text = " • ${driver.nationality}",
                                color = F1TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Points & Wins
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = driver.displayPoints(),
                            color = F1TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        if ((driver.points ?: 0.0) < leaderPoints) {
                            val gap = leaderPoints - (driver.points ?: 0.0)
                            val formattedGap = if (gap % 1.0 == 0.0) gap.toInt().toString() else gap.toString()
                            Text(
                                text = " (-$formattedGap)",
                                color = F1TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if ((driver.wins ?: 0) > 0) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Wins",
                                tint = FlagYellow,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${driver.wins} Wins",
                                color = FlagYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if ((driver.podiums ?: 0) > 0) {
                            Text(
                                text = "${driver.podiums} Podiums",
                                color = F1TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CarbonDivider)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(teamColor)
                )
            }
        }
    }
}

@Composable
private fun ConstructorsStandingsList(constructors: List<ConstructorStanding>) {
    val leaderPoints = constructors.firstOrNull()?.points ?: 0.0
    val maxPoints = leaderPoints.coerceAtLeast(1.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            constructors,
            key = { it.team ?: it.position?.toString() ?: "constructor" }) { constructor ->
            ConstructorStandingCard(constructor = constructor, maxPoints = maxPoints, leaderPoints = leaderPoints)
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConstructorStandingCard(constructor: ConstructorStanding, maxPoints: Double, leaderPoints: Double) {
    val progress = ((constructor.points ?: 0.0) / maxPoints).coerceIn(0.0, 1.0).toFloat()
    val teamColor = com.example.ui.theme.getTeamColor(constructor.team, constructor.teamColour)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("constructor_standing_${constructor.position}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PositionPill(position = constructor.position?.toString() ?: "-")
                Spacer(modifier = Modifier.width(8.dp))
                TeamLiveryBar(teamColour = constructor.teamColour, teamName = constructor.team)
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = constructor.team ?: "Unknown Team",
                        color = F1TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if ((constructor.wins ?: 0) > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Victories",
                                tint = FlagYellow,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${constructor.wins} Wins",
                                color = FlagYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Points and gap to the championship leader
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = constructor.displayPoints(),
                        color = F1TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    if ((constructor.points ?: 0.0) < leaderPoints) {
                        val gap = leaderPoints - (constructor.points ?: 0.0)
                        val formattedGap = if (gap % 1.0 == 0.0) gap.toInt().toString() else gap.toString()
                        Text(
                            text = " (-$formattedGap)",
                            color = F1TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CarbonDivider)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(teamColor)
                )
            }
        }
    }
}
