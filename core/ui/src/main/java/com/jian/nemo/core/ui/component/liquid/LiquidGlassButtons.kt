package com.jian.nemo.core.ui.component.liquid

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.jian.nemo.core.ui.component.Capsule
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * ============================================================================
 * 通用液态玻璃按钮架构规范与避坑指南 (Liquid Glass Design & Pitfall Handbook)
 * ============================================================================
 *
 * 核心设计对标官方 Demo 项目 `AndroidLiquidGlass`，封装 3 种开箱即用的标准液态玻璃按钮：
 * 1. [ClearLiquidGlassButton]   : 全透明液态玻璃按钮（纯透镜折射 + 高斯模糊 + 物理弹簧高光，无染色）
 * 2. [FrostedLiquidGlassButton] : 带底色磨砂液态玻璃按钮（默认叠加 30% 纯白轻磨砂底，文字与图标对比度极佳）
 * 3. [ColoredLiquidGlassButton] : 彩色液态玻璃按钮（BlendMode.Hue 色相注入，晶莹宝石质感，严禁加白底）
 *
 * ----------------------------------------------------------------------------
 * 🌟【真液态玻璃物理折射生效的四大必备法则】（想要真正看到玻璃折射必须这样做）
 * ----------------------------------------------------------------------------
 *
 * 为什么有时候感觉“没有玻璃效果”？因为液态玻璃的物理透镜折射（lens）、高斯模糊（blur）和色彩活力（vibrancy）
 * 依赖于 GPU 从真实的底层像素进行光学抓取与折射。要想看到惊艳的玻璃折射，必须满足以下 4 个条件：
 *
 * 1. 【必须显式传入非空采样源 (backdrop != null)】:
 *    若按钮的 `backdrop` 传入 `null`，底层渲染管道不会进行任何光学图层混合，按钮只会以安全独立模式运行，
 *    无法看到底层内容的透镜变形与折射！
 *    想要透镜折射，必须在父容器实例化 `val backdrop = rememberLayerBackdrop()` 并传给按钮 `backdrop = backdrop`。
 *
 * 2. 【被采样的底层内容必须挂载录制图层 (Modifier.layerBackdrop)】:
 *    底层卡片、滚动列表、富文本或图片容器，必须添加 `Modifier.layerBackdrop(backdrop)`。
 *    只有挂载了该修饰符，GPU 才会将底层视图渲染到离屏缓冲区供上层按钮采样。
 *
 * 3. 【必须遵守同级兄弟节点结构 (Sibling Architecture)】:
 *    底层被采样的容器与上层悬浮的按钮必须处于同一父容器（如同一个 Box）下的【兄弟节点】关系！
 *    ⚠️ 严禁父子自包含递归采样（例如给 Scaffold 挂 layerBackdrop，其内部 topBar 按钮又去采样它），
 *    这会在小米设备 (MIUI/HyperOS) 或 Android 14+ 的 Skia 渲染流水线中引发 RenderThread 死锁闪退 (SIGSEGV 11)！
 *
 * 4. 【严禁使用不透明像素盖死光学折射】:
 *    - 阻断层 1：底层与按钮之间【严禁放置 100% 不透明的渐变蒙层或纯色遮罩】（如旧版学习界面底部的 160.dp 纯色遮罩），
 *      否则会直接把底层的折射像素完全遮死。
 *    - 阻断层 2：按钮自身的 `surfaceColor` 【必须具备半透明度】（如 `containerColor.copy(alpha = 0.65f)` 或 `0.3f`）。
 *      若 `surfaceColor` 为完全不透明色（alpha = 1.0f），按钮表层直接被涂满，底层的折射渲染将被 100% 遮挡！
 *
 * ----------------------------------------------------------------------------
 * 📋【标准接入代码范本】（以学习卡片与底部悬浮评分按钮为例）
 * ----------------------------------------------------------------------------
 * ```kotlin
 * val cardBackdrop = rememberLayerBackdrop()
 *
 * Box(modifier = Modifier.fillMaxSize()) {
 *     // 1. 底层被采样的卡片/列表容器（挂载 layerBackdrop，不要有任何纯色遮罩盖住它）
 *     Box(
 *         modifier = Modifier
 *             .fillMaxSize()
 *             .layerBackdrop(cardBackdrop)
 *     ) {
 *         LearningCardContent(...)
 *     }
 *
 *     // 2. 悬浮在同级上层的液态玻璃按钮（传入同一个 cardBackdrop，底色带半透明）
 *     FrostedLiquidGlassButton(
 *         onClick = { ... },
 *         backdrop = cardBackdrop,
 *         surfaceColor = containerColor.copy(alpha = 0.65f), // 必须半透明！
 *         border = null,                                     // 磨砂玻璃推荐无描边
 *         modifier = Modifier.align(Alignment.BottomCenter)
 *     ) {
 *         Text("评分按钮")
 *     }
 * }
 * ```
 *
 * ----------------------------------------------------------------------------
 * 💡【何时保持 backdrop = null？】
 * ----------------------------------------------------------------------------
 * - **顶栏返回键/刷新键等独立按钮**：顶栏通常是纯白/单色背景，纯色像素无论怎样折射依然是纯色。
 *   顶栏按钮请【始终保持 backdrop = null】，直接使用 [FrostedLiquidGlassButton]，
 *   借助默认 30% 纯白磨砂底色（Color.White.copy(0.3f)）与漫反射高光凝聚出高级悬浮质感，安全且轻快。
 * ============================================================================
 */

/**
 * 通用液态玻璃按钮基础组件 (Core Liquid Glass Button)
 *
 * 移植并升级自 AndroidLiquidGlass 核心渲染引擎：
 * 1. 物理透镜折射 [lens] + 色彩活力 [vibrancy] + 柔化高斯模糊 [blur]；
 * 2. 触摸跟随弹簧形变与拖拽拉伸物理效果；
 * 3. 动态 [tint] 色相注入 (BlendMode.Hue) 与 [surfaceColor] 半透底色混合。
 *
 * @param onClick 点击事件回调
 * @param modifier 修饰符
 * @param backdrop 背景采样源 (可选，若为 null 则自动使用空采样并优雅呈现自适应微光质感)
 * @param tint 彩色液态玻璃着色 (如 Nemo 品牌蓝 #0E68FF、活力橙等)
 * @param surfaceColor 玻璃表面半透底色
 * @param shape 按钮轮廓形状，默认为药丸胶囊 [Capsule]
 * @param border 可选外边框
 * @param isInteractive 是否开启物理弹簧拖拽形变与触摸高光反馈
 * @param contentPadding 内容内边距
 * @param content 按钮内容
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    tint: Color = Color.Unspecified,
    tintAlpha: Float = 0.75f,
    surfaceColor: Color = Color.Unspecified,
    shape: Shape = Capsule(),
    border: BorderStroke? = null,
    hasLens: Boolean = true,
    chromaticAberration: Boolean = false,
    isInteractive: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val effectiveBackdrop = backdrop ?: remember { emptyBackdrop() }

    Row(
        modifier = modifier
            .then(
                if (border != null) Modifier.border(border, shape) else Modifier
            )
            .drawBackdrop(
                backdrop = effectiveBackdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    if (hasLens) {
                        lens(
                            12f.dp.toPx(),
                            24f.dp.toPx(),
                            chromaticAberration = chromaticAberration
                        )
                    }
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4f.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = tintAlpha))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * 1. 全透明液态玻璃按钮 (Clear Liquid Glass Button)
 *
 * 极致通透，纯粹基于背景透镜折射与高光，不添加色相与染色覆层。
 * 极简空气感，最适合悬浮于图片、动态壁纸或复杂卡片之上。
 */
@Composable
fun ClearLiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    shape: Shape = Capsule(),
    border: BorderStroke? = null,
    hasLens: Boolean = true,
    chromaticAberration: Boolean = false,
    isInteractive: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        tint = Color.Unspecified,
        surfaceColor = Color.Unspecified,
        shape = shape,
        border = border,
        hasLens = hasLens,
        chromaticAberration = chromaticAberration,
        isInteractive = isInteractive,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * 2. 带底色磨砂液态玻璃按钮 (Frosted Surface Liquid Glass Button)
 *
 * 在透镜折射的基础上，叠加一层轻柔半透明底色 (默认随主题自适应：浅色 30% 纯白磨砂，深色 15% 纯白微光)。
 * 无论背景多复杂都能保证极佳的文字可读性与温润触感。
 */
@Composable
fun FrostedLiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    surfaceColor: Color = Color.Unspecified,
    shape: Shape = Capsule(),
    border: BorderStroke? = null,
    hasLens: Boolean = true,
    chromaticAberration: Boolean = false,
    isInteractive: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val effectiveSurfaceColor = if (surfaceColor.isSpecified) {
        surfaceColor
    } else {
        Color.White.copy(alpha = if (isDark) 0.15f else 0.3f)
    }

    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        tint = Color.Unspecified,
        surfaceColor = effectiveSurfaceColor,
        shape = shape,
        border = border,
        hasLens = hasLens,
        chromaticAberration = chromaticAberration,
        isInteractive = isInteractive,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * 3. 彩色液态玻璃按钮 (Colored / Tinted Liquid Glass Button)
 *
 * 采用 BlendMode.Hue 色相图层注入，呈现如晶莹宝石、极光般的通透彩色流体质感。
 * 默认使用 Demo 原版标准纯正蓝 Color(0xFF0088FF)，可自由传入任意主题色。
 */
@Composable
fun ColoredLiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF0088FF),
    tintAlpha: Float = 0.75f,
    surfaceColor: Color = Color.Unspecified,
    backdrop: Backdrop? = null,
    shape: Shape = Capsule(),
    border: BorderStroke? = null,
    hasLens: Boolean = true,
    chromaticAberration: Boolean = false,
    isInteractive: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        tint = tint,
        tintAlpha = tintAlpha,
        surfaceColor = surfaceColor,
        shape = shape,
        border = border,
        hasLens = hasLens,
        chromaticAberration = chromaticAberration,
        isInteractive = isInteractive,
        contentPadding = contentPadding,
        content = content
    )
}
