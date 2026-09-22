package io.github.diegog0477.zombiebox.client.features.dial.presentation.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.github.diegog0477.zombiebox.client.MainActivity
import io.github.diegog0477.zombiebox.client.features.dial.domain.model.DialLaunch
import io.github.diegog0477.zombiebox.client.features.dial.platform.NativeDialPolicy

/** Untrusted OEM launch boundary. Never forwards launch extras or performs network I/O. */
class NativeDialActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val allowed =
            try {
                NativeDialPolicy.enabled(this) &&
                    DialLaunch.accepts(
                        intent.action,
                        intent.getStringExtra("com.amazon.extra.DIAL_PARAM"),
                    )
            } catch (_: Exception) {
                false
            }
        if (allowed)
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        finish()
    }
}
