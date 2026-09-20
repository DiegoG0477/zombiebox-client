package tv.zombiebox.client

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Bootstrap probe only: no provider, player or final Home implementation. */
class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var closed = false
    @Volatile private var connection: HttpURLConnection? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(24, 24, 24, 24)
        root.setBackgroundColor(Color.rgb(10, 15, 15))
        val status = TextView(this)
        status.text = getString(R.string.hello, android.os.Build.VERSION.SDK_INT)
        status.setTextColor(Color.rgb(80, 240, 109))
        status.textSize = 24f
        val address = EditText(this)
        address.setSingleLine(true)
        address.setText(intent.getStringExtra("gatewayUrl") ?: "http://10.0.2.2:8090")
        val button = Button(this)
        button.setText(R.string.check_health)
        root.addView(status)
        root.addView(address)
        root.addView(button)
        setContentView(root)
        button.setOnClickListener {
            val base = address.text.toString().trimEnd('/')
            button.isEnabled = false
            status.setText(R.string.connecting)
            worker.execute {
                val result = try {
                    val url = URL(base + "/health")
                    require(url.protocol == "http" || url.protocol == "https")
                    val http = url.openConnection() as HttpURLConnection
                    connection = http
                    http.connectTimeout = 3000
                    http.readTimeout = 3000
                    http.instanceFollowRedirects = false
                    val code = http.responseCode
                    if (code != 200) {
                        getString(R.string.http_status, code)
                    } else {
                        val bytes = ByteArray(4096)
                        val input = http.inputStream
                        var total = 0
                        try {
                            while (total < bytes.size) {
                                val read = input.read(bytes, total, bytes.size - total)
                                if (read < 0) break
                                total += read
                            }
                        } finally { input.close() }
                        String(bytes, 0, total, charset("UTF-8"))
                    }
                } catch (e: Exception) {
                    getString(R.string.unavailable, e.javaClass.simpleName)
                } finally {
                    connection?.disconnect()
                    connection = null
                }
                runOnUiThread {
                    if (!closed) {
                        status.text = result
                        button.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        closed = true
        connection?.disconnect()
        worker.shutdownNow()
        super.onDestroy()
    }
}
