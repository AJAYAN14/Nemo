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
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.data.local.entity.TestRecordEntity
import com.jian.nemo.core.designsystem.theme.BentoColors
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
    val containerColor = if (isDark) MaterialTheme.colorScheme.background else BentoColors.BgBase
    val wordCardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val qCardBg = if (isDark) IosColors.Orange.copy(alpha = 0.15f) else Color(0xFFFFF7EB)
    val qCardBorderColor = if (isDark) IosColors.Orange.copy(alpha = 0.2f) else IosColors.Orange.copy(alpha = 0.1f)

    var showHistoryView by remember { mutableStateOf(false) }
    val historyRecords by viewModel.historyRecords.collectAsState(initial = emptyList())

    var showExitDialog by remember { mutableStateOf(false) }

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
                title = "单词填空",
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
                                contentDescription = "重新换一批",
                                tint = textMain
                            )
                        }
                    }
                    IconButton(onClick = { showHistoryView = true }) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "历史记录",
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
            Crossfade(targetState = uiState, label = "cloze_state_transition") { state ->
                when (state) {
                    is ClozeUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = IosColors.Orange)
                        }
                    }
                    is ClozeUiState.LevelSelecting -> {
                        LevelSelectionView(
                            isDark = isDark,
                            onSelectLevel = { viewModel.onLevelSelected(it) }
                        )
                    }
                    is ClozeUiState.Ready -> {
                        key(state.currentIndex) {
                            ReadyView(
                                state = state,
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
                    is ClozeUiState.Finished -> {
                        ResultView(
                            correctCount = state.correctCount,
                            totalCount = state.totalCount,
                            onRestart = { viewModel.restart() },
                            onRegenerate = { viewModel.forceRegenerate() },
                            onBack = onNavigateBack
                        )
                    }
                    is ClozeUiState.Error -> {
                        ClozeErrorView(
                            message = state.message,
                            onRetry = { viewModel.restart() }
                        )
                    }
                }
            }
        }
    }

    AbilityExitDialog(
        show = showExitDialog,
        title = "退出单词填空",
        message = "您是否要保留当前的答题进度？若保留，下次进入时将直接无缝恢复断点；若销毁，将彻底清空当前测试进度。",
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
        Triple("N5", "初级基础词汇填空", Color(0xFF10B981)),
        Triple("N4", "常用社交词汇填空", Color(0xFF3B82F6)),
        Triple("N3", "中级日常词汇填空", Color(0xFFF59E0B)),
        Triple("N2", "中高级书面词汇填空", Color(0xFFF97316)),
        Triple("N1", "高级专业词汇填空", Color(0xFFEF4444))
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
            text = "选择单词填空等级",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textMain
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "从本地词库精选单词，智能生成平假名挖空，通过英文键盘直输罗马音完成检验！",
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
    
    // 输入框的焦点状态
    var isGlobalFocused by remember { mutableStateOf(false) }

    // 记录在提交判定后，各挖空是否属于拼写错误
    val itemErrors = remember(state.currentIndex, q.maskIndices.size) {
        mutableStateListOf(*Array(q.maskIndices.size) { false })
    }

    // 错误左右抖动位移值
    val shakeOffset = remember { Animatable(0f) }
    
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
        val savedText = state.userInputs[state.currentIndex].filter { it.isNotEmpty() }.joinToString("")
        globalInput.value = TextFieldValue(
            text = savedText,
            selection = TextRange(savedText.length),
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

    // 全部拼对判定成功后的 800 毫秒后自动跳下一题
    LaunchedEffect(isCurrentQCorrect) {
        if (isCurrentQCorrect) {
            kotlinx.coroutines.delay(800L)
            viewModel.nextQuestion()
        }
    }

    // 拼对瞬间卡片的淡绿呼吸变色与微缩放动效
    val cardBgColor by animateColorAsState(
        targetValue = if (isCurrentQCorrect) {
            if (isDark) Color(0xFF14532D) else Color(0xFFF0FDF4)
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
                if (newVal.composition == null) {
                    // 已确认上屏（没有带下划线的草稿）时，再过滤空白并进行严格限长（最大长度锁死为挖空假名数）
                    var filteredText = newVal.text.filter { !it.isWhitespace() }
                    if (filteredText.length > q.maskIndices.size) {
                        filteredText = filteredText.take(q.maskIndices.size)
                    }
                    
                    val updatedValue = if (filteredText != newVal.text) {
                        TextFieldValue(
                            text = filteredText,
                            selection = TextRange(filteredText.length),
                            composition = null
                        )
                    } else {
                        newVal
                    }
                    
                    globalInput.value = updatedValue
                    
                    // 将拼写确认上屏的假名逐字映射更新同步回 ViewModel 的 userInputs
                    q.maskIndices.forEachIndexed { i, _ ->
                        val charStr = if (i < updatedValue.text.length) {
                            updatedValue.text[i].toString()
                        } else {
                            ""
                        }
                        viewModel.updateUserInput(state.currentIndex, i, charStr)
                    }
                } else {
                    // 正在打字联想/组合中，百分之百原封不动保留，任由输入法在此转换拼写！
                    globalInput.value = newVal
                    
                    // 将已经确认并提交的部分同步回 ViewModel
                    val confirmedText = newVal.text.substring(0, newVal.composition?.start ?: newVal.text.length)
                    q.maskIndices.forEachIndexed { i, _ ->
                        val charStr = if (i < confirmedText.length) {
                            confirmedText[i].toString()
                        } else {
                            ""
                        }
                        viewModel.updateUserInput(state.currentIndex, i, charStr)
                    }
                }
                
                // 只要输入发生变化，该题所有的格子报错红亮瞬间解除！
                q.maskIndices.forEachIndexed { i, _ ->
                    itemErrors[i] = false
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.None
            ),
            modifier = Modifier
                .size(1.dp)
                .absoluteOffset(y = 2000.dp) 
                .focusRequester(focusRequester)
                .onFocusChanged { isGlobalFocused = it.isFocused }
                .onPreviewKeyEvent { keyEvent ->
                    // 仅当没有输入法拼写草稿状态时才物理拦截退格键，防止破坏输入法的组合拼写
                    if (globalInput.value.composition == null && keyEvent.key == Key.Backspace && keyEvent.type == KeyEventType.KeyDown) {
                        val currentText = globalInput.value.text
                        if (currentText.isNotEmpty()) {
                            val newText = currentText.dropLast(1)
                            val newSel = TextRange(newText.length)
                            globalInput.value = TextFieldValue(text = newText, selection = newSel, composition = null)
                            
                            // 同步回 ViewModel
                            q.maskIndices.forEachIndexed { i, _ ->
                                val charStr = if (i < newText.length) newText[i].toString() else ""
                                viewModel.updateUserInput(state.currentIndex, i, charStr)
                            }
                            q.maskIndices.forEachIndexed { i, _ ->
                                itemErrors[i] = false
                            }
                            true // 消费该退格删除事件
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
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
                    text = "进度: ${state.currentIndex + 1} / ${state.questions.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = textSub
                )
                Text(
                    text = "正确数: ${state.correctCount}",
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
                    .shadow(
                        elevation = if (isCurrentQCorrect) 1.dp else 4.dp,
                        shape = RoundedCornerShape(32.dp),
                        clip = false
                    ),
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
                    // 中文大字释义 (还原 H5 设计)
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
                            if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
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
                                    // 智能计算该格显示的假名（从全局输入串拆分提取）
                                    val textInCell = if (maskIndexInQuestion < globalInput.value.text.length) {
                                        globalInput.value.text[maskIndexInQuestion].toString()
                                    } else ""

                                    // 判定当前格子是否为处于“准备接收/正在打字”的激活编辑格子
                                    val isActive = !isCurrentQCorrect && maskIndexInQuestion == globalInput.value.text.length.coerceAtMost(q.maskIndices.size - 1)
                                    val hasError = itemErrors[maskIndexInQuestion]

                                    val boxBg = if (isCurrentQCorrect) {
                                        if (isDark) Color(0xFF14532D) else Color(0xFFF0FDF4)
                                    } else {
                                        if (hasError) {
                                            if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2)
                                        } else if (isActive && isGlobalFocused) {
                                            if (isDark) NemoNeutrals.Gray800 else Color.White
                                        } else {
                                            if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                                        }
                                    }

                                    val boxBorderColor = if (isCurrentQCorrect) {
                                        Color(0xFF22C55E)
                                    } else {
                                        if (hasError) {
                                            Color(0xFFEF4444)
                                        } else if (isActive && isGlobalFocused) {
                                            IosColors.Orange
                                        } else {
                                            Color.Transparent
                                        }
                                    }

                                    val fontColor = if (isCurrentQCorrect) {
                                        Color(0xFF22C55E)
                                    } else {
                                        if (hasError) {
                                            Color(0xFFEF4444)
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
                                                    // 如果点击的是已经被填了字符的挖空格子，智能截断退回/删除到该格子，提供最佳的手感
                                                    val currentText = globalInput.value.text
                                                    if (maskIndexInQuestion < currentText.length) {
                                                        val newText = currentText.take(maskIndexInQuestion)
                                                        globalInput.value = TextFieldValue(
                                                            text = newText,
                                                            selection = TextRange(newText.length),
                                                            composition = null
                                                        )
                                                        q.maskIndices.forEachIndexed { i, _ ->
                                                            val charStr = if (i < newText.length) newText[i].toString() else ""
                                                            viewModel.updateUserInput(state.currentIndex, i, charStr)
                                                        }
                                                        q.maskIndices.forEachIndexed { i, _ ->
                                                            itemErrors[i] = false
                                                        }
                                                    }
                                                }
                                                focusRequester.requestFocus()
                                                keyboardController?.show()
                                            }, // 点击圆格子，智能退焦重置并强聚焦全局输入框
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCurrentQCorrect) {
                                            // 已经判定全对，只读锁定显示大字绿色平假名
                                            Text(
                                                text = char.toString(),
                                                style = TextStyle(
                                                    fontSize = 26.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = fontColor
                                                )
                                            )
                                        } else {
                                            if (isActive && compositionText.isNotEmpty()) {
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
                                                    if (isActive && isGlobalFocused) {
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
                                    // 该假名未挖空，以静态不可交互卡片展示 (还原 H5 style)
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
                                    contentDescription = "清空输入",
                                    tint = textSub,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "清空输入",
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
                        contentDescription = "上一题",
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // 中间提交主按钮
                Button(
                    onClick = {
                        val userSequence = globalInput.value.text
                        var allCorrect = true
                        
                        // 遍历各个挖空格子逐一进行假名比对
                        q.maskIndices.forEachIndexed { maskIndexInQuestion, origCharIndex ->
                            val char = q.hiragana[origCharIndex]
                            val userInput = if (maskIndexInQuestion < userSequence.length) {
                                userSequence[maskIndexInQuestion].toString()
                            } else ""
                            
                            val isCorrect = viewModel.checkInputIsCorrect(char, userInput)
                            if (isCorrect) {
                                itemErrors[maskIndexInQuestion] = false
                            } else {
                                itemErrors[maskIndexInQuestion] = true
                                allCorrect = false
                            }
                        }
                        
                        // 若全对且打满，通关
                        if (allCorrect && userSequence.length == q.maskIndices.size) {
                            viewModel.submitAnswerSuccess(state.currentIndex)
                        } else {
                            viewModel.submitAnswerError(state.currentIndex)
                        }
                    },
                    enabled = !isCurrentQCorrect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IosColors.Orange,
                        disabledContainerColor = IosColors.Green.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = if (isCurrentQCorrect) "正确" else "提交",
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
                        contentDescription = "下一题",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 悬浮式毛玻璃解析 Overlay 浮层卡片 (100% 像素级高度还原 H5 覆盖层)
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
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp)),
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
                            contentDescription = "关闭",
                            tint = textSub,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "答案解析",
                            style = TextStyle(
                                fontSize = 13.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = IosColors.Orange
                            )
                        )
                        
                        // 大字日语原文与假名读音排布 (HTML同款设计)
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
            text = "测试挑战完成！",
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
                    text = "正确率",
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
                .shadow(4.dp, shape = RoundedCornerShape(24.dp)),
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
                    Text(text = "答对题数", style = MaterialTheme.typography.bodyMedium, color = textSub)
                    Text(
                        text = "$correctCount / $totalCount 题",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textMain
                    )
                }

                Divider(color = if (isDark) NemoNeutrals.Gray800 else NemoNeutrals.Gray100)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "无错通关奖励", style = MaterialTheme.typography.bodyMedium, color = textSub)
                    Text(
                        text = if (correctCount == totalCount) "完美 +100 金币" else "无",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (correctCount == totalCount) IosColors.Orange else textSub
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
                text = "再来一局",
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
                    text = "换个等级",
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
                    text = "退出挑战",
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
    val containerColor = if (isDark) MaterialTheme.colorScheme.background else BentoColors.BgBase
    
    Scaffold(
        topBar = {
            CommonHeader(
                title = "练习历史",
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
                        text = "暂无练习历史，快去挑战一局吧！",
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
                                    text = "测试结果：答对 ${record.correctAnswers} / ${record.totalQuestions} 题",
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
                Text(text = "重新加载", color = Color.White)
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
