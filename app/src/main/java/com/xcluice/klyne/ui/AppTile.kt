package com.xcluice.klyne.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xcluice.klyne.core.AppEntry

private val MonochromeFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SquircleAppTile(
    app: AppEntry,
    iconSize: Dp,
    showLabel: Boolean,
    monochromeIcons: Boolean,
    labelColor: Color,
    shadowColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(OneUiSquircleShape)
                .background(Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (monochromeIcons) MonochromeFilter else null
                )
            } else {
                Text(
                    text = app.label.take(1).uppercase(),
                    color = labelColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = app.label,
                color = labelColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = shadowColor,
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier.width(iconSize + 18.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SquircleFolderTile(
    name: String,
    previewApps: List<AppEntry>,
    iconSize: Dp,
    showLabel: Boolean,
    monochromeIcons: Boolean,
    palette: KlynePalette,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "folderScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(OneUiSquircleShape)
                .background(palette.dockSurface)
                .border(0.8.dp, palette.dockBorder, OneUiSquircleShape)
                .padding(6.dp)
        ) {
            val mini = (iconSize - 18.dp) / 2f
            val items = previewApps.take(4)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (row in 0..1) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (col in 0..1) {
                            val idx = row * 2 + col
                            val entry = items.getOrNull(idx)
                            Box(
                                modifier = Modifier
                                    .size(mini)
                                    .clip(OneUiSquircleShape)
                                    .background(if (entry == null) Color.Transparent else Color(0x1AFFFFFF))
                            ) {
                                if (entry?.icon != null) {
                                    Image(
                                        bitmap = entry.icon,
                                        contentDescription = entry.label,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        colorFilter = if (monochromeIcons) MonochromeFilter else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = name,
                color = palette.onSurface,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = palette.homeTextShadow,
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier.width(iconSize + 18.dp)
            )
        }
    }
}
