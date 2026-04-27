package com.jian.nemo.feature.learning.presentation.home

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlin.math.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.components.sheets.LevelSelectionBottomSheet
import com.jian.nemo.feature.learning.presentation.home.components.*
import com.jian.nemo.feature.learning.R

@Composable
fun HomeScreen(
    onNavigateToLearning: (String, LearningMode) -> Unit,
    onNavigateToKanaChart: () -> Unit,
    onNavigateToGrammarList: () -> Unit,
    onNavigateToHeatmap: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
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
    
    // 动态语义颜色
    val backgroundColor = if (isDark) colorScheme.background else BentoColors.BgBase
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else BentoColors.Surface
    val textMain = if (isDark) colorScheme.onSurface else BentoColors.TextMain
    val textSub = if (isDark) colorScheme.onSurfaceVariant else BentoColors.TextSub
    val textMuted = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else BentoColors.TextMuted
    val dividerColor = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.2f) else BentoColors.BgBase

    // 动态生成副标题短语 (中文随机版本)
    val subGreeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val phrases = when (hour) {
            in 5..10 -> listOf(
                "新的一天，从第一项学习开始。",
                "晨间微凉，静心学习最相宜。",
                "早安，今天也要元气满满。",
                "清晨的学习，是为了遇见更好的自己。",
                "日积月累，梦想终会开花。"
            )
            in 11..17 -> listOf(
                "享受午后的学习时光吧。",
                "阳光正好，适合温故而知新。",
                "午后的宁静，是进步最好的陪伴。",
                "偶尔小憩，是为了更有力地前进。",
                "慢慢来，每一步积累都算数。"
            )
            in 18..23 -> listOf(
                "今天辛苦了，收个好尾吧。",
                "晚风习习，伴你复习今日所得。",
                "总结今日，满怀期待迎接明天。",
                "夜晚的学习，是对心灵最好的慰藉。",
                "今日事今日毕，晚安前的最后冲刺。"
            )
            else -> listOf(
                "夜深了，忙完这项就早点休息哦。",
                "星光不问赶路人，但也要记得睡觉。",
                "深夜的灵感，请留给明天的晨曦。",
                "熬夜是不行的哦，身体才是本钱。",
                "静谧的夜，愿你带着收获入梦。"
            )
        }
        phrases.random()
    }

    // 动态生成问候语 (日文版本)
    val greeting = remember(uiState.user) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 0..4 -> "こんばんは"    // 凌晨/深夜
            in 5..10 -> "おはよう"    // 早上
            in 11..17 -> "こんにちは"  // 中午/下午
            in 18..23 -> "こんばんは"  // 傍晚/晚上
            else -> "こんにちは"
        }
        val name = uiState.user?.username ?: "Nemo"
        "$timeGreeting、$name さん"
    }

    if (uiState.showLevelSheet) {
        LevelSelectionBottomSheet(
            show = true,
            title = if (uiState.learningMode == LearningMode.Word)
                stringResource(R.string.title_select_word_level)
            else
                stringResource(R.string.title_select_grammar_level),
            levels = uiState.levels,
            selectedLevel = uiState.selectedLevel,
            primaryColor = if (uiState.learningMode == LearningMode.Word) BentoColors.Primary else BentoColors.GrammarPrimary,
            onDismiss = { viewModel.toggleLevelSheet(false) },
            onLevelSelected = {
                viewModel.selectLevel(it)
                viewModel.toggleLevelSheet(false)
            }
        )
    }

    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    // --- 主按钮呼吸闪烁动效 ---
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    // -------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusBarHeight + 16.dp,
                bottom = navigationBarHeight + 104.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 顶部 Header (动态日期与问候)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 20.dp),
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
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
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
            }

            // 2. Bento Grid 核心布局区
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bento 1: 控制卡片 (全宽跨越)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = surfaceColor,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 级别选择胶囊
                            Surface(
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { viewModel.toggleLevelSheet(true) }
                                ),
                                shape = CircleShape,
                                color = if (uiState.learningMode == LearningMode.Word) BentoColors.PrimaryLight else BentoColors.GrammarPrimaryLight
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "JLPT ${uiState.selectedLevel}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (uiState.learningMode == LearningMode.Word) BentoColors.Primary else BentoColors.GrammarPrimary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = (if (uiState.learningMode == LearningMode.Word) BentoColors.Primary else BentoColors.GrammarPrimary).copy(alpha = 0.5f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                            // 模式切换
                            Surface(
                                shape = CircleShape,
                                color = dividerColor
                            ) {
                                Row(Modifier.padding(4.dp)) {
                                    BentoModeSwitchButton(
                                        text = "单词",
                                        isSelected = uiState.learningMode == LearningMode.Word,
                                        isDark = isDark
                                    ) { viewModel.setLearningMode(LearningMode.Word) }
                                    BentoModeSwitchButton(
                                        text = "语法",
                                        isSelected = uiState.learningMode == LearningMode.Grammar,
                                        isDark = isDark
                                    ) { viewModel.setLearningMode(LearningMode.Grammar) }
                                }
                            }
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
                            shape = RoundedCornerShape(24.dp),
                            color = surfaceColor,
                            shadowElevation = 0.dp,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(vertical = 24.dp, horizontal = 16.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "今日新学进度",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textSub
                                )
                                Spacer(Modifier.height(16.dp))
                                Box(contentAlignment = Alignment.Center) {
                                    NemoCircularProgress(
                                        progress = uiState.progressFraction,
                                        isLoading = showLoadingRing,
                                        modifier = Modifier.size(100.dp),
                                        color = if (uiState.learningMode == LearningMode.Word) BentoColors.Primary else BentoColors.GrammarPrimary,
                                        trackColor = dividerColor
                                    )
                                    
                                    // 数字只有在非加载状态下淡入显示
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !showLoadingRing,
                                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                                        exit = fadeOut()
                                    ) {
                                        Text(
                                            text = "${uiState.currentProgress}",
                                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                                            color = textMain
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "新学目标 ${uiState.dailyGoal}",
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
                                shape = RoundedCornerShape(24.dp),
                                color = surfaceColor,
                                shadowElevation = 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BentoColors.IconBgOrange
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Restore,
                                            contentDescription = null,
                                            tint = BentoColors.AccentOrange,
                                            modifier = Modifier.padding(8.dp).size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    val reviewOutstanding = uiState.itemsDue
                                    val reviewDone = uiState.reviewedToday
                                    val reviewTotal = reviewDone + reviewOutstanding

                                    if (reviewOutstanding > 0) {
                                        Text(
                                            text = buildAnnotatedString {
                                                append(reviewDone.toString())
                                                withStyle(
                                                    SpanStyle(
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = textSub
                                                    )
                                                ) {
                                                    append("/$reviewTotal")
                                                }
                                            },
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontSize = 30.sp,
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = textMain
                                        )
                                    } else {
                                        Column {
                                            Text(
                                                text = "暂无复习项目",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = textMain
                                            )
                                        }
                                    }
                                    Text(
                                        text = "复习进度",
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
                                shape = RoundedCornerShape(24.dp),
                                color = surfaceColor,
                                shadowElevation = 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BentoColors.IconBgGreen
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = BentoColors.AccentGreen,
                                            modifier = Modifier.padding(8.dp).size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "${uiState.dailyCompletionRate}%",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Black
                                        ),
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

                    // Bento 5: 底部主按钮 (全宽)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigateToLearning(uiState.selectedLevel, uiState.learningMode) }
                            ),
                        shape = RoundedCornerShape(24.dp),
                        color = if (uiState.learningMode == LearningMode.Word) BentoColors.Primary else BentoColors.GrammarPrimary,
                        contentColor = Color.White
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = if (uiState.currentProgress > 0) stringResource(R.string.btn_continue_home) else stringResource(R.string.btn_start_home),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            )
                            // 基于学习进度切换图标及应用闪烁动效
                            val isLearned = uiState.currentProgress > 0
                            Icon(
                                imageVector = if (isLearned) Icons.Rounded.KeyboardDoubleArrowRight else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        alpha = pulseAlpha
                                    },
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // 3. 学习资源区块 (分组处理以确保间距逻辑对齐进度页)
            item {
                Column {
                    Text(
                        text = stringResource(R.string.title_learning_resources),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = textSub,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp, bottom = 12.dp) // Gap Above = 20 (spacedBy) + 12 = 32 | Gap Below = 12
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 全宽大卡片（特征区 - 热力图）
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onNavigateToHeatmap
                                ),
                            shape = RoundedCornerShape(24.dp),
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
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = BentoColors.IconBgPurple
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = BentoColors.AccentPurple,
                                            modifier = Modifier.padding(12.dp).size(24.dp)
                                        )
                                    }
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

                        // 均分小卡片（单词本、语法点）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 左边半宽: 单词本
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
                                shape = RoundedCornerShape(24.dp),
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
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BentoColors.IconBgBlue
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Language,
                                                contentDescription = null,
                                                tint = BentoColors.AccentBlue,
                                                modifier = Modifier.padding(10.dp).size(20.dp)
                                            )
                                        }
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

                            // 右边半宽: 语法点
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .aspectRatio(1.3f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onNavigateToGrammarList
                                    ),
                                shape = RoundedCornerShape(24.dp),
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
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BentoColors.IconBgGreen
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Create,
                                                contentDescription = null,
                                                tint = BentoColors.AccentGreen,
                                                modifier = Modifier.padding(10.dp).size(20.dp)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = textSub.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(R.string.menu_grammar_book_title),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = textMain
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.menu_grammar_book_subtitle),
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
        }

        ForcedNotificationPopup(
            notification = uiState.activeNotification,
            onDismiss = { viewModel.dismissNotification(it) },
            canDismissByBackdrop = false
        )
    }
}

/**
 * 局部复用的私有切换按钮组件（避免影响或修改 HomeComponents.kt 中的封装）
 */
@Composable
private fun BentoModeSwitchButton(
    text: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else BentoColors.Surface
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else BentoColors.TextMain
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else BentoColors.TextSub

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) surfaceColor else Color.Transparent,
        label = "bgColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) textMain else textSub,
        label = "textColor"
    )

    Surface(
        modifier = Modifier
            .height(30.dp)
            .padding(horizontal = 2.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
            )
        }
    }
}

/**
 * 自定义高保真环形进度条组件 (直接填充版)
 * 具备“呼吸缺口”与“数值生长动效”
 */
@Composable
private fun NemoCircularProgress(
    progress: Float,
    isLoading: Boolean, // 用于触发归零重填转场
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val strokeWidth = 12.dp
    
    // 进度值从 0 平滑生长至目标值 (Cubic 曲线)
    val animatedProgress by animateFloatAsState(
        targetValue = if (isLoading) 0f else progress,
        animationSpec = tween(
            durationMillis = 800,
            easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1.0f)
        ),
        label = "progress"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val strokeWidthPx = strokeWidth.toPx()
        val radius = (size.minDimension - strokeWidthPx) / 2
        
        // 显式构造 Rect
        val rect = Rect(
            left = center.x - radius,
            top = center.y - radius,
            right = center.x + radius,
            bottom = center.y + radius
        )

        val gapAngleDegrees = if (radius > 0) (1.5f * strokeWidthPx / radius) * (180f / PI.toFloat()) else 0f
        val progressSweep = animatedProgress * 360f

        // 1. 绘制进度条 (Progress)
        if (progressSweep > 0.1f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progressSweep,
                useCenter = false,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // 2. 绘制底轨 (Track) - 包含动态避让缺口逻辑
        val trackSweepAngle = if (progressSweep > 0.1f) {
            (360f - progressSweep - 2 * gapAngleDegrees).coerceAtLeast(0f)
        } else {
            360f
        }

        if (trackSweepAngle > 1f) {
            val trackStartAngle = if (progressSweep > 0.1f) {
                -90f + progressSweep + gapAngleDegrees
            } else {
                -90f
            }

            drawArc(
                color = trackColor,
                startAngle = trackStartAngle,
                sweepAngle = trackSweepAngle,
                useCenter = false,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(
                    width = strokeWidthPx, 
                    cap = if (progressSweep > 0.1f) StrokeCap.Round else StrokeCap.Butt
                )
            )
        }
    }
}



