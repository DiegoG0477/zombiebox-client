package io.github.diegog0477.zombiebox.client.platform

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.view.InputDevice
import io.github.diegog0477.zombiebox.client.model.*
import java.io.File
import java.security.MessageDigest

class HardwareScanner(context: Context) : HardwareSource {
    private val context = context.applicationContext

    override fun scan(): HardwareReport {
        val abis = ArrayList<String>()
        fun abi(value: String?) {
            if (
                value != null &&
                    value !in listOf("unknown", "", "null") &&
                    value.length < 80 &&
                    !abis.contains(value) &&
                    abis.size < 8
            )
                abis.add(value)
        }
        if (Build.VERSION.SDK_INT >= 21)
            try {
                (Build::class.java.getField("SUPPORTED_ABIS").get(null) as? Array<*>)?.forEach {
                    abi(it as? String)
                }
            } catch (_: Exception) {}
        abi(Build.CPU_ABI)
        abi(Build.CPU_ABI2)
        if (abis.isEmpty())
            try {
                val buffer = CharArray(16384)
                val length = File("/proc/cpuinfo").reader().use { it.read(buffer) }
                val cpu = if (length > 0) String(buffer, 0, length).lowercase() else ""
                when {
                    cpu.contains("aarch64") || cpu.contains("armv8") -> abi("arm64-v8a")
                    cpu.contains("armv7") -> abi("armeabi-v7a")
                    cpu.contains("intel") || cpu.contains("amd") -> abi("x86-family")
                }
            } catch (_: Exception) {}
        val codecs =
            if (Build.VERSION.SDK_INT >= 16)
                try {
                    (Class.forName(
                                "io.github.diegog0477.zombiebox.client.platform.Api16CodecDiscovery"
                            )
                            .getConstructor()
                            .newInstance() as CodecDiscovery)
                        .decoders()
                } catch (_: Exception) {
                    emptyList()
                } catch (_: LinkageError) {
                    emptyList()
                }
            else emptyList()
        var keyboard = false
        var mouse = false
        try {
            for (id in InputDevice.getDeviceIds().take(32)) {
                val sources = InputDevice.getDevice(id)?.sources ?: 0
                keyboard =
                    keyboard ||
                        (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
                mouse = mouse || (sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE
            }
        } catch (_: Exception) {}
        val free =
            try {
                val fs = StatFs(context.filesDir.path)
                fs.availableBlocks.toLong() * fs.blockSize.toLong() / 1048576
            } catch (_: Exception) {
                0L
            }
        val network =
            try {
                val manager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val active = manager.activeNetworkInfo
                if (active?.isConnected != true) "OFFLINE"
                else
                    when (active.type) {
                        1 -> "WIFI"
                        9 -> "ETHERNET"
                        0 -> "MOBILE"
                        else -> "OTHER"
                    }
            } catch (_: Exception) {
                "UNKNOWN"
            }
        val players =
            try {
                context.packageManager
                    .queryIntentActivities(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(
                                Uri.parse("http://gateway.invalid/probe.mp4"),
                                "video/mp4",
                            ),
                        0,
                    )
                    .map { it.activityInfo.packageName }
                    .distinct()
                    .take(32)
            } catch (_: Exception) {
                emptyList()
            }
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val gles =
            try {
                manager.deviceConfigurationInfo.reqGlEsVersion
            } catch (_: Exception) {
                0
            }
        val fingerprint =
            MessageDigest.getInstance("SHA-256")
                .digest(
                    (Build.FINGERPRINT + "|" + abis.joinToString(",") + "|scanner-1").toByteArray(
                        Charsets.UTF_8
                    )
                )
                .joinToString("") { "%02x".format(it.toInt() and 255) }
        return HardwareReport(
            fingerprint,
            Build.PRODUCT.take(200),
            Build.DEVICE.take(200),
            abis,
            Runtime.getRuntime().availableProcessors(),
            HardwareMemory.physicalMb(),
            free,
            gles,
            keyboard,
            mouse,
            network,
            codecs,
            players,
        )
    }
}
