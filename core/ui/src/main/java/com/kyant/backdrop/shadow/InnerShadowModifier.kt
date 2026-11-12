package com.kyant.backdrop.shadow

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.backdrop.internal.ShapeProvider
import com.kyant.backdrop.internal.clipOutline
import com.kyant.backdrop.isRenderEffectSupported

internal class InnerShadowElement(
    val shapeProvider: ShapeProvider,
    val shadow: () -> InnerShadow?
) : ModifierNodeElement<InnerShadowNode>() {

    override fun create(): InnerShadowNode {
        return InnerShadowNode(shapeProvider, shadow)
    }

    override fun update(node: InnerShadowNode) {
        node.shapeProvider = shapeProvider
        node.shadow = shadow
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "innerShadow"
        properties["shapeProvider"] = shapeProvider
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InnerShadowElement) return false

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

internal class InnerShadowNode(
    var shapeProvider: ShapeProvider,
    var shadow: () -> InnerShadow?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var shadowLayer: GraphicsLayer? = null

    private val paint = Paint()
    private var clipPath: Path? = null

    private var prevRadius = Float.NaN
    private var cachedSize = Size.Unspecified
    private var cachedDensity = Float.NaN
    private var cachedFontScale = Float.NaN
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedShape: Shape? = null
    private var cachedShadow: InnerShadow? = null
    private var cachedOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        drawContent()

        if (!isRenderEffectSupported()) return

        val shadow = shadow() ?: return
        if (shadow.alpha <= 0f || shadow.color.alpha <= 0f) return

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
                shadow != cachedShadow ||
                cachedOutline == null
            ) {
                val densityObj: Density = this
                val radius = shadow.radius.toPx()
                val offsetX = shadow.offset.x.toPx()
                val offsetY = shadow.offset.y.toPx()

                val outline = shape.createOutline(size, layoutDir, densityObj)
                val targetClipPath =
                    if (outline is Outline.Rounded) {
                        clipPath ?: Path().also { clipPath = it }
                    } else {
                        null
                    }

                configurePaint(shadow)

                shadowLayer.alpha = shadow.alpha
                shadowLayer.blendMode = shadow.blendMode
                if (prevRadius != radius) {
                    shadowLayer.renderEffect =
                        if (radius > 0f) {
                            BlurEffect(radius, radius, TileMode.Decal)
                        } else {
                            null
                        }
                    prevRadius = radius
                }
                shadowLayer.record {
                    val canvas = drawContext.canvas
                    canvas.save()
                    canvas.clipOutline(outline, targetClipPath)
                    canvas.drawOutline(outline, paint)
                    canvas.translate(offsetX, offsetY)
                    canvas.drawOutline(outline, ShadowMaskPaint)
                    canvas.translate(-offsetX, -offsetY)
                    canvas.restore()
                }

                cachedSize = size
                cachedDensity = density
                cachedFontScale = fontScale
                cachedLayoutDirection = layoutDir
                cachedShape = shape
                cachedShadow = shadow
                cachedOutline = outline
            }

            val outline = cachedOutline ?: return
            val targetClipPath = if (outline is Outline.Rounded) clipPath else null
            val canvas = drawContext.canvas
            canvas.save()
            canvas.clipOutline(outline, targetClipPath)
            drawLayer(shadowLayer)
            canvas.restore()
        }
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
        prevRadius = Float.NaN
        cachedSize = Size.Unspecified
        cachedDensity = Float.NaN
        cachedFontScale = Float.NaN
        cachedLayoutDirection = null
        cachedShape = null
        cachedShadow = null
        cachedOutline = null
    }

    private fun DrawScope.configurePaint(shadow: InnerShadow) {
        paint.color = shadow.color
    }
}

private val ShadowMaskPaint = Paint().apply {
    blendMode = BlendMode.Clear
}
