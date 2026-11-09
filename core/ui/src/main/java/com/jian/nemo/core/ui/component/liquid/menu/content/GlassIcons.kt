package com.jian.nemo.core.ui.component.liquid.menu.content

import android.graphics.Path
import android.graphics.RectF

internal object GlassIcons {
    private val cache = HashMap<String, Path>()

    fun path(name: String): Path? = cache[name] ?: build(name)?.also { cache[name] = it }

    private fun build(name: String): Path? = when (name) {
        "check" -> Path().apply { moveTo(4.5f, 12.8f); lineTo(9.8f, 18.5f); lineTo(19.5f, 5.5f) }
        "grid" -> Path().apply {
            for (x in listOf(3.5f, 13.5f)) for (y in listOf(3.5f, 13.5f)) {
                addRoundRect(RectF(x, y, x + 7f, y + 7f), 1.8f, 1.8f, Path.Direction.CW)
            }
        }
        "list" -> Path().apply {
            for (y in listOf(6f, 12f, 18f)) {
                moveTo(8.5f, y); lineTo(21f, y)
                addCircle(3.9f, y, .35f, Path.Direction.CW)
            }
        }
        "plus" -> Path().apply { moveTo(12f, 4f); lineTo(12f, 20f); moveTo(4f, 12f); lineTo(20f, 12f) }
        "filter" -> Path().apply {
            moveTo(3.5f, 7f); lineTo(20.5f, 7f); moveTo(6.5f, 12f); lineTo(17.5f, 12f); moveTo(9.5f, 17f); lineTo(14.5f, 17f)
        }
        "ellipsis" -> Path().apply { for (x in listOf(5f, 12f, 19f)) addCircle(x, 12f, .9f, Path.Direction.CW) }
        "pause" -> Path().apply {
            moveTo(8f, 5.5f); lineTo(8f, 18.5f)
            moveTo(16f, 5.5f); lineTo(16f, 18.5f)
        }
        "trash" -> Path().apply {
            moveTo(4f, 6.5f); lineTo(20f, 6.5f)
            moveTo(9f, 6.5f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 6.5f)
            moveTo(6f, 6.5f); lineTo(7f, 20f); lineTo(17f, 20f); lineTo(18f, 6.5f)
            moveTo(10f, 10f); lineTo(10.3f, 16.5f); moveTo(14f, 10f); lineTo(13.7f, 16.5f)
        }
        "share" -> Path().apply {
            moveTo(12f, 15f); lineTo(12f, 3.5f); moveTo(8f, 7f); lineTo(12f, 3.5f); lineTo(16f, 7f)
            moveTo(8f, 10.5f); lineTo(5.5f, 10.5f); lineTo(5.5f, 20.5f); lineTo(18.5f, 20.5f); lineTo(18.5f, 10.5f); lineTo(16f, 10.5f)
        }
        "pencil" -> Path().apply {
            moveTo(4f, 20f); lineTo(5f, 15.5f); lineTo(16f, 4.5f); lineTo(19.5f, 8f); lineTo(8.5f, 19f); close()
            moveTo(14f, 6.5f); lineTo(17.5f, 10f)
        }
        "copy" -> Path().apply {
            addRoundRect(RectF(8.5f, 8.5f, 20f, 20f), 2.5f, 2.5f, Path.Direction.CW)
            moveTo(15.5f, 5.5f); lineTo(15.5f, 5f); quadTo(15.5f, 4f, 13.5f, 4f); lineTo(6f, 4f)
            quadTo(4f, 4f, 4f, 6f); lineTo(4f, 13.5f); quadTo(4f, 15.5f, 5.5f, 15.5f)
        }
        "heart" -> Path().apply {
            moveTo(12f, 20f)
            cubicTo(5f, 15f, 3f, 11.5f, 3f, 8.6f); cubicTo(3f, 6f, 5f, 4.2f, 7.4f, 4.2f); cubicTo(9.3f, 4.2f, 11f, 5.3f, 12f, 7f)
            cubicTo(13f, 5.3f, 14.7f, 4.2f, 16.6f, 4.2f); cubicTo(19f, 4.2f, 21f, 6f, 21f, 8.6f); cubicTo(21f, 11.5f, 19f, 15f, 12f, 20f); close()
        }
        "folder" -> Path().apply {
            moveTo(3.5f, 18f); lineTo(3.5f, 6.5f); quadTo(3.5f, 5f, 5f, 5f); lineTo(9.5f, 5f); lineTo(11.5f, 7.5f); lineTo(19f, 7.5f)
            quadTo(20.5f, 7.5f, 20.5f, 9f); lineTo(20.5f, 18f); quadTo(20.5f, 19.5f, 19f, 19.5f); lineTo(5f, 19.5f); quadTo(3.5f, 19.5f, 3.5f, 18f); close()
        }
        "sort" -> Path().apply {
            moveTo(8f, 19.5f); lineTo(8f, 4.5f); moveTo(4f, 8.5f); lineTo(8f, 4.5f); lineTo(12f, 8.5f)
            moveTo(16f, 4.5f); lineTo(16f, 19.5f); moveTo(12f, 15.5f); lineTo(16f, 19.5f); lineTo(20f, 15.5f)
        }
        "clock" -> Path().apply {
            addCircle(12f, 12f, 8.5f, Path.Direction.CW)
            moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15.5f, 14f)
        }
        "moon" -> Path().apply {
            moveTo(12f, 3f)
            cubicTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            cubicTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            cubicTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            cubicTo(21f, 11.39f, 20.94f, 10.79f, 20.82f, 10.21f)
            cubicTo(19.78f, 11.3f, 18.32f, 12f, 16.71f, 12f)
            cubicTo(13.27f, 12f, 10.48f, 9.21f, 10.48f, 5.77f)
            cubicTo(10.48f, 4.16f, 11.18f, 2.7f, 12.27f, 1.66f)
            cubicTo(12.18f, 1.66f, 12.09f, 1.65f, 12f, 1.65f)
            close()
        }
        "sun" -> Path().apply {
            addCircle(12f, 12f, 4.5f, Path.Direction.CW)
            moveTo(12f, 2f); lineTo(12f, 4.5f)
            moveTo(12f, 19.5f); lineTo(12f, 22f)
            moveTo(2f, 12f); lineTo(4.5f, 12f)
            moveTo(19.5f, 12f); lineTo(22f, 12f)
            moveTo(4.93f, 4.93f); lineTo(6.7f, 6.7f)
            moveTo(17.3f, 17.3f); lineTo(19.07f, 19.07f)
            moveTo(4.93f, 19.07f); lineTo(6.7f, 17.3f)
            moveTo(17.3f, 6.7f); lineTo(19.07f, 4.93f)
        }
        else -> null
    }
}
