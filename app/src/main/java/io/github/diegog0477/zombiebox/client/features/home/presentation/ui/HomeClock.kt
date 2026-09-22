package io.github.diegog0477.zombiebox.client.features.home.presentation.ui

import android.content.Context
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.github.diegog0477.zombiebox.client.core.ui.TvWidgets
import java.util.Date

/** Local device time; no network, seconds ticker or hidden-screen work. */
class HomeClock(context: Context, ui: TvWidgets) : LinearLayout(context) {
    private val time = ui.text("", 22f, ui.muted)
    private val date = ui.text("", 11f, ui.muted)
    private var attached = false
    private val tick =
        object : Runnable {
            override fun run() {
                if (!attached || windowVisibility != View.VISIBLE) return
                val now = Date()
                time.text = DateFormat.getTimeFormat(context).format(now)
                date.text = DateFormat.getDateFormat(context).format(now)
                postDelayed(this, 60000L - System.currentTimeMillis() % 60000L)
            }
        }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        setPadding(ui.dp(18), 0, 0, 0)
        addView(time)
        addView(date)
        isFocusable = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        schedule()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        schedule()
    }

    private fun schedule() {
        if (!attached) return
        removeCallbacks(tick)
        if (attached && windowVisibility == View.VISIBLE) post(tick)
    }

    override fun onDetachedFromWindow() {
        attached = false
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }
}
