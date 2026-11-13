package com.jian.nemo.core.ui.component.liquid.menu.content

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import androidx.core.graphics.PathParser
import com.jian.nemo.core.ui.component.liquid.menu.records.GlassMenuItemRecord
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private object Metrics {
    const val MinimumWidth = 245f
    const val MaximumWidth = 300f
    const val ContentInsetY = 10f
    const val PlateInsetX = 8f
    const val RowHeight = 42f
    const val SubtitleExtra = 16f
    const val SeparatorBlock = 21f
    const val SeparatorInset = 24f
    const val HeadingHeight = 30f
    const val CheckCenter = 28f
    const val LeadingWithCheck = 44f
    const val IconCenterWithCheck = 55f
    const val TextAfterIconWithCheck = 80f
    const val LeadingPlain = 28f
    const val IconCenterPlain = 40f
    const val TextAfterIconPlain = 64f
    const val TrailingInset = 20f
    const val TitleSize = 17f
    const val SubtitleSize = 13f
    const val IconSize = 19.5f
}

internal class GlassMenuContent(
    private val context: Context,
    private val density: Float,
    private val iconView: (Int) -> View? = { null }
) {
    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val checkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconMatrix = Matrix()
    private val iconPath = Path()
    private var reservesCheck = false

    var rows: List<GlassRow> = emptyList()
        private set
    var width = 0f
        private set
    var height = 0f
        private set
    var dark = true

    fun setItems(items: List<GlassMenuItemRecord>, requestedWidth: Float?, maximumWidth: Float, fontFamily: String? = null) {
        val typeface = resolveTypeface(fontFamily)
        val fontScale = min(context.resources.configuration.fontScale, 1.35f)
        titlePaint.typeface = typeface
        subtitlePaint.typeface = typeface
        titlePaint.textSize = Metrics.TitleSize * density * fontScale * FontFit
        subtitlePaint.textSize = Metrics.SubtitleSize * density * fontScale * FontFit
        checkPaint.textSize = 14f * density * fontScale * FontFit
        titlePaint.letterSpacing = TitleTracking
        subtitlePaint.letterSpacing = 0f
        reservesCheck = items.any { it.checked != null }
        val built = items.mapIndexed { index, item -> GlassRow(item, index) }

        var maxNeeded = 0f
        for (row in built) {
            if (row.isSeparator) continue
            val titleW = titlePaint.measureText(row.item.title)
            val subtitleW = row.item.subtitle?.let { subtitlePaint.measureText(it) } ?: 0f
            val textW = max(titleW, subtitleW)
            val needed = leading(row.item) * density + textW + Metrics.TrailingInset * density
            maxNeeded = max(maxNeeded, needed)
        }
        val clampedWidest = maxNeeded.coerceIn(Metrics.MinimumWidth * density, Metrics.MaximumWidth * density)
        val safeMaxWidth = max(maximumWidth, clampedWidest)
        // 调用方传入的 requestedWidth 与 maximumWidth 同为像素值，此处直接使用，避免二次乘 density 导致菜单被放大至铺满全屏
        width = min(requestedWidth ?: ceil(clampedWidest), safeMaxWidth)

        var y = Metrics.ContentInsetY * density
        val rowScale = max(fontScale, 1f)
        for (row in built) {
            val rowHeight = when {
                row.isSeparator -> Metrics.SeparatorBlock
                row.isHeading -> Metrics.HeadingHeight * rowScale
                row.item.subtitle != null -> (Metrics.RowHeight + Metrics.SubtitleExtra) * rowScale
                else -> Metrics.RowHeight * rowScale
            } * density
            row.rect.set(Metrics.PlateInsetX * density, y, width - Metrics.PlateInsetX * density, y + rowHeight)
            y += rowHeight
            if (!row.isSeparator) {
                val available = max(width - leading(row.item) * density - Metrics.TrailingInset * density, 0f)
                row.title = if (available > 0f) {
                    TextUtils.ellipsize(row.item.title, titlePaint, available, TextUtils.TruncateAt.END)
                } else {
                    row.item.title
                }
                row.subtitle = row.item.subtitle?.let {
                    if (available > 0f) TextUtils.ellipsize(it, subtitlePaint, available, TextUtils.TruncateAt.END) else it
                }
            }
        }
        height = y + Metrics.ContentInsetY * density
        rows = built
    }

    private fun leading(item: GlassMenuItemRecord): Float = when {
        item.heading -> if (reservesCheck) Metrics.LeadingWithCheck else Metrics.LeadingPlain
        reservesCheck -> if (hasIcon(item)) Metrics.TextAfterIconWithCheck else Metrics.LeadingWithCheck
        else -> if (hasIcon(item)) Metrics.TextAfterIconPlain else Metrics.LeadingPlain
    }

    private fun hasIcon(item: GlassMenuItemRecord) = item.icon != null || item.iconPath != null || item.iconSlot != null

    private fun resolveTypeface(family: String?): Typeface {
        if (family != null) {
            val fromSystem = runCatching { Typeface.create(family, Typeface.NORMAL) }.getOrNull()
            if (fromSystem != null) return fromSystem
        }
        return runCatching { Typeface.createFromAsset(context.assets, "fonts/Inter-Regular.ttf") }
            .getOrDefault(Typeface.DEFAULT)
    }

    fun rowAt(x: Float, y: Float): GlassRow? =
        rows.firstOrNull { it.selectable && y >= it.rect.top && y < it.rect.bottom && x >= 0f && x <= width }

    fun nearestRow(x: Float, y: Float, slack: Float): GlassRow? {
        if (x < -slack || x > width + slack || y < -slack || y > height + slack) return null
        var best: GlassRow? = null
        var distance = Float.MAX_VALUE
        for (row in rows) {
            if (!row.selectable) continue
            val gap = when {
                y < row.rect.top -> row.rect.top - y
                y > row.rect.bottom -> y - row.rect.bottom
                else -> 0f
            }
            if (gap < distance) {
                distance = gap
                best = row
            }
        }
        return best
    }

    fun drawPlate(canvas: Canvas, plate: RectF, alpha: Float, lightX: Float, strength: Float, shine: Float) {
        if (alpha <= 0.001f) return
        val base = if (dark) PlateDark else PlateLight
        val edge = (base * alpha * strength).coerceIn(0f, 1f)
        val center = ((base + (if (dark) PlateShine else 0f) * shine) * alpha * strength).coerceIn(0f, 1f)
        val tone = if (dark) 235 else 60
        fun shade(opacity: Float) = Color.argb((opacity * 255f).toInt(), tone, tone, if (dark) 245 else 67)
        val radius = plate.height() / 2f
        fillPaint.color = Color.BLACK
        fillPaint.shader = RadialGradient(
            lightX.coerceIn(plate.left, plate.right), plate.centerY(), plate.width() * 0.62f,
            shade(center), shade(edge), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(plate, radius, radius, fillPaint)
        fillPaint.shader = null
    }

    fun draw(canvas: Canvas, scroll: Float) {
        val foreground = if (dark) Color.argb(245, 255, 255, 255) else Color.argb(235, 0, 0, 0)
        val muted = if (dark) Color.argb(153, 235, 235, 245) else Color.argb(153, 60, 60, 67)
        val destructive = if (dark) Color.rgb(255, 82, 84) else Color.rgb(220, 30, 48)
        canvas.save()
        canvas.translate(0f, -scroll)
        for (row in rows) {
            if (row.isSeparator) {
                fillPaint.color = if (dark) Color.argb(17, 255, 255, 255) else Color.argb(26, 0, 0, 0)
                val y = row.rect.centerY()
                val half = density / 2f
                canvas.drawRect(Metrics.SeparatorInset * density, y - half, width - Metrics.SeparatorInset * density, y + half, fillPaint)
                continue
            }
            val alpha = if (row.item.disabled) 0.38f else 1f
            val base = if (row.item.destructive) destructive else foreground
            val color = withAlpha(base, alpha)
            val left = leading(row.item) * density
            if (row.isHeading) {
                subtitlePaint.color = muted
                val baseline = row.rect.bottom - 8f * density
                canvas.drawText(row.title, 0, row.title.length, left, baseline, subtitlePaint)
                continue
            }
            val centerY = row.rect.centerY()
            val itemAccentColor = row.item.iconColor?.let { withAlpha(it, alpha) }
            if (row.item.checked == true) {
                val checkColor = itemAccentColor ?: color
                checkPaint.color = checkColor
                val checkMetrics = checkPaint.fontMetrics
                val checkBaseline = centerY - (checkMetrics.ascent + checkMetrics.descent) / 2f
                canvas.drawText("\u2714", Metrics.CheckCenter * density, checkBaseline, checkPaint)
            }
            if (hasIcon(row.item)) {
                val center = (if (reservesCheck) Metrics.IconCenterWithCheck else Metrics.IconCenterPlain) * density
                val slot = row.item.iconSlot
                if (slot != null) {
                    drawIconView(canvas, slot, center, centerY, alpha)
                } else {
                    val iconTint = itemAccentColor ?: color
                    drawIcon(canvas, row.item.icon, row.item.iconPath, center, centerY, Metrics.IconSize, iconTint, 1.45f)
                }
            }
            titlePaint.color = color
            val metrics = titlePaint.fontMetrics
            val subtitle = row.subtitle
            if (subtitle == null) {
                val baseline = centerY - (metrics.ascent + metrics.descent) / 2f
                canvas.drawText(row.title, 0, row.title.length, left, baseline, titlePaint)
            } else {
                canvas.drawText(row.title, 0, row.title.length, left, centerY - 2f * density, titlePaint)
                subtitlePaint.color = withAlpha(muted, alpha)
                canvas.drawText(subtitle, 0, subtitle.length, left, centerY + 16f * density, subtitlePaint)
            }
        }
        canvas.restore()
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((Color.alpha(color) * alpha).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    private fun drawIconView(canvas: Canvas, slot: Int, centerX: Float, centerY: Float, alpha: Float) {
        val view = iconView(slot) ?: return
        if (view.width == 0 || view.height == 0) return
        try {
            val save = if (alpha < 1f) {
                canvas.saveLayerAlpha(
                    centerX - view.width / 2f, centerY - view.height / 2f,
                    centerX + view.width / 2f, centerY + view.height / 2f, (alpha * 255f).toInt()
                )
            } else canvas.save()
            canvas.translate(centerX - view.width / 2f, centerY - view.height / 2f)
            view.draw(canvas)
            canvas.restoreToCount(save)
        } catch (_: Throwable) {
        }
    }

    private fun drawIcon(
        canvas: Canvas, name: String?, customPath: String?, centerX: Float, centerY: Float,
        size: Float, color: Int, weight: Float
    ) {
        val pixels = size * density
        val scale = pixels / 24f
        iconMatrix.setScale(scale, scale)
        iconMatrix.postTranslate(centerX - pixels / 2f, centerY - pixels / 2f)
        iconPath.reset()
        if (customPath != null) {
            val parsed = runCatching { PathParser.createPathFromPathData(customPath) }.getOrNull() ?: return
            parsed.transform(iconMatrix, iconPath)
            fillPaint.color = color
            canvas.drawPath(iconPath, fillPaint)
            return
        }
        val shape = GlassIcons.path(name ?: return) ?: return
        shape.transform(iconMatrix, iconPath)
        strokePaint.color = color
        strokePaint.strokeWidth = weight * density
        canvas.drawPath(iconPath, strokePaint)
    }

    private companion object {
        const val FontFit = 0.978f
        const val TitleTracking = -0.012f
        const val PlateDark = 0.155f
        const val PlateShine = 0.03f
        const val PlateLight = 0.09f
    }
}
