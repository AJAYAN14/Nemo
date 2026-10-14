package com.jian.nemo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.Capsule
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.ClearLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt

/**
 * 液态玻璃实验室 (Liquid Glass Lab Screen)
 *
 * 1:1 对标官方 Demo 项目 `AndroidLiquidGlass`，展示四款标准液态玻璃按钮。
 * 提供多层次绚彩装饰背景，且每个按钮均支持自由拖拽移动，以便全面检视物理透镜折射、高斯模糊与色彩活力。
 */
@Composable
fun LiquidGlassLabScreen(
    onNavigateBack: () -> Unit
) {
    val backdrop = rememberLayerBackdrop()

    Scaffold(
        topBar = {
            CommonHeader(
                title = "液态玻璃实验室",
                onBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ==========================================
            // 1. 底层视觉装饰背景层 (被 backdrop 采样源录制)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LabBackgroundDecorations()
            }

            // ==========================================
            // 2. 悬浮液态玻璃按钮层 (同级上层，读取 backdrop)
            // ==========================================
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部操作提示条
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "按住按钮可自由拖拽到不同背景上对比透镜折射",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4 款可自由拖拽的液态玻璃按钮 (1:1 对标 Demo 项目 ButtonsContent.kt)
                DraggableLiquidButtonItem(
                    initialOffset = Offset(0f, -140f),
                    alignment = Alignment.Center
                ) {
                    ClearLiquidGlassButton(
                        onClick = {},
                        backdrop = backdrop,
                        shape = Capsule(),
                        modifier = Modifier.height(48.dp)
                    ) {
                        BasicText(
                            "全透明液态按钮",
                            style = TextStyle(Color.Black, 15f.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }

                DraggableLiquidButtonItem(
                    initialOffset = Offset(0f, -50f),
                    alignment = Alignment.Center
                ) {
                    FrostedLiquidGlassButton(
                        onClick = {},
                        backdrop = backdrop,
                        surfaceColor = Color.White.copy(alpha = 0.3f),
                        shape = Capsule(),
                        modifier = Modifier.height(48.dp)
                    ) {
                        BasicText(
                            "带底色液态按钮",
                            style = TextStyle(Color.Black, 15f.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }

                DraggableLiquidButtonItem(
                    initialOffset = Offset(0f, 40f),
                    alignment = Alignment.Center
                ) {
                    ColoredLiquidGlassButton(
                        onClick = {},
                        backdrop = backdrop,
                        tint = Color(0xFF0088FF),
                        surfaceColor = Color.Unspecified,
                        shape = Capsule(),
                        modifier = Modifier.height(48.dp)
                    ) {
                        BasicText(
                            "彩色液态按钮 (蓝)",
                            style = TextStyle(Color.White, 15f.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }

                DraggableLiquidButtonItem(
                    initialOffset = Offset(0f, 130f),
                    alignment = Alignment.Center
                ) {
                    ColoredLiquidGlassButton(
                        onClick = {},
                        backdrop = backdrop,
                        tint = Color(0xFFFF8D28),
                        surfaceColor = Color.Unspecified,
                        shape = Capsule(),
                        modifier = Modifier.height(48.dp)
                    ) {
                        BasicText(
                            "彩色液态按钮 (橙)",
                            style = TextStyle(Color.White, 15f.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 可拖动液态玻璃按钮包装容器
 */
@Composable
private fun DraggableLiquidButtonItem(
    initialOffset: Offset = Offset.Zero,
    alignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit
) {
    var dragOffset by remember { mutableStateOf(initialOffset) }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                }
        ) {
            content()
        }
    }
}

/**
 * 实验室视觉装饰背景组件 (提供色彩对比强烈的折射样本)
 */
@Composable
private fun LabBackgroundDecorations() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 极光渐变光斑 1 (左上角蔚蓝与青翠)
        Box(
            modifier = Modifier
                .offset(x = (-40).dp, y = 40.dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF00C6FF).copy(alpha = 0.55f),
                            Color(0xFF0072FF).copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .blur(30.dp)
        )

        // 极光渐变光斑 2 (右中侧紫粉活力光斑)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 60.dp, y = (-20).dp)
                .size(260.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFF2A85).copy(alpha = 0.55f),
                            Color(0xFF8A2387).copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .blur(35.dp)
        )

        // 极光渐变光斑 3 (底部金橙落日微光)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 20.dp, y = 30.dp)
                .size(240.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFF8008).copy(alpha = 0.5f),
                            Color(0xFFFFC837).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
                .blur(35.dp)
        )

        // 中间主内容排版（多层高对比卡片与大文字）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部模拟学习卡片
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0072FF), Color(0xFF00C6FF)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Science,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "物理透镜光学折射样本",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Lens(12dp, 24dp) + Vibrancy + Blur",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 中部大字号高对比度艺术排版
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                Text(
                    text = "LIQUID GLASS",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "リアルタイム屈折レンダリング",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 底部模拟统计信息卡片
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "12 dp",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0072FF)
                        )
                        Text(
                            text = "透镜厚度",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "24 dp",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF8D28)
                        )
                        Text(
                            text = "折射范围",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "60 fps",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2ECC71)
                        )
                        Text(
                            text = "实时帧率",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
