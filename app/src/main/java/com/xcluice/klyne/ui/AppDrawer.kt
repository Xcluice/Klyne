package com.xcluice.klyne.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xcluice.klyne.core.AppEntry
import com.xcluice.klyne.core.DrawerStyle
import com.xcluice.klyne.core.KlyneSettings
import kotlinx.coroutines.launch

@Composable
fun AppDrawerOverlay(
    apps: List<AppEntry>,
    frequentApps: List<AppEntry>,
    settings: KlyneSettings,
    palette: KlynePalette,
    autoFocusSearch: Boolean,
    onLaunchApp: (AppEntry) -> Unit,
    onLongClickApp: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var activeLetter by remember { mutableStateOf<Char?>(null) }

    LaunchedEffect(autoFocusSearch) {
        if (autoFocusSearch) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val visibleApps = remember(apps, settings.hiddenPackages, query) {
        val base = apps.filter { it.packageName !in settings.hiddenPackages }
        if (query.isBlank()) base
        else {
            val q = query.trim().lowercase()
            base.filter {
                it.label.lowercase().contains(q) || fuzzyMatch(it.label.lowercase(), q)
            }.sortedByDescending { it.label.lowercase().startsWith(q) }
        }
    }

    val mathResult = remember(query) { evaluateSimpleMath(query) }

    val letters = remember(visibleApps) {
        visibleApps.map { c ->
            val ch = c.label.firstOrNull()?.uppercaseChar() ?: '#'
            if (ch in 'A'..'Z') ch else '#'
        }.distinct()
    }

    val statusPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .imePadding()
            .padding(top = statusPad + 8.dp, bottom = navPad + 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pull-down handle bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 26f) onClose()
                        }
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(palette.onSurfaceMuted.copy(alpha = 0.4f))
                )
            }

            // Inline quick math result pill
            if (mathResult != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.cardSurface)
                        .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = query.trim(),
                        color = palette.onSurfaceMuted,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "= $mathResult",
                        color = palette.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Main app list (Vertical or Paged)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (settings.drawerStyle == DrawerStyle.PAGED && query.isBlank()) {
                    val perPage = settings.gridColumns * settings.gridRows
                    val pages = remember(visibleApps, perPage) {
                        visibleApps.chunked(perPage.coerceAtLeast(12))
                    }
                    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
                    Column(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f)
                        ) { pageIdx ->
                            val pageApps = pages.getOrElse(pageIdx) { emptyList() }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(settings.gridColumns),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(pageApps, key = { it.key }) { app ->
                                    SquircleAppTile(
                                        app = app,
                                        iconSize = settings.iconSizeDp.dp,
                                        showLabel = settings.showLabels,
                                        monochromeIcons = settings.monochromeIcons,
                                        labelColor = palette.onSurface,
                                        shadowColor = palette.homeTextShadow,
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { onLongClickApp(app) }
                                    )
                                }
                            }
                        }
                        if (pages.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(pages.size) { i ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .size(if (pagerState.currentPage == i) 7.dp else 5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (pagerState.currentPage == i) palette.onSurface
                                                else palette.onSurfaceMuted.copy(alpha = 0.35f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(settings.gridColumns),
                        contentPadding = PaddingValues(start = 14.dp, end = 30.dp, top = 6.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (query.isBlank() && frequentApps.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = "FREQUENT",
                                    color = palette.onSurfaceMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
                                )
                            }
                            items(frequentApps.take(settings.gridColumns), key = { "freq_${it.key}" }) { app ->
                                SquircleAppTile(
                                    app = app,
                                    iconSize = settings.iconSizeDp.dp,
                                    showLabel = settings.showLabels,
                                    monochromeIcons = settings.monochromeIcons,
                                    labelColor = palette.onSurface,
                                    shadowColor = palette.homeTextShadow,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onLongClickApp(app) }
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = "ALL APPS",
                                    color = palette.onSurfaceMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 2.dp)
                                )
                            }
                        }

                        items(visibleApps, key = { it.key }) { app ->
                            SquircleAppTile(
                                app = app,
                                iconSize = settings.iconSizeDp.dp,
                                showLabel = settings.showLabels,
                                monochromeIcons = settings.monochromeIcons,
                                labelColor = palette.onSurface,
                                shadowColor = palette.homeTextShadow,
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongClickApp(app) }
                            )
                        }
                    }

                    // Kinetic A-Z side scrubber
                    if (query.isBlank() && letters.size > 4) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(0.76f)
                                .width(24.dp)
                                .pointerInput(letters, visibleApps) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { activeLetter = null },
                                        onDragCancel = { activeLetter = null }
                                    ) { change, _ ->
                                        change.consume()
                                        val frac = (change.position.y / size.height).coerceIn(0f, 0.999f)
                                        val idx = (frac * letters.size).toInt().coerceIn(0, letters.lastIndex)
                                        val letter = letters[idx]
                                        if (activeLetter != letter) {
                                            activeLetter = letter
                                            val targetIdx = visibleApps.indexOfFirst {
                                                val ch = it.label.firstOrNull()?.uppercaseChar() ?: '#'
                                                (if (ch in 'A'..'Z') ch else '#') == letter
                                            }
                                            if (targetIdx >= 0) {
                                                val headerOffset = if (frequentApps.isNotEmpty()) settings.gridColumns + 2 else 0
                                                scope.launch { gridState.scrollToItem(targetIdx + headerOffset) }
                                            }
                                        }
                                    }
                                },
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            letters.forEach { ch ->
                                Text(
                                    text = ch.toString(),
                                    color = if (activeLetter == ch) palette.onSurface else palette.onSurfaceMuted.copy(alpha = 0.7f),
                                    fontSize = if (activeLetter == ch) 12.sp else 9.5.sp,
                                    fontWeight = if (activeLetter == ch) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Floating letter bubble
                    AnimatedVisibility(
                        visible = activeLetter != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 42.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(OneUiSquircleShape)
                                .background(palette.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeLetter?.toString() ?: "",
                                color = palette.onAccent,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // One UI 9 Bottom-Anchored Finder Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(palette.pillSurface)
                    .border(0.8.dp, palette.dockBorder, RoundedCornerShape(25.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchIcon(tint = palette.onSurfaceMuted, size = 18.dp)
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search",
                            color = palette.onSurfaceMuted,
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = palette.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(palette.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { query = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        CloseIcon(tint = palette.onSurfaceMuted, size = 15.dp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsIcon(tint = palette.onSurfaceMuted, size = 19.dp)
                    }
                }
            }
        }
    }
}

private fun fuzzyMatch(text: String, query: String): Boolean {
    var qi = 0
    for (c in text) {
        if (qi < query.length && c == query[qi]) qi++
    }
    return qi == query.length
}

private fun evaluateSimpleMath(input: String): String? {
    val clean = input.replace(" ", "").replace("x", "*").replace("×", "*").replace("÷", "/")
    if (clean.length < 3 || !clean.any { it in "+-*/" }) return null
    if (!clean.all { it.isDigit() || it in "+-*/." }) return null
    return runCatching {
        val opIdx = clean.indexOfFirst { it in "+-*/" && clean.indexOf(it) > 0 }
        if (opIdx <= 0 || opIdx >= clean.lastIndex) return null
        val a = clean.substring(0, opIdx).toDoubleOrNull() ?: return null
        val b = clean.substring(opIdx + 1).toDoubleOrNull() ?: return null
        val res = when (clean[opIdx]) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> if (b == 0.0) return null else a / b
            else -> return null
        }
        if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(res).trimEnd('0').trimEnd('.')
    }.getOrNull()
}
