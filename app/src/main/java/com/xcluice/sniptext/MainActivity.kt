package com.xcluice.sniptext

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.xcluice.sniptext.core.ForceUpdater
import com.xcluice.sniptext.core.GrabHistoryItem
import com.xcluice.sniptext.core.OcrProcessor
import com.xcluice.sniptext.core.ReleaseInfo
import com.xcluice.sniptext.core.SettingsStore
import com.xcluice.sniptext.core.SnipAccessibilityService
import com.xcluice.sniptext.core.SnipSettings
import com.xcluice.sniptext.ui.ForceUpdateScreen
import com.xcluice.sniptext.ui.SnipColors
import com.xcluice.sniptext.ui.SnipTextTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnipTextTheme {
                var pendingUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }

                LaunchedEffect(Unit) {
                    pendingUpdate = ForceUpdater.checkForUpdate()
                }

                val update = pendingUpdate
                if (update != null) {
                    ForceUpdateScreen(release = update)
                } else {
                    SnipHomeScreen()
                }
            }
        }
    }
}

@Composable
private fun SnipHomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember { SettingsStore(context) }

    var settings by remember { mutableStateOf(store.loadSettings()) }
    var history by remember { mutableStateOf(store.loadHistory()) }
    var serviceEnabled by remember { mutableStateOf(isAccessibilityEnabled(context)) }
    var ignoringBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceEnabled = isAccessibilityEnabled(context)
                ignoringBattery = isIgnoringBatteryOptimizations(context)
                history = store.loadHistory()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun updateSettings(newSettings: SnipSettings) {
        settings = newSettings
        store.saveSettings(newSettings)
        SnipAccessibilityService.instance?.reloadSettings()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SnipColors.Bg),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeaderSection(serviceEnabled = serviceEnabled)
        }

        item {
            StatusHeroCard(
                serviceEnabled = serviceEnabled,
                ignoringBattery = ignoringBattery,
                shakeEnabled = settings.shakeEnabled,
                onOpenAccessibility = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onTestSnip = {
                    val svc = SnipAccessibilityService.instance
                    if (svc != null) {
                        svc.triggerCapture()
                    } else {
                        Toast.makeText(
                            context,
                            "Enable SnipText Trigger in Accessibility first",
                            Toast.LENGTH_SHORT
                        ).show()
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
                onRequestBatteryOpt = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }
            )
        }

        item {
            HowItWorksCard()
        }

        item {
            SettingsCard(
                settings = settings,
                onChange = ::updateSettings
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Grabs",
                    color = SnipColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = {
                        store.clearHistory()
                        history = emptyList()
                    }) {
                        Text("Clear all", color = SnipColors.TextSecondary, fontSize = 12.5.sp)
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Surface(
                    color = SnipColors.Surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SnipColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No grabs yet. Shake your phone twice anywhere to select text on screen.",
                        color = SnipColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(history, key = { it.id }) { item ->
                HistoryCard(
                    item = item,
                    onCopy = {
                        OcrProcessor.copyToClipboard(context, item.text)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        store.deleteHistory(item.id)
                        history = store.loadHistory()
                    }
                )
            }
        }

        item {
            Text(
                text = "SnipText v${BuildConfig.VERSION_NAME} • Offline OCR",
                color = SnipColors.AccentMuted,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun HeaderSection(serviceEnabled: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SnipText",
                color = SnipColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Double-Shake screen text grabber",
                color = SnipColors.TextSecondary,
                fontSize = 13.sp
            )
        }

        Surface(
            color = SnipColors.Surface,
            shape = RoundedCornerShape(99.dp),
            border = BorderStroke(1.dp, SnipColors.Border)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(12.dp)) {
                    if (serviceEnabled) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(pulseScale)
                                .alpha(pulseAlpha)
                                .background(SnipColors.ActiveDot, CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (serviceEnabled) SnipColors.ActiveDot else SnipColors.AccentMuted,
                                CircleShape
                            )
                    )
                }
                Text(
                    text = if (serviceEnabled) "Armed" else "Off",
                    color = if (serviceEnabled) SnipColors.TextPrimary else SnipColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StatusHeroCard(
    serviceEnabled: Boolean,
    ignoringBattery: Boolean,
    shakeEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
    onTestSnip: () -> Unit,
    onRequestBatteryOpt: () -> Unit
) {
    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { 16 }) {
        Surface(
            color = SnipColors.Surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, SnipColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (serviceEnabled) "Trigger Ready" else "Enable Screen Trigger",
                    color = SnipColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (serviceEnabled) {
                        if (shakeEnabled) {
                            "Shake your phone twice anywhere to freeze the screen and select text."
                        } else {
                            "Tap Volume Up + Volume Down together anywhere to freeze the screen and select text."
                        }
                    } else {
                        "Turn on SnipText in Accessibility Settings so the app can capture your screen on Double-Shake."
                    },
                    color = SnipColors.TextSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = if (serviceEnabled) onTestSnip else onOpenAccessibility,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SnipColors.TextPrimary,
                            contentColor = SnipColors.Bg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = if (serviceEnabled) "Test Snip Now" else "Enable Service",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                    }

                    if (!ignoringBattery) {
                        OutlinedButton(
                            onClick = onRequestBatteryOpt,
                            border = BorderStroke(1.dp, SnipColors.Border),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(
                                text = "Keep Alive",
                                color = SnipColors.Accent,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Surface(
        color = SnipColors.Surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SnipColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "3-Step Flow",
                color = SnipColors.TextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            StepRow("1", "Shake your phone twice (or tap Vol Up + Down) to freeze any screen.")
            StepRow("2", "Drag a box over the text you want to capture.")
            StepRow("3", "Edit, share, or copy instantly — 100% offline.")
        }
    }
}

@Composable
private fun StepRow(num: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(SnipColors.SurfaceElevated, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = num, color = SnipColors.TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = text, color = SnipColors.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun SettingsCard(
    settings: SnipSettings,
    onChange: (SnipSettings) -> Unit
) {
    Surface(
        color = SnipColors.Surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SnipColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Triggers & Preferences",
                color = SnipColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            ToggleRow(
                title = "Double-Shake to Snip",
                subtitle = "Shake twice while screen is on (zero pocket drain)",
                checked = settings.shakeEnabled,
                onCheckedChange = { onChange(settings.copy(shakeEnabled = it)) }
            )

            if (settings.shakeEnabled) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Shake Sensitivity",
                            color = SnipColors.TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${settings.shakeSensitivity} / 10",
                            color = SnipColors.Accent,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = settings.shakeSensitivity.toFloat(),
                        onValueChange = {
                            onChange(settings.copy(shakeSensitivity = it.roundToInt().coerceIn(1, 10)))
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = SnipColors.TextPrimary,
                            activeTrackColor = SnipColors.TextPrimary,
                            inactiveTrackColor = SnipColors.SurfaceElevated
                        )
                    )
                }
            }

            ToggleRow(
                title = "Vol Up + Vol Down Combo",
                subtitle = "Keep hardware volume chord enabled as backup",
                checked = settings.volumeComboEnabled,
                onCheckedChange = { onChange(settings.copy(volumeComboEnabled = it)) }
            )
            ToggleRow(
                title = "Instant Grab on Release",
                subtitle = "Run OCR immediately when you lift your finger",
                checked = settings.instantGrab,
                onCheckedChange = { onChange(settings.copy(instantGrab = it)) }
            )
            ToggleRow(
                title = "Auto-Copy & Dismiss",
                subtitle = "Copy extracted text directly to clipboard without preview",
                checked = settings.autoCopy,
                onCheckedChange = { onChange(settings.copy(autoCopy = it)) }
            )
            ToggleRow(
                title = "Join Line Breaks",
                subtitle = "Merge wrapped lines into clean paragraphs",
                checked = settings.joinLineBreaks,
                onCheckedChange = { onChange(settings.copy(joinLineBreaks = it)) }
            )
            ToggleRow(
                title = "Haptic Pulse",
                subtitle = "Subtle vibration when triggered and copied",
                checked = settings.hapticEnabled,
                onCheckedChange = { onChange(settings.copy(hapticEnabled = it)) }
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = SnipColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = SnipColors.TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SnipColors.Bg,
                checkedTrackColor = SnipColors.TextPrimary,
                uncheckedThumbColor = SnipColors.TextSecondary,
                uncheckedTrackColor = SnipColors.SurfaceElevated,
                uncheckedBorderColor = SnipColors.Border
            )
        )
    }
}

@Composable
private fun HistoryCard(
    item: GrabHistoryItem,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val timeLabel = remember(item.timestamp) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.timestamp))
    }
    Surface(
        color = SnipColors.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SnipColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.text,
                color = SnipColors.TextPrimary,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = timeLabel, color = SnipColors.AccentMuted, fontSize = 11.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Copy",
                        color = SnipColors.TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onCopy() }
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Delete",
                        color = SnipColors.TextSecondary,
                        fontSize = 12.5.sp,
                        modifier = Modifier.clickable { onDelete() }
                    )
                }
            }
        }
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(context, SnipAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabled)
    while (splitter.hasNext()) {
        if (splitter.next().equals(expected, ignoreCase = true)) return true
    }
    return false
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
