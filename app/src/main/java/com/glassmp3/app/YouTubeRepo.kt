package com.glassmp3.app

import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class AudioChoice(val url: String, val ext: String, val mime: String, val kbps: Int)

object YouTubeRepo {
    private var ready = false

    @Synchronized
    private fun init() {
        if (!ready) {
            NewPipe.init(YtDownloader())
            ready = true
        }
    }

    /** Blocking - call from a background thread. */
    fun search(query: String): List<Track> {
        init()
        val yt = ServiceList.YouTube
        val handler = yt.searchQHFactory.fromQuery(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
            "",
        )
        val info = SearchInfo.getInfo(yt, handler)
        return info.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { Track(it.name ?: "(untitled)", it.uploaderName ?: "", it.duration, it.url) }
    }

    /** Blocking - call from a background thread. */
    fun bestAudio(videoUrl: String, preferM4a: Boolean): AudioChoice {
        init()
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        val streams = info.audioStreams.filter { it.isUrl && !it.content.isNullOrBlank() }
        if (streams.isEmpty()) {
            throw IllegalStateException("No audio stream found (YouTube may have changed - update the app)")
        }
        val pool = if (preferM4a) {
            streams.filter { it.format == MediaFormat.M4A }.ifEmpty { streams }
        } else {
            streams
        }
        val pick = pool.maxByOrNull { it.averageBitrate } ?: pool.first()
        val fmt = pick.format
        return AudioChoice(
            url = pick.content,
            ext = fmt?.suffix ?: "m4a",
            mime = fmt?.mimeType ?: "audio/mp4",
            kbps = pick.averageBitrate,
        )
    }
}
