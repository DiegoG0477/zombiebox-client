package io.github.diegog0477.zombiebox.client.features.catalog.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.core.ui.TvWidgets
import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.CatalogOverlay
import io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel.GuideViewModel
import java.text.DateFormat
import java.util.Date

/** Recycled channel rows; left/right navigate a shared time slice, up/down select channels. */
class GuideDialog(
    private val activity: Activity,
    private var channels: List<MediaItem>,
    private val play: (MediaItem) -> Unit,
    private val reload: (((List<MediaItem>) -> Unit, () -> Unit) -> Unit)? = null,
    private val previousPage: (() -> Unit)? = null,
    private val nextPage: (() -> Unit)? = null,
    private val dismissed: () -> Unit = {},
) {
    private val handler = Handler()
    private var refreshing = false
    private var paging = false
    private var pageStatus: TextView? = null
    private val pageButtons = ArrayList<View>()

    fun pageFailed() {
        if (!visible) return
        paging = false
        pageButtons.forEach { it.isEnabled = true }
        pageStatus?.setText(R.string.guide_page_failed)
    }

    private fun changePage(action: () -> Unit) {
        if (refreshing || paging) return
        paging = true
        pageButtons.forEach { it.isEnabled = false }
        pageStatus?.setText(R.string.loading)
        action()
    }

    private var tick: Runnable? = null
    private var dialog: AlertDialog? = null
    private var capture: () -> CatalogOverlay = { CatalogOverlay() }
    val activeDialog: AlertDialog?
        get() = dialog?.takeIf { it.isShowing }

    val visible: Boolean
        get() = dialog?.isShowing == true

    fun snapshot(): CatalogOverlay = if (visible) capture() else CatalogOverlay()

    fun close() {
        tick?.let { handler.removeCallbacks(it) }
        tick = null
        dialog?.dismiss()
        dialog = null
    }

    fun show(saved: CatalogOverlay = CatalogOverlay()) {
        val ui = TvWidgets(activity) { activity.resources.getColor(R.color.accent_iptv) }
        var model = GuideViewModel(channels, System.currentTimeMillis() / 1000)
        if (saved.guideTime > 0) model.restore(saved.guideTime)
        val header = ui.text("", 18f)
        val list = ListView(activity)
        var selectedChannel = saved.selectedChannel
        list.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    channels.getOrNull(position)?.let { selectedChannel = it.id }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
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
                if (channels.any { it.guideState == "STALE" })
                    addView(ui.text(activity.getString(R.string.guide_stale), 13f, ui.muted))
                addView(ui.text(activity.getString(R.string.guide_help), 13f, ui.muted))
                pageStatus = ui.text("", 13f, ui.muted)
                addView(pageStatus)
                addView(list, LinearLayout.LayoutParams(-1, ui.dp(320)))
                addView(
                    ui.row().apply {
                        previousPage?.let { action ->
                            addView(
                                ui.button(R.string.guide_previous_channels) { changePage(action) }
                                    .also { pageButtons.add(it) }
                            )
                        }
                        nextPage?.let { action ->
                            addView(
                                ui.button(R.string.guide_next_channels) { changePage(action) }
                                    .also { pageButtons.add(it) }
                            )
                        }
                    }
                )
                addView(
                    ui.row().apply {
                        addView(ui.button(R.string.guide_earlier) { shift(-1800) })
                        addView(ui.button(R.string.guide_later) { shift(1800) })
                    }
                )
            }
        val current =
            AlertDialog.Builder(activity)
                .setTitle(R.string.guide)
                .setView(content)
                .setNegativeButton(R.string.close, null)
                .create()
        list.setOnItemClickListener { _, _, index, _ ->
            current.dismiss()
            play(channels[index])
        }
        shift(0)
        dialog = current
        capture = {
            CatalogOverlay(
                "guide",
                guideTime = model.time,
                selectedChannel =
                    selectedChannel.ifEmpty {
                        channels.getOrNull(list.firstVisiblePosition)?.id ?: ""
                    },
            )
        }
        tick =
            object : Runnable {
                override fun run() {
                    if (dialog !== current || !current.isShowing) return
                    if (!refreshing && !paging && reload != null) {
                        refreshing = true
                        reload.invoke(
                            { fresh ->
                                refreshing = false
                                if (dialog === current && current.isShowing) {
                                    val selectedID = selectedChannel
                                    val time = model.time
                                    channels = fresh
                                    model =
                                        GuideViewModel(channels, System.currentTimeMillis() / 1000)
                                    model.restore(time)
                                    shift(0)
                                    pageStatus?.text = ""
                                    channels
                                        .indexOfFirst { it.id == selectedID }
                                        .takeIf { it >= 0 }
                                        ?.let { list.setSelection(it) }
                                }
                            },
                            {
                                refreshing = false
                                if (dialog === current && current.isShowing)
                                    pageStatus?.setText(R.string.guide_stale)
                            },
                        )
                    }
                    handler.postDelayed(this, 60000)
                }
            }
        current.setOnDismissListener {
            tick?.let { handler.removeCallbacks(it) }
            dismissed()
        }
        current.show()
        tick?.let { handler.postDelayed(it, 60000) }
        val selected = channels.indexOfFirst { it.id == saved.selectedChannel }
        if (selected >= 0) list.setSelection(selected)
        list.requestFocus()
    }
}
