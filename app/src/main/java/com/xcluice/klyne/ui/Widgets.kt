package com.xcluice.klyne.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OneUiClockWidget(
    palette: KlynePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var timeStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }
    var batteryPct by remember { mutableIntStateOf(100) }

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            timeStr = timeFmt.format(now)
            dateStr = dateFmt.format(now)
            val bm = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val lvl = bm?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = bm?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            if (lvl >= 0 && scale > 0) batteryPct = (lvl * 100) / scale
            delay(15_000L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = timeStr,
            color = palette.onSurface,
            fontSize = 54.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp,
            style = TextStyle(
                shadow = Shadow(
                    color = palette.homeTextShadow,
                    offset = Offset(0f, 2f),
                    blurRadius = 8f
                )
            )
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = dateStr,
                color = palette.onSurface.copy(alpha = 0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(
                    shadow = Shadow(
                        color = palette.homeTextShadow,
                        offset = Offset(0f, 1.5f),
                        blurRadius = 5f
                    )
                )
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.pillSurface)
                    .border(0.7.dp, palette.dockBorder, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$batteryPct%",
                    color = palette.onSurfaceMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun FinderSearchPill(
    palette: KlynePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(palette.pillSurface)
            .border(0.8.dp, palette.dockBorder, RoundedCornerShape(23.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchIcon(tint = palette.onSurfaceMuted, size = 17.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Search apps or math…",
            color = palette.onSurfaceMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
