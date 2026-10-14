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
 * ⚠️【重大避坑准则】避免系统底层渲染死锁闪退 (RenderThread SIGSEGV 11)
 * ----------------------------------------------------------------------------
 *
 * 1. 【严禁父子自包含图层采样】:
 *    在小米设备 (MIUI / HyperOS) 等底层硬件加速流水线 (libhwui.so) 中，当父容器 (如 Scaffold)
 *    挂载了 `Modifier.layerBackdrop(backdrop)` 录制图层，而其内部子节点 (如 topBar 里的按钮)
 *    又调用了 `drawBackdrop(backdrop)` 去读取该未完成图层时，会造成 Skia RenderNode 的自循环死锁，
 *    在 `MiBackgroundBlurBlend::preUpdateInfo` 阶段触发 SEGV_ACCERR 崩溃！
 *
 * 2. 【正确范式：同级兄弟节点原则 (Sibling Architecture)】:
 *    被采样的内容层必须与悬浮读取层处于同一层级（即兄弟关系）。
 *    - 正确做法：
 *      ```kotlin
 *      val backdrop = rememberLayerBackdrop()
 *      Box(modifier = Modifier.fillMaxSize()) {
 *          // 1. 被采样的底层内容（如滚动列表、图片、卡片）挂载 layerBackdrop
 *          Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
 *              LazyColumn(...)
 *          }
 *          // 2. 悬浮液态玻璃按钮作为同级浮层置于上层，读取 backdrop
 *          ColoredLiquidGlassButton(
 *              backdrop = backdrop,
 *              modifier = Modifier.align(Alignment.BottomCenter)...
 *          )
 *      }
 *      ```
 *
 * 3. 【顶栏与纯色背景按钮选型铁律 (必须使用带底色磨砂款 Frosted)】:
 *    - **光学原理与纯色失效**：
 *      透镜物理折射 [lens] 的本质是对底层像素进行光学位移、弯曲与放大。当底层具有复杂色彩、渐变或
 *      卡片文字时（如实验室背景或滚动列表上方），折射效果极其惊艳。但顶栏通常是纯白/单色背景，
 *      纯色像素无论如何折射位移，显示出来的依旧是纯色，导致【全透明款 Clear】在顶栏几乎看不出任何透镜感，
 *      反而显得空虚发灰、缺乏实体支撑。
 *    - **带底色磨砂款的最佳实践**：
 *      顶栏返回按钮、刷新按钮等独立圆形图标按钮，**必须使用 [FrostedLiquidGlassButton]**！
 *      借助默认 30% 纯白磨砂底色（Color.White.copy(0.3f)）与漫反射高光，能在单色顶栏上瞬间凝聚出
 *      温润剔透的悬浮玻璃圆盘实体感，图标对比度最高，视觉体验最为高级自然。
 *    - **独立安全渲染**：
 *      顶栏按钮请**始终保持 backdrop = null**，组件会自动使用安全独立渲染通道，绝不引发底层渲染死锁。
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
    surfaceColor: Color = Color.Unspecified,
    shape: Shape = Capsule(),
    border: BorderStroke? = null,
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
                    lens(12f.dp.toPx(), 24f.dp.toPx())
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
                        drawRect(tint.copy(alpha = 0.75f))
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
        isInteractive = isInteractive,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * 2. 带底色磨砂液态玻璃按钮 (Frosted Surface Liquid Glass Button)
 *
 * 在透镜折射的基础上，叠加一层轻柔半透明底色 (默认随主题自适应为透亮奶白半透或高级深灰半透)。
 * 无论背景多复杂都能保证极佳的文字可读性与温润触感。
 */
@Composable
fun FrostedLiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    surfaceColor: Color = Color.White.copy(alpha = 0.3f),
    shape: Shape = Capsule(),
    border: BorderStroke? = null,
    isInteractive: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        tint = Color.Unspecified,
        surfaceColor = surfaceColor,
        shape = shape,
        border = border,
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
    surfaceColor: Color = Color.Unspecified,
    backdrop: Backdrop? = null,
    shape: Shape = Capsule(),
    border: BorderStroke? = null,
    isInteractive: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        tint = tint,
        surfaceColor = surfaceColor,
        shape = shape,
        border = border,
        isInteractive = isInteractive,
        contentPadding = contentPadding,
        content = content
    )
}
