package com.jian.nemo.feature.learning.presentation.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
import com.jian.nemo.core.ui.component.speaker.SpeakerButton
import com.jian.nemo.feature.learning.R
import kotlinx.coroutines.delay
import com.airbnb.lottie.compose.*
import androidx.compose.ui.res.painterResource
import com.jian.nemo.core.designsystem.R as DesignR
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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

    Scaffold(
        topBar = {
            CommonHeader(
                title = "AI 日语阅读",
                onBack = {
                    if (uiState.currentArticle != null) {
                        viewModel.onEvent(AIReadingEvent.ResetReader)
                    } else {
                        onNavigateBack()
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "阅读历史",
                            tint = colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.onEvent(AIReadingEvent.QuickSwitchPlatform) }) {
                        val platform = uiState.aiPlatform
                        when (platform) {
                            "gemini" -> Icon(painterResource(DesignR.drawable.ic_gemini), contentDescription = "Gemini", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                            "deepseek" -> Icon(painterResource(DesignR.drawable.ic_deepseek), contentDescription = "DeepSeek", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                            "openai" -> Icon(painterResource(DesignR.drawable.ic_openai), contentDescription = "OpenAI", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                            "claude" -> Icon(painterResource(DesignR.drawable.ic_claude), contentDescription = "Claude", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                            "doubao" -> Icon(painterResource(DesignR.drawable.ic_doubao), contentDescription = "Doubao", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                            "mimo" -> Icon(painterResource(DesignR.drawable.ic_mimo), contentDescription = "Mimo", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                            else -> Icon(Icons.Rounded.Memory, contentDescription = "Custom", tint = colorScheme.onSurface)
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "AI设置",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                backgroundColor = if (isDark) colorScheme.background else BentoColors.BgBase
            )
        },
        containerColor = if (isDark) colorScheme.background else BentoColors.BgBase
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

            // 切换配置别名气泡提示（弹出后 2.5s 自动消失）
            AnimatedVisibility(
                visible = uiState.switchedConfigName != null,
                enter = fadeIn(androidx.compose.animation.core.tween(200)) +
                        slideInVertically(androidx.compose.animation.core.tween(200)) { -it },
                exit  = fadeOut(androidx.compose.animation.core.tween(300)) +
                        slideOutVertically(androidx.compose.animation.core.tween(300)) { -it },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp)
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
                        .padding(16.dp)
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
                    .padding(top = 8.dp)
            )
        }
    }
}

/**
 * 1. 级别与主题选择配置页
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

    val levels = listOf("N5", "N4", "N3", "N2", "N1")

    // 精选的 6 个日式主题，带渐变底色和精美图标
    val themes = listOf(
        ThemeCardInfo("日常生活", "贴近日本社会与暖心细节的琐碎日常", Icons.Rounded.Park, BentoColors.IconBgBlue, BentoColors.AccentBlue),
        ThemeCardInfo("传统文化", "茶道、和风建筑、传统祭典的深度魅力", Icons.Rounded.Landscape, BentoColors.IconBgOrange, BentoColors.AccentOrange),
        ThemeCardInfo("民间故事", "桃太郎、竹取物语等日本世代相传的物语", Icons.Rounded.AutoAwesome, BentoColors.IconBgPurple, BentoColors.AccentPurple),
        ThemeCardInfo("美食料理", "寿司、拉面与日式便当背后的温暖物语", Icons.Rounded.Restaurant, BentoColors.IconBgGreen, BentoColors.AccentGreen),
        ThemeCardInfo("名胜风光", "富士山、京都赏樱与温泉名所游记", Icons.Rounded.Explore, BentoColors.IconBgPurple, BentoColors.AccentPurple),
        ThemeCardInfo("商务工作", "日本职场礼仪与严谨的敬语交流氛围", Icons.Rounded.BusinessCenter, BentoColors.IconBgBlue, BentoColors.AccentBlue)
    )

    // 呼吸动画 - 精致微呼吸 (Semi-Flat 2.0 规范)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 卡片 1: 级别选择
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.School,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "选择 JLPT 级别",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) colorScheme.onSurface else BentoColors.TextMain
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    levels.forEach { level ->
                        val isSelected = uiState.difficulty == level
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onEvent(AIReadingEvent.UpdateDifficulty(level)) }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colorScheme.primary else (if (isDark) colorScheme.surfaceContainerHigh else BentoColors.BgBase),
                            contentColor = if (isSelected) Color.White else (if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 卡片 2: 主题选择
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "选择阅读主题",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) colorScheme.onSurface else BentoColors.TextMain
                    )
                }
                Spacer(Modifier.height(16.dp))

                // 2列网格卡片
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in themes.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeGridItem(themes[i], uiState.readingTheme == themes[i].name, Modifier.weight(1f)) {
                                onEvent(AIReadingEvent.UpdateTheme(themes[i].name))
                            }
                            if (i + 1 < themes.size) {
                                ThemeGridItem(themes[i + 1], uiState.readingTheme == themes[i + 1].name, Modifier.weight(1f)) {
                                    onEvent(AIReadingEvent.UpdateTheme(themes[i + 1].name))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 智能生成主按钮 (带呼吸缩放)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onEvent(AIReadingEvent.GenerateArticle) }
                ),
            shape = RoundedCornerShape(20.dp),
            color = BentoColors.Primary,
            contentColor = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "开启智能阅读训练",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private data class ThemeCardInfo(
    val name: String,
    val desc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bg: Color,
    val accent: Color
)

@Composable
private fun ThemeGridItem(
    info: ThemeCardInfo,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    // 扁平配色体系 (和风 Semi-Flat 2.0 规范)
    val baseSurfaceColor = if (isDark) colorScheme.surfaceContainerHigh else BentoColors.Surface
    val cardBgColor = if (isSelected) {
        if (isDark) colorScheme.primaryContainer.copy(alpha = 0.12f) else BentoColors.PrimaryLight.copy(alpha = 0.7f)
    } else {
        baseSurfaceColor
    }

    val borderCol = if (isSelected) {
        BentoColors.Primary
    } else {
        if (isDark) colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100
    }

    // 选中时的垂直物理弹性微平移与微缩放 (Spring Animation)
    val animOffset by animateDpAsState(
        targetValue = if (isSelected) (-3).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetY"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.01f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // 克制的环境空气感软阴影 (选中的时候轻微悬浮，整体依然为 Flat 风格)
    val shadowElevation by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 200, easing = EaseInOutQuad),
        label = "shadowElevation"
    )

    // 图标水印不透明度平滑过渡
    val iconAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.14f else 0.06f,
        animationSpec = tween(durationMillis = 300, easing = EaseInOutQuad),
        label = "iconAlpha"
    )

    Surface(
        modifier = modifier
            .aspectRatio(1.25f)
            .offset(y = animOffset)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
            }
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(18.dp),
                clip = false,
                ambientColor = if (isDark) Color.Transparent else BentoColors.Primary.copy(alpha = 0.15f),
                spotColor = if (isDark) Color.Transparent else BentoColors.Primary.copy(alpha = 0.1f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderCol),
        color = cardBgColor
    ) {
        // 使用绝对定位重构的 Box 容器，开启裁剪以完美支持水印溢出美感
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        ) {
            // 1. 右上角超大水印图标 (绝对定位)
            Icon(
                imageVector = info.icon,
                contentDescription = null,
                tint = if (isSelected) BentoColors.Primary else info.accent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(76.dp)
                    .offset(x = 12.dp, y = (-12).dp) // 微微向右上溢出，体现截断艺术感
                    .graphicsLayer {
                        alpha = iconAlpha
                    }
            )

            // 2. 右上角极小高精致勾选状态 (位于水印之上，但十分协调)
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = BentoColors.Primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(18.dp)
                )
            }

            // 3. 左下角排版：大标题 + 优雅扁平小正文
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
                    .fillMaxWidth(0.85f), // 留出一点右侧呼吸空间，防止大字贴边
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold, // 升级为 ExtraBold，强化大字对比
                        fontSize = 18.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = if (isSelected && !isDark) BentoColors.Primary else (if (isDark) colorScheme.onSurface else BentoColors.TextMain)
                )
                Text(
                    text = info.desc,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    ),
                    color = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 2. 个性化 Lottie 加载动画页
 */
@Composable
private fun LoadingScreen() {
    val loadingTexts = listOf(
        "正在为您精选最契合主题的词汇...",
        "正在为您构建地道优雅的日式句子...",
        "正在筹备富有挑战性的小测验...",
        "即将开启一段美好的日语阅读之旅...",
        "AI 正在忠实还原地道表达，马上就好..."
    )

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.anim_ai_thinking)
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    var currentTextIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            currentTextIndex = (currentTextIndex + 1) % loadingTexts.size
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { lottieProgress },
                    modifier = Modifier.size(240.dp)
                )
            } else {
                NemoChasingDotsLoader(size = 64.dp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = loadingTexts[currentTextIndex],
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> -height } + fadeOut()
                },
                label = "loadingText"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        Spacer(Modifier.height(24.dp))
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
