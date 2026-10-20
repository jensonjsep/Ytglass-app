package com.glassmp3.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class PlayCtx { NONE, LIBRARY, RESULTS }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val appCtx: Context = app.applicationContext
    private val prefs = appCtx.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val player: ExoPlayer = PlayerHolder.get(appCtx)

    // ---- UI state
    var tab by mutableIntStateOf(0)
    var query by mutableStateOf("")
    var searching by mutableStateOf(false)
    var status by mutableStateOf("Type a song name and press Search.")
    val results = mutableStateListOf<Track>()
    val jobs = mutableStateListOf<DownloadJob>()
    val library = mutableStateListOf<LocalSong>()
    var theme by mutableStateOf(Themes.byName(prefs.getString("theme", "Ocean")))

    // ---- player UI state
    var hasMedia by mutableStateOf(false)
    var nowTitle by mutableStateOf("")
    var nowSub by mutableStateOf("")
    var nowId by mutableStateOf("")
    var isPlaying by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)

    private var playCtx = PlayCtx.NONE
    private var queue: List<Track> = emptyList()
    private var queueIndex = -1
    private var playToken = 0
    private var nextJobId = 1
    private val jobChannel = Channel<DownloadJob>(Channel.UNLIMITED)

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = syncNow()

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && playCtx == PlayCtx.RESULTS) next(auto = true)
            }

            override fun onPlayerError(error: PlaybackException) {
                nowSub = "Playback error"
                status = "Playback error: ${error.errorCodeName}"
            }
        })
        syncNow()
        isPlaying = player.isPlaying

        // position ticker
        viewModelScope.launch {
            while (true) {
                delay(400)
                if (player.mediaItemCount > 0) {
                    position = player.currentPosition.coerceAtLeast(0L)
                    duration = player.duration.let { if (it < 0) 0L else it }
                }
            }
        }
        // download worker (one at a time)
        viewModelScope.launch(Dispatchers.IO) {
            for (job in jobChannel) runJob(job)
        }
        refreshLibrary()
    }

    private fun syncNow() {
        val item = player.currentMediaItem ?: return
        hasMedia = true
        nowId = item.mediaId
        nowTitle = item.mediaMetadata.title?.toString() ?: ""
        nowSub = item.mediaMetadata.artist?.toString() ?: ""
    }

    // ---- theme
    fun applyTheme(t: GlassTheme) {
        theme = t
        prefs.edit().putString("theme", t.name).apply()
    }

    // ---- search
    fun search() {
        val q = query.trim()
        if (q.isEmpty() || searching) return
        searching = true
        status = "Searching YouTube for “$q”…"
        viewModelScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { YouTubeRepo.search(q) }
                results.clear()
                results.addAll(r)
                status = if (r.isEmpty()) "No results." else "${r.size} results — tap ▶ to play or ⬇ MP3 to download."
            } catch (e: Exception) {
                status = "Search failed: ${(e.message ?: e.javaClass.simpleName).take(140)}"
            }
            searching = false
        }
    }

    // ---- online playback
    fun playFromResults(index: Int) {
        queue = results.toList()
        playQueue(index)
    }

    private fun playQueue(index: Int) {
        val t = queue.getOrNull(index) ?: return
        playCtx = PlayCtx.RESULTS
        queueIndex = index
        val token = ++playToken
        player.stop()
        player.clearMediaItems()
        hasMedia = true
        nowTitle = t.title
        nowSub = "Loading stream…"
        nowId = t.url
        position = 0L
        duration = 0L
        status = "Loading stream for “${t.title}”…"
        viewModelScope.launch {
            try {
                val a = withContext(Dispatchers.IO) { YouTubeRepo.bestAudio(t.url, preferM4a = true) }
                if (token != playToken) return@launch
                val item = MediaItem.Builder()
                    .setUri(a.url)
                    .setMediaId(t.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder().setTitle(t.title).setArtist(t.channel).build(),
                    )
                    .build()
                player.setMediaItem(item)
                player.prepare()
                player.play()
                status = "Playing online (not downloaded)."
            } catch (e: Exception) {
                if (token == playToken) {
                    nowSub = "Couldn't load stream"
                    status = "Play failed: ${(e.message ?: e.javaClass.simpleName).take(140)}"
                }
            }
        }
    }

    // ---- library playback
    fun refreshLibrary() {
        viewModelScope.launch {
            val l = withContext(Dispatchers.IO) { Storage.queryLibrary(appCtx) }
            library.clear()
            library.addAll(l)
        }
    }

    fun playLibrary(index: Int) {
        if (library.isEmpty()) return
        playCtx = PlayCtx.LIBRARY
        playToken++
        val items = library.map { s ->
            MediaItem.Builder()
                .setUri(s.uri)
                .setMediaId(s.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(s.title).setArtist("Local file").build(),
                )
                .build()
        }
        player.setMediaItems(items, index.coerceIn(0, items.lastIndex), 0L)
        player.prepare()
        player.play()
        status = "Playing from your library."
    }

    // ---- transport
    fun togglePlay() {
        if (player.isPlaying) player.pause() else if (player.mediaItemCount > 0) player.play()
    }

    fun seekTo(ms: Long) = player.seekTo(ms)

    fun next(auto: Boolean = false) {
        when (playCtx) {
            PlayCtx.LIBRARY -> if (player.hasNextMediaItem()) player.seekToNextMediaItem()
            PlayCtx.RESULTS -> {
                if (queueIndex + 1 < queue.size) playQueue(queueIndex + 1)
                else if (auto) nowSub = "Finished — end of list"
            }
            PlayCtx.NONE -> {}
        }
    }

    fun prev() {
        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }
        when (playCtx) {
            PlayCtx.LIBRARY ->
                if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() else player.seekTo(0)
            PlayCtx.RESULTS ->
                if (queueIndex > 0) playQueue(queueIndex - 1) else player.seekTo(0)
            PlayCtx.NONE -> player.seekTo(0)
        }
    }

    // ---- downloads
    fun download(track: Track) {
        if (jobs.any { it.track.url == track.url && (it.kind == JobKind.QUEUED || it.kind == JobKind.ACTIVE) }) {
            status = "Already in the download queue."
            return
        }
        val job = DownloadJob(nextJobId++, track)
        jobs.add(0, job)
        jobChannel.trySend(job)
        status = "Added “${track.title}” to the download queue."
    }

    fun clearFinished() {
        jobs.removeAll { it.kind == JobKind.DONE || it.kind == JobKind.ERROR }
    }

    private fun runJob(job: DownloadJob) {
        var src: File? = null
        val mp3 = File(appCtx.cacheDir, "dl_${job.id}.mp3")
        try {
            job.set("Looking up audio stream…", 0.03f, JobKind.ACTIVE)
            val a = YouTubeRepo.bestAudio(job.track.url, preferM4a = false)
            val input = File(appCtx.cacheDir, "dl_${job.id}_in.${a.ext}")
            src = input
            job.set("Downloading audio… 0%", 0.05f, JobKind.ACTIVE)
            FileDownloader.download(a.url, input) { f ->
                job.set("Downloading audio… ${(f * 100).toInt()}%", 0.05f + 0.75f * f, JobKind.ACTIVE)
            }
            job.set("Converting to MP3…", 0.85f, JobKind.ACTIVE)
            val artist = job.track.channel.removeSuffix(" - Topic")
            val converted = Mp3Converter.convert(input, mp3, job.track.title, artist)
            val file = if (converted) mp3 else input
            val ext = if (converted) "mp3" else a.ext
            val mime = if (converted) "audio/mpeg" else a.mime
            job.set("Saving to Music/YouTube MP3…", 0.95f, JobKind.ACTIVE)
            Storage.saveToMusic(appCtx, file, Storage.safeName(job.track.title) + "." + ext, mime)
            job.set(
                if (converted) "✓ Saved as MP3" else "✓ Saved as ${ext.uppercase()} (no MP3 encoder in this build)",
                1f,
                JobKind.DONE,
            )
            refreshLibrary()
        } catch (e: Exception) {
            job.set("✕ ${(e.message ?: e.javaClass.simpleName).take(120)}", 0f, JobKind.ERROR)
        } finally {
            src?.delete()
            mp3.delete()
        }
    }
}
