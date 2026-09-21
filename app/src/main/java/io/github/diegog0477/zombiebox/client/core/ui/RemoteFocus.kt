package io.github.diegog0477.zombiebox.client.core.ui

import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup

/** View adapter; Android retains native click, accessibility and touch handling. */
class RemoteFocus {
    private val model = FocusModel()
    private val views = LinkedHashMap<String, View>()
    private val windows = LinkedHashMap<String, WindowedRow>()

    fun rebuild(rows: List<Pair<String, ViewGroup>>, restore: Boolean) {
        views.clear()
        windows.clear()
        model.rebuild(
            rows.map { (id, group) ->
                if (group is WindowedRow) {
                    group.selected = { model.select(it) }
                    group.keys.forEach { windows[it] = group }
                    return@map FocusModel.Row(id, group.keys)
                }
                val keys = ArrayList<String>()
                for (i in 0 until group.childCount) {
                    val child = group.getChildAt(i)
                    if (!child.isFocusable || child.visibility != View.VISIBLE || !child.isEnabled)
                        continue
                    val key = child.tag as? String ?: "$id:$i"
                    child.tag = key
                    keys.add(key)
                    views[key] = child
                    child.setOnFocusChangeListener { _, focused -> if (focused) model.select(key) }
                }
                FocusModel.Row(id, keys)
            }
        )
        if (restore) restore()
    }

    fun restore() {
        model.selected?.let { focus(it) }
    }

    private fun focus(key: String) {
        (views[key] ?: windows[key]?.view(key))?.let { view ->
            model.select(key)
            view.requestFocus()
            view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), false)
        }
    }

    fun move(key: Int, current: View?): Boolean {
        val delta =
            when (key) {
                KeyEvent.KEYCODE_DPAD_LEFT -> Pair(-1, 0)
                KeyEvent.KEYCODE_DPAD_RIGHT -> Pair(1, 0)
                KeyEvent.KEYCODE_DPAD_UP -> Pair(0, -1)
                KeyEvent.KEYCODE_DPAD_DOWN -> Pair(0, 1)
                else -> return false
            }
        if (
            current != null &&
                views.values.none { it === current } &&
                windows[current.tag as? String] == null
        )
            return false
        model.move(delta.first, delta.second)?.let { focus(it) }
        return views.isNotEmpty() || windows.isNotEmpty()
    }
}
