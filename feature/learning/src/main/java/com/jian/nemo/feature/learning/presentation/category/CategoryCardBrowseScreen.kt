package com.jian.nemo.feature.learning.presentation.category

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCard
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCardDark
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackground
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackgroundDark
import com.jian.nemo.feature.learning.presentation.components.dialogs.TypingPracticeDialog
import com.jian.nemo.feature.learning.presentation.components.cards.WordCard
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCardBrowseScreen(
    category: String,
    categoryTitle: String,
    onNavigateBack: () -> Unit,
    viewModel: CategoryCardBrowseViewModel = hiltViewModel(creationCallback = { factory: CategoryCardBrowseViewModel.Factory ->
        factory.create(category)
    })
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 跟打练习对话框状态
    var showTypingDialog by remember { mutableStateOf(false) }
    // 答题卡抽屉状态
    var showAnswerSheetDrawer by remember { mutableStateOf(false) }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val cardColor = if (isDarkTheme) NemoSurfaceCardDark else NemoSurfaceCard

    // UI/UX PRO: 根据分类锁定全局主题色 (不再随单词动态切换)
    val fixedThemeColor = remember(category, isDarkTheme) {
        com.jian.nemo.feature.learning.presentation.components.cards.getThemeColorForCategory(
            categoryId = category,
            pos = null,
            isDark = isDarkTheme
        )
    }

    // 构建带有项数的标题
    val titleWithCount = if (!uiState.isLoading && uiState.error == null) {
        "${categoryTitle}（${uiState.currentWordIndex + 1}/${uiState.words.size}）"
    } else {
        categoryTitle
    }

    Scaffold(
        topBar = {
            com.jian.nemo.core.ui.component.common.CommonHeader(
                title = titleWithCount,
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                actions = {
                    IconButton(
                        onClick = { showAnswerSheetDrawer = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = "答题卡",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isDarkTheme) Color(0xFF121212) else fixedThemeColor.copy(alpha = 0.05f),
                            if (isDarkTheme) Color(0xFF1E1E1E) else fixedThemeColor.copy(alpha = 0.15f)
                        )
                    )
                )
        ) {
            when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    NemoChasingDotsLoader()
                }
            }
            uiState.error != null -> {
                val errorMessage = uiState.error
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "未知错误",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            uiState.words.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "暂无词汇",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "该分类下暂时没有词汇",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            else -> {
                // 主内容区域
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        // 实现横向滑动切换手势
                        .pointerInput(Unit) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    val threshold = 50.dp.toPx()
                                    if (totalDrag > threshold) {
                                        // Swipe Right -> Previous (if available)
                                        if (uiState.hasPrevious) viewModel.previousWord()
                                    } else if (totalDrag < -threshold) {
                                        // Swipe Left -> Next (if available)
                                        if (uiState.hasNext) viewModel.nextWord()
                                    }
                                    totalDrag = 0f
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDrag += dragAmount
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 缓存滑动方向用于过渡动画
                    val slideDirection = remember(uiState.currentWordIndex) { uiState.slideDirection }

                    // 单词卡片与滑动过渡动画
                    AnimatedContent(
                        targetState = uiState.currentWord,
                        transitionSpec = {
                            // 根据滑动方向选择动画
                            if (slideDirection == SlideDirection.FORWARD) {
                                // 向前：从右进入，向左退出
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(400)
                                ) + fadeIn(animationSpec = tween(400))) togetherWith
                                    (slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> -fullWidth },
                                        animationSpec = tween(400)
                                    ) + fadeOut(animationSpec = tween(400)))
                            } else {
                                // 向后：从左进入，从右退出
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = tween(400)
                                ) + fadeIn(animationSpec = tween(400))) togetherWith
                                    (slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> fullWidth },
                                        animationSpec = tween(400)
                                    ) + fadeOut(animationSpec = tween(400)))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(1f) // 宽度铺满
                            .heightIn(min = 450.dp, max = 650.dp) // 定义高度范围
                            .padding(horizontal = 16.dp) // 布局内边距
                            .padding(bottom = 120.dp), // 为底部操作留出空间
                        label = "wordCardTransition"
                    ) { targetWord ->
                        targetWord?.let { word ->
                            // 处理卡片切换时的翻转状态同步
                            // uiState.isFlipped 会在切换时重置；本地状态用于保持退出卡片的翻转状态
                            val isCurrentWord = word.id == uiState.currentWord?.id
 
                             // 本地翻转状态保持
                             // 确保每个单词使用独立的 id 作为 key
                            var localFlippedState by remember(word.id) { mutableStateOf(uiState.isFlipped) }
 
                             // 仅对当前活跃卡片同步全局状态
                            if (isCurrentWord) {
                                localFlippedState = uiState.isFlipped
                            }
 

                            WordCard(
                                word = word,
                                isFlipped = localFlippedState, // 使用本地状态
                                onFlip = { viewModel.flipCard() },
                                onSpeak = { viewModel.speakCurrentWord() },
                                onSpeakText = { text -> viewModel.speakText(text) },
                                cardColor = cardColor,
                                categoryId = category // 传入 category 以锁定颜色
                            )
                        }
                    }

                    // 底部操作按钮区域 (同步主题色)

                    CategoryCardBrowseActionButtons(
                        hasPrevious = uiState.hasPrevious,
                        hasNext = uiState.hasNext,
                        onPrevious = { viewModel.previousWord() },
                        onPractice = { showTypingDialog = true },
                        onNext = { viewModel.nextWord() },
                        themeColor = fixedThemeColor, // 使用锁定的主题色
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

        // 跟打练习对话框
        if (showTypingDialog && uiState.currentWord != null) {
            TypingPracticeDialog(
                word = uiState.currentWord!!,
                onDismiss = { showTypingDialog = false },
                themeColor = fixedThemeColor // 传入锁定的主题色
            )
        }

        // Answer sheet drawer
        if (showAnswerSheetDrawer && !uiState.isLoading && uiState.words.isNotEmpty()) {
            AnswerSheetDrawer(
                totalWords = uiState.words.size,
                currentIndex = uiState.currentWordIndex,
                canGoBack = uiState.canGoBack,
                onDismiss = { showAnswerSheetDrawer = false },
                themeColor = fixedThemeColor, // 传入锁定的主题色
                onWordSelected = { sequenceNumber ->
                    viewModel.jumpToWord(sequenceNumber)
                    showAnswerSheetDrawer = false
                },
                onGoBack = {
                    viewModel.goBack()
                }
            )
        }
    }
}


/**
 * 分类卡片浏览界面的操作按钮组件
 */
@Composable
fun CategoryCardBrowseActionButtons(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onPractice: () -> Unit,
    onNext: () -> Unit,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp), // Increased horizontal padding
        horizontalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 向后翻页按钮
        TintedSquircleIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            enabled = hasPrevious,
            onClick = onPrevious,
            themeColor = themeColor,
            modifier = Modifier.size(56.dp)
        )

        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        // 跟打练习按钮
        Button(
            onClick = onPractice,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .softCardShadow(borderRadius = 28.dp, isDark = isDark),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = themeColor,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Text(
                text = "跟打练习",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 向前翻页按钮
        TintedSquircleIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            enabled = hasNext,
            onClick = onNext,
            themeColor = themeColor,
            modifier = Modifier.size(56.dp)
        )
    }
}

/**
 * 带色调的圆角矩形图标按钮组件
 */
@Composable
private fun TintedSquircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    val containerColor = if (enabled) themeColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val contentColor = if (enabled) themeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)) // Squircle
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 带有网格布局的答题卡抽屉组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerSheetDrawer(
    totalWords: Int,
    currentIndex: Int,
    canGoBack: Boolean,
    onDismiss: () -> Unit,
    onWordSelected: (Int) -> Unit,
    onGoBack: () -> Unit,
    themeColor: Color = NemoPrimary
) {
    var jumpInput by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // 1-based sequence number
    val currentSequence = currentIndex + 1

    // Scroll to active item when drawer expands
    LaunchedEffect(Unit) {
        // Calculate grid position (5 columns)
        val rowIndex = currentIndex / 5
        coroutineScope.launch {
            gridState.animateScrollToItem(index = rowIndex * 5)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // 头部区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "编号列表",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // 导航控制区域
                Row(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Backward button
                    SmallSquircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        enabled = canGoBack,
                        onClick = onGoBack
                    )

                    // Sequence input field
                    BasicTextField(
                        value = jumpInput,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 4) {
                                jumpInput = it
                            }
                        },
                        modifier = Modifier
                            .width(50.dp)
                            .height(32.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (jumpInput.isEmpty()) {
                                    Text(
                                        text = "#",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // 跳转按钮
                    SmallSquircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        enabled = jumpInput.isNotBlank() && jumpInput.toIntOrNull()?.let { it in 1..totalWords } == true,
                        isPrimary = true,
                        themeColor = themeColor,
                        onClick = {
                            val targetNumber = jumpInput.toIntOrNull()
                            if (targetNumber != null && targetNumber in 1..totalWords) {
                                onWordSelected(targetNumber)
                                jumpInput = ""
                            }
                        }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // 序号网格（5列布局）
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                state = gridState
            ) {
                items(
                    count = totalWords,
                    key = { index -> "word_item_${index + 1}" },
                    contentType = { "number_circle" }
                ) { index ->
                    val sequenceNumber = index + 1
                    val isActive = sequenceNumber == currentSequence

                    // 活跃项的渐变背景
                    val itemBrush = remember(isActive) {
                        if (isActive) {
                            Brush.linearGradient(
                                colors = listOf(themeColor, themeColor.copy(alpha = 0.7f))
                            )
                        } else {
                            // 非活跃项透明背景
                             Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                        }
                    }

                    val backgroundColor = if (isActive) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                    val contentColor = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp)) // 圆角矩形裁剪
                            .background(backgroundColor)
                            .background(itemBrush)
                            .clickable {
                                onWordSelected(sequenceNumber)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sequenceNumber.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 16.sp,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallSquircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    themeColor: Color = NemoPrimary
) {
    val containerColor = if (isPrimary) {
        if (enabled) themeColor else themeColor.copy(alpha = 0.3f)
    } else {
        if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
    }

    val contentColor = if (isPrimary) {
        Color.White
    } else {
        if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = contentColor
        )
    }
}
