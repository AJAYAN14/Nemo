package com.jian.nemo.feature.learning.presentation.review

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.common.CommonHeader

// --- 颜色定义 ---
private val NemoGreen = Color(0xFF50E3C2)
private val NemoOrange = Color(0xFFFF9F0A)

private const val MOCK_HEADER_TITLE = "今日到期复习"

/**
 * 复习准备屏幕组件 (UI/UX Pro Max)
 *
 * 逻辑接入：
 * 1. 列表数据来自 ViewModel (SessionPrepUiState.reviewItems)
 * 2. 样式升级：Premium Card, Squircle Icons
 * 3. 布局升级：悬浮按钮 (Floating Button)
 */
@Composable
fun SessionPrepScreen(
    onBack: () -> Unit = {},
    onStartReview: () -> Unit = {},
    viewModel: SessionPrepViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = MaterialTheme.colorScheme.background

    // [New] 监听生命周期，回到页面时自动刷新数据
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CommonHeader(
                title = MOCK_HEADER_TITLE,
                onBack = onBack,
                backgroundColor = backgroundColor
            )
        },
        containerColor = backgroundColor
        // Removed bottomBar to allow floating content
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = 100.dp // 增加底部内边距，防止被悬浮按钮遮挡
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. 复习统计卡片
                    item {
                        StatisticsSection(
                             reviewed = 0,
                             remaining = uiState.totalDueCount,
                             total = uiState.totalDueCount
                        )
                    }

                    // 2. 列表标题
                    item {
                        Text(
                            "今日待复习内容",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // 3. 复习列表项
                    items(uiState.reviewItems) { item ->
                        when (item) {
                            is ReviewPreviewItem.WordItem -> {
                                PreviewWordCard(
                                    japanese = item.word.japanese,
                                    hiragana = item.word.hiragana ?: "",
                                    chinese = item.word.chinese,
                                    level = item.word.level
                                )
                            }
                            is ReviewPreviewItem.GrammarItem -> {
                                PreviewGrammarCard(
                                    grammar = item.grammar.grammar,
                                    info = item.grammar.usages.firstOrNull()?.connection ?: "点击查看详情",
                                    level = item.grammar.grammarLevel
                                )
                            }
                        }
                    }

                    // 空状态
                    if (uiState.reviewItems.isEmpty()) {
                         item {
                             PremiumCard {
                                 Box(
                                     modifier = Modifier.fillMaxWidth().padding(32.dp),
                                     contentAlignment = Alignment.Center
                                 ) {
                                     Text("今日无待复习内容", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f))
                                 }
                             }
                         }
                    }
                }

                // 悬浮底部按钮
                // 背景是透明的(Box本身透明)，按钮悬浮在内容之上
                Box(
                     modifier = Modifier
                         .align(Alignment.BottomCenter)
                         .fillMaxWidth()
                         .padding(20.dp)
                         .safeDrawingPadding()
                 ) {
                     Button(
                         onClick = {
                             onStartReview()
                         },
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(56.dp)
                             .shadow(8.dp, RoundedCornerShape(28.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                         shape = RoundedCornerShape(28.dp),
                         colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                     ) {
                         Text("开始复习", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                     }
                 }
            }
        }
    }
}

/**
 * 统计区域卡片
 */
@Composable
private fun StatisticsSection(
    reviewed: Int,
    remaining: Int,
    total: Int
) {
    PremiumCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "复习统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItemSquircle(
                    value = reviewed.toString(),
                    label = "已复习",
                    icon = Icons.Rounded.CheckCircle,
                    color = NemoGreen,
                    modifier = Modifier.weight(1f)
                )

                StatItemSquircle(
                    value = remaining.toString(),
                    label = "剩余",
                    icon = Icons.Rounded.Schedule,
                    color = NemoOrange,
                    modifier = Modifier.weight(1f)
                )

                StatItemSquircle(
                    value = total.toString(),
                    label = "总计",
                    icon = Icons.AutoMirrored.Rounded.FormatListBulleted, // 使用 Rounded 变体
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 圆角矩形统计项 (Squircle Style)
 */
@Composable
private fun StatItemSquircle(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PreviewWordCard(
    japanese: String,
    hiragana: String,
    chinese: String,
    level: String
) {
    PremiumCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧色条装饰 (Optional detail)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = japanese,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = hiragana,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chinese,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            JlptTag(level = level)
        }
    }
}

@Composable
private fun PreviewGrammarCard(
    grammar: String,
    info: String,
    level: String
) {
    PremiumCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧色条装饰
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NemoGreen)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = grammar,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = info,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            JlptTag(level = level)
        }
    }
}

@Composable
private fun JlptTag(level: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = level,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Premium Card Composable (Copied for consistency)
 */
@Composable
private fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by if(onClick != null) {
         val isPressed by interactionSource.collectIsPressedAsState()
         animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            label = "cardScale",
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    val shadowElevation = if (isDark) 2.dp else 10.dp
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.03f)

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(26.dp),
                spotColor = shadowColor,
                ambientColor = shadowColor
            ),
        interactionSource = interactionSource,
        content = { Column(modifier = Modifier.padding(20.dp), content = content) }
    )
}
