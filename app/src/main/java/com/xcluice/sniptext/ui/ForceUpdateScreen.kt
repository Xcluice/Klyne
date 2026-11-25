package com.xcluice.sniptext.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xcluice.sniptext.BuildConfig
import com.xcluice.sniptext.core.ForceUpdater
import com.xcluice.sniptext.core.ReleaseInfo
import kotlinx.coroutines.launch

@Composable
fun ForceUpdateScreen(release: ReleaseInfo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SnipColors.Bg)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it / 8 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Update Required",
                    color = SnipColors.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version ${release.versionName} is available (installed: ${BuildConfig.VERSION_NAME}).",
                    color = SnipColors.TextSecondary,
                    fontSize = 14.sp
                )

                Surface(
                    color = SnipColors.Surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SnipColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "What's new",
                            color = SnipColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        release.whatsNew.forEach { bullet ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp)
                                        .size(5.dp)
                                        .background(SnipColors.Accent, RoundedCornerShape(50))
                                )
                                Text(
                                    text = bullet,
                                    color = SnipColors.Accent,
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }

                if (downloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = SnipColors.TextPrimary,
                            trackColor = SnipColors.SurfaceElevated
                        )
                        Text(
                            text = "Downloading update… ${(progress * 100).toInt()}%",
                            color = SnipColors.TextSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                }

                errorMsg?.let { msg ->
                    Text(text = msg, color = SnipColors.Accent, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (downloading) return@Button
                        downloading = true
                        errorMsg = null
                        scope.launch {
                            val result = ForceUpdater.downloadAndInstall(
                                context = context,
                                apkUrl = release.apkUrl,
                                onProgress = { progress = it }
                            )
                            downloading = false
                            result.onFailure {
                                errorMsg = "Download failed: ${it.message}"
                            }
                        }
                    },
                    enabled = !downloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SnipColors.TextPrimary,
                        contentColor = SnipColors.Bg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (downloading) "Downloading…" else "Download & Install v${release.versionName}",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(release.releasePageUrl))
                        )
                    },
                    border = BorderStroke(1.dp, SnipColors.Border),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(text = "Open GitHub Release", color = SnipColors.TextSecondary)
                }
            }
        }
    }
}
