package io.github.diegog0477.zombiebox.client.core.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout

/** Seven realized cards; semantic focus can still address the entire row. */
class WindowedRow(context: Context, val keys: List<String>, private val create: (Int) -> View) :
    LinearLayout(context) {
    private var first = -1
    var selected: ((String) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        realize(0)
    }

    fun view(key: String): View? {
        val index = keys.indexOf(key)
        if (index < 0) return null
        realize(index)
        return getChildAt(index - first)
    }

    fun page(forward: Boolean) {
        val target = if (forward) first + childCount else first - 1
        if (target in keys.indices) realize(target)
    }

    private fun realize(index: Int) {
        if (index in first until first + childCount) return
        val start = (index - 3).coerceAtLeast(0).coerceAtMost((keys.size - 7).coerceAtLeast(0))
        if (start == first) return
        first = start
        removeAllViews()
        for (i in start until minOf(start + 7, keys.size)) {
            val child = create(i)
            child.tag = keys[i]
            child.setOnFocusChangeListener { _, focused -> if (focused) selected?.invoke(keys[i]) }
            addView(child)
        }
    }
}
