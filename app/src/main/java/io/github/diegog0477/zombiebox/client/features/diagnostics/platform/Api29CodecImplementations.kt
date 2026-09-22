package io.github.diegog0477.zombiebox.client.features.diagnostics.platform

import android.annotation.TargetApi
import android.media.MediaCodecList
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.CodecImplementations

@TargetApi(29)
class Api29CodecImplementations : CodecImplementations {
    override fun declaredAcceleration(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (codec in MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.take(128)) {
            try {
                val hardware = codec.isHardwareAccelerated
                val software = codec.isSoftwareOnly
                result[codec.name.take(200)] =
                    when {
                        hardware && !software -> "HARDWARE"
                        software && !hardware -> "SOFTWARE"
                        else -> "UNKNOWN"
                    }
            } catch (_: Exception) {} catch (_: LinkageError) {}
        }
        return result
    }
}
