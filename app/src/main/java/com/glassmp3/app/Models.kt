package com.glassmp3.app

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class Track(
    val title: String,
    val channel: String,
    val durationSec: Long,
    val url: String,
)

data class LocalSong(
    val id: Long,
    val title: String,
    val sizeBytes: Long,
    val uri: Uri,
)

enum class JobKind { QUEUED, ACTIVE, DONE, ERROR }

class DownloadJob(val id: Int, val track: Track) {
    var stage by mutableStateOf("Queued…")
    var progress by mutableFloatStateOf(0f)
    var kind by mutableStateOf(JobKind.QUEUED)

    fun set(stage: String, progress: Float, kind: JobKind) {
        this.stage = stage
        this.progress = progress
        this.kind = kind
    }
}

data class GlassTheme(
    val name: String,
    val light: Color,
    val mid: Color,
    val dark: Color,
    val bg: Color,
)

object Themes {
    val all = listOf(
        GlassTheme("Ocean", Color(0xFF7DC8FF), Color(0xFF3282FF), Color(0xFF0A41C8), Color(0xFF5A96FF)),
        GlassTheme("Violet", Color(0xFFCDAAFF), Color(0xFF915FFF), Color(0xFF5028C8), Color(0xFF966EFF)),
        GlassTheme("Emerald", Color(0xFF8CF0C3), Color(0xFF1EB97D), Color(0xFF056E4B), Color(0xFF3CC896)),
        GlassTheme("Sunset", Color(0xFFFFCD87), Color(0xFFFF8737), Color(0xFFC8460A), Color(0xFFFF9650)),
        GlassTheme("Rose", Color(0xFFFFAFCD), Color(0xFFF04B87), Color(0xFFAA1450), Color(0xFFF06496)),
        GlassTheme("Graphite", Color(0xFFD7DCE6), Color(0xFF7D879B), Color(0xFF3C4252), Color(0xFF96A0B4)),
    )

    fun byName(name: String?): GlassTheme = all.firstOrNull { it.name == name } ?: all.first()
}

fun fmtDur(sec: Long): String {
    if (sec <= 0) return "--:--"
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
