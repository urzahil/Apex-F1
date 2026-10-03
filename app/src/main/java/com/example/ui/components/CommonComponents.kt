package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardElevated
import com.example.ui.theme.CarbonDivider
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.F1Red
import com.example.ui.theme.F1TextPrimary
import com.example.ui.theme.F1TextSecondary
import com.example.ui.theme.FlagGreen
import com.example.ui.theme.FlagRed
import com.example.ui.theme.FlagYellow
import com.example.ui.theme.TyreHard
import com.example.ui.theme.TyreInter
import com.example.ui.theme.TyreMedium
import com.example.ui.theme.TyreSoft
import com.example.ui.theme.TyreWet
import com.example.ui.theme.parseHexColor

@Composable
fun LivePulsingBadge(
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLive) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(F1Red)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .alpha(alpha)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(CarbonCardElevated)
                .border(1.dp, CarbonDivider, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "FINALISED",
                color = F1TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun TrackStatusIndicator(
    statusMessage: String?,
    modifier: Modifier = Modifier
) {
    val message = statusMessage?.uppercase() ?: "ALL CLEAR"
    val (bgColor, textColor, flagDesc) = when {
        message.contains("YELLOW") -> Triple(FlagYellow, Color.Black, "YELLOW FLAG")
        message.contains("RED") -> Triple(FlagRed, Color.White, "RED FLAG")
        message.contains("SAFETY CAR") || message.contains("VSC") -> Triple(FlagYellow, Color.Black, "SAFETY CAR")
        else -> Triple(FlagGreen, Color.Black, "TRACK CLEAR")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = 0.2f))
            .border(1.dp, bgColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = flagDesc,
            tint = bgColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = flagDesc,
            color = bgColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun WeatherCardView(
    airTemp: String?,
    trackTemp: String?,
    humidity: String?,
    windSpeed: String?,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherItem(
                icon = Icons.Default.Thermostat,
                label = "AIR",
                value = if (!airTemp.isNullOrBlank()) "$airTemp°C" else "--"
            )
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(CarbonDivider))
            WeatherItem(
                icon = Icons.Default.Thermostat,
                label = "TRACK",
                value = if (!trackTemp.isNullOrBlank()) "$trackTemp°C" else "--",
                accent = F1Red
            )
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(CarbonDivider))
            WeatherItem(
                icon = Icons.Default.WaterDrop,
                label = "HUMIDITY",
                value = if (!humidity.isNullOrBlank()) "$humidity%" else "--"
            )
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(CarbonDivider))
            WeatherItem(
                icon = Icons.Default.Air,
                label = "WIND",
                value = if (!windSpeed.isNullOrBlank()) "$windSpeed m/s" else "--"
            )
        }
    }
}

@Composable
private fun WeatherItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color = F1TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = F1TextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = F1TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun PositionPill(
    position: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (position) {
        "1" -> Pair(Color(0xFFFFD700), Color.Black)
        "2" -> Pair(Color(0xFFC0C0C0), Color.Black)
        "3" -> Pair(Color(0xFFCD7F32), Color.White)
        else -> Pair(CarbonCardElevated, F1TextPrimary)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
    ) {
        Text(
            text = position,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun TeamLiveryBar(
    teamColour: String? = null,
    teamName: String? = null,
    modifier: Modifier = Modifier
) {
    val color = com.example.ui.theme.getTeamColor(teamName, teamColour)
    Box(
        modifier = modifier
            .width(4.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

@Composable
fun TyreCompoundBadge(
    compound: String?,
    modifier: Modifier = Modifier
) {
    val upper = compound?.uppercase() ?: return
    val (color, letter) = when {
        upper.contains("SOFT") -> Pair(TyreSoft, "S")
        upper.contains("MED") -> Pair(TyreMedium, "M")
        upper.contains("HARD") -> Pair(TyreHard, "H")
        upper.contains("INTER") -> Pair(TyreInter, "I")
        upper.contains("WET") -> Pair(TyreWet, "W")
        else -> Pair(F1TextSecondary, upper.take(1))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(2.dp, color, CircleShape)
            .background(Color.Black)
    ) {
        Text(
            text = letter,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
