package com.xcluice.sniptext.core

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SelectionOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val handler = Handler(Looper.getMainLooper())
    private val store = SettingsStore(context)
    private var rootLayout: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var currentBitmap: Bitmap? = null
    private var canvasView: SelectionCanvasView? = null
    private var topPillBar: LinearLayout? = null
    private var adjustActionBar: LinearLayout? = null
    private var resultCardContainer: LinearLayout? = null
    private var toastPill: TextView? = null

    val isShowing: Boolean
        get() = rootLayout != null

    fun show(bitmap: Bitmap) {
        dismiss()
        val settings = store.loadSettings()
        currentBitmap = bitmap

        val root = FrameLayout(context)
        val selectionView = SelectionCanvasView(
            context = context,
            bitmap = bitmap,
            instantGrab = settings.instantGrab,
            onSelectionComplete = { rect ->
                if (settings.instantGrab) {
                    runOcrOnRect(rect)
                } else {
                    showAdjustBar(rect)
                }
            },
            onSelectionStarted = {
                hideAdjustBar()
                hideResultCard()
            }
        )
        canvasView = selectionView
        root.addView(
            selectionView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val topBar = buildTopPillBar(selectionView)
        topPillBar = topBar
        root.addView(
            topBar,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                topMargin = dp(44)
            }
        )

        val adjBar = buildAdjustBar()
        adjBar.visibility = View.GONE
        adjustActionBar = adjBar
        root.addView(
            adjBar,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = dp(40)
            }
        )

        val pill = TextView(context).apply {
            setTextColor(Color.parseColor("#F4F5F7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            background = pillDrawable("#181A1F", "#343842")
            visibility = View.GONE
            elevation = dp(8).toFloat()
        }
        toastPill = pill
        root.addView(
            pill,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = dp(48)
            }
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        rootLayout = root
        layoutParams = params
        windowManager.addView(root, params)
        selectionView.animateIntro()
    }

    fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        rootLayout?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
        rootLayout = null
        layoutParams = null
        canvasView = null
        topPillBar = null
        adjustActionBar = null
        resultCardContainer = null
        toastPill = null
        currentBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        currentBitmap = null
    }

    private fun buildTopPillBar(selectionView: SelectionCanvasView): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = pillDrawable("#E614161A", "#33FFFFFF")
            elevation = dp(8).toFloat()

            val hint = TextView(context).apply {
                text = "Drag to grab text"
                setTextColor(Color.parseColor("#CBD5E1"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(dp(6), dp(4), dp(10), dp(4))
            }
            addView(hint)

            val fullBtn = TextView(context).apply {
                text = "Full Screen"
                setTextColor(Color.parseColor("#F4F5F7"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = pillDrawable("#262930", "#40FFFFFF")
                setOnClickListener {
                    val w = selectionView.width.toFloat()
                    val h = selectionView.height.toFloat()
                    if (w > 10f && h > 10f) {
                        val fullRect = RectF(dp(8).toFloat(), dp(8).toFloat(), w - dp(8), h - dp(8))
                        selectionView.setSelectionRect(fullRect)
                        runOcrOnRect(fullRect)
                    }
                }
            }
            addView(fullBtn)

            val closeBtn = TextView(context).apply {
                text = "✕"
                setTextColor(Color.parseColor("#E2E8F0"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dp(12), dp(4), dp(6), dp(4))
                setOnClickListener { dismiss() }
            }
            addView(closeBtn)
        }
    }

    private fun buildAdjustBar(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = pillDrawable("#F014161A", "#38FFFFFF")
            elevation = dp(10).toFloat()

            val clearBtn = TextView(context).apply {
                text = "Reset"
                setTextColor(Color.parseColor("#94A3B8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setOnClickListener {
                    canvasView?.clearSelection()
                    hideAdjustBar()
                }
            }
            addView(clearBtn)

            val grabBtn = TextView(context).apply {
                text = "Grab Text"
                setTextColor(Color.parseColor("#111214"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(18), dp(8), dp(18), dp(8))
                background = pillDrawable("#F4F5F7", "#F4F5F7")
                setOnClickListener {
                    canvasView?.currentSelection()?.let { rect ->
                        hideAdjustBar()
                        runOcrOnRect(rect)
                    }
                }
            }
            addView(grabBtn)
        }
    }

    private fun showAdjustBar(rect: RectF) {
        if (rect.width() < dp(16) || rect.height() < dp(16)) return
        adjustActionBar?.apply {
            alpha = 0f
            translationY = dp(16).toFloat()
            visibility = View.VISIBLE
            animate().alpha(1f).translationY(0f).setDuration(180).start()
        }
    }

    private fun hideAdjustBar() {
        adjustActionBar?.visibility = View.GONE
    }

    private fun runOcrOnRect(rect: RectF) {
        val bmp = currentBitmap ?: return
        val view = canvasView ?: return
        val settings = store.loadSettings()

        view.setScanning(true)
        OcrProcessor.recognizeRegion(
            source = bmp,
            viewSelection = rect,
            viewWidth = view.width,
            viewHeight = view.height,
            joinLines = settings.joinLineBreaks,
            onResult = { rawText, formattedText ->
                view.setScanning(false)
                if (formattedText.isBlank()) {
                    showToastPill("No text found in selection")
                    return@recognizeRegion
                }
                store.addHistory(formattedText)
                if (settings.autoCopy) {
                    OcrProcessor.copyToClipboard(context, formattedText)
                    vibrateLight(settings.hapticEnabled)
                    showToastPill("Copied • ${formattedText.length} chars", autoDismiss = true)
                } else {
                    vibrateLight(settings.hapticEnabled)
                    showResultCard(rawText, formattedText, settings.joinLineBreaks)
                }
            },
            onError = { err ->
                view.setScanning(false)
                showToastPill(err)
            }
        )
    }

    private fun showToastPill(message: String, autoDismiss: Boolean = false) {
        val pill = toastPill ?: return
        pill.text = message
        pill.alpha = 0f
        pill.translationY = dp(12).toFloat()
        pill.visibility = View.VISIBLE
        pill.animate().alpha(1f).translationY(0f).setDuration(180).start()

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (autoDismiss) {
                dismiss()
            } else {
                pill.animate().alpha(0f).setDuration(180).withEndAction {
                    pill.visibility = View.GONE
                }.start()
            }
        }, if (autoDismiss) 1350L else 1900L)
    }

    private fun showResultCard(rawText: String, initialFormatted: String, initialJoin: Boolean) {
        hideResultCard()
        val root = rootLayout ?: return

        layoutParams?.let { params ->
            params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            try {
                windowManager.updateViewLayout(root, params)
            } catch (_: Exception) {
            }
        }

        var joinLines = initialJoin
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = cardDrawable("#F514161A", "#38FFFFFF")
            elevation = dp(16).toFloat()
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(context).apply {
            text = "Extracted Text"
            setTextColor(Color.parseColor("#F4F5F7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(titleView)

        val editText = EditText(context).apply {
            setText(initialFormatted)
            setTextColor(Color.parseColor("#E2E8F0"))
            setHintTextColor(Color.parseColor("#64748B"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            background = cardDrawable("#1E2128", "#2C313B")
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minLines = 3
            maxLines = 8
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = Gravity.TOP or Gravity.START
        }

        val joinChip = TextView(context).apply {
            text = if (joinLines) "Join Lines: ON" else "Join Lines: OFF"
            setTextColor(if (joinLines) Color.parseColor("#111214") else Color.parseColor("#CBD5E1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = pillDrawable(
                if (joinLines) "#E2E8F0" else "#23262E",
                "#3A404D"
            )
            setOnClickListener {
                joinLines = !joinLines
                text = if (joinLines) "Join Lines: ON" else "Join Lines: OFF"
                setTextColor(if (joinLines) Color.parseColor("#111214") else Color.parseColor("#CBD5E1"))
                background = pillDrawable(
                    if (joinLines) "#E2E8F0" else "#23262E",
                    "#3A404D"
                )
                editText.setText(OcrProcessor.formatText(rawText, joinLines))
            }
        }
        headerRow.addView(joinChip)
        card.addView(headerRow)

        card.addView(
            editText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(14)
            }
        )

        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val resnipBtn = TextView(context).apply {
            text = "Re-snip"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(12), dp(9), dp(12), dp(9))
            setOnClickListener {
                hideResultCard()
                canvasView?.clearSelection()
            }
        }
        actionRow.addView(resnipBtn)

        val shareBtn = TextView(context).apply {
            text = "Share"
            setTextColor(Color.parseColor("#E2E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = pillDrawable("#23262E", "#38FFFFFF")
            setOnClickListener {
                val txt = editText.text.toString()
                dismiss()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, txt)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share grabbed text").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
        actionRow.addView(
            shareBtn,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
        )

        val copyBtn = TextView(context).apply {
            text = "Copy Text"
            setTextColor(Color.parseColor("#111214"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(18), dp(9), dp(18), dp(9))
            background = pillDrawable("#F4F5F7", "#F4F5F7")
            setOnClickListener {
                val txt = editText.text.toString()
                OcrProcessor.copyToClipboard(context, txt)
                vibrateLight(store.loadSettings().hapticEnabled)
                hideResultCard()
                showToastPill("Copied • ${txt.length} chars", autoDismiss = true)
            }
        }
        actionRow.addView(copyBtn)
        card.addView(actionRow)

        resultCardContainer = card
        root.addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply {
                leftMargin = dp(16)
                rightMargin = dp(16)
                bottomMargin = dp(24)
            }
        )

        card.alpha = 0f
        card.translationY = dp(40).toFloat()
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideResultCard() {
        val root = rootLayout ?: return
        resultCardContainer?.let { card ->
            root.removeView(card)
        }
        resultCardContainer = null
        layoutParams?.let { params ->
            params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            try {
                windowManager.updateViewLayout(root, params)
            } catch (_: Exception) {
            }
        }
    }

    private fun vibrateLight(enabled: Boolean) {
        if (!enabled) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(22L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun pillDrawable(fillHex: String, strokeHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(99).toFloat()
            setColor(Color.parseColor(fillHex))
            setStroke(dp(1), Color.parseColor(strokeHex))
        }
    }

    private fun cardDrawable(fillHex: String, strokeHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setColor(Color.parseColor(fillHex))
            setStroke(dp(1), Color.parseColor(strokeHex))
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).roundToInt()
    }
}

private class SelectionCanvasView(
    context: Context,
    private val bitmap: Bitmap,
    private val instantGrab: Boolean,
    private val onSelectionComplete: (RectF) -> Unit,
    private val onSelectionStarted: () -> Unit
) : View(context) {

    private var scrimAlpha = 0f
    private var scanning = false
    private var scanProgress = 0f
    private var scanAnimator: ValueAnimator? = null

    private var startX = 0f
    private var startY = 0f
    private var hasSelection = false
    private val selRect = RectF()
    private val viewBounds = RectF()

    private enum class DragMode { NONE, NEW_BOX, MOVE, TL, TR, BL, BR }
    private var dragMode = DragMode.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val density = context.resources.displayMetrics.density
    private val handleRadius = 24f * density
    private val cornerLen = 16f * density
    private val cornerRadius = 10f * density

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(158, 8, 10, 14)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88F4F5F7")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F4F5F7")
        style = Paint.Style.STROKE
        strokeWidth = 3.2f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC14161A")
        style = Paint.Style.FILL
    }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        textSize = 11.5f * density
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    fun animateIntro() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                scrimAlpha = it.animatedValue as Float
                scrimPaint.alpha = (158 * scrimAlpha).roundToInt()
                invalidate()
            }
            start()
        }
    }

    fun setSelectionRect(rect: RectF) {
        selRect.set(rect)
        hasSelection = true
        invalidate()
    }

    fun clearSelection() {
        hasSelection = false
        selRect.setEmpty()
        invalidate()
    }

    fun currentSelection(): RectF? = if (hasSelection) RectF(selRect) else null

    fun setScanning(active: Boolean) {
        scanning = active
        scanAnimator?.cancel()
        if (active) {
            scanAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 950L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    scanProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        scanAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        viewBounds.set(0f, 0f, width.toFloat(), height.toFloat())
        if (!bitmap.isRecycled) {
            canvas.drawBitmap(bitmap, null, viewBounds, null)
        }

        if (!hasSelection || selRect.width() < 2f || selRect.height() < 2f) {
            canvas.drawRect(viewBounds, scrimPaint)
            return
        }

        canvas.drawRect(0f, 0f, width.toFloat(), selRect.top, scrimPaint)
        canvas.drawRect(0f, selRect.bottom, width.toFloat(), height.toFloat(), scrimPaint)
        canvas.drawRect(0f, selRect.top, selRect.left, selRect.bottom, scrimPaint)
        canvas.drawRect(selRect.right, selRect.top, width.toFloat(), selRect.bottom, scrimPaint)

        canvas.drawRoundRect(selRect, cornerRadius, cornerRadius, borderPaint)

        val l = selRect.left
        val t = selRect.top
        val r = selRect.right
        val b = selRect.bottom
        val cl = min(cornerLen, min(selRect.width() / 2.5f, selRect.height() / 2.5f))

        canvas.drawLine(l, t + cl, l, t, bracketPaint)
        canvas.drawLine(l, t, l + cl, t, bracketPaint)
        canvas.drawLine(r - cl, t, r, t, bracketPaint)
        canvas.drawLine(r, t, r, t + cl, bracketPaint)
        canvas.drawLine(l, b - cl, l, b, bracketPaint)
        canvas.drawLine(l, b, l + cl, b, bracketPaint)
        canvas.drawLine(r - cl, b, r, b, bracketPaint)
        canvas.drawLine(r, b - cl, r, b, bracketPaint)

        val dimText = "${selRect.width().roundToInt()} × ${selRect.height().roundToInt()}"
        val tw = badgeTextPaint.measureText(dimText)
        val padX = 8f * density
        val padY = 4f * density
        val badgeTop = max(8f * density, t - 26f * density)
        val badgeRect = RectF(l, badgeTop, l + tw + padX * 2, badgeTop + 20f * density)
        canvas.drawRoundRect(badgeRect, 10f * density, 10f * density, badgeBgPaint)
        canvas.drawText(dimText, badgeRect.left + padX, badgeRect.bottom - padY - 1f * density, badgeTextPaint)

        if (scanning) {
            val scanY = t + (b - t) * scanProgress
            val beamHeight = 28f * density
            val beamTop = max(t, scanY - beamHeight / 2f)
            val beamBottom = min(b, scanY + beamHeight / 2f)
            scanPaint.shader = LinearGradient(
                l, beamTop, l, beamBottom,
                intArrayOf(Color.TRANSPARENT, Color.argb(120, 244, 245, 247), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(l, beamTop, r, beamBottom, scanPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (scanning) return true
        val x = event.x.coerceIn(0f, width.toFloat())
        val y = event.y.coerceIn(0f, height.toFloat())

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onSelectionStarted()
                if (!instantGrab && hasSelection) {
                    dragMode = hitTest(x, y)
                    if (dragMode == DragMode.NEW_BOX) {
                        startNewBox(x, y)
                    }
                } else {
                    dragMode = DragMode.NEW_BOX
                    startNewBox(x, y)
                }
                lastTouchX = x
                lastTouchY = y
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastTouchX
                val dy = y - lastTouchY
                when (dragMode) {
                    DragMode.NEW_BOX -> {
                        selRect.set(
                            min(startX, x),
                            min(startY, y),
                            max(startX, x),
                            max(startY, y)
                        )
                    }
                    DragMode.MOVE -> {
                        val w = selRect.width()
                        val h = selRect.height()
                        val newL = (selRect.left + dx).coerceIn(0f, width - w)
                        val newT = (selRect.top + dy).coerceIn(0f, height - h)
                        selRect.set(newL, newT, newL + w, newT + h)
                    }
                    DragMode.TL -> {
                        selRect.left = min(x, selRect.right - 32f * density)
                        selRect.top = min(y, selRect.bottom - 32f * density)
                    }
                    DragMode.TR -> {
                        selRect.right = max(x, selRect.left + 32f * density)
                        selRect.top = min(y, selRect.bottom - 32f * density)
                    }
                    DragMode.BL -> {
                        selRect.left = min(x, selRect.right - 32f * density)
                        selRect.bottom = max(y, selRect.top + 32f * density)
                    }
                    DragMode.BR -> {
                        selRect.right = max(x, selRect.left + 32f * density)
                        selRect.bottom = max(y, selRect.top + 32f * density)
                    }
                    DragMode.NONE -> Unit
                }
                lastTouchX = x
                lastTouchY = y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragMode = DragMode.NONE
                if (selRect.width() >= 18f * density && selRect.height() >= 18f * density) {
                    onSelectionComplete(RectF(selRect))
                } else {
                    clearSelection()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startNewBox(x: Float, y: Float) {
        startX = x
        startY = y
        hasSelection = true
        selRect.set(x, y, x, y)
    }

    private fun hitTest(x: Float, y: Float): DragMode {
        if (abs(x - selRect.left) < handleRadius && abs(y - selRect.top) < handleRadius) return DragMode.TL
        if (abs(x - selRect.right) < handleRadius && abs(y - selRect.top) < handleRadius) return DragMode.TR
        if (abs(x - selRect.left) < handleRadius && abs(y - selRect.bottom) < handleRadius) return DragMode.BL
        if (abs(x - selRect.right) < handleRadius && abs(y - selRect.bottom) < handleRadius) return DragMode.BR
        if (selRect.contains(x, y)) return DragMode.MOVE
        return DragMode.NEW_BOX
    }
}
