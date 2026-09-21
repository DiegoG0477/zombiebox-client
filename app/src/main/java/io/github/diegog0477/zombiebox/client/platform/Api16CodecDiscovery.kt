package io.github.diegog0477.zombiebox.client.platform
import android.annotation.TargetApi
import android.media.MediaCodecList
import io.github.diegog0477.zombiebox.client.model.*

/** Codec enumeration is a hint, never a PASS result or a hardware acceleration claim. */
@TargetApi(16)
class Api16CodecDiscovery:CodecDiscovery {
 override fun decoders():List<CodecHint> {
  val result=ArrayList<CodecHint>()
  for(index in 0 until MediaCodecList.getCodecCount().coerceAtMost(128))try {
   val codec=MediaCodecList.getCodecInfoAt(index)
   if(!codec.isEncoder)result.add(CodecHint(codec.name.take(200),codec.supportedTypes.take(16).map{it.take(100)}))
  }catch(_:Exception){}
  return result
 }
}
