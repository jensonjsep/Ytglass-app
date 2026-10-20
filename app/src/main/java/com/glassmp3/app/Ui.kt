package com.glassmp3.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------ glass building blocks

fun Modifier.glass(shape: Shape = RoundedCornerShape(20.dp), strength: Float = 0.10f): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = strength + 0.08f), Color.White.copy(alpha = strength)),
        ),
    )
    .border(
        1.dp,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.10f))),
        shape,
    )

@Composable
fun GlassButton(
    text: String,
    theme: GlassTheme,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    fontSize: TextUnit = 15.sp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val a = if (enabled) 1f else 0.45f

    val fill: Brush = if (primary) {
        val md = Color(
            red = (theme.mid.red * 0.45f + theme.dark.red * 0.55f),
            green = (theme.mid.green * 0.45f + theme.dark.green * 0.55f),
            blue = (theme.mid.blue * 0.45f + theme.dark.blue * 0.55f),
        )
        Brush.verticalGradient(
            0f to theme.light.copy(alpha = 0.92f * a),
            0.5f to theme.mid.copy(alpha = 0.88f * a),
            0.51f to md.copy(alpha = 0.90f * a),
            1f to theme.dark.copy(alpha = 0.95f * a),
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.24f * a), Color.White.copy(alpha = 0.08f * a)),
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val s = if (pressed) 0.97f else 1f
                scaleX = s
                scaleY = s
            }
            .shadow(
                elevation = if (primary && enabled) 14.dp else 0.dp,
                shape = shape,
                ambientColor = theme.mid,
                spotColor = theme.mid,
            )
            .clip(shape)
            .background(fill)
            .drawBehind {
                // glossy top half
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = (if (primary) 0.40f else 0.22f) * a),
                            Color.White.copy(alpha = 0.03f * a),
                        ),
                        endY = size.height / 2f,
                    ),
                    size = Size(size.width, size.height / 2f),
                )
                if (pressed) drawRect(Color.Black.copy(alpha = 0.18f))
            }
            .border(
                1.2.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.9f * a), Color.White.copy(alpha = 0.3f * a)),
                ),
                shape,
            )
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = a),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
fun GlassProgress(progress: Float, theme: GlassTheme, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(8.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(theme.light, theme.mid))),
        )
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val kb = LocalSoftwareKeyboardController.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            kb?.hide()
            onSearch()
        }),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .glass(RoundedCornerShape(16.dp), 0.12f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text("Song name or artist…", color = Color.White.copy(alpha = 0.45f), fontSize = 16.sp)
                }
                inner()
            }
        },
    )
}

// ------------------------------------------------------------ app shell

@Composable
fun GlassApp(vm: MainViewModel) {
    val t = vm.theme
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(Color(0xFF0B1226), Color(0xFF101A38), Color(0xFF070B18))),
            ),
    ) {
        // soft colour glows behind the glass
        Box(
            Modifier
                .size(340.dp)
                .offset((-90).dp, (-70).dp)
                .background(Brush.radialGradient(listOf(t.bg.copy(alpha = 0.55f), Color.Transparent)), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(400.dp)
                .offset(110.dp, 90.dp)
                .background(Brush.radialGradient(listOf(t.mid.copy(alpha = 0.45f), Color.Transparent)), CircleShape),
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Header(vm)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (vm.tab) {
                    0 -> SearchTab(vm)
                    1 -> ActivityTab(vm)
                    else -> LibraryTab(vm)
                }
            }
            if (vm.hasMedia) NowPlayingCard(vm)
            NavBar(vm)
        }
    }
}

@Composable
fun Header(vm: MainViewModel) {
    var showThemes by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("YouTube → MP3", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Search, play online, or download",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            }
            GlassButton(
                text = "Theme",
                theme = vm.theme,
                primary = false,
                fontSize = 13.sp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) { showThemes = !showThemes }
        }
        if (showThemes) {
            Row(
                Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Themes.all.forEach { th ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(th.light, th.mid, th.dark)))
                            .border(
                                if (th.name == vm.theme.name) 3.dp else 1.dp,
                                Color.White.copy(alpha = if (th.name == vm.theme.name) 1f else 0.4f),
                                CircleShape,
                            )
                            .clickable { vm.setTheme(th) },
                    )
                }
            }
        }
    }
}

@Composable
fun NavBar(vm: MainViewModel) {
    val active = vm.jobs.count { it.kind == JobKind.QUEUED || it.kind == JobKind.ACTIVE }
    val labels = listOf("Search", if (active > 0) "Activity ($active)" else "Activity", "Library")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .glass(RoundedCornerShape(22.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEachIndexed { i, label ->
            GlassButton(
                text = label,
                theme = vm.theme,
                modifier = Modifier.weight(1f),
                primary = vm.tab == i,
                fontSize = 14.sp,
                contentPadding = PaddingValues(vertical = 11.dp),
            ) { vm.tab = i }
        }
    }
}

// ------------------------------------------------------------ search tab

@Composable
fun SearchTab(vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SearchField(vm.query, { vm.query = it }, { vm.search() }, Modifier.weight(1f))
            GlassButton(
                text = "Search",
                theme = vm.theme,
                primary = false,
                enabled = !vm.searching,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) { vm.search() }
        }
        Spacer(Modifier.height(8.dp))
        Text(vm.status, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 2)
        Spacer(Modifier.height(6.dp))
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            itemsIndexed(vm.results) { i, tr ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(18.dp))
                        .clickable { vm.playFromResults(i) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            tr.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${tr.channel} · ${fmtDur(tr.durationSec)}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    GlassButton(
                        "▶", vm.theme, primary = false, fontSize = 14.sp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                    ) { vm.playFromResults(i) }
                    Spacer(Modifier.width(6.dp))
                    GlassButton(
                        "⬇ MP3", vm.theme, primary = true, fontSize = 13.sp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                    ) { vm.download(tr) }
                }
            }
        }
    }
}

// ------------------------------------------------------------ activity tab

@Composable
fun ActivityTab(vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Downloads",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                "Clear done", vm.theme, primary = false, fontSize = 12.sp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) { vm.clearFinished() }
        }
        Spacer(Modifier.height(8.dp))
        if (vm.jobs.isEmpty()) {
            Text(
                "No downloads yet.\nProgress and each processing step will show up here.",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(vm.jobs) { _, job ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(16.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        job.track.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        job.stage,
                        color = when (job.kind) {
                            JobKind.DONE -> Color(0xFF8FF0B6)
                            JobKind.ERROR -> Color(0xFFFF9D9D)
                            else -> Color.White.copy(alpha = 0.65f)
                        },
                        fontSize = 12.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    GlassProgress(job.progress, vm.theme, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ------------------------------------------------------------ library tab

@Composable
fun LibraryTab(vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (vm.library.isEmpty()) "No downloaded songs yet" else "${vm.library.size} songs",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                "↻ Refresh", vm.theme, primary = false, fontSize = 12.sp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) { vm.refreshLibrary() }
        }
        Text(
            "Saved in Music/YouTube MP3 · plays one after another",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(vm.library) { i, song ->
                val playing = vm.nowId == song.id.toString()
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .glass(shape, if (playing) 0.20f else 0.10f)
                        .then(if (playing) Modifier.border(1.5.dp, vm.theme.light, shape) else Modifier)
                        .clickable { vm.playLibrary(i) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            (if (playing) "♪  " else "") + song.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "%.1f MB".format(song.sizeBytes / 1048576f),
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ now playing

@Composable
fun NowPlayingCard(vm: MainViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .glass(RoundedCornerShape(22.dp), 0.12f)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            vm.nowTitle.ifEmpty { "…" },
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            vm.nowSub,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        var dragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableFloatStateOf(0f) }
        val dur = vm.duration.coerceAtLeast(1L)
        val shown = if (dragging) dragValue else (vm.position.toFloat() / dur).coerceIn(0f, 1f)
        Slider(
            value = shown,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                vm.seekTo((dragValue * dur).toLong())
                dragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = vm.theme.light,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${fmtDur(vm.position / 1000).replace("--:--", "0:00")} / ${fmtDur(vm.duration / 1000).replace("--:--", "0:00")}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassButton(
                    "⏮", vm.theme, primary = false, fontSize = 16.sp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) { vm.prev() }
                GlassButton(
                    if (vm.isPlaying) "⏸" else "▶", vm.theme, primary = true, fontSize = 18.sp,
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 9.dp),
                ) { vm.togglePlay() }
                GlassButton(
                    "⏭", vm.theme, primary = false, fontSize = 16.sp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) { vm.next() }
            }
        }
    }
}
