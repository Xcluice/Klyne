package com.xcluice.klyne.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xcluice.klyne.BuildConfig
import com.xcluice.klyne.core.ForceUpdater
import com.xcluice.klyne.core.ReleaseInfo
import kotlinx.coroutines.launch

@Composable
fun ForceUpdateScreen(
    release: ReleaseInfo,
    palette: KlynePalette
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(palette.cardSurface)
                .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(28.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KlyneLogoMark(tint = palette.onSurface, accentTint = palette.onSurfaceMuted, size = 56.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Update Required",
                color = palette.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Klyne ${release.versionTag} is available (current v${BuildConfig.VERSION_NAME})",
                color = palette.onSurfaceMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = release.notes,
                color = palette.onSurface.copy(alpha = 0.88f),
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))

            if (downloading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = palette.accent,
                    trackColor = palette.pillSurface
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Downloading… ${(progress * 100).toInt()}%",
                    color = palette.onSurfaceMuted,
                    fontSize = 12.5.sp
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(palette.accent)
                        .clickable {
                            downloading = true
                            errorMsg = null
                            scope.launch {
                                val ok = ForceUpdater.downloadAndInstall(context, release) { p ->
                                    progress = p
                                }
                                downloading = false
                                if (!ok) errorMsg = "Download failed. Check connection and try again."
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Update Now",
                        color = palette.onAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorMsg!!, color = palette.onSurfaceMuted, fontSize = 12.sp)
            }
        }
    }
}
