package com.jian.nemo.feature.learning.presentation.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.LearningPalette
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackground
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCardDark
import com.jian.nemo.core.designsystem.theme.NemoSurfaceSubcardDark
import com.jian.nemo.feature.learning.R

// 包含颜色信息的数据类
data class ColoredPath(
    val path: Path,
    val color: Color
)

@Composable
fun NemoWhiteboard(
    wordId: Int,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 8f,
    onClose: (() -> Unit)? = null
) {
    // 每次切换单词（wordId 变化）时，自动清空画板
    val paths = remember(wordId) { mutableStateListOf<ColoredPath>() }
    
    // 强制使用深/浅色模式自适应配置，模拟真实画板
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 预设笔刷颜色 (根据深浅色模式自适应)
    val penColors = remember(isDarkTheme) {
        if (isDarkTheme) {
            listOf(
                LearningPalette.WhiteboardPenPearlWhite, // 珍珠白 (默认)
                LearningPalette.WhiteboardPenNeonRed,    // 霓虹红
                LearningPalette.WhiteboardPenNeonBlue,   // 霓虹蓝
                LearningPalette.WhiteboardPenMintGreen   // 薄荷绿
            )
        } else {
            listOf(
                LearningPalette.WhiteboardPenInkBlack,   // 墨黑 (默认)
                LearningPalette.WhiteboardPenPureRed,    // 正红
                LearningPalette.WhiteboardPenLakeBlue,   // 湖蓝
                LearningPalette.WhiteboardPenGrassGreen  // 草绿
            )
        }
    }
    var currentColor by remember(wordId, isDarkTheme) { mutableStateOf(penColors[0]) }

    // 当前正在绘制的路径
    var currentPath by remember(wordId) { mutableStateOf<Path?>(null) }
    
    // 用于触发重绘的 State (仅触发 Draw 阶段，不触发 Recomposition，极大提升跟手性)
    var drawTrigger by remember(wordId) { mutableIntStateOf(0) }

    // 背景与工具栏暗色适配
    val boardBg = if (isDarkTheme) NemoSurfaceCardDark else Color.White
    val toolbarBg = if (isDarkTheme) NemoSurfaceSubcardDark.copy(alpha = 0.9f) else NemoSurfaceBackground.copy(alpha = 0.9f)
    val iconTint = if (isDarkTheme) NemoSurfaceBackground else LearningPalette.WhiteboardIconDark
    val iconBtnBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
    var isColorPaletteExpanded by remember(wordId) { mutableStateOf(false) }
    
    // 绘制样式
    val drawStyle = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .softCardShadow(borderRadius = 24.dp, isDark = isDarkTheme)
            .clip(RoundedCornerShape(24.dp))
            .background(boardBg)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(wordId) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = Path().apply {
                                moveTo(offset.x, offset.y)
                            }
                            drawTrigger++
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath?.lineTo(change.position.x, change.position.y)
                            drawTrigger++ // 通知 Canvas 重新绘制
                        },
                        onDragEnd = {
                            currentPath?.let {
                                paths.add(ColoredPath(path = it, color = currentColor))
                            }
                            currentPath = null
                        },
                        onDragCancel = {
                            currentPath = null
                        }
                    )
                }
        ) {
            // 读取 drawTrigger，使其与当前绘制作用域绑定
            drawTrigger.let { }

            // 绘制已经完成的路径
            paths.forEach { coloredPath ->
                drawPath(
                    path = coloredPath.path,
                    color = coloredPath.color,
                    style = drawStyle
                )
            }
            // 绘制当前正在滑动的路径
            currentPath?.let { path ->
                drawPath(
                    path = path,
                    color = currentColor,
                    style = drawStyle
                )
            }
        }

        // 顶部左侧工具栏 (退出/隐藏按钮，与清空按钮对称)
        if (onClose != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBtnBg)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_hide_whiteboard),
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 顶部工具栏 (清空按钮)
        if (paths.isNotEmpty() || currentPath != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBtnBg)
                    .clickable {
                        paths.clear()
                        currentPath = null
                        drawTrigger++
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.action_clear_whiteboard),
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 顶部工具栏 (折叠式颜色选择器：平时仅展示单色圆钮，点击展开可选列表)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .animateContentSize(animationSpec = tween(250))
                .background(toolbarBg, RoundedCornerShape(50))
                .clickable { isColorPaletteExpanded = !isColorPaletteExpanded }
                .padding(horizontal = if (isColorPaletteExpanded) 10.dp else 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isColorPaletteExpanded) {
                // 收起态：仅占用 32dp 的当前颜色微圆钮
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(
                                width = 1.5.dp,
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                    )
                }
            } else {
                // 展开态：展示全部笔刷颜色
                penColors.forEach { color ->
                    val isSelected = currentColor == color
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .clickable {
                                currentColor = color
                                isColorPaletteExpanded = false
                            }
                    )
                }
            }
        }
    }
}
