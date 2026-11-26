package dev.sjaramillo.nemi.ui

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.sjaramillo.nemi.R
import kotlin.math.roundToInt

internal data class Palette(
    val background: Int,
    val surface: Int,
    val text: Int,
    val accent: Int,
    val track: Int,
    val bar: Int,
) {
    val secondary: Int get() = text.withAlpha(0.72f)
    val divider: Int get() = text.withAlpha(0.10f)
}

internal val LightPalette =
    Palette(0xFFF8F7F2.toInt(), Color.WHITE, 0xFF202722.toInt(), 0xFF3D5948.toInt(), 0xFFECF2ED.toInt(), 0xFF49728C.toInt())
internal val DarkPalette =
    Palette(0xFF111914.toInt(), 0xFF1C2920.toInt(), 0xFFE6F0E7.toInt(), 0xFF9BDAAA.toInt(), 0xFF35483A.toInt(), 0xFF8EB4CD.toInt())

internal fun Int.withAlpha(alpha: Float): Int = (this and 0xFFFFFF) or ((255 * alpha).roundToInt() shl 24)

internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

internal fun Context.rounded(
    color: Int,
    radius: Int,
): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

internal fun View.pad(
    horizontal: Int,
    vertical: Int = horizontal,
) {
    setPadding(context.dp(horizontal), context.dp(vertical), context.dp(horizontal), context.dp(vertical))
}

internal fun Context.column(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

internal fun Context.row(): LinearLayout = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }

internal fun LinearLayout.item(
    view: View,
    top: Int = 0,
    height: Int = WRAP_CONTENT,
) {
    addView(view, LinearLayout.LayoutParams(MATCH_PARENT, height).apply { topMargin = context.dp(top) })
}

internal fun LinearLayout.weighted(view: View) {
    addView(view, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
}

internal fun Context.label(
    value: CharSequence,
    color: Int,
    size: Int = 16,
    weight: Int = 400,
    centered: Boolean = false,
): TextView =
    TextView(this).apply {
        text = value
        setTextColor(color)
        textSize = size.toFloat()
        typeface = Typeface.create(Typeface.DEFAULT, weight, false)
        includeFontPadding = false
        if (centered) gravity = Gravity.CENTER
    }

internal fun Context.ripple(
    color: Int,
    maskRadius: Int = 8,
    fill: Int? = null,
): RippleDrawable =
    RippleDrawable(
        ColorStateList.valueOf(color.withAlpha(0.12f)),
        fill?.let { rounded(it, maskRadius) },
        rounded(Color.WHITE, maskRadius),
    )

internal fun Context.action(
    title: String,
    colors: Palette,
    onClick: () -> Unit,
): Button =
    Button(this).apply {
        text = title
        isAllCaps = false
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, 600, false)
        includeFontPadding = false
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        minWidth = dp(48)
        minHeight = dp(48)
        minimumWidth = dp(48)
        minimumHeight = dp(48)
        pad(8, 4)
        stateListAnimator = null
        background = ripple(colors.accent)
        setTextColor(
            ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(colors.secondary, colors.accent),
            ),
        )
        setOnClickListener { onClick() }
    }

internal fun Context.card(
    title: Int,
    colors: Palette,
): LinearLayout =
    column().apply {
        background = rounded(colors.surface, 24)
        pad(20)
        item(label(getString(title), colors.text, 18, 600, centered = true).apply { isAccessibilityHeading = true })
        item(View(context), height = dp(12))
    }

internal fun Context.divider(colors: Palette): View = View(this).apply { setBackgroundColor(colors.divider) }

internal class ScreenLayout(
    context: Context,
    colors: Palette,
    title: Int,
    icon: Int,
    actionLabel: Int,
    actionAtStart: Boolean = false,
    onAction: () -> Unit,
) {
    val content = context.column().apply { pad(16) }
    val scroll =
        HeaderScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            addView(content, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
    private val toolbar =
        BackdropHeader(context, scroll, content, colors).apply {
            minimumHeight = context.dp(56)
            addView(
                context.label(context.getString(title), colors.text, 24, 600, centered = true).apply {
                    isAccessibilityHeading = true
                },
                FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER).apply {
                    marginStart = context.dp(48)
                    marginEnd = context.dp(48)
                },
            )
            addView(
                ImageButton(context).apply {
                    setImageResource(icon)
                    imageTintList = ColorStateList.valueOf(colors.accent)
                    contentDescription = context.getString(actionLabel)
                    background = context.ripple(colors.accent, 24)
                    pad(12)
                    setOnClickListener { onAction() }
                },
                FrameLayout.LayoutParams(
                    context.dp(48),
                    context.dp(48),
                    Gravity.CENTER_VERTICAL or if (actionAtStart) Gravity.START else Gravity.END,
                ),
            )
        }
    val root: FrameLayout =
        object : FrameLayout(context) {
            override fun onMeasure(
                widthMeasureSpec: Int,
                heightMeasureSpec: Int,
            ) {
                measureChildWithMargins(toolbar, widthMeasureSpec, 0, heightMeasureSpec, 0)
                if (scroll.paddingTop != toolbar.measuredHeight) {
                    scroll.setPadding(scroll.paddingLeft, toolbar.measuredHeight, scroll.paddingRight, scroll.paddingBottom)
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }

            override fun onDescendantInvalidated(
                child: View,
                target: View,
            ) {
                super.onDescendantInvalidated(child, target)
                if (child === scroll) toolbar.invalidate()
            }
        }.apply {
            setBackgroundColor(colors.background)
            addView(scroll, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(toolbar, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            scroll.setOnScrollChangeListener { _, _, _, _, _ -> toolbar.invalidate() }
            setOnApplyWindowInsetsListener { _, insets ->
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val gutter = context.dp(16)
                toolbar.minimumHeight = safe.top + context.dp(56)
                toolbar.setPadding(safe.left + gutter, safe.top, safe.right + gutter, 0)
                scroll.setPadding(safe.left, scroll.paddingTop, safe.right, safe.bottom)
                insets
            }
        }

    fun restoreScroll(position: Int) {
        if (position > 0) scroll.post { scroll.scrollTo(0, position) }
    }
}

internal class HeaderScrollView(context: Context) : ScrollView(context) {
    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int =
        scrollDeltaForPinnedHeader(rect.top, rect.bottom, scrollY + paddingTop, scrollY + height - paddingBottom)
}

internal fun scrollDeltaForPinnedHeader(
    targetTop: Int,
    targetBottom: Int,
    viewportTop: Int,
    viewportBottom: Int,
): Int {
    val targetHeight = targetBottom - targetTop
    val viewportHeight = viewportBottom - viewportTop
    return when {
        targetBottom > viewportBottom && targetTop > viewportTop ->
            if (targetHeight > viewportHeight) targetTop - viewportTop else targetBottom - viewportBottom
        targetTop < viewportTop && targetBottom < viewportBottom ->
            if (targetHeight > viewportHeight) targetBottom - viewportBottom else targetTop - viewportTop
        else -> 0
    }
}

private class BackdropHeader(
    context: Context,
    private val scroll: ScrollView,
    private val content: View,
    private val colors: Palette,
) : FrameLayout(context) {
    private val blurRadius = context.dp(14).toFloat()
    private val overscan = context.dp(42)
    private val backdrop =
        RenderNode("Header backdrop").apply {
            setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP))
        }
    private val tint = colors.background.withAlpha(0.50f)

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        if (canvas.isHardwareAccelerated && width > 0 && height > 0) {
            // Include the area below the toolbar so the blur kernel can sample across its bottom edge.
            backdrop.setPosition(0, 0, width, height + overscan)
            val recording = backdrop.beginRecording()
            try {
                recording.drawColor(colors.background)
                recording.translate(content.left.toFloat(), (content.top - scroll.scrollY).toFloat())
                content.draw(recording)
            } finally {
                backdrop.endRecording()
            }
            val checkpoint = canvas.save()
            canvas.clipRect(0, 0, width, height)
            canvas.drawRenderNode(backdrop)
            canvas.restoreToCount(checkpoint)
            canvas.drawColor(tint)
        } else {
            canvas.drawColor(tint)
        }
    }

    // Keep covered card controls from receiving taps through the toolbar.
    override fun onTouchEvent(event: MotionEvent): Boolean = true

    override fun onDetachedFromWindow() {
        backdrop.discardDisplayList()
        super.onDetachedFromWindow()
    }
}

internal fun showAppDialog(
    context: Context,
    colors: Palette,
    title: Int,
    message: String,
    onDismiss: () -> Unit,
): Dialog =
    Dialog(context).apply {
        val dialog = this
        val content =
            context.column().apply {
                background = context.rounded(colors.surface, 20)
                pad(20)
                item(context.label(context.getString(title), colors.text, 24, 600).apply { isAccessibilityHeading = true })
                item(context.label(message, colors.text), top = 8)
                item(context.action(context.getString(R.string.close), colors, dialog::dismiss), top = 8)
            }
        setContentView(ScrollView(context).apply { addView(content) })
        setOnDismissListener { onDismiss() }
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        show()
    }
