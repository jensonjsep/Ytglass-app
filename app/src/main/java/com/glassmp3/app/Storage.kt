package com.glassmp3.app

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import okhttp3.Request
import java.io.File
import java.io.IOException

object Storage {
    private const val REL_PATH = "Music/YouTube MP3"

    fun safeName(s: String): String =
        s.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(120).ifBlank { "song" }

    private fun collection(): Uri =
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    /** Copies [file] into the public Music/YouTube MP3 folder. */
    fun saveToMusic(ctx: Context, file: File, displayName: String, mime: String): Uri {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, REL_PATH)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection(), values) ?: throw IOException("Can't create the file")
        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        } ?: throw IOException("Can't write the file")
        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    fun queryLibrary(ctx: Context): List<LocalSong> {
        val list = ArrayList<LocalSong>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
        )
        try {
            ctx.contentResolver.query(
                collection(),
                projection,
                "${MediaStore.Audio.Media.RELATIVE_PATH} = ?",
                arrayOf("$REL_PATH/"),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    list.add(
                        LocalSong(
                            id = id,
                            title = c.getString(iName).substringBeforeLast('.'),
                            sizeBytes = c.getLong(iSize),
                            uri = ContentUris.withAppendedId(collection(), id),
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            // no permission / provider error -> empty library
        }
        return list
    }
}

object FileDownloader {
    fun download(url: String, dest: File, onProgress: (Float) -> Unit) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", Http.UA)
            .header("Range", "bytes=0-")
            .build()
        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Download failed (HTTP ${resp.code})")
            val body = resp.body ?: throw IOException("Empty response")
            val total = body.contentLength()
            dest.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
    }
}
