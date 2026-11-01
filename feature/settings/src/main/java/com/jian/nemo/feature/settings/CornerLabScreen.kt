package com.jian.nemo.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.shape.RoundedCornerStyle
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.NemoDanger
import com.jian.nemo.core.designsystem.theme.NemoDangerDark
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoPrimaryDark
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class LabDisplayMode {
    SideBySide,
    Overlay,
    CornerMacro
}

private enum class ShapeAspect(val width: Dp, val height: Dp) {
    Card(260.dp, 120.dp),
    Square(120.dp, 120.dp),
    Capsule(260.dp, 56.dp)
}

@Composable
fun CornerLabScreen(
    onNavigateBack: () -> Unit
) {
    var radius by remember { mutableFloatStateOf(26f) }
    var displayMode by remember { mutableStateOf(LabDisplayMode.SideBySide) }
    var aspect by remember { mutableStateOf(ShapeAspect.Card) }
    var isStrokeOnly by remember { mutableStateOf(false) }
    var showComb by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = MaterialTheme.colorScheme.background

    // 规范色彩对：红色为 G1 标准圆角，蓝色为 G2 平滑圆角
    val redColor = if (isDark) NemoDangerDark else NemoDanger
    val blueColor = if (isDark) NemoPrimaryDark else NemoPrimary

    val redFill = if (isDark) redColor.copy(alpha = 0.25f) else redColor.copy(alpha = 0.15f)
    val blueFill = if (isDark) blueColor.copy(alpha = 0.25f) else blueColor.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. 底层滚动内容（被 backdrop 采样源录制）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .verticalScroll(scrollState)
                .padding(
                    top = statusBarTop + 60.dp + 16.dp,
                    bottom = navBarBottom + 24.dp
                )
        ) {
            Text(
                text = stringResource(R.string.corner_lab_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. 画布检视区域
            SettingsSectionTitle(
                text = stringResource(R.string.corner_lab_section_canvas),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = displayMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "displayModeAnim"
                    ) { mode ->
                        when (mode) {
                            LabDisplayMode.SideBySide -> {
                                SideBySideComparisonView(
                                    radius = radius.dp,
                                    aspect = aspect,
                                    isStrokeOnly = isStrokeOnly,
                                    showComb = showComb,
                                    redColor = redColor,
                                    blueColor = blueColor,
                                    redFill = redFill,
                                    blueFill = blueFill
                                )
                            }
                            LabDisplayMode.Overlay -> {
                                OverlayComparisonView(
                                    radius = radius.dp,
                                    aspect = aspect,
                                    isStrokeOnly = isStrokeOnly,
                                    showComb = showComb,
                                    redColor = redColor,
                                    blueColor = blueColor,
                                    redFill = redFill,
                                    blueFill = blueFill
                                )
                            }
                            LabDisplayMode.CornerMacro -> {
                                MacroZoomCornerView(
                                    radius = radius.dp,
                                    isStrokeOnly = isStrokeOnly,
                                    isDark = isDark,
                                    showComb = showComb,
                                    redColor = redColor,
                                    blueColor = blueColor,
                                    redFill = redFill,
                                    blueFill = blueFill
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 交互控制参数面板
            SettingsSectionTitle(
                text = stringResource(R.string.corner_lab_section_controls),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 半径滑块与数值
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.corner_lab_radius_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${radius.roundToInt()} dp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = blueColor
                        )
                    }

                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 0f..60f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = blueColor,
                            activeTrackColor = blueColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 常用推荐半径快捷档位
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(8f, 16f, 20f, 26f, 32f).forEach { preset ->
                            val isSelected = radius.roundToInt() == preset.roundToInt()
                            Surface(
                                shape = RoundedRectangle(8.dp),
                                color = if (isSelected) blueColor else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { radius = preset }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${preset.toInt()}dp",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            if (isDark) Color.Black else Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 曲率梳分析开关 (Curvature Comb)
                    Surface(
                        shape = RoundedRectangle(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.corner_lab_switch_comb),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.corner_lab_desc_comb),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = showComb,
                                onCheckedChange = { showComb = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = blueColor
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 对比视图模式选择
                    Text(
                        text = stringResource(R.string.corner_lab_label_view_mode),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeChip(
                            title = stringResource(R.string.corner_lab_mode_side_by_side),
                            icon = Icons.AutoMirrored.Rounded.CompareArrows,
                            selected = displayMode == LabDisplayMode.SideBySide,
                            activeColor = blueColor,
                            isDark = isDark,
                            onClick = { displayMode = LabDisplayMode.SideBySide },
                            modifier = Modifier.weight(1f)
                        )
                        ModeChip(
                            title = stringResource(R.string.corner_lab_mode_overlay),
                            icon = Icons.Rounded.Layers,
                            selected = displayMode == LabDisplayMode.Overlay,
                            activeColor = blueColor,
                            isDark = isDark,
                            onClick = { displayMode = LabDisplayMode.Overlay },
                            modifier = Modifier.weight(1f)
                        )
                        ModeChip(
                            title = stringResource(R.string.corner_lab_mode_macro),
                            icon = Icons.Rounded.ZoomIn,
                            selected = displayMode == LabDisplayMode.CornerMacro,
                            activeColor = blueColor,
                            isDark = isDark,
                            onClick = { displayMode = LabDisplayMode.CornerMacro },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 卡片形态选择
                    Text(
                        text = stringResource(R.string.corner_lab_label_aspect),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShapeAspect.entries.forEach { item ->
                            val label = when (item) {
                                ShapeAspect.Card -> stringResource(R.string.corner_lab_aspect_card)
                                ShapeAspect.Square -> stringResource(R.string.corner_lab_aspect_square)
                                ShapeAspect.Capsule -> stringResource(R.string.corner_lab_aspect_capsule)
                            }
                            val isSelected = aspect == item
                            Surface(
                                shape = RoundedRectangle(8.dp),
                                color = if (isSelected) blueColor else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { aspect = item }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            if (isDark) Color.Black else Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 绘制方式（填充 vs 描边）
                    Text(
                        text = stringResource(R.string.corner_lab_label_render),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RenderStyleChip(
                            title = stringResource(R.string.corner_lab_render_fill),
                            selected = !isStrokeOnly,
                            activeColor = blueColor,
                            isDark = isDark,
                            onClick = { isStrokeOnly = false },
                            modifier = Modifier.weight(1f)
                        )
                        RenderStyleChip(
                            title = stringResource(R.string.corner_lab_render_border),
                            selected = isStrokeOnly,
                            activeColor = blueColor,
                            isDark = isDark,
                            onClick = { isStrokeOnly = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. 几何原理与技术说明卡片
            SettingsSectionTitle(
                text = stringResource(R.string.corner_lab_section_continuity_spec),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.corner_lab_explanation_g1),
                        style = MaterialTheme.typography.bodySmall,
                        color = redColor,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.corner_lab_explanation_g2),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = blueColor,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop，对齐 ActivityHeatmapScreen 标准）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.95f),
                            backgroundColor.copy(alpha = 0.75f),
                            backgroundColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = stringResource(R.string.corner_lab_title),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp
            )
        }
    }
}

@Composable
private fun SideBySideComparisonView(
    radius: Dp,
    aspect: ShapeAspect,
    isStrokeOnly: Boolean,
    showComb: Boolean,
    redColor: Color,
    blueColor: Color,
    redFill: Color,
    blueFill: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 标准圆角 (Circular G1) - 红色
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.corner_lab_style_circular),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = redColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShapeBox(
                shape = RoundedCornerShape(radius),
                width = aspect.width,
                height = aspect.height,
                isStrokeOnly = isStrokeOnly,
                showComb = showComb,
                isContinuous = false,
                radius = radius,
                fillColor = redFill,
                strokeColor = redColor
            )
        }

        // 苹果平滑圆角 (Continuous G2) - 蓝色
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.corner_lab_style_continuous),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = blueColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShapeBox(
                shape = RoundedRectangle(radius, RoundedCornerStyle.Continuous),
                width = aspect.width,
                height = aspect.height,
                isStrokeOnly = isStrokeOnly,
                showComb = showComb,
                isContinuous = true,
                radius = radius,
                fillColor = blueFill,
                strokeColor = blueColor
            )
        }
    }
}

@Composable
private fun OverlayComparisonView(
    radius: Dp,
    aspect: ShapeAspect,
    isStrokeOnly: Boolean,
    showComb: Boolean,
    redColor: Color,
    blueColor: Color,
    redFill: Color,
    blueFill: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(aspect.width, aspect.height),
            contentAlignment = Alignment.Center
        ) {
            // 底层：标准圆角 (红色)
            ShapeBox(
                shape = RoundedCornerShape(radius),
                width = aspect.width,
                height = aspect.height,
                isStrokeOnly = isStrokeOnly,
                showComb = showComb,
                isContinuous = false,
                radius = radius,
                fillColor = redFill,
                strokeColor = redColor
            )

            // 顶层：苹果平滑圆角 (蓝色)
            ShapeBox(
                shape = RoundedRectangle(radius, RoundedCornerStyle.Continuous),
                width = aspect.width,
                height = aspect.height,
                isStrokeOnly = isStrokeOnly,
                showComb = showComb,
                isContinuous = true,
                radius = radius,
                fillColor = blueFill,
                strokeColor = blueColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 图例标注
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(redColor, RoundedRectangle(2.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.corner_lab_legend_circular),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(blueColor, RoundedRectangle(2.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.corner_lab_legend_continuous),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MacroZoomCornerView(
    radius: Dp,
    isStrokeOnly: Boolean,
    isDark: Boolean,
    showComb: Boolean,
    redColor: Color,
    blueColor: Color,
    redFill: Color,
    blueFill: Color
) {
    val macroSize = 180.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 标准圆角放大特写 (截取左上角) - 红色
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.corner_lab_macro_g1),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = redColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(macroSize)
                    .clip(RoundedRectangle(8.dp))
                    .background(redColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                    .border(1.dp, redColor.copy(alpha = 0.35f), RoundedRectangle(8.dp))
            ) {
                ShapeBox(
                    shape = RoundedCornerShape(radius * 2f),
                    width = macroSize * 1.6f,
                    height = macroSize * 1.6f,
                    isStrokeOnly = isStrokeOnly,
                    showComb = showComb,
                    isContinuous = false,
                    radius = radius * 2f,
                    fillColor = redFill,
                    strokeColor = redColor,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }

        // 苹果平滑圆角放大特写 (截取左上角) - 蓝色
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.corner_lab_macro_g2),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = blueColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(macroSize)
                    .clip(RoundedRectangle(8.dp))
                    .background(blueColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                    .border(1.dp, blueColor.copy(alpha = 0.35f), RoundedRectangle(8.dp))
            ) {
                ShapeBox(
                    shape = RoundedRectangle(radius * 2f, RoundedCornerStyle.Continuous),
                    width = macroSize * 1.6f,
                    height = macroSize * 1.6f,
                    isStrokeOnly = isStrokeOnly,
                    showComb = showComb,
                    isContinuous = true,
                    radius = radius * 2f,
                    fillColor = blueFill,
                    strokeColor = blueColor,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

@Composable
private fun ShapeBox(
    shape: Shape,
    width: Dp,
    height: Dp,
    isStrokeOnly: Boolean,
    showComb: Boolean = false,
    isContinuous: Boolean = false,
    radius: Dp = 0.dp,
    fillColor: Color,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }

    Box(
        modifier = modifier
            .size(width, height)
    ) {
        // 底层形状绘制
        if (isStrokeOnly) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.5.dp, strokeColor, shape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fillColor, shape)
                    .border(1.dp, strokeColor.copy(alpha = 0.5f), shape)
            )
        }

        // 顶层覆盖绘制：曲率梳 (Curvature Comb)
        if (showComb && radiusPx > 4f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCurvatureComb(
                    isContinuous = isContinuous,
                    radiusPx = radiusPx,
                    combColor = strokeColor
                )
            }
        }
    }
}

/**
 * 绘制转角曲率梳 (Curvature Comb)
 * 沿着左上角边缘向外绘制法线引线，梳齿长度直观映射局部曲率大小。
 */
private fun DrawScope.drawCurvatureComb(
    isContinuous: Boolean,
    radiusPx: Float,
    combColor: Color,
    maxCombHeight: Float = 26f,
    numSpikes: Int = 24
) {
    val envelopePath = Path()
    var isFirst = true

    for (i in 0..numSpikes) {
        val t = i.toFloat() / numSpikes.toFloat()
        // 左上角圆弧区间 (从垂直 180° 到 水平 270°)
        val angle = (PI * (1.0 + 0.5 * t)).toFloat()

        // 局部曲率高度映射
        val combHeight = if (isContinuous) {
            // G2: 平滑钟形过渡山峰 (两端接缝平缓切入归 0，45°中间达到平滑顶点)
            val envelope = sin(t * PI.toFloat())
            maxCombHeight * envelope * envelope
        } else {
            // G1: 恒定曲率断崖 (接缝处瞬间垂直断崖跳变，圆弧段恒定高度)
            maxCombHeight * 0.85f
        }

        val normalX = cos(angle)
        val normalY = sin(angle)

        // 曲线上的基准锚点
        val baseX = radiusPx + radiusPx * normalX
        val baseY = radiusPx + radiusPx * normalY

        // 沿法线向外的梳齿端点
        val spikeX = baseX + normalX * combHeight
        val spikeY = baseY + normalY * combHeight

        // 绘制单根梳齿法线
        drawLine(
            color = combColor.copy(alpha = 0.6f),
            start = Offset(baseX, baseY),
            end = Offset(spikeX, spikeY),
            strokeWidth = 1.5f
        )

        // 连缀梳齿外包络线 (Envelope)
        if (isFirst) {
            if (!isContinuous) {
                // G1 直角断崖起步：从基准线 (0) 瞬间直角拔起至梳齿顶端
                envelopePath.moveTo(baseX, baseY)
                envelopePath.lineTo(spikeX, spikeY)
            } else {
                envelopePath.moveTo(spikeX, spikeY)
            }
            isFirst = false
        } else {
            envelopePath.lineTo(spikeX, spikeY)
        }

        if (i == numSpikes && !isContinuous) {
            // G1 结束处直角瞬间跌落回基底
            envelopePath.lineTo(baseX, baseY)
        }
    }

    // 绘制外包络线
    drawPath(
        path = envelopePath,
        color = combColor,
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
}

@Composable
private fun ModeChip(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedRectangle(8.dp),
        color = if (selected) activeColor else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) {
                    if (isDark) Color.Black else Color.White
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    if (isDark) Color.Black else Color.White
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun RenderStyleChip(
    title: String,
    selected: Boolean,
    activeColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedRectangle(8.dp),
        color = if (selected) activeColor else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    if (isDark) Color.Black else Color.White
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
