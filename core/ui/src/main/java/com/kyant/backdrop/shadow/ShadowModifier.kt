package com.kyant.backdrop.shadow

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.backdrop.internal.ShapeProvider
import com.kyant.backdrop.internal.blur
import kotlin.math.ceil

internal class ShadowElement(
    val shapeProvider: ShapeProvider,
    val shadow: () -> Shadow?
) : ModifierNodeElement<ShadowNode>() {

    override fun create(): ShadowNode {
        return ShadowNode(shapeProvider, shadow)
    }

    override fun update(node: ShadowNode) {
        node.shapeProvider = shapeProvider
        node.shadow = shadow
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shadow"
        properties["shapeProvider"] = shapeProvider
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShadowElement) return false

        if (shapeProvider != other.shapeProvider) return false
        if (shadow != other.shadow) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shapeProvider.hashCode()
        result = 31 * result + shadow.hashCode()
        return result
    }
}

internal class ShadowNode(
    var shapeProvider: ShapeProvider,
    var shadow: () -> Shadow?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var shadowLayer: GraphicsLayer? = null

    private val paint = Paint()
    private var cachedSize = Size.Unspecified
    private var cachedDensity = Float.NaN
    private var cachedFontScale = Float.NaN
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedShape: Shape? = null
    private var cachedShadow: Shadow? = null
    private var cachedRadiusPx = 0f

    override fun ContentDrawScope.draw() {
        val shadow = shadow()
        if (shadow == null || shadow.alpha <= 0f || shadow.color.alpha <= 0f) {
            return drawContent()
        }

        val shadowLayer = shadowLayer
        if (shadowLayer != null) {
            val size = size
            val shape = shapeProvider.shape
            val layoutDir = layoutDirection
            if (size != cachedSize ||
                density != cachedDensity ||
                fontScale != cachedFontScale ||
                layoutDir != cachedLayoutDirection ||
                shape != cachedShape ||
                shadow != cachedShadow
            ) {
                val densityObj: Density = this
                val radius = shadow.radius.toPx()
                val offsetX = shadow.offset.x.toPx()
                val offsetY = shadow.offset.y.toPx()
                val shadowSize = IntSize(
                    ceil(size.width + radius * 4f + offsetX).toInt(),
                    ceil(size.height + radius * 4f + offsetY).toInt()
                )
                val outline = shape.createOutline(size, layoutDir, densityObj)

                configurePaint(shadow)

                shadowLayer.alpha = shadow.alpha
                shadowLayer.blendMode = shadow.blendMode
                shadowLayer.record(shadowSize) {
                    translate(radius * 2f + offsetX, radius * 2f + offsetY) {
                        val canvas = drawContext.canvas
                        canvas.drawOutline(outline, paint)
                        canvas.translate(-offsetX, -offsetY)
                        canvas.drawOutline(outline, ShadowMaskPaint)
                        canvas.translate(offsetX, offsetY)
                    }
                }

                cachedSize = size
                cachedDensity = density
                cachedFontScale = fontScale
                cachedLayoutDirection = layoutDir
                cachedShape = shape
                cachedShadow = shadow
                cachedRadiusPx = radius
            }

            translate(-cachedRadiusPx * 2f, -cachedRadiusPx * 2f) {
                drawLayer(shadowLayer)
            }
        }

        drawContent()
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        shadowLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        shadowLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            shadowLayer = null
        }
        cachedSize = Size.Unspecified
        cachedDensity = Float.NaN
        cachedFontScale = Float.NaN
        cachedLayoutDirection = null
        cachedShape = null
        cachedShadow = null
        cachedRadiusPx = 0f
    }

    private fun DrawScope.configurePaint(shadow: Shadow) {
        paint.color = shadow.color
        paint.blur(shadow.radius.toPx())
    }
}

private val ShadowMaskPaint = Paint().apply {
    blendMode = BlendMode.Clear
}
