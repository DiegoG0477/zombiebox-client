package io.github.diegog0477.zombiebox.client.platform

import java.io.File

/** Linux procfs is an optional measurement, independent of API16 MemoryInfo. */
object HardwareMemory {
    fun physicalMb(): Int = try {
        File("/proc/meminfo").bufferedReader().use { reader ->
            var total = 0
            for (index in 0 until 64) {
                val line = reader.readLine() ?: break
                if (line.startsWith("MemTotal:")) {
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 3 && parts[2] == "kB") total = (parts[1].toLong() / 1024).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
                    break
                }
            }
            total
        }
    } catch (_: Exception) { 0 }
}
