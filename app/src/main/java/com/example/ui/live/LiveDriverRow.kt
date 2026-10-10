package com.example.ui.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimingDriverLine
import com.example.ui.components.DriverHeadshotAvatar
import com.example.ui.components.PositionPill
import com.example.ui.components.TeamLiveryBar
import com.example.ui.components.TyreCompoundBadge
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.F1Red
import com.example.ui.theme.F1TextPrimary
import com.example.ui.theme.F1TextSecondary
import com.example.ui.theme.F1TextTertiary
import com.example.ui.theme.FlagYellow

@Composable
fun LiveDriverRow(
    driver: TimingDriverLine,
    useBestLapTime: Boolean = false,
    onClick: () -> Unit = {}
) {
    val currentTyre = driver.getCurrentTyreCompound()

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PositionPill(position = driver.getDisplayPosition())
                Spacer(modifier = Modifier.width(6.dp))
                TeamLiveryBar(teamColour = driver.teamColour)
                Spacer(modifier = Modifier.width(8.dp))
                DriverHeadshotAvatar(
                    headshotUrl = driver.getEffectiveHeadshotUrl(),
                    driverName = driver.name ?: driver.getDisplayTla(),
                    size = 32.dp
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp)
                ) {
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
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = driver.team ?: "",
                            color = F1TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        currentTyre?.let {
                            Spacer(modifier = Modifier.width(6.dp))
                            TyreCompoundBadge(
                                compound = it,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.width(86.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val isP1 =
                        driver.getDisplayPosition() == "1" || driver.getDisplayPosition() == "P1"
                    val effectiveLap = if (useBestLapTime) {
                        driver.bestLapTime ?: driver.lapTime ?: driver.lastLapTime
                    } else {
                        driver.lastLapTime ?: driver.lapTime
                    }
                    val effectiveGap = driver.gapToLeader ?: driver.gap
                    val primaryTime =
                        if (isP1) {
                            if (useBestLapTime) effectiveLap ?: "-" else driver.totalRaceTime ?: effectiveLap ?: "-"
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

                    driver.getStatusText()?.let { statusText ->
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
}
