package com.xcluice.sniptext.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.roundToInt

object OcrProcessor {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun recognizeRegion(
        source: Bitmap,
        viewSelection: RectF,
        viewWidth: Int,
        viewHeight: Int,
        joinLines: Boolean,
        onResult: (rawText: String, formattedText: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (viewWidth <= 0 || viewHeight <= 0) {
            onError("Invalid overlay dimensions")
            return
        }

        val scaleX = source.width.toFloat() / viewWidth.toFloat()
        val scaleY = source.height.toFloat() / viewHeight.toFloat()

        val left = (viewSelection.left * scaleX).roundToInt().coerceIn(0, source.width - 1)
        val top = (viewSelection.top * scaleY).roundToInt().coerceIn(0, source.height - 1)
        val right = (viewSelection.right * scaleX).roundToInt().coerceIn(left + 1, source.width)
        val bottom = (viewSelection.bottom * scaleY).roundToInt().coerceIn(top + 1, source.height)

        val cropW = (right - left).coerceAtLeast(1)
        val cropH = (bottom - top).coerceAtLeast(1)

        val cropped = try {
            Bitmap.createBitmap(source, left, top, cropW, cropH)
        } catch (e: Exception) {
            onError("Could not crop region: ${e.message}")
            return
        }

        val image = InputImage.fromBitmap(cropped, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (cropped !== source) cropped.recycle()
                val raw = visionText.text.trim()
                val formatted = formatText(raw, joinLines)
                onResult(raw, formatted)
            }
            .addOnFailureListener { e ->
                if (cropped !== source) cropped.recycle()
                onError(e.message ?: "OCR failed")
            }
    }

    fun formatText(raw: String, joinLines: Boolean): String {
        if (!joinLines) return raw.trim()
        return raw
            .split(Regex("\\n\\s*\\n"))
            .joinToString("\n\n") { paragraph ->
                paragraph.replace(Regex("\\s*\\n\\s*"), " ").trim()
            }
            .trim()
    }

    fun copyToClipboard(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("SnipText", text))
    }
}
