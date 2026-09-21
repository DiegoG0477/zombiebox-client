package io.github.diegog0477.zombiebox.client.features.catalog.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.core.ui.TvWidgets
import io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel.GuideViewModel
import java.text.DateFormat
import java.util.Date

/** Recycled channel rows; left/right navigate a shared time slice, up/down select channels. */
class GuideDialog(
    private val activity: Activity,
    private val channels: List<MediaItem>,
    private val play: (MediaItem) -> Unit,
) {
    fun show() {
        val ui = TvWidgets(activity) { activity.resources.getColor(R.color.accent_iptv) }
        val model = GuideViewModel(channels, System.currentTimeMillis() / 1000)
        val header = ui.text("", 18f)
        val list = ListView(activity)
        val adapter =
            object : BaseAdapter() {
                override fun getCount() = channels.size

                override fun getItem(position: Int) = channels[position]

                override fun getItemId(position: Int) = position.toLong()

                override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
                    val row =
                        recycled as? LinearLayout
                            ?: ui.column().apply {
                                setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(8))
                                addView(ui.text("", 18f))
                                addView(ui.text("", 14f, ui.muted))
                            }
                    val channel = channels[position]
                    val programme = model.programme(position)
                    (row.getChildAt(0) as TextView).text = channel.title
                    (row.getChildAt(1) as TextView).text =
                        programme?.let {
                            DateFormat.getTimeInstance(DateFormat.SHORT)
                                .format(Date(it.start * 1000)) + " — " + it.title
                        } ?: activity.getString(R.string.guide_no_programme)
                    return row
                }
            }
        fun shift(delta: Long) {
            model.shift(delta)
            header.text =
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(model.time * 1000))
            adapter.notifyDataSetChanged()
        }
        list.adapter = adapter
        list.setOnKeyListener { _, key, event ->
            if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (event.action == KeyEvent.ACTION_DOWN)
                    shift(if (key == KeyEvent.KEYCODE_DPAD_LEFT) -1800 else 1800)
                true
            } else false
        }
        val content =
            ui.column().apply {
                addView(header)
                addView(ui.text(activity.getString(R.string.guide_help), 13f, ui.muted))
                addView(list, LinearLayout.LayoutParams(-1, ui.dp(320)))
                addView(
                    ui.row().apply {
                        addView(ui.button(R.string.guide_earlier) { shift(-1800) })
                        addView(ui.button(R.string.guide_later) { shift(1800) })
                    }
                )
            }
        val dialog =
            AlertDialog.Builder(activity)
                .setTitle(R.string.guide)
                .setView(content)
                .setNegativeButton(R.string.close, null)
                .create()
        list.setOnItemClickListener { _, _, index, _ ->
            dialog.dismiss()
            play(channels[index])
        }
        shift(0)
        dialog.show()
        list.requestFocus()
    }
}
