package io.github.diegog0477.zombiebox.client.core.platform

import android.annotation.TargetApi
import android.view.View

@TargetApi(21)
@Suppress("DEPRECATION")
class ModernWindowInsetsPolicy : WindowInsetsPolicy {
    override fun install(view: View) {
        val left = view.paddingLeft
        val top = view.paddingTop
        val right = view.paddingRight
        val bottom = view.paddingBottom
        view.setOnApplyWindowInsetsListener { target, insets ->
            target.setPadding(
                left + insets.systemWindowInsetLeft,
                top + insets.systemWindowInsetTop,
                right + insets.systemWindowInsetRight,
                bottom + insets.systemWindowInsetBottom,
            )
            insets
        }
    }
}
