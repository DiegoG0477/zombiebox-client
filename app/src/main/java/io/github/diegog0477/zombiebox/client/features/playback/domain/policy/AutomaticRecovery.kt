package io.github.diegog0477.zombiebox.client.features.playback.domain.policy

/** Three attempts per incident; only a minute of playback restores the budget. */
class AutomaticRecovery {
    private var attempts = 0
    private var due = 0L
    private var healthySince = 0L

    fun reset() {
        attempts = 0
        due = 0
        healthySince = 0
    }

    fun observe(status: String, now: Long) {
        if (status == "PLAYING") {
            if (healthySince == 0L) healthySince = now
            if (now - healthySince >= 60000) attempts = 0
        } else healthySince = 0
    }

    fun schedule(now: Long): Boolean {
        if (attempts >= 3) return false
        if (due == 0L) due = now + (2000L shl attempts)
        return true
    }

    fun take(now: Long): Int? {
        if (due == 0L || now < due) return null
        due = 0
        attempts++
        return attempts
    }
}
