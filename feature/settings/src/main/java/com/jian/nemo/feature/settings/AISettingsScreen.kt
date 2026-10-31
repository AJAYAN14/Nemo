@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
package com.jian.nemo.feature.settings

import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clip
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import androidx.compose.ui.res.painterResource
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.jian.nemo.core.designsystem.R as DesignR
import com.jian.nemo.core.domain.repository.AIConfig

@Composable
fun AISettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AISettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val hazeState = remember { HazeState() }

    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    var configToDelete by remember { mutableStateOf<AIConfig?>(null) }

    LaunchedEffect(uiState.testResult) {
        if (uiState.testResult != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val backdrop = rememberLayerBackdrop()
    val backgroundColor = MaterialTheme.colorScheme.background
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. 底层列表内容 (挂载 layerBackdrop，全屏向上贯穿至状态栏)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NemoChasingDotsLoader(size = 40.dp)
                }
            } else if (uiState.configs.isEmpty()) {
                EmptyAIConfigView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = statusBarTop + 60.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = statusBarTop + 64.dp,
                        bottom = navigationBarHeight + 32.dp
                    )
                ) {
                        item {
                            val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
                            PremiumCard(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isPlumpDuo) {
                                        Box(
                                            modifier = Modifier.size(46.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            PlumpDuoIcon(
                                                icon = PlumpIcon.AiBot,
                                                color = IsoluminantPalette.GlazeMint,
                                                size = 36.dp
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    RoundedCornerShape(14.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "AI 智能工坊",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "配置专属密钥，解锁AI单词智能解析、实时翻译与发音评估等强大功能。",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            SettingsSectionTitle(text = "配置列表")
                        }
                        items(uiState.configs, key = { it.id }) { config ->
                            val isActive = uiState.activeConfigId == config.id
                            SwipeableActionTile(
                                onDelete = { configToDelete = config }
                            ) {
                                AIConfigCard(
                                    config = config,
                                    isActive = isActive,
                                    isDark = isDark,
                                    isTesting = uiState.testingConfigId == config.id,
                                    onSelect = { viewModel.onEvent(AISettingsEvent.SelectActiveConfig(config.id)) },
                                    onEdit = { viewModel.onEvent(AISettingsEvent.OpenEditModal(config.id)) },
                                    onTest = { viewModel.onEvent(AISettingsEvent.TestConfig(config)) },
                                    onLongClick = { configToDelete = config }
                                )
                            }
                        }
                    }
                }
            }

        // 2. 悬浮液态玻璃顶栏 (多阶垂直渐变遮罩 + 30/48 黄金比例返回键与加号按钮)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.98f),
                            backgroundColor.copy(alpha = 0.88f),
                            backgroundColor.copy(alpha = 0.55f),
                            backgroundColor.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 16.dp)
        ) {
            CommonHeader(
                title = "AI 模型配置",
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                actions = {
                    FrostedLiquidGlassButton(
                        onClick = { viewModel.onEvent(AISettingsEvent.OpenEditModal(null)) },
                        backdrop = backdrop,
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        isInteractive = true,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "新建配置",
                            tint = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            )
        }

        // 全新 AI 神经算力核心独立全屏装配视界
        var lastEditingConfig by remember { mutableStateOf<AIConfig?>(null) }
        if (uiState.editingConfig != null) {
            lastEditingConfig = uiState.editingConfig
        }

        AnimatedVisibility(
            visible = uiState.editingConfig != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            val editingConfig = lastEditingConfig
            if (editingConfig != null) {
                AIModelStudioScreen(
                    config = editingConfig,
                    isNew = uiState.configs.none { it.id == editingConfig.id },
                    isTesting = uiState.isTesting,
                    testResult = uiState.testResult,
                    onConfigChange = { viewModel.onEvent(AISettingsEvent.UpdateEditingConfig(it)) },
                    onTestConnection = { viewModel.onEvent(AISettingsEvent.TestConnection) },
                    onSave = { viewModel.onEvent(AISettingsEvent.SaveConfig) },
                    onClose = { viewModel.onEvent(AISettingsEvent.CloseEditModal) }
                )
            }
        }

        // 测试状态气泡提示
        uiState.testResult?.let { result ->
            NemoSnackbar(
                visible = true,
                message = if (result.success) "API 连接正常，测试成功" else result.message,
                type = if (result.success) NemoSnackbarType.SUCCESS else NemoSnackbarType.ERROR,
                icon = if (result.success) Icons.Rounded.TaskAlt else Icons.Rounded.ErrorOutline,
                onDismiss = { viewModel.onEvent(AISettingsEvent.ClearTestResult) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarHeight + 8.dp)
            )
        }

        // 删除二次确认弹窗
        configToDelete?.let { config ->
            NemoDialog(
                onDismissRequest = { configToDelete = null },
                title = "确认删除此配置？",
                text = "您确认要删除 AI 配置 \"${config.name}\" 吗？删除后该配置的密钥和代理信息将永久丢失，且无法恢复。",
                confirmText = "确认删除",
                dismissText = "取消",
                isDangerous = true,
                onConfirm = {
                    viewModel.onEvent(AISettingsEvent.DeleteConfig(config.id))
                    configToDelete = null
                }
            )
        }
    }
}

/**
 * 借鉴 portal_labs SwipeableActionTile 理念的物理弹性侧滑卡片：
 * 1. 支持水平阻尼拖拽与弹簧弹性吸附；
 * 2. 向左滑动平滑露出高质感珊瑚红删除胶囊按键；
 * 3. 滑动过阈值触发震动感知；
 * 4. 点击卡片或右滑自动平滑回弹复位。
 */
@Composable
fun SwipeableActionTile(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val maxDragOffset = with(LocalDensity.current) { (-84).dp.toPx() }
    var hasHapticTicked by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 底层操作层：右对齐的删除操作胶囊按键
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            val progress = (-offsetX.value / -maxDragOffset).coerceIn(0f, 1.2f)
            val buttonScale = (0.7f + 0.3f * progress.coerceAtMost(1f))
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 400f)) }
                    onDelete()
                },
                shape = CircleShape,
                color = Color(0xFFF43F5E),
                shadowElevation = if (progress > 0.5f) 4.dp else 0.dp,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(46.dp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                        alpha = progress.coerceIn(0f, 1f)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "删除配置",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 前层卡片内容：跟随手指阻尼滑动
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hasHapticTicked = false
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value < maxDragOffset * 0.45f) {
                                    // 弹性吸附滑开
                                    offsetX.animateTo(maxDragOffset, spring(dampingRatio = 0.75f, stiffness = 400f))
                                } else {
                                    // 弹性关闭
                                    offsetX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 400f))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 400f))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = offsetX.value + dragAmount
                            val clamped = if (newOffset > 0f) {
                                newOffset * 0.15f
                            } else if (newOffset < maxDragOffset) {
                                val overscroll = maxDragOffset - newOffset
                                maxDragOffset - overscroll * 0.25f
                            } else {
                                newOffset
                            }
                            coroutineScope.launch {
                                offsetX.snapTo(clamped)
                            }
                            if (clamped <= maxDragOffset && !hasHapticTicked) {
                                hasHapticTicked = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (clamped > maxDragOffset) {
                                hasHapticTicked = false
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun AIConfigCard(
    config: AIConfig,
    isActive: Boolean,
    isDark: Boolean,
    isTesting: Boolean = false,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val platformColor = when (config.platform) {
        "deepseek" -> Color(0xFF0E68FF)
        "doubao" -> Color(0xFF00B4D8)
        "glm" -> Color(0xFF3762FF)
        "kimi" -> Color(0xFF007AFF)
        "mimo" -> Color(0xFFFF6A00)
        "qwen" -> Color(0xFF615CED)
        "claude" -> Color(0xFFD97706)
        "openai" -> Color(0xFF10A37F)
        "gemini" -> Color(0xFFAF52DE)
        else -> Color(0xFF8B5CF6)
    }
    
    val platformName = when (config.platform) {
        "deepseek" -> "DeepSeek"
        "doubao" -> "豆包"
        "glm" -> "智谱 GLM"
        "kimi" -> "Kimi"
        "mimo" -> "Mimo"
        "qwen" -> "通义千问"
        "claude" -> "Claude"
        "openai" -> "OpenAI"
        "gemini" -> "Gemini"
        else -> "自定义平台"
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "scale",
        animationSpec = tween(200)
    )

    val containerColor = if (isDark) {
        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color.White
    }

    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.2f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = borderColor
        ),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 原汁原味官方图标容器（完全无底色透明，保留原始矢量色彩）
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                val painterId = when (config.platform) {
                    "deepseek" -> DesignR.drawable.ic_deepseek
                    "doubao" -> DesignR.drawable.ic_doubao
                    "glm" -> DesignR.drawable.ic_glm
                    "kimi" -> DesignR.drawable.ic_kimi
                    "mimo" -> DesignR.drawable.ic_mimo
                    "qwen" -> DesignR.drawable.ic_qwen
                    "claude" -> DesignR.drawable.ic_claude
                    "openai" -> DesignR.drawable.ic_openai
                    "gemini" -> DesignR.drawable.ic_gemini
                    else -> null
                }
                
                if (painterId != null) {
                    val iconSize = when (config.platform) {
                        "gemini" -> 28.dp
                        "qwen" -> 27.dp
                        else -> 24.dp
                    }
                    Icon(
                        painter = painterResource(id = painterId),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = Color.Unspecified
                    )
                } else {
                    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
                    if (isPlumpDuo) {
                        PlumpDuoIcon(
                            icon = PlumpIcon.Cpu,
                            color = platformColor,
                            size = 28.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Cable,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = platformColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // middle details column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "当前启用",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "$platformName  ·  ${config.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
            
            // 操作按键区：测试在前，编辑在后，均包裹精致微胶囊底座
            // 1. 测试连通性按钮：品牌色微光圆环底座
            Surface(
                onClick = {
                    if (!isTesting) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTest()
                    }
                },
                enabled = !isTesting,
                shape = CircleShape,
                color = platformColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                border = BorderStroke(0.5.dp, platformColor.copy(alpha = 0.45f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = platformColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = "测试连通性",
                            tint = platformColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. 编辑按钮：浅冷灰微晶片底座
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEdit()
                },
                shape = CircleShape,
                color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f) else Color(0xFFF1F5F9),
                border = BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "编辑配置",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FormSectionCard(
    title: String,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        NemoNeutrals.Gray50.copy(alpha = 0.7f)
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.12f else 0.18f)

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 0.5.sp
            ),
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformDropdownSelector(
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit
) {
    val platforms = listOf(
        Triple("gemini", "Gemini", DesignR.drawable.ic_gemini),
        Triple("deepseek", "DeepSeek", DesignR.drawable.ic_deepseek),
        Triple("openai", "ChatGPT", DesignR.drawable.ic_openai),
        Triple("claude", "Claude", DesignR.drawable.ic_claude),
        Triple("doubao", "Doubao", DesignR.drawable.ic_doubao),
        Triple("mimo", "Mimo", DesignR.drawable.ic_mimo),
        Triple("custom", "自定义平台 (OpenAI 格式)", Icons.Rounded.Cable)
    )

    var expanded by remember { mutableStateOf(false) }
    val currentPlatform = platforms.find { it.first == selectedPlatform } ?: Triple("custom", "自定义平台 (OpenAI 格式)", Icons.Rounded.Cable)

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val inputBgColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            readOnly = true,
            value = currentPlatform.second,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.25f else 0.35f),
                focusedContainerColor = inputBgColor,
                unfocusedContainerColor = inputBgColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            leadingIcon = {
                val icon = currentPlatform.third
                if (icon is Int) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                } else if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                    val color = when (currentPlatform.first) {
                        "claude" -> Color(0xFFE05C2B)
                        "doubao" -> Color(0xFF00B4D8)
                        "mimo" -> Color(0xFFFF6700)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = color
                    )
                }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White)
        ) {
            platforms.forEach { (id, name, icon) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (icon is Int) {
                                Icon(
                                    painter = painterResource(id = icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Unspecified
                                )
                            } else if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                                val color = when (id) {
                                    "claude" -> Color(0xFFE05C2B)
                                    "doubao" -> Color(0xFF00B4D8)
                                    "mimo" -> Color(0xFFFF6700)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = color
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    },
                    onClick = {
                        onPlatformSelected(id)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendedModelChips(
    platform: String,
    currentModel: String,
    onModelSelected: (String) -> Unit
) {
    val suggestedModels = when (platform) {
        "gemini" -> listOf("gemini-3.1-pro-preview", "gemini-3-flash-preview", "gemini-3.1-flash-lite-preview")
        "deepseek" -> listOf("deepseek-v4-pro", "deepseek-v4-flash")
        "openai" -> listOf("gpt-5.5", "gpt-5.5-instant", "gpt-5.5-cyber")
        "claude" -> listOf("claude-4.7-opus", "claude-4.6-sonnet", "claude-4.5-haiku")
        "doubao" -> listOf("doubao-pro-256k", "doubao-lite-256k")
        "mimo" -> listOf("mimo-v2.5-pro", "mimo-v2.5", "mimo-v2-pro", "mimo-v2-flash")
        else -> emptyList()
    }

    val platformColor = when (platform) {
        "gemini" -> Color(0xFF6E56CF)
        "deepseek" -> Color(0xFF1E88E5)
        "openai" -> Color(0xFF10A37F)
        "claude" -> Color(0xFFE05C2B)
        "doubao" -> Color(0xFF00B4D8)
        "mimo" -> Color(0xFFFF6700)
        else -> MaterialTheme.colorScheme.primary
    }

    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    if (suggestedModels.isNotEmpty()) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
            Text(
                text = "推荐预设：",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedModels.forEach { model ->
                    val isSelected = currentModel == model
                    
                    val chipBgColor = if (isSelected) {
                        platformColor.copy(alpha = 0.15f)
                    } else {
                        if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White
                    }
                    
                    val chipBorderColor = if (isSelected) {
                        platformColor
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.2f else 0.3f)
                    }
                    
                    val chipTextColor = if (isSelected) {
                        platformColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBgColor)
                            .border(
                                width = if (isSelected) 1.2.dp else 1.dp,
                                color = chipBorderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onModelSelected(model)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = model,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = chipTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormBottomActionBar(
    isTesting: Boolean,
    canTest: Boolean,
    canSave: Boolean,
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit,
    isDark: Boolean
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 次按钮：验证连接 (占 38% 宽度)
        OutlinedButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTestClick()
            },
            enabled = canTest && !isTesting,
            modifier = Modifier
                .weight(0.38f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (canTest && !isTesting) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.3f)
                }
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f) else NemoNeutrals.Gray50,
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            if (isTesting) {
                NemoChasingDotsLoader(size = 16.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "测试中...", 
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "验证连接", 
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 主按钮：保存配置 (占 62% 宽度)
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSaveClick()
            },
            enabled = canSave && !isTesting,
            modifier = Modifier
                .weight(0.62f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "保存配置", 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AISettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPasswordField: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val inputBgColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White
    var passwordVisible by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    text = placeholder, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                ) 
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.25f else 0.35f),
                focusedContainerColor = inputBgColor,
                unfocusedContainerColor = inputBgColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            visualTransformation = if (isPasswordField && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPasswordField) {
                {
                    val image = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = image,
                            contentDescription = if (passwordVisible) "隐藏密钥" else "显示密钥",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null
        )
    }
}

/**
 * 空状态展示视图
 */
@Composable
private fun EmptyAIConfigView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "暂无 AI 模型配置",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右上角「+」添加您的第一个 AI 模型配置",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

