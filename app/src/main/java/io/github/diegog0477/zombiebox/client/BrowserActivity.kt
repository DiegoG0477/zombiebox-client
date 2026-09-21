package io.github.diegog0477.zombiebox.client

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.KeyEvent
import android.widget.*
import io.github.diegog0477.zombiebox.client.data.GatewayBrowserRepository
import io.github.diegog0477.zombiebox.client.presentation.BrowserViewModel
import io.github.diegog0477.zombiebox.shared.GatewayApi
import java.util.concurrent.Executors

class BrowserActivity : Activity() {
    private val api = GatewayApi()
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler()
    private lateinit var model: BrowserViewModel
    private val poll =
        object : Runnable {
            override fun run() {
                model.refresh()
                handler.postDelayed(this, 3000)
            }
        }

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        val prefs = getSharedPreferences("zombie", MODE_PRIVATE)
        api.configure(
            prefs.getString("gateway", "")!!,
            prefs.getString("device", "")!!,
            prefs.getString("token", "")!!,
        )
        model =
            BrowserViewModel(
                GatewayBrowserRepository(api),
                { work -> executor.execute { work() } },
                { work -> handler.post { work() } },
            )
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.rgb(10, 15, 16))
                setPadding(12, 12, 12, 12)
            }
        val toolbar = LinearLayout(this)
        val address =
            EditText(this).apply {
                setHint(R.string.browser_address)
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                setSingleLine(true)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            }
        toolbar.addView(address, LinearLayout.LayoutParams(0, -2, 1f))
        toolbar.addView(
            Button(this).apply {
                setText(R.string.browser_open)
                setOnClickListener { model.open(address.text.toString().trim()) }
            }
        )
        root.addView(toolbar)
        val status =
            TextView(this).apply {
                setText(R.string.browser_detail)
                setTextColor(Color.LTGRAY)
            }
        root.addView(status)
        val image =
            ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                isFocusable = true
                isFocusableInTouchMode = true
                contentDescription = getString(R.string.browser_page)
            }
        image.setOnKeyListener { _, key, event ->
            val value =
                when (key) {
                    KeyEvent.KEYCODE_DPAD_UP -> "ArrowUp"
                    KeyEvent.KEYCODE_DPAD_DOWN -> "ArrowDown"
                    KeyEvent.KEYCODE_DPAD_LEFT -> "ArrowLeft"
                    KeyEvent.KEYCODE_DPAD_RIGHT -> "ArrowRight"
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> "Enter"
                    else -> null
                }
            if (value != null) {
                if (event.action == KeyEvent.ACTION_UP) model.input("key", value)
                true
            } else false
        }
        root.addView(image, LinearLayout.LayoutParams(-1, 0, 1f))
        val controls = LinearLayout(this)
        fun control(id: Int, action: () -> Unit) {
            controls.addView(
                Button(this).apply {
                    setText(id)
                    setOnClickListener { action() }
                }
            )
        }
        control(R.string.browser_back) { model.input("back") }
        control(R.string.browser_previous_link) { model.input("key", "Shift+Tab") }
        control(R.string.browser_next_link) { model.input("key", "Tab") }
        control(R.string.browser_activate) { model.input("key", "Enter") }
        control(R.string.browser_page) { image.requestFocus() }
        root.addView(HorizontalScrollView(this).apply { addView(controls) })
        val entry = LinearLayout(this)
        val text =
            EditText(this).apply {
                setHint(R.string.browser_text)
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                setSingleLine(true)
                isSaveEnabled = false
            }
        entry.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        entry.addView(
            Button(this).apply {
                setText(R.string.browser_type)
                setOnClickListener {
                    val value = text.text.toString()
                    text.setText("")
                    model.input("text", value)
                }
            }
        )
        root.addView(entry)
        setContentView(root)
        var lastFrame: ByteArray? = null
        model.observer = { value ->
            status.setText(
                if (value.failed) R.string.unavailable
                else if (value.loading) R.string.loading else R.string.browser_detail
            )
            value.frame?.let { bytes ->
                if (bytes !== lastFrame) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth in 1..960 && bounds.outHeight in 1..540) {
                        image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                        lastFrame = bytes
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(poll, 3000)
    }

    override fun onPause() {
        handler.removeCallbacks(poll)
        super.onPause()
    }

    override fun onDestroy() {
        model.close()
        executor.execute { api.close() }
        executor.shutdown()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
