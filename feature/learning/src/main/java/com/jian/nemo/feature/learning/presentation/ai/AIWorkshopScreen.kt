package com.jian.nemo.feature.learning.presentation.ai

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.domain.model.AIGradeResult
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import com.jian.nemo.feature.learning.presentation.ai.components.AIAuroraBackground
import androidx.compose.foundation.text.selection.SelectionContainer
import com.jian.nemo.feature.learning.R
import com.jian.nemo.core.designsystem.R as DesignR
import com.jian.nemo.core.ui.component.speaker.SpeakerButton
import com.jian.nemo.core.ui.component.speaker.SoundWaveAnimation
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.core.text.HtmlCompat



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIWorkshopScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: AIWorkshopViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showHelp by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    
    // 语义化颜色映射
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val textPrimary = colorScheme.onSurface
    val haptic = LocalHapticFeedback.current

    // 监听错误，触发震动
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val backdrop = rememberLayerBackdrop()

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
        // 1. 底层页面滚动内容（被 backdrop 采样源录制，全屏贯穿至状态栏）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部弹性占位：未滑动时避开顶栏，滑动时无阻碍滑入顶栏遮罩
                Spacer(modifier = Modifier.height(statusBarTop + 60.dp + 12.dp))

                if (!uiState.isConfigured) {
                    ConfigRequiredView(onNavigateToSettings = onNavigateToSettings)
                } else {
                    // 核心操作区
                    WorkshopContent(
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                }
                
                Spacer(modifier = Modifier.height(navBarBottom + if (uiState.currentExercise == null) 100.dp else 32.dp))
            }
        }

        // 2. 底部悬浮“开始练习”全宽大按钮（未开始答题时显示，带渐变防遮挡遮罩）
        if (uiState.isConfigured && uiState.currentExercise == null && !(uiState.workshopMode == WorkshopMode.GRAMMAR && !uiState.hasGrammarData)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor.copy(alpha = 0.85f),
                                backgroundColor
                            )
                        )
                    )
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = navBarBottom + 16.dp)
            ) {
                Button(
                    onClick = { viewModel.onEvent(AIWorkshopEvent.GenerateNewExercise) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedRectangle(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(stringResource(R.string.workshop_btn_start_practice), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
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
                title = stringResource(R.string.learn_workshop_title),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FrostedLiquidGlassButton(
                            onClick = { viewModel.onEvent(AIWorkshopEvent.QuickSwitchPlatform) },
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
                                else -> Icon(Icons.Rounded.Memory, contentDescription = "Custom", modifier = Modifier.size(30.dp), tint = textPrimary)
                            }
                        }
                        FrostedLiquidGlassButton(
                            onClick = onNavigateToHistory,
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.learn_workshop_desc_history), modifier = Modifier.size(30.dp), tint = textPrimary)
                        }
                        FrostedLiquidGlassButton(
                            onClick = { showHelp = true },
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = stringResource(R.string.learn_workshop_desc_help), modifier = Modifier.size(30.dp), tint = textPrimary)
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
                    shape = RoundedRectangle(20.dp),
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

        // 加载层 - 采用 code_artifact.html WebGL2 全屏极光引擎 + 浮动 AI 动态思维链硬解码卡片
        if (uiState.isLoading) {
            val context = LocalContext.current
            val visibleSteps = remember { mutableStateListOf<WorkshopThinkingStepItem>() }
            var currentLibIndex by remember { mutableIntStateOf(0) }
            var stepCounter by remember { mutableIntStateOf(0) }
            val maxVisibleCount = 4 // 固定最多显示 4 条，第 5 条出现时第 1 条平滑消失
            val thinkingSteps = remember(context) {
                WorkshopThinkingStepResIds.map { context.getString(it) }
            }

            LaunchedEffect(Unit) {
                while (true) {
                    val stepText = thinkingSteps[currentLibIndex % thinkingSteps.size]
                    currentLibIndex++
                    val newItemId = ++stepCounter
                    val newItem = WorkshopThinkingStepItem(uniqueId = newItemId, text = stepText, isCompleted = false)

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
                    .background(colorScheme.background)
                    .clickable(enabled = false) {}
            ) {
                // 1. 底层：全屏 WebGL2 极光引擎
                AIAuroraBackground(
                    isDark = isDark,
                    containerBg = colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. 顶层：悬浮 AI 动态思维链硬解码卡片（避开 statusBarTop + 64dp）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarTop + 64.dp, start = 18.dp, end = 18.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Surface(
                        shape = RoundedRectangle(22.dp),
                        color = if (isDark) LearningPalette.AiSlateText.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.14f) else LearningPalette.DrawerBorderLight
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
                                        tint = if (isDark) LearningPalette.AiSkyBlueDark else LearningPalette.AiRoyalBlueLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.workshop_ai_generating),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = if (isDark) Color.White else LearningPalette.AiSlateText
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
                                                        .background(LearningPalette.ReadingAccentGreen),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = stringResource(R.string.status_completed),
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
                                                        color = if (isDark) LearningPalette.AiSkyBlueDark else LearningPalette.AiRoyalBlueLight
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(10.dp))

                                            // 右侧文字：假名乱码硬解码 -> 锁定真文
                                            WorkshopHardDecodedText(
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

    uiState.error?.let {
            NemoSnackbar(
                visible = true,
                message = stringResource(R.string.learn_workshop_error_retry),
                type = NemoSnackbarType.ERROR,
                icon = Icons.Rounded.ErrorOutline,
                onDismiss = { viewModel.onEvent(AIWorkshopEvent.ClearError) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarTop + 8.dp)
            )
        }

        // 帮助弹窗
        if (showHelp) {
            HelpDialog(onDismiss = { showHelp = false })
        }
    }
}
}

@Composable
private fun WorkshopContent(
    uiState: AIWorkshopUiState,
    onEvent: (AIWorkshopEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. 模式切换面板（Apple Style 胶囊）
        ModeSection(
            currentMode = uiState.workshopMode,
            onModeChange = { onEvent(AIWorkshopEvent.UpdateWorkshopMode(it)) },
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. 难度切换
        DifficultySection(
            currentLevel = uiState.difficulty,
            onLevelChange = { onEvent(AIWorkshopEvent.UpdateDifficulty(it)) },
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 主视图切换
        AnimatedContent(
            targetState = uiState.currentExercise == null,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ViewSwitch"
        ) { isStartView ->
            if (isStartView) {
                // 语法专项模式下，如果无数据则显示兜底视图
                if (uiState.workshopMode == WorkshopMode.GRAMMAR && !uiState.hasGrammarData) {
                    GrammarDataEmptyView()
                } else {
                    HeroStartView(
                        isGrammarMode = uiState.workshopMode == WorkshopMode.GRAMMAR
                    )
                }
            } else {
                ExerciseMainView(
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }
}

/**
 * 模式切换：流体高光胶囊控件
 */
@Composable
private fun ModeSection(
    currentMode: WorkshopMode,
    onModeChange: (WorkshopMode) -> Unit,
    enabled: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val activeColor = if (currentMode == WorkshopMode.GRAMMAR) {
        if (isDark) IosColors.GreenDark else IosColors.Green
    } else {
        if (isDark) IosColors.BlueDark else IosColors.Blue
    }
    val trackBg = if (isDark) colorScheme.surfaceContainerHigh else LearningPalette.AiTrackBgLight
    val haptic = LocalHapticFeedback.current
    val modes = listOf(WorkshopMode.FREE, WorkshopMode.GRAMMAR)
    val modeLabels = listOf(stringResource(R.string.learn_workshop_mode_free), stringResource(R.string.learn_workshop_mode_grammar))
    val selectedIndex = modes.indexOf(currentMode).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(trackBg, RoundedRectangle(14.dp))
            .border(
                width = 1.dp,
                color = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.2f) else LearningPalette.AiBorderLight.copy(alpha = 0.8f),
                shape = RoundedRectangle(14.dp)
            )
            .padding(4.dp)
    ) {
        val animatedIndex by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = 420f
            ),
            label = "modeCapsuleIndex"
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(animatedIndex.coerceAtLeast(0.001f)))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .shadow(elevation = 2.dp, shape = RoundedRectangle(11.dp))
                    .background(
                        color = activeColor,
                        shape = RoundedRectangle(11.dp)
                    )
            )
            Spacer(modifier = Modifier.weight((modes.size - 1 - animatedIndex).coerceAtLeast(0.001f)))
        }

        Row(modifier = Modifier.fillMaxSize()) {
            modes.forEachIndexed { index, mode ->
                val isSelected = mode == currentMode
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else (if (isDark) colorScheme.onSurfaceVariant else LearningPalette.AiMutedText),
                    animationSpec = tween(180),
                    label = "modeTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedRectangle(11.dp))
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onModeChange(mode)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = modeLabels[index],
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

/**
 * 语法数据为空时的兜底视图
 */
@Composable
private fun GrammarDataEmptyView() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)

    val emptyIconColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else NemoNeutrals.Gray500
    val emptyContainerBg = if (isSoftTranslucent) {
        if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray100
    } else {
        emptyIconColor
    }
    val emptyIconTint = if (isSoftTranslucent) emptyIconColor else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(emptyContainerBg, RoundedRectangle(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = emptyIconTint
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.learn_workshop_empty_grammar_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.learn_workshop_empty_grammar_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun DifficultySection(
    currentLevel: String,
    onLevelChange: (String) -> Unit,
    enabled: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val activeColor = if (isDark) IosColors.IndigoDark else IosColors.Indigo
    val trackBg = if (isDark) colorScheme.surfaceContainerHigh else LearningPalette.AiTrackBgLight
    val haptic = LocalHapticFeedback.current
    val levels = listOf("N5", "N4", "N3", "N2", "N1")
    val selectedIndex = levels.indexOf(currentLevel).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(trackBg, RoundedRectangle(14.dp))
            .border(
                width = 1.dp,
                color = if (isDark) colorScheme.outlineVariant.copy(alpha = 0.2f) else LearningPalette.AiBorderLight.copy(alpha = 0.8f),
                shape = RoundedRectangle(14.dp)
            )
            .padding(4.dp)
    ) {
        val animatedIndex by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = 420f
            ),
            label = "difficultyCapsuleIndex"
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(animatedIndex.coerceAtLeast(0.001f)))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .shadow(elevation = 2.dp, shape = RoundedRectangle(11.dp))
                    .background(
                        color = activeColor,
                        shape = RoundedRectangle(11.dp)
                    )
            )
            Spacer(modifier = Modifier.weight((levels.size - 1 - animatedIndex).coerceAtLeast(0.001f)))
        }

        Row(modifier = Modifier.fillMaxSize()) {
            levels.forEach { level ->
                val isSelected = level == currentLevel
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else (if (isDark) colorScheme.onSurfaceVariant else LearningPalette.AiMutedText),
                    animationSpec = tween(180),
                    label = "difficultyTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedRectangle(11.dp))
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLevelChange(level)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStartView(
    isGrammarMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val textPrimary = colorScheme.onSurface

    val icon = when {
        isGrammarMode -> Icons.AutoMirrored.Rounded.MenuBook
        else -> Icons.Rounded.AutoAwesome
    }
    val title = when {
        isGrammarMode -> stringResource(R.string.learn_workshop_hero_title_grammar)
        else -> stringResource(R.string.learn_workshop_hero_title_free)
    }
    val desc = when {
        isGrammarMode -> stringResource(R.string.learn_workshop_hero_desc_grammar)
        else -> stringResource(R.string.learn_workshop_hero_desc_free)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val iconStyle = LocalSettingIconStyle.current
        val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
        val heroBaseColor = if (isGrammarMode) {
            if (isDark) IosColors.GreenDark else IosColors.Green
        } else {
            if (isDark) IosColors.BlueDark else IosColors.Blue
        }
        val heroContainerBg = if (isSoftTranslucent) heroBaseColor.copy(alpha = 0.15f) else heroBaseColor
        val heroIconTint = if (isSoftTranslucent) heroBaseColor else Color.White

        Box(
            modifier = Modifier
                .size(84.dp)
                .background(heroContainerBg, RoundedRectangle(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = heroIconTint
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            desc,
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun ExerciseMainView(
    uiState: AIWorkshopUiState,
    onEvent: (AIWorkshopEvent) -> Unit
) {
    ExerciseContent(uiState, onEvent)
}

@Composable
private fun ExerciseContent(
    uiState: AIWorkshopUiState,
    onEvent: (AIWorkshopEvent) -> Unit
) {
    val exercise = uiState.currentExercise ?: return

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f) else NemoNeutrals.Gray100

    Column(modifier = Modifier.fillMaxWidth()) {
        // 题目面板
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shape = RoundedRectangle(24.dp),
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isCnToJp = exercise.type == "CN_TO_JP"
                    val labelColor = if (isCnToJp) {
                        if (isDark) IosColors.BlueDark else IosColors.Blue
                    } else {
                        if (isDark) IosColors.PurpleDark else IosColors.Purple
                    }
                    Surface(
                        color = labelColor.copy(alpha = if (isDark) 0.16f else 0.1f),
                        shape = RoundedRectangle(6.dp)
                    ) {
                        Text(
                            text = if (isCnToJp) stringResource(R.string.learn_workshop_type_cn_to_jp) else stringResource(R.string.learn_workshop_type_jp_to_cn),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // 语法专项模式标签
                    uiState.currentGrammarPoint?.let { grammarPoint ->
                        Spacer(modifier = Modifier.width(8.dp))
                        val grammarColor = if (isDark) IosColors.GreenDark else IosColors.Green
                        Surface(
                            color = grammarColor.copy(alpha = if (isDark) 0.16f else 0.1f),
                            shape = RoundedRectangle(6.dp)
                        ) {
                            val label = buildString {
                                append(grammarPoint)
                                uiState.currentGrammarSubtype?.let { subtype ->
                                    if (subtype.isNotBlank()) append(" · $subtype")
                                }
                            }
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = grammarColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (exercise.type == "JP_TO_CN") {
                        val isPlaying = uiState.playingAudioId == "question"
                        val audioTint = if (isDark) IosColors.BlueDark else IosColors.Blue
                        FrostedLiquidGlassButton(
                            onClick = { onEvent(AIWorkshopEvent.SpeakText(exercise.question, "question")) },
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (isPlaying) {
                                SoundWaveAnimation(
                                    tint = audioTint,
                                    size = 20.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = stringResource(R.string.learn_workshop_desc_speak),
                                    tint = audioTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    FrostedLiquidGlassButton(
                        onClick = { onEvent(AIWorkshopEvent.GenerateNewExercise) },
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        isInteractive = true,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.learn_workshop_desc_refresh),
                            tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.question,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 34.sp
                        )
                    }
                }

                
                if (exercise.hints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    val hintColor = if (isDark) IosColors.OrangeDark else IosColors.Orange
                    val hintBgColor = hintColor.copy(alpha = if (isDark) 0.12f else 0.08f)
                    val hintBorder = BorderStroke(0.5.dp, hintColor.copy(alpha = if (isDark) 0.3f else 0.18f))
                    Surface(
                        color = hintBgColor,
                        shape = RoundedRectangle(12.dp),
                        border = hintBorder
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = hintColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.learn_workshop_hint_prefix, exercise.hints.joinToString(", ")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 输入与评分区
        if (uiState.gradeResult == null) {
            InputSection(
                answer = uiState.userAnswer,
                onAnswerChange = { onEvent(AIWorkshopEvent.UpdateUserAnswer(it)) },
                onSubmit = { onEvent(AIWorkshopEvent.SubmitAnswer) },
                enabled = !uiState.isLoading
            )
        } else {
            FeedbackSection(
                result = uiState.gradeResult,
                userAnswer = uiState.userAnswer,
                exerciseType = exercise.type,
                playingAudioId = uiState.playingAudioId,
                onSpeak = { text, id -> onEvent(AIWorkshopEvent.SpeakText(text, id)) },
                onNext = { onEvent(AIWorkshopEvent.GenerateNewExercise) }
            )

        }
    }
}

@Composable
private fun InputSection(
    answer: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    Column {
        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            placeholder = { 
                Text(
                    stringResource(R.string.learn_workshop_input_placeholder),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            shape = RoundedRectangle(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.4f),
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor,
                disabledContainerColor = surfaceColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            enabled = enabled
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedRectangle(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray200,
                disabledContentColor = if (isDark) Color.White.copy(alpha = 0.3f) else NemoNeutrals.Gray400
            ),
            enabled = answer.isNotBlank() && enabled,
            elevation = null
        ) {
            Text(stringResource(R.string.learn_workshop_btn_submit_eval), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeedbackSection(
    result: AIGradeResult,
    userAnswer: String,
    exerciseType: String,
    playingAudioId: String?,
    onSpeak: (String, String) -> Unit,
    onNext: () -> Unit
) {

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val scoreColor = when {
        result.score >= 80 -> if (isDark) IosColors.GreenDark else IosColors.Green
        result.score >= 60 -> if (isDark) IosColors.OrangeDark else IosColors.Orange
        else -> if (isDark) IosColors.RedDark else IosColors.Red
    }


    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shape = RoundedRectangle(24.dp),
            border = BorderStroke(2.dp, scoreColor.copy(alpha = 0.3f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = if (result.is_correct) stringResource(R.string.learn_workshop_feedback_great) else stringResource(R.string.learn_workshop_feedback_keep_going),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.learn_workshop_feedback_score_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Surface(
                        color = scoreColor,
                        shape = RoundedRectangle(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.learn_workshop_feedback_score_format, result.score),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                
                // 我的答案
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.learn_workshop_label_my_answer),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.05f),
                    shape = RoundedRectangle(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            text = userAnswer,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 参考答案展示
                result.standard_answer?.let { answer ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.learn_workshop_label_standard_answer),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NemoSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = NemoSecondary.copy(alpha = if (isDark) 0.15f else 0.05f),
                        shape = RoundedRectangle(12.dp),
                        border = BorderStroke(1.dp, NemoSecondary.copy(alpha = if (isDark) 0.3f else 0.1f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            SelectionContainer(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = answer,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (exerciseType == "CN_TO_JP") {
                                Spacer(modifier = Modifier.width(8.dp))
                                val isPlaying = playingAudioId == "standard"
                                FrostedLiquidGlassButton(
                                    onClick = { onSpeak(answer, "standard") },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    isInteractive = true,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    if (isPlaying) {
                                        SoundWaveAnimation(
                                            tint = NemoSecondary,
                                            size = 18.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = stringResource(R.string.learn_workshop_desc_speak),
                                            tint = NemoSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(0.2f) else NemoNeutrals.Gray100)
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = stringResource(R.string.learn_workshop_label_ai_review),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = result.feedback,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ColoredLiquidGlassButton(
            onClick = onNext,
            tint = MaterialTheme.colorScheme.primary,
            shape = RoundedRectangle(18.dp),
            isInteractive = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text(stringResource(R.string.learn_workshop_btn_next_question), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ConfigRequiredView(onNavigateToSettings: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)

    val configIconColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f) else NemoNeutrals.Gray500
    val configContainerBg = if (isSoftTranslucent) {
        if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray100
    } else {
        configIconColor
    }
    val configIconTint = if (isSoftTranslucent) configIconColor else Color.White
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(configContainerBg, RoundedRectangle(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Memory,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = configIconTint
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.learn_workshop_config_required_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.learn_workshop_config_required_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        ColoredLiquidGlassButton(
            onClick = onNavigateToSettings,
            tint = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            isInteractive = true,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.learn_workshop_btn_go_config),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    NemoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.learn_workshop_help_title),
        confirmText = stringResource(R.string.learn_workshop_help_btn_confirm),
        dismissText = null,
        onConfirm = onDismiss,
        content = {
            Column {
                HelpItem("1", stringResource(R.string.learn_workshop_help_item_1_title), stringResource(R.string.learn_workshop_help_item_1_desc))
                HelpItem("2", stringResource(R.string.learn_workshop_help_item_2_title), stringResource(R.string.learn_workshop_help_item_2_desc))
                HelpItem("3", stringResource(R.string.learn_workshop_help_item_3_title), stringResource(R.string.learn_workshop_help_item_3_desc))
                HelpItem("4", stringResource(R.string.learn_workshop_help_item_4_title), stringResource(R.string.learn_workshop_help_item_4_desc))
            }
        }
    )
}

@Composable
private fun HelpItem(index: String, title: String, content: String) {
    Row(modifier = Modifier.padding(vertical = 10.dp)) {
        Surface(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = index,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

private val WorkshopThinkingStepResIds = listOf(
    R.string.learning_ai_workshop_label_step_parse_requirements,
    R.string.learning_ai_workshop_label_step_search_context,
    R.string.learning_ai_workshop_label_step_build_skeleton,
    R.string.learning_ai_workshop_label_step_check_conjugation,
    R.string.learning_ai_workshop_label_step_generate_candidates,
    R.string.learning_ai_workshop_label_step_optimize_grammar,
    R.string.learning_ai_workshop_label_step_build_translation,
    R.string.learning_ai_workshop_label_step_draft_analysis,
    R.string.learning_ai_workshop_label_step_prepare_scoring,
    R.string.learning_ai_workshop_label_step_assemble_exercise
)

private data class WorkshopThinkingStepItem(
    val uniqueId: Int,
    val text: String,
    val isCompleted: Boolean = false
)

/**
 * 例文工坊假名乱码硬解码文本组件 (Hard-Decoded Text)
 */
@Composable
private fun WorkshopHardDecodedText(
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
            if (isDark) Color.White.copy(alpha = 0.95f) else LearningPalette.AiSlateText
        } else {
            if (isDark) LearningPalette.AiSkyBlueDark else LearningPalette.AiRoyalBlueLight
        },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

