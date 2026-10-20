package com.glassmp3.app

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

object Mp3Converter {
    /**
     * Converts [input] to a 320 kbps MP3 at [output].
     * Returns false if no MP3 encoder is available or conversion failed;
     * the caller then keeps the original audio file instead.
     */
    fun convert(input: File, output: File, title: String, artist: String): Boolean {
        for (encoder in listOf("libmp3lame", "libshine")) {
            try {
                output.delete()
                val args = arrayOf(
                    "-y", "-i", input.absolutePath,
                    "-vn",
                    "-c:a", encoder,
                    "-b:a", "320k",
                    "-metadata", "title=$title",
                    "-metadata", "artist=$artist",
                    output.absolutePath,
                )
                val session = FFmpegKit.executeWithArguments(args)
                if (ReturnCode.isSuccess(session.returnCode) && output.length() > 0) return true
            } catch (t: Throwable) {
                // native library missing / unsupported device -> try next / fall back
            }
        }
        output.delete()
        return false
    }
}
