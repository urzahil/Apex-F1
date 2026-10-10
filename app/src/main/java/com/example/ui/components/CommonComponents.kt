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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.TyreCompound
import com.example.data.model.TyreStint
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardElevated
import com.example.ui.theme.CarbonDivider
import com.example.ui.theme.F1Red
import com.example.ui.theme.F1TextPrimary
import com.example.ui.theme.F1TextSecondary
import com.example.ui.theme.F1TextTertiary
import com.example.ui.theme.FlagGreen
import com.example.ui.theme.FlagRed
import com.example.ui.theme.FlagYellow
import com.example.ui.theme.TyreHard
import com.example.ui.theme.TyreInter
import com.example.ui.theme.TyreMedium
import com.example.ui.theme.TyreSoft
import com.example.ui.theme.TyreWet

enum class LiveBadgeStatus {
    LIVE,
    NOT_STARTED,
    FINALISED
}

@Composable
fun LivePulsingBadge(
    status: LiveBadgeStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        LiveBadgeStatus.LIVE -> {
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
        }

        LiveBadgeStatus.NOT_STARTED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(FlagYellow.copy(alpha = 0.15f))
                    .border(1.dp, FlagYellow.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(FlagYellow)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "NOT STARTED",
                    color = FlagYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        LiveBadgeStatus.FINALISED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CarbonCardElevated)
                    .border(1.dp, CarbonDivider, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "ENDED",
                    color = F1TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun LivePulsingBadge(
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    LivePulsingBadge(
        status = if (isLive) LiveBadgeStatus.LIVE else LiveBadgeStatus.FINALISED,
        modifier = modifier
    )
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
        message.contains("SAFETY CAR") || message.contains("SCDEPLOYED") || message.contains("VSC") -> Triple(
            FlagYellow,
            Color.Black,
            "SAFETY CAR"
        )

        else -> Triple(FlagGreen, Color.Black, "TRACK CLEAR")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor.copy(alpha = 0.2f))
            .border(1.dp, bgColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = flagDesc,
            tint = bgColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = flagDesc,
            color = bgColor,
            fontSize = 11.sp,
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
    rainfall: String?,
    windSpeed: String?,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherItem(
                modifier = Modifier.weight(1f),
                label = "AIR",
                value = if (!airTemp.isNullOrBlank()) "$airTemp°C" else "--",
                icon = Icons.Default.Thermostat
            )
            WeatherDivider()
            WeatherItem(
                modifier = Modifier.weight(1f),
                label = "TRACK",
                value = if (!trackTemp.isNullOrBlank()) "$trackTemp°C" else "--",
                icon = Icons.Default.DeviceThermostat,
                accent = F1Red,
                iconTint = F1Red
            )
            WeatherDivider()
            WeatherItem(
                modifier = Modifier.weight(1f),
                label = "RAIN",
                value = when (rainfall?.trim()) {
                    "1" -> "YES"
                    "0" -> "NO"
                    else -> "--"
                },
                icon = Icons.Default.WaterDrop,
                accent = if (rainfall?.trim() == "1") Color(0xFF4DA3FF) else F1TextPrimary,
                iconTint = if (rainfall?.trim() == "1") Color(0xFF4DA3FF) else F1TextSecondary
            )
            WeatherDivider()
            WeatherItem(
                modifier = Modifier.weight(1f),
                label = "HUMIDITY",
                value = if (!humidity.isNullOrBlank()) "$humidity%" else "--",
                icon = Icons.Default.Opacity
            )
            WeatherDivider()
            WeatherItem(
                modifier = Modifier.weight(1f),
                label = "WIND",
                value = if (!windSpeed.isNullOrBlank()) "$windSpeed m/s" else "--",
                icon = Icons.Default.Air
            )
        }
    }
}

@Composable
private fun WeatherDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(CarbonDivider)
    )
}

@Composable
private fun WeatherItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color = F1TextPrimary,
    iconTint: Color = F1TextSecondary
) {
    BoxWithConstraints(
        modifier = modifier.padding(horizontal = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        val labelSize = (maxWidth.value / (label.length * 0.62f)).coerceIn(5f, 10f)
        val valueSize = (maxWidth.value / (value.length * 0.62f)).coerceIn(5f, 13f)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                fontSize = labelSize.sp,
                color = F1TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                fontSize = valueSize.sp,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
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
    val type = TyreCompound.fromApiValue(compound)
    if (type == TyreCompound.UNKNOWN) return

    val (color, letter) = when (type) {
        TyreCompound.SOFT -> TyreSoft to "S"
        TyreCompound.MEDIUM -> TyreMedium to "M"
        TyreCompound.HARD -> TyreHard to "H"
        TyreCompound.INTERMEDIATE -> TyreInter to "I"
        TyreCompound.WET -> TyreWet to "W"
        TyreCompound.UNKNOWN -> F1TextSecondary to "?"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(2.dp, color, CircleShape)
            .background(Color.Black)
    ) {
        Text(
            text = letter,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun TyreHistory(
    stints: List<TyreStint>,
    modifier: Modifier = Modifier
) {
    if (stints.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stints.forEachIndexed { index, stint ->
            TyreCompoundBadge(compound = stint.compound)
            if (index < stints.lastIndex) {
                Text("→", color = F1TextTertiary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun DriverHeadshotAvatar(
    headshotUrl: String?,
    driverName: String? = null,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val url = headshotUrl?.takeIf { it.isNotBlank() }

    if (url != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = driverName ?: "Driver Photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(CarbonCardElevated)
                .border(1.dp, CarbonDivider, CircleShape)
        )
    } else {
        val initials =
            driverName?.trim()?.split("\\s+".toRegex())?.mapNotNull { it.firstOrNull()?.toString() }
                ?.take(2)?.joinToString("") ?: ""
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(CarbonCardElevated)
                .border(1.dp, CarbonDivider, CircleShape)
        ) {
            if (initials.isNotBlank()) {
                Text(
                    text = initials.uppercase(),
                    color = F1TextSecondary,
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = driverName ?: "Driver Photo",
                    tint = F1TextSecondary,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}
