package com.jian.nemo.feature.learning.presentation.home

import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.ui.component.liquid.LiquidButton

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.math.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.shape.CircleShape
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.translate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jian.nemo.core.ui.animation.rememberIsTransitionSettled
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.designsystem.theme.Rubik
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.animation.LocalDecorativeAnimationsPaused
import com.jian.nemo.core.ui.component.progress.NemoCircularProgress
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.ui.modifier.softShadow
import com.jian.nemo.core.domain.model.LearningMode
import com.jian.nemo.feature.learning.presentation.home.components.*
import com.jian.nemo.feature.learning.R

@Composable
fun HomeScreen(
    onNavigateToLearning: (LearningMode) -> Unit,
    onNavigateToKanaChart: () -> Unit,
    onNavigateToGrammarList: () -> Unit,
    onNavigateToHeatmap: () -> Unit,
    onNavigateToAIWorkshop: () -> Unit,
    onNavigateToAIReading: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToVerbHandbook: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // --- 环形进度加载控制逻辑 ---
    var isInitialLoading by remember { mutableStateOf(true) }
    var isModeSwitching by remember { mutableStateOf(false) }
    

    // 首屏进入加载模拟 (500ms 演示感)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        isInitialLoading = false
    }
    
    // 模式切换加载模拟 (每一次模式变更触发一次 400ms 的旋转转场)
    LaunchedEffect(uiState.learningMode) {
        if (!isInitialLoading) {
            isModeSwitching = true
            kotlinx.coroutines.delay(400)
            isModeSwitching = false
        }
    }
    
    val showLoadingRing = isInitialLoading || isModeSwitching
    // -------------------------
    
    // 深色模式适配逻辑
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val surfaceColor = colorScheme.surface
    val textMain = colorScheme.onSurface
    val textSub = colorScheme.onSurfaceVariant
    val textMuted = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val dividerColor = colorScheme.outlineVariant.copy(alpha = 0.2f)
    // 单一权威调色盘规范色 (系统动态主色，严格遵照 docs/design/color-palette 手册)
    val wordModeColor = if (isDark) SystemColors.OrangeDark else SystemColors.Orange
    val grammarModeColor = if (isDark) SystemColors.GreenDark else SystemColors.Green
    val primaryModeColor = if (uiState.learningMode == LearningMode.Word) wordModeColor else grammarModeColor

    val reviewColor = if (isDark) SystemColors.OrangeDark else SystemColors.Orange
    val completionColor = if (isDark) SystemColors.GreenDark else SystemColors.Green
    val heatmapColor = if (isDark) SystemColors.PurpleDark else SystemColors.Purple
    val kanaColor = if (isDark) SystemColors.BlueDark else SystemColors.Blue
    val verbColor = if (isDark) SystemColors.IndigoDark else SystemColors.Indigo
    val aiWorkshopColor = if (isDark) SystemColors.PurpleDark else SystemColors.Purple
    val aiReadingColor = if (isDark) SystemColors.GreenDark else SystemColors.Green


    val morningPhrases = stringArrayResource(R.array.home_greeting_subtitles_morning)
    val afternoonPhrases = stringArrayResource(R.array.home_greeting_subtitles_afternoon)
    val eveningPhrases = stringArrayResource(R.array.home_greeting_subtitles_evening)
    val nightPhrases = stringArrayResource(R.array.home_greeting_subtitles_night)

    // 动态生成副标题短语
    val subGreeting = remember(morningPhrases, afternoonPhrases, eveningPhrases, nightPhrases) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val phrases = when (hour) {
            in 5..10 -> morningPhrases
            in 11..17 -> afternoonPhrases
            in 18..23 -> eveningPhrases
            else -> nightPhrases
        }
        phrases.random()
    }

    // 动态生成问候语 (统一使用英文)
    val greeting = remember(uiState.user) {
        val name = uiState.user?.username ?: "Nemo"
        "Hi, $name さん"
    }

    var lastClickTime by remember { mutableLongStateOf(0L) }



    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarHeight + 16.dp,
                    bottom = navigationBarHeight + 104.dp
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .graphicsLayer {}
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = NotoSerifJP
                            ),
                            color = textMain,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.basicMarquee(),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subGreeting,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = textSub,
                            maxLines = 1
                        )
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    
                    AvatarImage(
                        username = uiState.user?.username ?: "Nemo",
                        avatarPath = uiState.user?.avatarUrl,
                        size = 44.dp,
                        borderWidth = 2.dp,
                        borderColor = textMuted.copy(alpha = 0.3f),
                        padding = 2.dp,
                        modifier = Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onNavigateToProfile
                        )
                    )
                }

            // 2. Bento Grid 核心布局区
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.graphicsLayer {},
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bento 1: 控制卡片 (全宽跨越)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor, RoundedRectangle(24.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeBgColor = if (isDark) BentoColors.SegmentTrackDark else MaterialTheme.colorScheme.surfaceVariant
                            val textColor = MaterialTheme.colorScheme.onSurface
                            
                            StreakBadgeWithEasterEgg(
                                streakDays = uiState.stats.dailyStreak,
                                badgeBgColor = badgeBgColor,
                                textColor = textColor
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            // 模式切换
                            val modeOptions = listOf(
                                stringResource(R.string.mode_word),
                                stringResource(R.string.mode_grammar)
                            )
                            val modeIndex = if (uiState.learningMode == LearningMode.Word) 0 else 1
                            BentoAnimatedSegmentedControl(
                                options = modeOptions,
                                selectedIndex = modeIndex,
                                isDark = isDark,
                                onOptionSelected = { index ->
                                    viewModel.setLearningMode(if (index == 0) LearningMode.Word else LearningMode.Grammar)
                                }
                            )
                        }
                    }

                    // 中部网格行: 进度卡片 + 统计数据
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bento 2: 进度大卡片 (左侧，纵跨)
                        Surface(
                            shape = RoundedRectangle(24.dp),
                            color = surfaceColor,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .softCardShadow(borderRadius = 24.dp, isDark = isDark)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(vertical = 24.dp, horizontal = 16.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.home_bento_learn_progress_title),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textSub
                                )
                                Spacer(Modifier.height(16.dp))
                                Box(contentAlignment = Alignment.Center) {
                                    val progressColor = primaryModeColor
                                    NemoCircularProgress(
                                        progress = uiState.progressFraction,
                                        isLoading = showLoadingRing,
                                        modifier = Modifier.size(100.dp),
                                        color = progressColor,
                                        trackColor = progressColor.copy(alpha = if (isDark) 0.15f else 0.12f)
                                    )
                                    
                                    // 数字只有在非加载状态下淡入显示
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !showLoadingRing,
                                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                                        exit = fadeOut()
                                    ) {
                                        Text(
                                            text = "${uiState.currentProgress}",
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontFamily = Rubik,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = textMain
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.home_bento_learn_goal_format, uiState.dailyGoal),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textMuted
                                )
                            }
                        }

                        // Bento 3 & 4: 统计数据卡片 (右侧上下排列)
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 统计 1: 待复习项目
                            Surface(
                                shape = RoundedRectangle(24.dp),
                                color = surfaceColor,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .softCardShadow(borderRadius = 24.dp, isDark = isDark)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    HomeSquircleIconBox(
                                        icon = Icons.Rounded.Restore,
                                        iconColor = reviewColor,
                                        containerSize = 36.dp,
                                        iconSize = 24.dp,
                                        cornerRadius = 10.dp
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    val reviewOutstanding = uiState.itemsDue
                                    val reviewDone = uiState.reviewedToday
                                    val reviewTotal = reviewDone + reviewOutstanding

                                    if (reviewOutstanding > 0) {
                                        Text(
                                            text = buildAnnotatedString {
                                                withStyle(
                                                    SpanStyle(
                                                        fontFamily = Rubik,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 30.sp
                                                    )
                                                ) {
                                                    append(reviewDone.toString())
                                                }
                                                withStyle(
                                                    SpanStyle(
                                                        fontFamily = Rubik,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp,
                                                        color = textSub
                                                    )
                                                ) {
                                                    append("/$reviewTotal")
                                                }
                                            },
                                            color = textMain
                                        )
                                    } else {
                                        Column {
                                            Text(
                                                text = stringResource(R.string.home_bento_review_empty),
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = textMain
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.home_bento_review_progress_title),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = textSub
                                    )
                                }
                            }
                            // 统计 2: 学习达成率（仅新学）
                            Surface(
                                shape = RoundedRectangle(24.dp),
                                color = surfaceColor,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .softCardShadow(borderRadius = 24.dp, isDark = isDark)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    HomeSquircleIconBox(
                                        icon = Icons.Rounded.CheckCircle,
                                        iconColor = completionColor,
                                        containerSize = 36.dp,
                                        iconSize = 24.dp,
                                        cornerRadius = 10.dp
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(
                                                SpanStyle(
                                                    fontFamily = Rubik,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 30.sp
                                                )
                                            ) {
                                                append(uiState.dailyCompletionRate.toString())
                                            }
                                            withStyle(
                                                SpanStyle(
                                                    fontFamily = Rubik,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = textSub
                                                )
                                            ) {
                                                append("%")
                                            }
                                        },
                                        color = textMain
                                    )
                                    Text(
                                        text = stringResource(R.string.label_completion_rate),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = textSub
                                    )
                                }
                            }
                        }
                    }
                }

                // Bento 5: 底部主按钮 (全宽)
                BentoStartButton(
                    btnColor = primaryModeColor,
                    currentProgress = uiState.currentProgress,
                    isDark = isDark,
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime > 2000L) {
                            lastClickTime = now
                            onNavigateToLearning(uiState.learningMode)
                        }
                    }
                )
            }

            // 3. 学习资源区块 (分组处理以确保间距逻辑对齐进度页)
            Column(modifier = Modifier.graphicsLayer {}) {
                HomeSectionTitle(
                        text = stringResource(R.string.title_learning_resources)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 全宽大卡片（特征区 - 热力图）
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .softCardShadow(borderRadius = 24.dp, isDark = isDark)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onNavigateToHeatmap
                                ),
                            shape = RoundedRectangle(24.dp),
                            color = surfaceColor,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HomeSquircleIconBox(
                                        icon = Icons.Rounded.EmojiEvents,
                                        iconColor = heatmapColor,
                                        containerSize = 42.dp,
                                        iconSize = 28.dp,
                                        cornerRadius = 12.dp
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.title_heatmap),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = textMain
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.desc_heatmap),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = textSub
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = dividerColor
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = textSub,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                            }
                        }

                        // 均分小卡片第一组：五十音图 + 动词活用手册 (基础资料速查)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 左边半宽: 五十音图
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .aspectRatio(1.3f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onNavigateToKanaChart
                                    ),
                                shape = RoundedRectangle(24.dp),
                                color = surfaceColor,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        HomeSquircleIconBox(
                                            icon = Icons.Rounded.Translate,
                                            iconColor = kanaColor,
                                            containerSize = 36.dp,
                                            iconSize = 24.dp,
                                            cornerRadius = 10.dp
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = textSub.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(R.string.menu_kana_chart_title),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = textMain
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.menu_kana_chart_subtitle),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSub
                                        )
                                    }
                                }
                            }

                            // 右边半宽: 动词活用
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .aspectRatio(1.3f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onNavigateToVerbHandbook
                                    ),
                                shape = RoundedRectangle(24.dp),
                                color = surfaceColor,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        HomeSquircleIconBox(
                                            icon = Icons.Rounded.Transform,
                                            iconColor = verbColor,
                                            containerSize = 36.dp,
                                            iconSize = 24.dp,
                                            cornerRadius = 10.dp
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = textSub.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(R.string.home_menu_verb_conjugation_title),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = textMain
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.home_menu_verb_conjugation_subtitle),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSub
                                        )
                                    }
                                }
                            }
                        }

                        // 均分小卡片第二组：AI 工坊 + AI 阅读 (智能专项工具)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 左边半宽: AI 工坊
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .aspectRatio(1.3f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onNavigateToAIWorkshop
                                    ),
                                shape = RoundedRectangle(24.dp),
                                color = surfaceColor,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        HomeSquircleIconBox(
                                            icon = Icons.Rounded.AutoAwesome,
                                            iconColor = aiWorkshopColor,
                                            containerSize = 36.dp,
                                            iconSize = 24.dp,
                                            cornerRadius = 10.dp
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = textSub.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(R.string.menu_ai_workshop_title),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = textMain
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.menu_ai_workshop_subtitle),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSub
                                        )
                                    }
                                }
                            }

                            // 右边半宽: AI 阅读
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .aspectRatio(1.3f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onNavigateToAIReading
                                    ),
                                shape = RoundedRectangle(24.dp),
                                color = surfaceColor,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        HomeSquircleIconBox(
                                            icon = Icons.Rounded.MenuBook,
                                            iconColor = aiReadingColor,
                                            containerSize = 36.dp,
                                            iconSize = 24.dp,
                                            cornerRadius = 10.dp
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = textSub.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(R.string.home_menu_ai_reading_title),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = textMain
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.home_menu_ai_reading_subtitle),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSub
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
        }

        ForcedNotificationPopup(
            notification = uiState.activeNotification,
            onDismiss = { viewModel.dismissNotification(it) },
            canDismissByBackdrop = false
        )
    }
}
}

/**
 * 带有平滑弹性滑动指示器的切换组件
 */
@Composable
private fun BentoAnimatedSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    isDark: Boolean,
    onOptionSelected: (Int) -> Unit
) {
    val trackColor = if (isDark) BentoColors.SegmentTrackDark else MaterialTheme.colorScheme.surfaceVariant
    val pillColor = if (isDark) BentoColors.SegmentThumbDark else BentoColors.Surface

    Surface(
        shape = CircleShape,
        color = trackColor
    ) {
        BoxWithConstraints(
            modifier = Modifier.padding(3.dp).height(32.dp)
        ) {
            val optionWidth = 60.dp

            // 滑动指示器位置动画 (弹性 Spring 动画)
            val indicatorOffset by animateDpAsState(
                targetValue = optionWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "indicatorOffset"
            )

            // 背景滑块 (Pill)
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(optionWidth)
                    .fillMaxHeight()
                    .shadow(
                        elevation = if (isDark) 2.dp else 1.dp,
                        shape = CircleShape,
                        clip = false
                    )
                    .clip(CircleShape)
                    .background(pillColor)
            )

            Row {
                options.forEachIndexed { index, text ->
                    val isSelected = selectedIndex == index
                    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else BentoColors.TextMain
                    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else BentoColors.TextSub
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) textMain else textSub,
                        animationSpec = tween(300),
                        label = "textColor_$index"
                    )

                    Box(
                        modifier = Modifier
                            .width(optionWidth)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onOptionSelected(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = textColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private val BentoStartShape = RoundedRectangle(24.dp)

@Composable
private fun BentoStartButton(
    btnColor: Color,
    currentProgress: Int,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val isTransitionSettled = rememberIsTransitionSettled()
    val isDecorativePaused = LocalDecorativeAnimationsPaused.current?.value == true
    val shouldRun = lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && isTransitionSettled && !isDecorativePaused
    val shouldRunState = rememberUpdatedState(shouldRun)

    val pulseAlpha = remember { Animatable(1f) }
    val shimmerProgress = remember { Animatable(-1.5f) }

    LaunchedEffect(shouldRun) {
        if (shouldRun) {
            launch {
                pulseAlpha.snapTo(1f)
                pulseAlpha.animateTo(
                    targetValue = 0.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            launch {
                shimmerProgress.snapTo(-1.5f)
                shimmerProgress.animateTo(
                    targetValue = 2.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2400, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        } else {
            pulseAlpha.snapTo(1f)
            shimmerProgress.snapTo(-1.5f)
        }
    }

    val isLearned = currentProgress > 0
    val iconVector = if (isLearned) Icons.Rounded.KeyboardDoubleArrowRight else Icons.Rounded.PlayArrow
    val iconPainter = rememberVectorPainter(iconVector)
    val whiteColorFilter = remember { ColorFilter.tint(Color.White) }
    val shimmerAlpha = if (isDark) 0.15f else 0.25f

    LiquidButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .softCardShadow(borderRadius = 24.dp, isDark = isDark),
        backgroundColor = btnColor,
        shape = BentoStartShape,
        elevation = 0.dp,
        clipToShape = false
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = if (isLearned) stringResource(R.string.btn_continue_home) else stringResource(R.string.btn_start_home),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(24.dp)
                    .graphicsLayer {
                        alpha = if (shouldRunState.value) pulseAlpha.value else 1f
                    }
                    .drawBehind {
                        with(iconPainter) {
                            draw(
                                size = size,
                                colorFilter = whiteColorFilter
                            )
                        }
                    }
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    clip = true
                    shape = BentoStartShape
                }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val active = shouldRunState.value
                        alpha = if (active) 1f else 0f
                        translationX = if (active) shimmerProgress.value * size.width else 0f
                    }
                    .drawWithCache {
                        val dx = size.width * 0.4f
                        val dy = size.height * 1.2f
                        val safeDx = dx.coerceAtLeast(1f)
                        val leftPad = (size.height * dy / safeDx) + 2f
                        val rightPad = (dy * dy / safeDx) + 2f
                        val rectWidth = maxOf(size.width, leftPad + dx + rightPad)
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = shimmerAlpha),
                                Color.Transparent
                            ),
                            start = Offset(leftPad, 0f),
                            end = Offset(leftPad + dx, dy)
                        )
                        onDrawBehind {
                            drawRect(
                                brush = shimmerBrush,
                                topLeft = Offset.Zero,
                                size = Size(rectWidth, size.height)
                            )
                        }
                    }
            )
        }
    }
}
