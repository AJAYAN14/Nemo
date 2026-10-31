package com.jian.nemo.feature.test.presentation.ability

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.data.local.entity.TestRecordEntity
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.NemoDanger
import com.jian.nemo.core.designsystem.theme.NemoDangerDark
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoPrimaryDark
import com.jian.nemo.core.designsystem.theme.NemoSecondary
import com.jian.nemo.core.designsystem.theme.NemoSecondaryDark
import com.jian.nemo.core.designsystem.theme.NemoWarning
import com.jian.nemo.core.designsystem.theme.NemoWarningDark
import com.jian.nemo.core.designsystem.theme.TestResultPalette
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.ui.component.common.CommonHeader
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordClozeScreen(
    onNavigateBack: () -> Unit,
    viewModel: WordClozeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500
    val containerColor = MaterialTheme.colorScheme.background
    val wordCardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val qCardBg = if (isDark) IosColors.Orange.copy(alpha = 0.15f) else IosColors.Orange.copy(alpha = 0.08f)
    val qCardBorderColor = if (isDark) IosColors.Orange.copy(alpha = 0.2f) else IosColors.Orange.copy(alpha = 0.1f)

    var showHistoryView by remember { mutableStateOf(false) }
    val historyRecords by viewModel.historyRecords.collectAsState(initial = emptyList())

    var showExitDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = uiState is ClozeUiState.Ready) {
        showExitDialog = true
    }

    if (showHistoryView) {
        ClozeHistoryView(
            historyRecords = historyRecords,
            onBack = { showHistoryView = false },
            isDark = isDark
        )
        return
    }

    Scaffold(
        topBar = {
            CommonHeader(
                title = stringResource(R.string.test_cloze_header_title),
                onBack = {
                    if (uiState is ClozeUiState.Ready) {
                        showExitDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                actions = {
                    if (uiState is ClozeUiState.Ready) {
                        IconButton(onClick = { viewModel.forceRegenerate() }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.test_cloze_cd_refresh),
                                tint = textMain
                            )
                        }
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Help,
                            contentDescription = stringResource(R.string.test_cloze_cd_guide),
                            tint = textMain
                        )
                    }
                }
            )
        },
        containerColor = containerColor
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val pageType = when (uiState) {
                is ClozeUiState.Loading -> 0
                is ClozeUiState.LevelSelecting -> 1
                is ClozeUiState.Ready -> 2
                is ClozeUiState.Finished -> 3
                is ClozeUiState.Error -> 4
            }
            Crossfade(targetState = pageType, label = "cloze_state_transition") { type ->
                when (type) {
                    0 -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = IosColors.Orange)
                        }
                    }
                    1 -> {
                        LevelSelectionView(
                            isDark = isDark,
                            onSelectLevel = { viewModel.onLevelSelected(it) }
                        )
                    }
                    2 -> {
                        val readyState = uiState as? ClozeUiState.Ready
                        if (readyState != null) {
                            key(readyState.currentIndex) {
                                ReadyView(
                                    state = readyState,
                                    viewModel = viewModel,
                                    isDark = isDark,
                                    textMain = textMain,
                                    textSub = textSub,
                                    containerColor = containerColor,
                                    wordCardBg = wordCardBg,
                                    qCardBg = qCardBg,
                                    qCardBorderColor = qCardBorderColor,
                                    onRegenerate = { viewModel.forceRegenerate() }
                                )
                            }
                        }
                    }
                    3 -> {
                        val finishedState = uiState as? ClozeUiState.Finished
                        if (finishedState != null) {
                            ResultView(
                                correctCount = finishedState.correctCount,
                                totalCount = finishedState.totalCount,
                                onRestart = { viewModel.restart() },
                                onRegenerate = { viewModel.forceRegenerate() },
                                onBack = onNavigateBack
                            )
                        }
                    }
                    4 -> {
                        val errorState = uiState as? ClozeUiState.Error
                        if (errorState != null) {
                            ClozeErrorView(
                                message = errorState.message,
                                onRetry = { viewModel.restart() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        JapaneseInputGuideDialog(
            isDark = isDark,
            textMain = textMain,
            textSub = textSub,
            onDismiss = { showHelpDialog = false }
        )
    }

    AbilityExitDialog(
        show = showExitDialog,
        title = stringResource(R.string.test_cloze_exit_dialog_title),
        message = stringResource(R.string.test_cloze_exit_dialog_message),
        themeColor = IosColors.Orange,
        isDark = isDark,
        textMain = textMain,
        textSub = textSub,
        onDismiss = { showExitDialog = false },
        onKeepAndExit = {
            showExitDialog = false
            onNavigateBack()
        },
        onDestroyAndExit = {
            showExitDialog = false
            viewModel.clearSession()
            onNavigateBack()
        }
    )
}

/**
 * 等级选择视图
 */
@Composable
private fun LevelSelectionView(
    isDark: Boolean,
    onSelectLevel: (String) -> Unit
) {
    val levels = listOf(
        Triple("N5", stringResource(R.string.test_cloze_level_n5_desc), if (isDark) NemoSecondaryDark else NemoSecondary),
        Triple("N4", stringResource(R.string.test_cloze_level_n4_desc), if (isDark) NemoPrimaryDark else NemoPrimary),
        Triple("N3", stringResource(R.string.test_cloze_level_n3_desc), if (isDark) NemoWarningDark else NemoWarning),
        Triple("N2", stringResource(R.string.test_cloze_level_n2_desc), if (isDark) NemoWarningDark else NemoWarning),
        Triple("N1", stringResource(R.string.test_cloze_level_n1_desc), if (isDark) NemoDangerDark else NemoDanger)
    )

    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.test_cloze_title_level_selection),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textMain
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.test_cloze_desc_level_selection),
            style = MaterialTheme.typography.bodyMedium,
            color = textSub,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        levels.forEach { (level, desc, color) ->
            AbilityLevelCard(
                level = level,
                description = desc,
                color = color,
                isDark = isDark,
                textMain = textMain,
                textSub = textSub,
                onClick = { onSelectLevel(level) }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 答题阶段 ReadyView
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadyView(
    state: ClozeUiState.Ready,
    viewModel: WordClozeViewModel,
    isDark: Boolean,
    textMain: Color,
    textSub: Color,
    containerColor: Color,
    wordCardBg: Color,
    qCardBg: Color,
    qCardBorderColor: Color,
    onRegenerate: () -> Unit
) {
    val q = state.questions[state.currentIndex]
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // 唯一的隐藏单输入框焦点与输入缓冲区
    val focusRequester = remember { FocusRequester() }
    val globalInput = remember(state.currentIndex) {
        mutableStateOf(TextFieldValue(""))
    }
    // 当前正在编辑的活跃挖空格子索引（在 q.maskIndices 中的索引）
    val activeMaskIndex = remember(state.currentIndex) {
        mutableStateOf(0)
    }
    
    // 输入框的焦点状态
    var isGlobalFocused by remember { mutableStateOf(false) }

    // 记录在提交判定后，各挖空是否属于拼写错误
    val itemErrors = remember(state.currentIndex, q.maskIndices.size) {
        mutableStateListOf(*Array(q.maskIndices.size) { false })
    }

    // 错误左右抖动位移值
    val shakeOffset = remember { Animatable(0f) }

    val submitAction = {
        // 先同步一次当前活跃格子的最新文本到 ViewModel
        val currentText = globalInput.value.text.replace("\n", "")
        viewModel.updateUserInput(state.currentIndex, activeMaskIndex.value, currentText)

        var allCorrect = true
        var hasEmpty = false
        val currentInputs = state.userInputs[state.currentIndex]

        q.maskIndices.forEachIndexed { maskIndexInQuestion, origCharIndex ->
            val char = q.hiragana[origCharIndex]
            val userInput = currentInputs.getOrNull(maskIndexInQuestion) ?: ""
            if (userInput.isEmpty()) {
                hasEmpty = true
            }
            val isCorrect = viewModel.checkInputIsCorrect(char, userInput)
            if (isCorrect) {
                itemErrors[maskIndexInQuestion] = false
            } else {
                itemErrors[maskIndexInQuestion] = true
                allCorrect = false
            }
        }
        
        if (allCorrect && !hasEmpty) {
            viewModel.submitAnswerSuccess(state.currentIndex)
        } else {
            viewModel.submitAnswerError(state.currentIndex)
        }
    }
    
    LaunchedEffect(state.isShakeTriggered) {
        if (state.isShakeTriggered) {
            repeat(3) {
                shakeOffset.animateTo(
                    targetValue = if (it % 2 == 0) 8f else -8f,
                    animationSpec = tween(durationMillis = 60, easing = LinearEasing)
                )
            }
            shakeOffset.animateTo(0f, animationSpec = tween(durationMillis = 40))
        }
    }

    // 从 ViewModel 的 userInputs 恢复已保存的输入，并锁死聚焦唯一的隐藏单输入框
    LaunchedEffect(state.currentIndex) {
        val L = q.maskIndices.size
        val savedList = state.userInputs[state.currentIndex]
        
        // 自动定位到第一个未填写的格子，若全部已填则定位到最后一格
        val firstEmpty = savedList.indexOfFirst { it.isEmpty() }
        val initialActiveIndex = if (firstEmpty != -1) firstEmpty else L - 1
        activeMaskIndex.value = initialActiveIndex

        val currentVal = savedList.getOrNull(initialActiveIndex) ?: ""
        globalInput.value = TextFieldValue(
            text = currentVal,
            selection = TextRange(0, currentVal.length),
            composition = null
        )
        
        // 显式清除焦点，强制下一次 requestFocus 时被系统识别为焦点状态变更，从而顺利呼出键盘
        focusManager.clearFocus()
        
        // 自动索要强焦点，避免键盘闪烁，并强制拉起键盘
        kotlinx.coroutines.delay(100L)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    val isCurrentQCorrect = state.questionCorrectStates[state.currentIndex]

    // 全部拼对判定成功后的 2500 毫秒后自动跳下一题
    LaunchedEffect(isCurrentQCorrect) {
        if (isCurrentQCorrect) {
            kotlinx.coroutines.delay(2500L)
            viewModel.nextQuestion()
        }
    }

    // 拼对瞬间卡片的淡绿呼吸变色与微缩放动效
    val cardBgColor by animateColorAsState(
        targetValue = if (isCurrentQCorrect) {
            if (isDark) TestResultPalette.CorrectCardBgDark else TestResultPalette.CorrectCardBgLight
        } else {
            wordCardBg
        },
        animationSpec = tween(durationMillis = 350),
        label = "card_success_bg"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isCurrentQCorrect) 1.02f else 1.0f,
        animationSpec = tween(durationMillis = 350),
        label = "card_scale"
    )

    // 自定义呼吸拟物光标
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    // 提取当前输入法正在联想/拼写中的英文草稿
    val compositionText = if (globalInput.value.composition != null) {
        globalInput.value.text.substring(
            globalInput.value.composition!!.start,
            globalInput.value.composition!!.end
        )
    } else ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
    ) {
        // 唯一的隐藏单输入框，完全不干扰键盘，拥有无限宽广的组合输入态（Composition）保护
        BasicTextField(
            value = globalInput.value,
            onValueChange = { newVal ->
                val L = q.maskIndices.size
                val oldVal = globalInput.value
                val oldText = oldVal.text
                val oldSel = oldVal.selection

                val hasEnter = newVal.text.contains('\n')

                if (newVal.composition == null) {
                    // 已确认上屏（没有打字组合草稿）
                    val updatedText = newVal.text.replace("\n", "")
                    
                    if (hasEnter) {
                        // 1. 用户按了回车（或软键盘 Next / Done），需要保存并跳转/提交
                        viewModel.updateUserInput(state.currentIndex, activeMaskIndex.value, updatedText)
                        
                        val nextIndex = activeMaskIndex.value + 1
                        if (nextIndex < L) {
                            activeMaskIndex.value = nextIndex
                            val nextVal = state.userInputs[state.currentIndex].getOrNull(nextIndex) ?: ""
                            globalInput.value = TextFieldValue(
                                text = nextVal,
                                selection = TextRange(0, nextVal.length),
                                composition = null
                            )
                        } else {
                            // 最后一格按回车，触发提交
                            submitAction()
                        }
                    } else {
                        // 2. 普通输入或退格（没有回车）
                        if (updatedText.length < oldText.length) {
                            // 发生退格删除
                            viewModel.updateUserInput(state.currentIndex, activeMaskIndex.value, "")
                            globalInput.value = TextFieldValue(
                                text = "",
                                selection = TextRange(0),
                                composition = null
                            )
                        } else if (oldText.isEmpty() && newVal.text.isEmpty()) {
                            // 空退格：当前格子已经为空，用户依然按了退格
                            // 跳回上一个格子，顺带清空前一格的内容以支持连续退格
                            val prevIndex = activeMaskIndex.value - 1
                            if (prevIndex >= 0) {
                                activeMaskIndex.value = prevIndex
                                viewModel.updateUserInput(state.currentIndex, prevIndex, "")
                                globalInput.value = TextFieldValue(
                                    text = "",
                                    selection = TextRange(0),
                                    composition = null
                                )
                            }
                        } else {
                            // 普通字符替换或输入
                            if (updatedText.length > 1) {
                                // 长度超过 1，说明是覆盖输入（比如本来有字，输入法上屏了新字，导致 newVal.text 包含了旧字和新字）
                                val charStr = updatedText.last().toString()
                                viewModel.updateUserInput(state.currentIndex, activeMaskIndex.value, charStr)
                                globalInput.value = TextFieldValue(
                                    text = charStr,
                                    selection = TextRange(0, 1), // 全选以支持原地覆盖替换
                                    composition = null
                                )
                            } else {
                                // 长度是 0 或 1，完美符合单格子限制，直接使用 copy 减少对输入法连接的干扰
                                viewModel.updateUserInput(state.currentIndex, activeMaskIndex.value, updatedText)
                                globalInput.value = newVal.copy(
                                    selection = TextRange(0, updatedText.length)
                                )
                            }
                        }
                    }
                } else {
                    // 正在打字联想/组合中，百分之百原封不动保留，任由输入法在此转换拼写！
                    globalInput.value = newVal
                    
                    // 将联想中的临时输入实时同步回 ViewModel 的对应格（主要是为了在卡片中渲染英文草稿）
                    viewModel.updateUserInput(state.currentIndex, activeMaskIndex.value, newVal.text)
                }

                // 只要输入发生变化，该题所有的格子报错红亮瞬间解除！
                q.maskIndices.forEachIndexed { i, _ ->
                    itemErrors[i] = false
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    val L = q.maskIndices.size
                    val nextIndex = activeMaskIndex.value + 1
                    if (nextIndex < L) {
                        activeMaskIndex.value = nextIndex
                        val nextVal = state.userInputs[state.currentIndex].getOrNull(nextIndex) ?: ""
                        globalInput.value = TextFieldValue(
                            text = nextVal,
                            selection = TextRange(0, nextVal.length),
                            composition = null
                        )
                    } else {
                        submitAction()
                    }
                }
            ),
            modifier = Modifier
                .size(1.dp)
                .absoluteOffset(y = 2000.dp) 
                .focusRequester(focusRequester)
                .onFocusChanged { isGlobalFocused = it.isFocused }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部进度条
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.test_cloze_progress_format, state.currentIndex + 1, state.questions.size),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = textSub
                )
                Text(
                    text = stringResource(R.string.test_cloze_correct_format, state.correctCount),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = IosColors.Green
                    )
                )
            }
            
            LinearProgressIndicator(
                progress = { (state.currentIndex).toFloat() / state.questions.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = IosColors.Orange,
                trackColor = if (isDark) NemoNeutrals.Gray800 else NemoNeutrals.Gray200
            )

            // 核心释义提示卡片 (完全剔除日语汉字与任何例句)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = cardScale, scaleY = cardScale)
                    .padding(vertical = 12.dp)
                    .softCardShadow(borderRadius = 32.dp, isDark = isDark),
                shape = RoundedCornerShape(32.dp),
                color = cardBgColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 48.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 中文释义展示
                    Text(
                        text = q.chinese,
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp,
                            lineHeight = 32.sp
                        ),
                        color = if (isCurrentQCorrect) {
                            if (isDark) NemoSecondaryDark else NemoSecondary
                        } else textMain
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // 单词假名展示与挖空输入组件组（加入抖动修饰符）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                        contentAlignment = Alignment.Center
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            maxItemsInEachRow = 8
                        ) {
                            q.hiragana.forEachIndexed { index, char ->
                                val maskIndexInQuestion = q.maskIndices.indexOf(index)
                                if (maskIndexInQuestion != -1) {
                                    val isActive = (maskIndexInQuestion == activeMaskIndex.value)
                                    val hasError = itemErrors[maskIndexInQuestion]

                                    val textInCell = if (isCurrentQCorrect) {
                                        char.toString()
                                    } else {
                                        if (isActive) {
                                            if (globalInput.value.composition == null) {
                                                globalInput.value.text
                                            } else ""
                                        } else {
                                            state.userInputs[state.currentIndex].getOrNull(maskIndexInQuestion) ?: ""
                                        }
                                    }

                                    val currentComposition = globalInput.value.composition
                                    val isCompositionInCell = isActive && currentComposition != null
                                    val isCursorInCell = isActive && !isCurrentQCorrect && globalInput.value.selection.collapsed && currentComposition == null

                                    val boxBg = if (isCurrentQCorrect) {
                                        if (isDark) TestResultPalette.CorrectCardBgDark else TestResultPalette.CorrectCardBgLight
                                    } else {
                                        if (hasError) {
                                            if (isDark) TestResultPalette.WrongCardBgDark else TestResultPalette.WrongCardBgLight
                                        } else if (isActive && isGlobalFocused) {
                                            if (isDark) NemoNeutrals.Gray800 else Color.White
                                        } else {
                                            if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray100
                                        }
                                    }

                                    val boxBorderColor = if (isCurrentQCorrect) {
                                        if (isDark) NemoSecondaryDark else NemoSecondary
                                    } else {
                                        if (hasError) {
                                            if (isDark) NemoDangerDark else NemoDanger
                                        } else if (isActive && isGlobalFocused) {
                                            IosColors.Orange
                                        } else {
                                            Color.Transparent
                                        }
                                    }

                                    val fontColor = if (isCurrentQCorrect) {
                                        if (isDark) NemoSecondaryDark else NemoSecondary
                                    } else {
                                        if (hasError) {
                                            if (isDark) NemoDangerDark else NemoDanger
                                        } else {
                                            if (isDark) Color.White else NemoNeutrals.Gray800
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(width = 52.dp, height = 64.dp)
                                            .shadow(
                                                elevation = if (isCurrentQCorrect) 1.dp else if (isActive && isGlobalFocused) 6.dp else 2.dp,
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(boxBg)
                                            .border(
                                                BorderStroke(
                                                    width = if (isCurrentQCorrect) 2.dp else if (hasError || (isActive && isGlobalFocused)) 2.dp else 0.dp,
                                                    color = boxBorderColor
                                                 ),
                                                RoundedCornerShape(20.dp)
                                            )
                                            .clickable {
                                                if (!isCurrentQCorrect) {
                                                    val clickedText = state.userInputs[state.currentIndex].getOrNull(maskIndexInQuestion) ?: ""
                                                    activeMaskIndex.value = maskIndexInQuestion
                                                    globalInput.value = TextFieldValue(
                                                        text = clickedText,
                                                        selection = TextRange(0, clickedText.length),
                                                        composition = null
                                                    )
                                                }
                                                focusRequester.requestFocus()
                                                keyboardController?.show()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCurrentQCorrect) {
                                            Text(
                                                text = char.toString(),
                                                style = TextStyle(
                                                    fontSize = 26.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = fontColor
                                                )
                                            )
                                        } else {
                                            if (isCompositionInCell && compositionText.isNotEmpty()) {
                                                // 极其惊艳！在激活格子中实时渲染日语输入法正在打字的未确认英文草稿（如 he）
                                                Text(
                                                    text = compositionText,
                                                    style = TextStyle(
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = IosColors.Orange.copy(alpha = 0.8f),
                                                        textAlign = TextAlign.Center
                                                    )
                                                )
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    if (textInCell.isNotEmpty()) {
                                                        Text(
                                                            text = textInCell,
                                                            style = TextStyle(
                                                                fontSize = 26.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = fontColor
                                                            )
                                                        )
                                                    }
                                                    if (isCursorInCell && isGlobalFocused) {
                                                        // 极其逼真高质感的橙色呼吸光标
                                                        Box(
                                                            modifier = Modifier
                                                                .width(2.dp)
                                                                .height(22.dp)
                                                                .graphicsLayer(alpha = cursorAlpha)
                                                                .background(IosColors.Orange)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // 该假名未挖空，以静态不可交互卡片展示
                                    Box(
                                        modifier = Modifier
                                            .size(width = 52.dp, height = 64.dp)
                                            .background(Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = char.toString(),
                                            style = TextStyle(
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Black,
                                                color = textMain
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 当有输入内容且未通关时，显示精致的一键“清空输入”按钮
                    if (globalInput.value.text.isNotEmpty() && !isCurrentQCorrect) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            onClick = {
                                globalInput.value = TextFieldValue("")
                                q.maskIndices.forEachIndexed { i, _ ->
                                    viewModel.updateUserInput(state.currentIndex, i, "")
                                    itemErrors[i] = false
                                }
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            },
                            color = if (isDark) NemoNeutrals.Gray800 else NemoNeutrals.Gray100,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = stringResource(R.string.test_cloze_btn_clear),
                                    tint = textSub,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.test_cloze_btn_clear),
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSub
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部辅助区：包含橙色大圆角“提交”主按钮的扁平三键式底栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧上一题
                IconButton(
                    onClick = { viewModel.previousQuestion() },
                    enabled = state.currentIndex > 0,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (state.currentIndex > 0) IosColors.Orange else textSub.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = stringResource(R.string.test_cloze_cd_prev_question),
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // 中间提交主按钮
                Button(
                    onClick = {
                        if (isCurrentQCorrect) {
                            viewModel.nextQuestion()
                        } else {
                            submitAction()
                        }
                    },
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentQCorrect) IosColors.Green else IosColors.Orange
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = if (isCurrentQCorrect) stringResource(R.string.test_cloze_btn_next) else stringResource(R.string.test_cloze_btn_submit),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                // 右侧下一题
                IconButton(
                    onClick = { viewModel.nextQuestion() },
                    enabled = state.currentIndex < state.questions.size - 1,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (state.currentIndex < state.questions.size - 1) IosColors.Orange else textSub.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.test_cloze_cd_next_question),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 悬浮式毛玻璃解析 Overlay 浮层卡片
        AnimatedVisibility(
            visible = state.showExplanation,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 250)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .softCardShadow(borderRadius = 28.dp, isDark = isDark),
                color = (if (isDark) NemoNeutrals.Gray900 else Color.White).copy(alpha = 0.88f), // 玻璃半透明感
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // 右上角关闭按钮
                    IconButton(
                        onClick = { viewModel.setExplanationVisible(false) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp)
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.08f) else NemoNeutrals.Gray100,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.test_cloze_cd_close),
                            tint = textSub,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.test_cloze_title_explanation),
                            style = TextStyle(
                                fontSize = 13.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = IosColors.Orange
                            )
                        )
                        
                        // 日语原文与假名读音排布
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = q.japanese,
                                style = TextStyle(
                                    fontSize = 26.sp, 
                                    fontWeight = FontWeight.Black
                                ),
                                color = textMain
                            )
                            Text(
                                text = q.hiragana,
                                style = TextStyle(
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Medium
                                ),
                                color = textSub,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = q.chinese,
                            style = TextStyle(
                                fontSize = 15.sp, 
                                fontWeight = FontWeight.Normal, 
                                lineHeight = 22.sp
                            ),
                            color = textSub
                        )
                    }
                }
            }
        }
    }
}

/**
 * 结算视图
 */
@Composable
private fun ResultView(
    correctCount: Int,
    totalCount: Int,
    onRestart: () -> Unit,
    onRegenerate: () -> Unit,
    onBack: () -> Unit
) {
    val accuracy = (correctCount.toFloat() / totalCount.toFloat())
    val animAccuracy = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animAccuracy.animateTo(
            targetValue = accuracy,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.test_cloze_title_challenge_completed),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textMain
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 环形进度显示环
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = if (isDark) NemoNeutrals.Gray800 else NemoNeutrals.Gray100,
                strokeWidth = 14.dp,
                strokeCap = StrokeCap.Round
            )
            CircularProgressIndicator(
                progress = { animAccuracy.value },
                modifier = Modifier.fillMaxSize(),
                color = IosColors.Orange,
                strokeWidth = 14.dp,
                strokeCap = StrokeCap.Round
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(animAccuracy.value * 100).roundToInt()}%",
                    style = TextStyle(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = IosColors.Orange,
                        letterSpacing = (-1).sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.test_cloze_label_accuracy),
                    style = MaterialTheme.typography.bodySmall,
                    color = textSub,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 结算卡片
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .softCardShadow(borderRadius = 24.dp, isDark = isDark),
            shape = RoundedCornerShape(24.dp),
            color = cardBg
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.test_cloze_label_correct_count), style = MaterialTheme.typography.bodyMedium, color = textSub)
                    Text(
                        text = stringResource(R.string.test_cloze_score_count_format, correctCount, totalCount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textMain
                    )
                }

            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 双排按钮
        Button(
            onClick = { onRegenerate() },
            colors = ButtonDefaults.buttonColors(containerColor = IosColors.Orange),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Rounded.Replay, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.test_cloze_btn_regenerate),
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onRestart() },
                border = BorderStroke(1.dp, IosColors.Orange.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.test_cloze_btn_restart),
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IosColors.Orange)
                )
            }

            Button(
                onClick = { onBack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) NemoNeutrals.Gray800 else NemoNeutrals.Gray200
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.test_cloze_btn_exit),
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else NemoNeutrals.Gray600
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 历史记录视图
 */
@Composable
private fun ClozeHistoryView(
    historyRecords: List<TestRecordEntity>,
    onBack: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val containerColor = MaterialTheme.colorScheme.background
    
    Scaffold(
        topBar = {
            CommonHeader(
                title = stringResource(R.string.test_cloze_history_title),
                onBack = onBack
            )
        },
        containerColor = containerColor
    ) { padding ->
        if (historyRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Inbox,
                        contentDescription = null,
                        tint = NemoNeutrals.Gray400,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = stringResource(R.string.test_cloze_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NemoNeutrals.Gray400
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyRecords) { record ->
                    val accuracy = (record.correctAnswers.toFloat() / record.totalQuestions.toFloat() * 100).roundToInt()
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, shape = RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (accuracy >= 80) IosColors.Green.copy(alpha = 0.1f)
                                        else IosColors.Orange.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$accuracy%",
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (accuracy >= 80) IosColors.Green else IosColors.Orange
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.test_cloze_history_record_format, record.correctAnswers, record.totalQuestions),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                val formattedDate = sdf.format(Date(record.timestamp))
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NemoNeutrals.Gray400
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 错误视图
 */
@Composable
private fun ClozeErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = IosColors.Red,
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = NemoNeutrals.Gray500
            )
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = IosColors.Orange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(R.string.test_cloze_btn_reload), color = Color.White)
            }
        }
    }
}

/**
 * 等待生成视图
 */
@Composable
private fun ClozeGeneratingView(
    isDark: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = IosColors.Orange)
    }
}

/**
 * 日语罗马音输入指南弹窗
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun JapaneseInputGuideDialog(
    isDark: Boolean,
    textMain: Color,
    textSub: Color,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val bg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray50
        val keyBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else NemoNeutrals.Gray200
        
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .padding(16.dp)
                .softCardShadow(borderRadius = 28.dp, isDark = isDark),
            shape = RoundedCornerShape(28.dp),
            color = bg,
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) MaterialTheme.colorScheme.outlineVariant else NemoNeutrals.Gray200
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部标题与图标区
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = IosColors.Orange.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.test_cloze_cd_tip),
                            tint = IosColors.Orange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.test_cloze_title_guide),
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMain
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 胶囊 Tab 控制器
                var selectedTab by remember { mutableStateOf(0) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else NemoNeutrals.Gray100,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabButton(
                            text = stringResource(R.string.test_cloze_guide_tab_basic),
                            isSelected = selectedTab == 0,
                            modifier = Modifier.weight(1f),
                            isDark = isDark,
                            onClick = { selectedTab = 0 }
                        )
                        TabButton(
                            text = stringResource(R.string.test_cloze_guide_tab_advanced),
                            isSelected = selectedTab == 1,
                            modifier = Modifier.weight(1f),
                            isDark = isDark,
                            onClick = { selectedTab = 1 }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 可滚动的切换内容区
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() with
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() with
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }
                        },
                        label = "tab_content_transition"
                    ) { tabIndex ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (tabIndex == 0) {
                                // 基础篇
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_tsu_double_title),
                                    method = stringResource(R.string.test_cloze_guide_tsu_double_method),
                                    example = stringResource(R.string.test_cloze_guide_tsu_double_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_n_title),
                                    method = stringResource(R.string.test_cloze_guide_n_method),
                                    example = stringResource(R.string.test_cloze_guide_n_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_katakana_chouon_title),
                                    method = stringResource(R.string.test_cloze_guide_katakana_chouon_method),
                                    example = stringResource(R.string.test_cloze_guide_katakana_chouon_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_hiragana_chouon_title),
                                    method = stringResource(R.string.test_cloze_guide_hiragana_chouon_method),
                                    example = stringResource(R.string.test_cloze_guide_hiragana_chouon_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_youon_title),
                                    method = stringResource(R.string.test_cloze_guide_youon_method),
                                    example = stringResource(R.string.test_cloze_guide_youon_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                            } else {
                                // 进阶篇
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_tsu_single_title),
                                    method = stringResource(R.string.test_cloze_guide_tsu_single_method),
                                    example = stringResource(R.string.test_cloze_guide_tsu_single_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_special_dakuon_title),
                                    method = stringResource(R.string.test_cloze_guide_special_dakuon_method),
                                    example = stringResource(R.string.test_cloze_guide_special_dakuon_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_foreign_combo_title),
                                    method = stringResource(R.string.test_cloze_guide_foreign_combo_method),
                                    example = stringResource(R.string.test_cloze_guide_foreign_combo_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_special_kana_title),
                                    method = stringResource(R.string.test_cloze_guide_special_kana_method),
                                    example = stringResource(R.string.test_cloze_guide_special_kana_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_small_kana_title),
                                    method = stringResource(R.string.test_cloze_guide_small_kana_method),
                                    example = stringResource(R.string.test_cloze_guide_small_kana_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                                GuideItemCard(
                                    title = stringResource(R.string.test_cloze_guide_ime_compat_title),
                                    method = stringResource(R.string.test_cloze_guide_ime_compat_method),
                                    example = stringResource(R.string.test_cloze_guide_ime_compat_example),
                                    keyBg = keyBg,
                                    cardBg = cardBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 确认关闭按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .softCardShadow(borderRadius = 24.dp, isDark = isDark),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IosColors.Orange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.test_cloze_guide_btn_confirm),
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * 胶囊 Tab 切换按钮
 */
@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) {
        if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else Color.White
    } else {
        Color.Transparent
    }
    val elevation = if (isSelected) 2.dp else 0.dp
    val textStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) {
            if (isDark) Color.White else NemoNeutrals.Gray800
        } else {
            if (isDark) NemoNeutrals.Gray400 else NemoNeutrals.Gray500
        }
    )
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = bg,
        shadowElevation = elevation,
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = textStyle)
        }
    }
}

/**
 * 指南卡片条目
 */
@Composable
private fun GuideItemCard(
    title: String,
    method: String,
    example: String,
    keyBg: Color,
    cardBg: Color,
    textMain: Color,
    textSub: Color,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outlineVariant else NemoNeutrals.Gray200
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // 仅高亮假名部分，减少大片橙色的突兀感
                val annotatedTitle = buildAnnotatedString {
                    val bracketStart = title.indexOf("(")
                    if (bracketStart != -1) {
                        append(title.substring(0, bracketStart))
                        withStyle(
                            style = SpanStyle(
                                color = IosColors.Orange,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(title.substring(bracketStart))
                        }
                    } else {
                        append(title)
                    }
                }
                
                Text(
                    text = annotatedTitle,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMain
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = example,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = textSub,
                        lineHeight = 16.sp
                    )
                )
            }
            
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = keyBg,
                shadowElevation = 1.dp,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = method,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textMain,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}
