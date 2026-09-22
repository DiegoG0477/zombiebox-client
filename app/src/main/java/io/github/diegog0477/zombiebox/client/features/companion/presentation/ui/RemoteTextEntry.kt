package io.github.diegog0477.zombiebox.client.features.companion.presentation.ui

import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import java.util.UUID

/** An ephemeral focus lease prevents delayed text from reaching a different field. */
class RemoteTextEntry(private val focused: () -> EditText?) {
    private var field: EditText? = null
    private var lease = ""

    fun inputId(): String {
        val current = focused()?.takeIf { safe(it) }
        if (current !== field) {
            field = current
            lease = if (current == null) "" else UUID.randomUUID().toString().replace("-", "")
        }
        return lease
    }

    fun paste(text: String, inputId: String): String {
        if (inputId.isEmpty() || this.inputId() != inputId) return "BUSY"
        if (text.isEmpty() || text.length > 512 || text.any { it.isISOControl() })
            return "UNSUPPORTED"
        val target = field ?: return "BUSY"
        val start = target.selectionStart.coerceAtLeast(0)
        val end = target.selectionEnd.coerceAtLeast(0)
        target.text.replace(minOf(start, end), maxOf(start, end), text)
        return "EXECUTED"
    }

    private fun safe(view: EditText): Boolean {
        val variation = view.inputType and InputType.TYPE_MASK_VARIATION
        return view.isEnabled &&
            view.hasFocus() &&
            view.isShown &&
            view.hasWindowFocus() &&
            view.transformationMethod !is PasswordTransformationMethod &&
            !(view.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT &&
                variation in
                    listOf(
                        InputType.TYPE_TEXT_VARIATION_PASSWORD,
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                        0xe0,
                    )) &&
            !(view.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER &&
                variation == 0x10)
    }
}
