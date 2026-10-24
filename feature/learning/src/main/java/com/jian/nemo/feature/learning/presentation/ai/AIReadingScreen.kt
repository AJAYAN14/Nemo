package com.jian.nemo.feature.learning.presentation.ai

import androidx.compose.animation.*
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.ui.modifier.softShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.AIReadingArticle
import com.jian.nemo.core.domain.model.ReadingQuestion
import com.jian.nemo.core.domain.model.ReadingVocabulary
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.graphics.Brush
import com.jian.nemo.core.ui.component.speaker.SpeakerButton
import com.jian.nemo.feature.learning.R
import kotlinx.coroutines.delay
import com.airbnb.lottie.compose.*
import androidx.compose.ui.res.painterResource
import com.jian.nemo.core.designsystem.R as DesignR
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.feature.learning.presentation.ai.components.AIWordTranslationSheet
import com.jian.nemo.feature.learning.presentation.ai.components.CustomTextToolbar
import com.jian.nemo.feature.learning.presentation.ai.components.AIAuroraBackground
import java.text.BreakIterator
import java.util.Locale

// AIReadingScreen 内部局部红色常量（BentoColors 不含红色，此处独立定义）
private val ReadingAccentRed = Color(0xFFEF4444)
private val ReadingIconBgRed = Color(0xFFFEE2E2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIReadingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: AIReadingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    var snackbarVisible by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarType by remember { mutableStateOf(NemoSnackbarType.SUCCESS) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val backdrop = rememberLayerBackdrop()
    val containerBg = if (isDark) colorScheme.background else BentoColors.BgBase

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerBg)
        ) {
        // 1. 底层页面滚动内容（被 backdrop 采样源录制，全屏贯穿至状态栏）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingScreen()
                }
                uiState.currentArticle == null && uiState.error != null -> {
                    ErrorScreen(
                        errorMsg = uiState.error!!,
                        onRetry = { viewModel.onEvent(AIReadingEvent.GenerateArticle) },
                        onBack = { viewModel.onEvent(AIReadingEvent.ClearError) }
                    )
                }
                uiState.currentArticle != null -> {
                    ArticleContentScreen(
                        article = uiState.currentArticle!!,
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        onShowCopySnackbar = { message ->
                            snackbarMessage = message
                            snackbarType = NemoSnackbarType.SUCCESS
                            snackbarVisible = true
                        }
                    )
                }
                else -> {
                    ConfigScreen(
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            containerBg.copy(alpha = 0.98f),
                            containerBg.copy(alpha = 0.88f),
                            containerBg.copy(alpha = 0.55f),
                            containerBg.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 16.dp)
        ) {
            CommonHeader(
                title = "AI 日语阅读",
                onBack = {
                    if (uiState.currentArticle != null) {
                        viewModel.onEvent(AIReadingEvent.ResetReader)
                    } else {
                        onNavigateBack()
                    }
                },
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FrostedLiquidGlassButton(
                            onClick = onNavigateToHistory,
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = "阅读历史",
                                modifier = Modifier.size(30.dp),
                                tint = colorScheme.onSurface
                            )
                        }
                        FrostedLiquidGlassButton(
                            onClick = { viewModel.onEvent(AIReadingEvent.QuickSwitchPlatform) },
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            val platform = uiState.aiPlatform
                            when (platform) {
                                "gemini" -> Icon(painterResource(DesignR.drawable.ic_gemini), contentDescription = "Gemini", modifier = Modifier.size(30.dp), tint = Color.Unspecified)
                                "deepseek" -> Icon(painterResource(DesignR.drawable.ic_deepseek), contentDescription = "DeepSeek", modifier = Modifier.size(30.dp), tint = Color.Unspecified)
                                "openai" -> Icon(painterResource(DesignR.drawable.ic_openai), contentDescription = "OpenAI", modifier = Modifier.size(30.dp), tint = Color.Unspecified)
                                "claude" -> Icon(painterResource(DesignR.drawable.ic_claude), contentDescription = "Claude", modifier = Modifier.size(30.dp), tint = Color.Unspecified)
                                "doubao" -> Icon(painterResource(DesignR.drawable.ic_doubao), contentDescription = "Doubao", modifier = Modifier.size(30.dp), tint = Color.Unspecified)
                                "mimo" -> Icon(painterResource(DesignR.drawable.ic_mimo), contentDescription = "Mimo", modifier = Modifier.size(30.dp), tint = Color.Unspecified)
                                else -> Icon(Icons.Rounded.Memory, contentDescription = "Custom", modifier = Modifier.size(30.dp), tint = colorScheme.onSurface)
                            }
                        }
                        FrostedLiquidGlassButton(
                            onClick = onNavigateToSettings,
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "AI设置",
                                modifier = Modifier.size(30.dp),
                                tint = colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }

        // 切换配置别名气泡提示（弹出后 2.5s 自动消失）
        AnimatedVisibility(
            visible = uiState.switchedConfigName != null,
            enter = fadeIn(androidx.compose.animation.core.tween(200)) +
                    slideInVertically(androidx.compose.animation.core.tween(200)) { -it },
            exit  = fadeOut(androidx.compose.animation.core.tween(300)) +
                    slideOutVertically(androidx.compose.animation.core.tween(300)) { -it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = statusBarTop + 68.dp, end = 12.dp)
        ) {
            uiState.switchedConfigName?.let { name ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.inverseSurface,
                    shadowElevation = 10.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = colorScheme.inverseOnSurface,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = colorScheme.inverseOnSurface
                        )
                    }
                }
            }
        }

        // 错误提示 Snackbar (只在非全屏错误状态下显示)
        if (uiState.currentArticle != null && uiState.error != null) {
            Snackbar(
                action = {
                    TextButton(onClick = { viewModel.onEvent(AIReadingEvent.ClearError) }) {
                        Text("确定", color = colorScheme.inversePrimary)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarBottom + 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(uiState.error ?: "")
            }
        }

        // 划词/点词翻译底部面板
        AIWordTranslationSheet(
            showSheet = uiState.showTranslationSheet,
            translatingText = uiState.translatingText,
            isTranslating = uiState.isTranslating,
            translationResult = uiState.translationResult,
            translationError = uiState.translationError,
            onDismiss = { viewModel.onEvent(AIReadingEvent.CloseTranslationSheet) },
            onSpeakWord = { word ->
                viewModel.onEvent(AIReadingEvent.SpeakText(word, "translate_word"))
            }
        )

        NemoSnackbar(
            visible = snackbarVisible,
            message = snackbarMessage,
            type = snackbarType,
            icon = Icons.Rounded.CheckCircle,
            onDismiss = { snackbarVisible = false },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarTop + 8.dp)
        )
    }
    }
}

/**
 * 1. 级别与主题选择配置页 (现代流体 Bento × 物理弹簧胶囊架构)
 */
@Composable
private fun ConfigScreen(
    uiState: AIReadingUiState,
    onEvent: (AIReadingEvent) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    val levels = remember { listOf("N5", "N4", "N3", "N2", "N1") }

    // 精选的 6 个核心阅读主题 (Apple 风格：极度克制、纯粹意象副标、大留白)
    val themes = remember {
        listOf(
            ThemeCardInfo(
                name = "日常生活",
                subtitle = "温暖生活琐事",
                icon = Icons.Rounded.Park,
                plumpIcon = PlumpIcon.Trees,
                isoluminantColor = IsoluminantPalette.EmeraldGreen,
                accent = BentoColors.AccentBlue,
                iconBg = BentoColors.IconBgBlue
            ),
            ThemeCardInfo(
                name = "传统文化",
                subtitle = "和风传统美学",
                icon = Icons.Rounded.Landscape,
                plumpIcon = PlumpIcon.Landmark,
                isoluminantColor = IsoluminantPalette.WarmAmber,
                accent = BentoColors.AccentOrange,
                iconBg = BentoColors.IconBgOrange
            ),
            ThemeCardInfo(
                name = "民间故事",
                subtitle = "经典奇幻物语",
                icon = Icons.Rounded.AutoAwesome,
                plumpIcon = PlumpIcon.Sparkles,
                isoluminantColor = IsoluminantPalette.LilacViolet,
                accent = BentoColors.AccentPurple,
                iconBg = BentoColors.IconBgPurple
            ),
            ThemeCardInfo(
                name = "美食料理",
                subtitle = "地道烟火风味",
                icon = Icons.Rounded.Restaurant,
                plumpIcon = PlumpIcon.Utensils,
                isoluminantColor = IsoluminantPalette.LakesideCyan,
                accent = BentoColors.GrammarPrimary,
                iconBg = BentoColors.IconBgGreen
            ),
            ThemeCardInfo(
                name = "名胜风光",
                subtitle = "四季列岛巡礼",
                icon = Icons.Rounded.Explore,
                plumpIcon = PlumpIcon.Compass,
                isoluminantColor = IsoluminantPalette.RoseRuby,
                accent = Color(0xFFEC4899),
                iconBg = Color(0xFFFCE7F3)
            ),
            ThemeCardInfo(
                name = "商务工作",
                subtitle = "职场严谨敬语",
                icon = Icons.Rounded.BusinessCenter,
                plumpIcon = PlumpIcon.Briefcase,
                isoluminantColor = IsoluminantPalette.SkyIndigo,
                accent = Color(0xFF6366F1),
                iconBg = Color(0xFFEEF2FF)
            )
        )
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 顶部弹性占位：未滑动时避开顶栏，滑动时无阻碍滑入顶栏遮罩
        Spacer(Modifier.height(statusBarTop + 60.dp + 12.dp))

        // 卡片 1: 级别选择 (现代化流体物理滑块 + 动态能力注解)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(
                    color = if (isDark) Color(0x26000000) else Color(0x0C000000),
                    borderRadius = 26.dp,
                    blurRadius = 18.dp,
                    offsetY = 4.dp
                ),
            color = surfaceColor,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO) {
                            PlumpDuoIcon(
                                icon = PlumpIcon.GraduationCap,
                                color = IsoluminantPalette.WarmAmber,
                                size = 28.dp
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BentoColors.Primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.School,
                                        contentDescription = null,
                                        tint = BentoColors.Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "选择 JLPT 级别",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) colorScheme.onSurface else BentoColors.TextMain
                            )
                            Text(
                                text = "难度自适应匹配 AI 生成语料",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextMuted
                            )
                        }
                    }

                    // 当前选中的微胶囊标头
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BentoColors.Primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = uiState.difficulty,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = BentoColors.Primary
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // 物理流体胶囊滑块 (portal_labs Discrete Tabs 动效)
                FluidLevelSegmentedPicker(
                    levels = levels,
                    selectedLevel = uiState.difficulty,
                    onSelectLevel = { onEvent(AIReadingEvent.UpdateDifficulty(it)) },
                    isDark = isDark
                )

                Spacer(Modifier.height(14.dp))

                // 动态能力梯度注解条 (AnimatedContent)
                LevelCapabilityCard(
                    selectedLevel = uiState.difficulty,
                    isDark = isDark
                )
            }
        }

        // 主题选择标题行 (去框悬浮流)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO) {
                    PlumpDuoIcon(
                        icon = PlumpIcon.BookOpen,
                        color = IsoluminantPalette.WarmAmber,
                        size = 28.dp
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BentoColors.Primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                tint = BentoColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "选择阅读主题",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) colorScheme.onSurface else BentoColors.TextMain
                    )
                    Text(
                        text = "沉浸式日式和风物语",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextMuted
                    )
                }
            }

            // 计数与提示微胶囊
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) colorScheme.surfaceContainerHigh else Color.White.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Text(
                    text = "6 个精选领域",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub
                )
            }
        }

        // 2列流光 Bento 网格 (直接悬浮在底色上，由各卡片独立的 softShadow 形成立体浮空感)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            for (i in themes.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ThemeBentoItem(
                        info = themes[i],
                        isSelected = uiState.readingTheme == themes[i].name,
                        modifier = Modifier.weight(1f),
                        onClick = { onEvent(AIReadingEvent.UpdateTheme(themes[i].name)) }
                    )
                    if (i + 1 < themes.size) {
                        ThemeBentoItem(
                            info = themes[i + 1],
                            isSelected = uiState.readingTheme == themes[i + 1].name,
                            modifier = Modifier.weight(1f),
                            onClick = { onEvent(AIReadingEvent.UpdateTheme(themes[i + 1].name)) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 底部高光主按钮 (Specular Action Button)
        SpecularCtaButton(
            onClick = { onEvent(AIReadingEvent.GenerateArticle) },
            text = "开启智能阅读训练"
        )

        Spacer(Modifier.height(navBarBottom + 24.dp))
    }
}

/**
 * 物理流体胶囊指示器 (借鉴 portal_labs Discrete Tabs)
 */
@Composable
private fun FluidLevelSegmentedPicker(
    levels: List<String>,
    selectedLevel: String,
    onSelectLevel: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val trackBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else BentoColors.BgBase
    val selectedIndex = remember(selectedLevel, levels) {
        val idx = levels.indexOf(selectedLevel)
        if (idx >= 0) idx else 0
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(trackBg, RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f) else NemoNeutrals.Gray100,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / levels.size.coerceAtLeast(1)

        // 物理弹簧滑动位移
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = 420f
            ),
            label = "fluidCapsuleOffset"
        )

        // 滑动的活动指示胶囊 (带高斯弥散光晕)
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .softShadow(
                    color = BentoColors.Primary.copy(alpha = 0.40f),
                    borderRadius = 13.dp,
                    blurRadius = 10.dp,
                    offsetY = 3.dp
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFB923C), // 珊瑚亮橙
                            BentoColors.Primary // 元气橘
                        )
                    ),
                    shape = RoundedCornerShape(13.dp)
                )
        )

        // 等级选项标签 Row
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            levels.forEachIndexed { index, level ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else BentoColors.TextSub),
                    animationSpec = tween(180),
                    label = "levelTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectLevel(level)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}

private data class LevelDetail(val tag: String, val scope: String)

/**
 * 动态能力梯度注解条
 */
@Composable
private fun LevelCapabilityCard(
    selectedLevel: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val detail = remember(selectedLevel) {
        when (selectedLevel) {
            "N5" -> LevelDetail("入门基石", "基础生活日常会话与简明短文")
            "N4" -> LevelDetail("初级进阶", "日常情境叙述与基础复合句表达")
            "N3" -> LevelDetail("中级跨越", "文化生活随笔与较完整话题阅读")
            "N2" -> LevelDetail("高级突破", "报刊社论、深度观点与复杂议论文")
            "N1" -> LevelDetail("学术精通", "文学原著、社科政经与地道深度论述")
            else -> LevelDetail("自适应", "AI 智能语料匹配与动态训练")
        }
    }

    AnimatedContent(
        targetState = detail,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, delayMillis = 40)) +
             slideInVertically(animationSpec = tween(220)) { it / 3 })
            .togetherWith(
                fadeOut(animationSpec = tween(150)) +
                slideOutVertically(animationSpec = tween(150)) { -it / 3 }
            )
        },
        label = "levelDetailContent",
        modifier = modifier.fillMaxWidth()
    ) { item ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(
                    color = BentoColors.Primary.copy(alpha = if (isDark) 0.16f else 0.08f),
                    borderRadius = 14.dp,
                    blurRadius = 10.dp,
                    offsetY = 2.dp
                ),
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f) else BentoColors.PrimaryLight.copy(alpha = 0.35f),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f) else BentoColors.Primary.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BentoColors.Primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.tag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BentoColors.Primary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = item.scope,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    ),
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else BentoColors.TextMain,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class ThemeCardInfo(
    val name: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val plumpIcon: PlumpIcon,
    val isoluminantColor: Color,
    val accent: Color,
    val iconBg: Color
)

/**
 * Apple 风格主题卡片组件 (遵循 Apple Design 极度克制、充裕留白与信息降噪)
 */
@Composable
private fun ThemeBentoItem(
    info: ThemeCardInfo,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    // 交互态按压弹性动画 (Apple 经典按压微缩放)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.965f
            isSelected -> 1.015f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "appleCardScale"
    )

    val animOffsetY by animateDpAsState(
        targetValue = if (isSelected) (-2.5).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "appleCardOffset"
    )

    // 精准而柔和的高斯弥散阴影参数
    val shadowAlpha by animateFloatAsState(
        targetValue = if (isSelected) (if (isDark) 0.32f else 0.22f) else (if (isDark) 0.12f else 0.04f),
        animationSpec = tween(180),
        label = "appleShadowAlpha"
    )
    val shadowBlur by animateDpAsState(
        targetValue = if (isSelected) 18.dp else 8.dp,
        animationSpec = tween(180),
        label = "appleShadowBlur"
    )

    // 边框质感：极细且克制 (Hairline 细微边框，避免视觉粗重)
    val borderStroke = if (isSelected) {
        BorderStroke(1.5.dp, info.accent)
    } else {
        BorderStroke(
            0.8.dp,
            if (isDark) colorScheme.outlineVariant.copy(alpha = 0.18f) else Color(0x0D000000)
        )
    }

    // 表面底色：极其纯净，给文字与图标最透气的基底
    val surfaceColor = if (isDark) {
        if (isSelected) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainer
    } else {
        if (isSelected) Color.White else Color(0xFFFAFAFB)
    }

    Surface(
        modifier = modifier
            .offset(y = animOffsetY)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .run {
                if (isSelected) {
                    this.softShadow(
                        color = info.accent.copy(alpha = shadowAlpha),
                        borderRadius = 22.dp,
                        blurRadius = shadowBlur,
                        offsetY = 5.dp
                    )
                } else {
                    this.softShadow(
                        color = if (isDark) Color(0x20000000) else Color(0x08000000),
                        borderRadius = 22.dp,
                        blurRadius = shadowBlur,
                        offsetY = 2.dp
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        shape = RoundedCornerShape(22.dp),
        border = borderStroke,
        color = surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            // 顶部：图标基座
            if (LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    PlumpDuoIcon(
                        icon = info.plumpIcon,
                        color = info.isoluminantColor,
                        size = 28.dp
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = if (isSelected) info.iconBg else (if (isDark) colorScheme.surfaceContainerHigh else info.iconBg.copy(alpha = 0.75f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = info.icon,
                            contentDescription = null,
                            tint = if (isSelected) info.accent else (if (isDark) colorScheme.onSurface else info.accent.copy(alpha = 0.9f)),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 主标题：大字重、纯净醒目、紧凑字距 (SF Pro 风格)
            Text(
                text = info.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = if (isSelected && !isDark) info.accent else (if (isDark) colorScheme.onSurface else BentoColors.TextMain)
            )

            Spacer(Modifier.height(4.dp))

            // 副标题：极简克制的意象短语，低对比度退后，绝不喧宾夺主
            Text(
                text = info.subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.1).sp
                ),
                color = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else BentoColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 底部高光流体主按钮 (react-bits SpecularButton)
 */
@Composable
private fun SpecularCtaButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // 扫光位移动画 (Specular Light Sweep)
    val infiniteTransition = rememberInfiniteTransition(label = "specularTransition")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )

    // 右箭头微弹律动
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .softShadow(
                color = BentoColors.Primary.copy(alpha = 0.38f),
                borderRadius = 22.dp,
                blurRadius = 18.dp,
                offsetY = 6.dp
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFB923C),
                        BentoColors.Primary,
                        Color(0xFFEA580C)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // 扫光层 (Specular Light Sweep)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val currentX = width * sweepProgress
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.22f),
                    Color.Transparent
                ),
                start = Offset(currentX - 80f, 0f),
                end = Offset(currentX + 80f, height)
            )
            drawRect(brush = brush)
        }

        // 按钮文字与动效箭头
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = Color.White
            )
            Spacer(Modifier.width(8.dp))
            if (LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO) {
                PlumpDuoIcon(
                    icon = PlumpIcon.ChevronsRight,
                    color = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = arrowOffset.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = arrowOffset.dp),
                    tint = Color.White
                )
            }
        }
    }
}

private val ThinkingStepsLibrary = listOf(
    "分析阅读偏好：明确选定主题与目标难度级别",
    "检索主题语境：搜寻地道自然的日式生活场景",
    "挑选核心词汇：筛选适合当前水平的重点假名与单词",
    "构建文章脉络：规划段落层级与情节展开走向",
    "组织日文段落：生成自然流畅、符合母语表达习惯的句子",
    "优化语法表达：校验用词准确性与文段连贯度",
    "对照中文翻译：生成忠实地道的中文译文与段落对照",
    "设计阅读测验：围绕短文核心细节生成理解问答题",
    "整理重点词条：提炼生词列表、假名读音与释义",
    "完成排版校验：梳理段落布局，准备开启阅读训练"
)

private data class ThinkingStepItem(
    val uniqueId: Int,
    val text: String,
    val isCompleted: Boolean = false
)

/**
 * 假名乱码硬解码文本组件 (Hard-Decoded Text)
 */
@Composable
private fun HardDecodedText(
    fullText: String,
    isCompleted: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val kanaPool = remember {
        listOf(
            'あ','い','う','え','お',
            'か','き','く','け','こ',
            'さ','し','す','せ','そ',
            'た','ち','つ','て','と',
            'な','に','ぬ','ね','の',
            'は','ひ','ふ','へ','ほ',
            'ま','み','む','め','も',
            'や','ゆ','よ','ら','り','る','れ','ろ','わ','を','ん'
        )
    }

    var textState by remember(fullText) { mutableStateOf(fullText) }

    LaunchedEffect(isCompleted, fullText) {
        if (!isCompleted) {
            val totalSteps = 6
            val stepDelay = 45L
            for (step in 0 until totalSteps) {
                val progress = (step + 1).toFloat() / totalSteps
                val resolvedCount = (fullText.length * progress).toInt().coerceIn(0, fullText.length)
                val sb = StringBuilder()
                for (i in fullText.indices) {
                    if (i < resolvedCount) {
                        sb.append(fullText[i])
                    } else if (fullText[i] == '：' || fullText[i] == ' ' || fullText[i] == '、') {
                        sb.append(fullText[i])
                    } else {
                        sb.append(kanaPool.random())
                    }
                }
                textState = sb.toString()
                delay(stepDelay)
            }
        } else {
            textState = fullText
        }
    }

    Text(
        text = if (isCompleted) fullText else textState,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp
        ),
        color = if (isCompleted) {
            if (isDark) Color.White.copy(alpha = 0.95f) else Color(0xFF0F172A)
        } else {
            if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
        },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * 2. 智能阅读训练加载页面：
 * 采用 code_artifact.html 原生 WebGL2 全屏极光引擎 + 浮动 AI 动态思维链硬解码卡片
 * 固定 4 条容量滑动窗口淘汰，绿色对勾图标，深浅色自适应
 */
@Composable
private fun LoadingScreen() {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val containerBg = if (isDark) colorScheme.background else BentoColors.BgBase
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val visibleSteps = remember { mutableStateListOf<ThinkingStepItem>() }
    var currentLibIndex by remember { mutableIntStateOf(0) }
    var stepCounter by remember { mutableIntStateOf(0) }
    val maxVisibleCount = 4 // 固定最多显示 4 条，第 5 条出现时第 1 条平滑消失

    LaunchedEffect(Unit) {
        while (true) {
            val stepText = ThinkingStepsLibrary[currentLibIndex % ThinkingStepsLibrary.size]
            currentLibIndex++
            val newItemId = ++stepCounter
            val newItem = ThinkingStepItem(uniqueId = newItemId, text = stepText, isCompleted = false)

            if (visibleSteps.size >= maxVisibleCount) {
                visibleSteps.removeAt(0)
            }
            visibleSteps.add(newItem)

            // 执行假名跳动硬解码
            delay(380)

            // 解码完成，标记完成，切换为绿色圆底打勾
            val idx = visibleSteps.indexOfFirst { it.uniqueId == newItemId }
            if (idx != -1) {
                visibleSteps[idx] = visibleSteps[idx].copy(isCompleted = true)
            }

            // 保持当前步骤展示并间隔 950ms，再进入下一步
            delay(950)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerBg)
    ) {
        // 1. 底层：全屏 WebGL2 极光引擎
        AIAuroraBackground(
            isDark = isDark,
            containerBg = containerBg,
            modifier = Modifier.fillMaxSize()
        )

        // 2. 顶层：悬浮 AI 动态思维链硬解码卡片（不遮挡顶栏按钮，避开 statusBarTop + 64dp）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarTop + 64.dp, start = 18.dp, end = 18.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.68f) else Color.White.copy(alpha = 0.85f),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.14f) else Color(0x18000000)
                ),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    // 头部：AI 思考标识与呼吸微光
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "AI 正在构建智能阅读训练",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                    )
                    Spacer(Modifier.height(12.dp))

                    // 步骤列表：最大容量 4 条，按固定顺序循环推进
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = tween(250))
                    ) {
                        visibleSteps.forEach { step ->
                            key(step.uniqueId) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // 左侧图标：进行中为微型环形转圈，已完成为绿色圆底白勾
                                    if (step.isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981)), // 祖母绿实心圆
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "已完成",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(18.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    // 右侧文字：假名乱码硬解码 -> 锁定真文
                                    HardDecodedText(
                                        fullText = step.text,
                                        isCompleted = step.isCompleted,
                                        isDark = isDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * 3. 日语小短文阅读理解与测验主交互页
 */
@Composable
private fun ArticleContentScreen(
    article: AIReadingArticle,
    uiState: AIReadingUiState,
    onEvent: (AIReadingEvent) -> Unit,
    onShowCopySnackbar: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    var showTranslation by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // BasicTextField 选区状态（用于划词翻译 + 点击查词）
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = article.contentRaw))
    }
    // 记录当前选中的文本，供 CustomTextToolbar 读取
    val selectedTextState = remember { mutableStateOf("") }
    // 追踪之前是否有选区，用于区分"取消选区"和"纯点击"
    var hadSelection by remember { mutableStateOf(false) }

    // 当文章内容变化时同步 TextFieldValue
    LaunchedEffect(article.contentRaw) {
        textFieldValue = TextFieldValue(text = article.contentRaw)
        selectedTextState.value = ""
        hadSelection = false
    }

    // 自定义 TextToolbar（方案一：划词翻译）
    val view = LocalView.current
    val customTextToolbar = remember(view) {
        CustomTextToolbar(
            view = view,
            getSelectedText = { selectedTextState.value },
            onTranslateRequested = { selectedText ->
                val contextSentence = findContextSentence(article.contentRaw, selectedText)
                onEvent(AIReadingEvent.TranslateText(selectedText, contextSentence))
            }
        )
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部弹性占位：未滑动时避开顶栏，滑动时无阻碍滑入顶栏遮罩
        Spacer(Modifier.height(statusBarTop + 60.dp + 12.dp))
        // 短文面板
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 头部：标题与发音按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = if (isDark) colorScheme.onSurface else BentoColors.TextMain,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))

                    val isPlaying = uiState.playingAudioId == "article_full"
                    SpeakerButton(
                        isPlaying = isPlaying,
                        onClick = {
                            onEvent(AIReadingEvent.SpeakText(article.contentRaw, "article_full"))
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = borderColor)
                Spacer(Modifier.height(16.dp))

                // 正文渲染：BasicTextField(readOnly) 方案
                // 方案一：长按拖选 → 弹出带"翻译"按钮的浮动菜单
                // 方案二：单击某个词 → 自动识别词边界并翻译
                // 点击其他位置 → 自动清除选区
                val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF2C3E50)

                CompositionLocalProvider(LocalTextToolbar provides customTextToolbar) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            val newSel = newValue.selection
                            if (newSel.collapsed) {
                                // 光标模式（非选区）
                                if (!hadSelection) {
                                    // 纯点击（不是从选区状态取消的）→ 触发点击查词（方案二）
                                    val offset = newSel.start
                                    if (offset in article.contentRaw.indices) {
                                        val word = extractWordAtOffset(article.contentRaw, offset)
                                        if (word.isNotBlank()) {
                                            val ctx = findContextSentence(article.contentRaw, word)
                                            onEvent(AIReadingEvent.TranslateText(word, ctx))
                                        }
                                    }
                                }
                                // 清除选区状态
                                hadSelection = false
                                selectedTextState.value = ""
                            } else {
                                // 有选区 → 更新选中文本
                                hadSelection = true
                                val min = newSel.min.coerceIn(0, article.contentRaw.length)
                                val max = newSel.max.coerceIn(0, article.contentRaw.length)
                                selectedTextState.value = article.contentRaw.substring(min, max)
                            }
                            // 只响应选区变化，文本内容始终锁定为原文
                            textFieldValue = newValue.copy(text = article.contentRaw)
                        },
                        readOnly = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 32.sp,
                            letterSpacing = 0.3.sp,
                            color = textColor
                        ),
                        cursorBrush = SolidColor(Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(20.dp))

                // 操作按钮栏：左侧复制原文，右侧切换中文对照
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 复制原文胶囊按钮
                    Surface(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                clipboardManager.setText(AnnotatedString(article.contentRaw))
                                onShowCopySnackbar("日文原文已复制到剪贴板")
                            }
                        ),
                        shape = CircleShape,
                        color = if (isDark) colorScheme.surfaceContainerHigh else BentoColors.BgBase
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                tint = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "复制原文",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub
                            )
                        }
                    }

                    // 译文控制开关
                    Surface(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showTranslation = !showTranslation }
                        ),
                        shape = CircleShape,
                        color = if (showTranslation) BentoColors.PrimaryLight else (if (isDark) colorScheme.surfaceContainerHigh else BentoColors.BgBase)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (showTranslation) Icons.Rounded.VisibilityOff else Icons.Rounded.Translate,
                                contentDescription = null,
                                tint = if (showTranslation) BentoColors.Primary else (if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (showTranslation) "隐藏中文对照" else "查看中文对照",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (showTranslation) BentoColors.Primary else (if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub)
                            )
                        }
                    }
                }

                // 中文对照翻译展开
                AnimatedVisibility(
                    visible = showTranslation,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = if (isDark) colorScheme.surfaceContainerHigh else BentoColors.BgBase.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = article.translation,
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                    color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 重点词汇板块
        if (article.vocabulary.isNotEmpty()) {
            Column {
                Text(
                    text = "核心重点词汇",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (isDark) colorScheme.onSurface else BentoColors.TextMain,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    article.vocabulary.forEachIndexed { index, voc ->
                        VocabularyItem(voc, index, uiState, onEvent)
                    }
                }
            }
        }

        // 阅读测验小题卡
        if (article.questions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "阅读理解小测验",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (isDark) colorScheme.onSurface else BentoColors.TextMain,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )

                // 正确率评分圆盘面板
                if (uiState.isSubmitted) {
                    val correctCount = article.questions.filterIndexed { index, q ->
                        uiState.selectedAnswers[index] == q.answer
                    }.size
                    val totalCount = article.questions.size

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = BentoColors.PrimaryLight.copy(alpha = if (isDark) 0.15f else 0.8f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(2.dp, BentoColors.Primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (correctCount == totalCount) "完璧！全部答对！" else "继续加油！答对 $correctCount/$totalCount 道题",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = BentoColors.Primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (correctCount == totalCount) "太棒了，您的阅读理解能力无懈可击！" else "阅读中还有细节需要厘清，查看下方解析学习吧！",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                shape = CircleShape,
                                color = BentoColors.Primary,
                                contentColor = Color.White
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Text(
                                        text = "${(correctCount * 100) / totalCount}%",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // 问题卡片列表
                article.questions.forEachIndexed { qIndex, question ->
                    QuestionCard(
                        question = question,
                        qIndex = qIndex,
                        selectedAns = uiState.selectedAnswers[qIndex],
                        isSubmitted = uiState.isSubmitted,
                        uiState = uiState,
                        onEvent = onEvent
                    )
                }

                // 提交与重置控制
                if (!uiState.isSubmitted) {
                    val allAnswered = uiState.selectedAnswers.all { it != null }
                    Button(
                        onClick = { onEvent(AIReadingEvent.SubmitAnswers) },
                        enabled = allAnswered,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            disabledContainerColor = if (isDark) colorScheme.surfaceContainerHigh else NemoNeutrals.Gray200
                        ),
                        elevation = null
                    ) {
                        Text(
                            text = if (allAnswered) "提交答题并查看评分" else "请先完成所有题目",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allAnswered) Color.White else (if (isDark) colorScheme.onSurface.copy(alpha = 0.3f) else NemoNeutrals.Gray400)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEvent(AIReadingEvent.ResetReader) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colorScheme.primary)
                        ) {
                            Text("返回配置", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onEvent(AIReadingEvent.GenerateArticle) },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = null
                        ) {
                            Text("换一篇短文", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(navBarBottom + 24.dp))
    }
}

/**
 * 重点词汇子项卡片
 */
@Composable
private fun VocabularyItem(
    voc: ReadingVocabulary,
    index: Int,
    uiState: AIReadingUiState,
    onEvent: (AIReadingEvent) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    val isPlaying = uiState.playingAudioId == "voc_$index"

    Surface(
        modifier = Modifier
            .width(160.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    onEvent(AIReadingEvent.SpeakText("${voc.word}、${voc.kana}", "voc_$index"))
                }
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        color = if (isPlaying) BentoColors.PrimaryLight.copy(alpha = if (isDark) 0.15f else 0.8f) else surfaceColor
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) colorScheme.surfaceContainerHigh else BentoColors.BgBase
                ) {
                    Text(
                        text = voc.pos,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = BentoColors.TextSub,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Icon(
                    imageVector = if (isPlaying) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeDown,
                    contentDescription = null,
                    tint = if (isPlaying) BentoColors.Primary else BentoColors.TextSub.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = voc.word,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = if (isDark) colorScheme.onSurface else BentoColors.TextMain
            )
            Text(
                text = voc.kana,
                style = MaterialTheme.typography.labelSmall,
                color = BentoColors.TextSub
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            Text(
                text = voc.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 单个测验问题卡片组件
 */
@Composable
private fun QuestionCard(
    question: ReadingQuestion,
    qIndex: Int,
    selectedAns: Int?,
    isSubmitted: Boolean,
    uiState: AIReadingUiState,
    onEvent: (AIReadingEvent) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 题干与音频
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${qIndex + 1}. ${question.question}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) colorScheme.onSurface else BentoColors.TextMain,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(10.dp))

                val isPlaying = uiState.playingAudioId == "question_$qIndex"
                SpeakerButton(
                    isPlaying = isPlaying,
                    onClick = {
                        onEvent(AIReadingEvent.SpeakText(question.question, "question_$qIndex"))
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // 选项列表
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { oIndex, option ->
                    val isSelected = selectedAns == oIndex
                    val isCorrect = question.answer == oIndex

                    // 计算提交后的各选项底色与边框色
                    val cardBg = when {
                        isSubmitted -> {
                            when {
                                isCorrect -> BentoColors.IconBgGreen.copy(alpha = if (isDark) 0.15f else 0.8f) // 正确选项恒为绿
                                isSelected -> ReadingIconBgRed.copy(alpha = if (isDark) 0.15f else 0.8f)  // 选错的显示为红
                                else -> if (isDark) colorScheme.surfaceContainerHigh else BentoColors.BgBase
                            }
                        }
                        else -> {
                            if (isSelected) colorScheme.primary.copy(alpha = 0.08f)
                            else if (isDark) colorScheme.surfaceContainerHigh
                            else BentoColors.BgBase
                        }
                    }

                    val cardBorder = when {
                        isSubmitted -> {
                            when {
                                isCorrect -> BentoColors.AccentGreen
                                isSelected -> ReadingAccentRed
                                else -> Color.Transparent
                            }
                        }
                        else -> {
                            if (isSelected) colorScheme.primary
                            else Color.Transparent
                        }
                    }

                    val textCol = when {
                        isSubmitted -> {
                            when {
                                isCorrect -> BentoColors.AccentGreen
                                isSelected -> ReadingAccentRed
                                else -> if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub
                            }
                        }
                        else -> {
                            if (isSelected) colorScheme.primary
                            else if (isDark) colorScheme.onSurfaceVariant
                            else BentoColors.TextSub
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !isSubmitted,
                                onClick = { onEvent(AIReadingEvent.SelectAnswer(qIndex, oIndex)) }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = if (cardBorder != Color.Transparent) BorderStroke(1.5.dp, cardBorder) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${('A' + oIndex)}: $option",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                color = textCol,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(Modifier.width(10.dp))

                            if (isSubmitted) {
                                when {
                                    isCorrect -> {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "正确",
                                            tint = BentoColors.AccentGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    isSelected -> {
                                        Icon(
                                            imageVector = Icons.Rounded.Cancel,
                                            contentDescription = "错误",
                                            tint = ReadingAccentRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.RadioButtonChecked,
                                        contentDescription = "已选",
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = "未选",
                                        tint = if (isDark) colorScheme.outline else NemoNeutrals.Gray300,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 解析与详解展开
            AnimatedVisibility(
                visible = isSubmitted,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = if (isDark) colorScheme.surfaceContainerHigh else BentoColors.BgBase.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = BentoColors.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "题目解析",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BentoColors.Primary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. 出错与重试引导页
 */
@Composable
private fun ErrorScreen(
    errorMsg: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 警告大图标
            Surface(
                shape = CircleShape,
                color = ReadingIconBgRed,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = ReadingAccentRed,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "生成短文失败",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) colorScheme.onSurface else BentoColors.TextMain
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 错误日志文本卡片
            Surface(
                color = if (isDark) colorScheme.surfaceContainerHigh else BentoColors.BgBase.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 交互按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colorScheme.primary)
                ) {
                    Text("返回配置", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = null
                ) {
                    Text("重新生成", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 利用 Java BreakIterator 按日语词边界提取点击位置所在的单词
 */
private fun extractWordAtOffset(text: String, offset: Int): String {
    if (text.isEmpty() || offset < 0 || offset >= text.length) return ""

    val iterator = BreakIterator.getWordInstance(Locale.JAPAN)
    iterator.setText(text)

    var start = iterator.preceding(offset + 1)
    if (start == BreakIterator.DONE) start = 0

    var end = iterator.following(offset)
    if (end == BreakIterator.DONE) end = text.length

    val word = text.substring(start, end).trim()

    // 过滤纯标点、空白和换行
    return if (word.isNotBlank() && !word.all { it.isWhitespace() || it in "。、！？「」『』（）…・〜ー—\n\r" }) {
        word
    } else ""
}

/**
 * 从全文中查找包含指定文本的整句（以句号、换行等为分隔）
 */
private fun findContextSentence(fullText: String, target: String): String {
    // 以日语常见句末标点和换行符作为分隔
    val sentences = fullText.split(Regex("[。！？\\n]+"))
    for (sentence in sentences) {
        if (sentence.contains(target)) {
            return sentence.trim()
        }
    }
    // 如果没有精确匹配到句子，返回全文前 200 字
    return fullText.take(200)
}
