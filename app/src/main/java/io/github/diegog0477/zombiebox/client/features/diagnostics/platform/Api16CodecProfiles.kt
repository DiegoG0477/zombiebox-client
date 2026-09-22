package io.github.diegog0477.zombiebox.client.features.diagnostics.platform

import android.annotation.TargetApi
import android.media.MediaCodecInfo
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.CodecProfileHint

@TargetApi(16)
object Api16CodecProfiles {
    fun read(codec: MediaCodecInfo, types: List<String>): List<CodecProfileHint> {
        val result = mutableListOf<CodecProfileHint>()
        for (mime in types) {
            if (!mime.startsWith("video/") && mime != "audio/mp4a-latm") continue
            try {
                for (entry in codec.getCapabilitiesForType(mime).profileLevels.take(16)) {
                    if (entry.profile >= 0 && entry.level >= 0)
                        result.add(CodecProfileHint(mime, entry.profile, entry.level))
                    if (result.size >= 8) return result.distinct()
                }
            } catch (_: Exception) {} catch (_: LinkageError) {}
        }
        return result.distinct()
    }
}
