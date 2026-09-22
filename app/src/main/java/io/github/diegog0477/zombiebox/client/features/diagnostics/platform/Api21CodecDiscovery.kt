package io.github.diegog0477.zombiebox.client.features.diagnostics.platform

import android.annotation.TargetApi
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.CodecHint
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.CodecDiscovery

/** Vendor declarations select diagnostics; they never authorize playback themselves. */
@TargetApi(21)
class Api21CodecDiscovery : CodecDiscovery {
    override fun decoders() = inventory(false)

    override fun encoders() = inventory(true)

    private fun inventory(encoder: Boolean): List<CodecHint> {
        val result = ArrayList<CodecHint>()
        for (codec in MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.take(128)) {
            if (codec.isEncoder != encoder) continue
            try {
                val types = codec.supportedTypes.take(16).map { it.take(100) }
                val candidates = ArrayList<String>()
                for (mime in types) {
                    if (encoder || (mime != "video/avc" && mime != "video/hevc")) continue
                    try {
                        val caps = codec.getCapabilitiesForType(mime)
                        val expected =
                            if (mime == "video/hevc")
                                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
                            else MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
                        if (!caps.profileLevels.any { it.profile == expected }) continue
                        val video = caps.videoCapabilities ?: continue
                        if (mime == "video/hevc" && video.areSizeAndRateSupported(1920, 1080, 30.0))
                            candidates.add("hevc-1080-main")
                        if (video.areSizeAndRateSupported(3840, 2160, 30.0))
                            candidates.add(
                                if (mime == "video/hevc") "hevc-2160-main" else "h264-2160-high"
                            )
                    } catch (_: Exception) {}
                }
                result.add(
                    CodecHint(
                        codec.name.take(200),
                        types,
                        candidates.distinct().take(3),
                        Api16CodecProfiles.read(codec, types),
                    )
                )
            } catch (_: Exception) {}
        }
        return result
    }
}
