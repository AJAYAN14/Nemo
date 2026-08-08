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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.designsystem.theme.Rubik
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.component.progress.NemoCircularProgress
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.ui.modifier.softShadow
import com.jian.nemo.feature.learning.presentation.LearningMode
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

    // 动态生成问候语 (统一使用英文)
    val greeting = remember(uiState.user) {
        val name = uiState.user?.username ?: "Nemo"
        "Hi, $name さん"
    }

    var lastClickTime by remember { mutableLongStateOf(0L) }



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
            }

            // 2. Bento Grid 核心布局区
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bento 1: 控制卡片 (全宽跨越)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor, RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeBgColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else BentoColors.BgBase
                            val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else BentoColors.TextMain
                            
                            StreakBadgeWithEasterEgg(
                                streakDays = uiState.stats.dailyStreak,
                                badgeBgColor = badgeBgColor,
                                textColor = textColor
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            // 模式切换
                            val modeOptions = listOf("单词", "语法")
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
                            shape = RoundedCornerShape(24.dp),
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

                    // Bento 5: 底部主按钮 (全宽)
                    val btnColor = if (uiState.learningMode == LearningMode.Word) BentoColors.Primary else BentoColors.GrammarPrimary

                    // 2.4s 45° 斜切流光扫过 (Shimmer Sweep) 动效
                    val shimmerTransition = rememberInfiniteTransition(label = "Bento5Shimmer")
                    val shimmerProgress by shimmerTransition.animateFloat(
                        initialValue = -1.5f,
                        targetValue = 2.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2400, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "ShimmerProgress"
                    )

                    LiquidButton(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime > 2000L) {
                                lastClickTime = now
                                onNavigateToLearning(uiState.learningMode)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        backgroundColor = btnColor,
                        shape = RoundedCornerShape(24.dp),
                        elevation = 6.dp
                    ) {
                        // 按钮内容
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = if (uiState.currentProgress > 0) stringResource(R.string.btn_continue_home) else stringResource(R.string.btn_start_home),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = Color.White
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
                        // 45° 斜切流光扫过绘制 (Shimmer Sweep Effect)，放在 content 内部以跟随按钮拖拽位移
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawWithContent {
                                    drawContent()
                                    val width = size.width
                                    val height = size.height
                                    val xOffset = shimmerProgress * width
                                    val shimmerBrush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = if (isDark) 0.15f else 0.25f),
                                            Color.Transparent
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(xOffset, 0f),
                                        end = androidx.compose.ui.geometry.Offset(xOffset + width * 0.4f, height * 1.2f)
                                    )
                                    val cornerRadiusPx = 24.dp.toPx()
                                    val clipPathObj = androidx.compose.ui.graphics.Path().apply {
                                        addRoundRect(
                                            androidx.compose.ui.geometry.RoundRect(
                                                rect = androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, size),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
                                            )
                                        )
                                    }
                                    clipPath(clipPathObj) {
                                        drawRect(brush = shimmerBrush)
                                    }
                                }
                        )
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
                                .softCardShadow(borderRadius = 24.dp, isDark = isDark)
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
                                            color = if (isDark) IosColors.IndigoDark.copy(alpha = 0.15f) else IosColors.Indigo.copy(alpha = 0.1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Transform,
                                                contentDescription = null,
                                                tint = if (isDark) IosColors.IndigoDark else IosColors.Indigo,
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
                                            text = "动词活用",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = textMain
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "13种变形速查",
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
                                            color = BentoColors.IconBgPurple
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = BentoColors.AccentPurple,
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
                                                imageVector = Icons.Rounded.ChromeReaderMode,
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
                                            text = "AI 阅读",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = textMain
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "分级智能短文",
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
 * 带有平滑弹性滑动指示器的切换组件
 */
@Composable
private fun BentoAnimatedSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    isDark: Boolean,
    onOptionSelected: (Int) -> Unit
) {
    val dividerColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else BentoColors.BgBase
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else BentoColors.Surface

    Surface(
        shape = CircleShape,
        color = dividerColor
    ) {
        BoxWithConstraints(
            modifier = Modifier.padding(4.dp).height(30.dp)
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
                    .clip(CircleShape)
                    .background(surfaceColor)
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
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
