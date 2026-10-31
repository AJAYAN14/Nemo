package com.jian.nemo.feature.library.presentation.category

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.animation.animateListItem
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBar
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBarStyle
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryOption
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.library.presentation.components.FloatingHeaderGradientContainer
import com.jian.nemo.feature.library.presentation.components.LibraryLevelSelectionMenu
import com.jian.nemo.feature.library.presentation.components.LibraryMenuScrimOverlay
import com.jian.nemo.feature.library.presentation.components.LibraryScrollToTopButton
import kotlinx.coroutines.launch

private enum class StudyFilter {
    ALL,      // 全部
    LEARNED,  // 已学
    UNLEARNED // 未学
}

/**
 * 分类单词列表界面 (接入 LibraryListComponents 通用组件)
 */
@Composable
fun CategoryWordsScreen(
    category: String,
    categoryTitle: String,
    onNavigateBack: () -> Unit,
    onNavigateToWordDetail: (Int) -> Unit = {},
    viewModel: CategoryWordsViewModel = hiltViewModel()
) {
    // 加载单词数据
    LaunchedEffect(category) {
        viewModel.loadWords(category)
    }

    val uiState by viewModel.uiState.collectAsState()
    val iconStyle by viewModel.iconStyle.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    CompositionLocalProvider(LocalSettingIconStyle provides iconStyle) {
        val backdrop = rememberLayerBackdrop()
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        var searchQuery by rememberSaveable { mutableStateOf("") }
        var filterState by rememberSaveable { mutableStateOf(StudyFilter.ALL) }
        var selectedLevel by rememberSaveable { mutableStateOf("ALL") }
        var isMenuExpanded by rememberSaveable { mutableStateOf(false) }

        val animatedBlurRadius by animateDpAsState(
            targetValue = if (isMenuExpanded) 14.dp else 0.dp,
            animationSpec = tween(durationMillis = 200),
            label = "category_blur"
        )

        // 过滤词汇数据
        val currentWords = remember(uiState.words, selectedLevel, searchQuery, filterState) {
            uiState.words.filter { word ->
                val matchesFilter = when (filterState) {
                    StudyFilter.ALL -> true
                    StudyFilter.LEARNED -> word.isLearned
                    StudyFilter.UNLEARNED -> !word.isLearned
                }
                if (!matchesFilter) return@filter false

                val matchesLevel = if (selectedLevel == "ALL") true else word.level.equals(selectedLevel, ignoreCase = true)
                if (!matchesLevel) return@filter false

                if (searchQuery.isBlank()) {
                    true
                } else {
                    word.japanese.contains(searchQuery, ignoreCase = true) ||
                    word.hiragana.contains(searchQuery, ignoreCase = true) ||
                    word.chinese.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        // 计算各等级词汇数量
        val countsByLevel = remember(uiState.words, searchQuery, filterState) {
            val baseFiltered = uiState.words.filter { word ->
                val matchesFilter = when (filterState) {
                    StudyFilter.ALL -> true
                    StudyFilter.LEARNED -> word.isLearned
                    StudyFilter.UNLEARNED -> !word.isLearned
                }
                if (!matchesFilter) return@filter false

                if (searchQuery.isBlank()) {
                    true
                } else {
                    word.japanese.contains(searchQuery, ignoreCase = true) ||
                    word.hiragana.contains(searchQuery, ignoreCase = true) ||
                    word.chinese.contains(searchQuery, ignoreCase = true)
                }
            }

            listOf("ALL", "N1", "N2", "N3", "N4", "N5").associateWith { lvl ->
                if (lvl == "ALL") baseFiltered.size
                else baseFiltered.count { it.level.equals(lvl, ignoreCase = true) }
            }
        }

        val options = remember(iconStyle) {
            if (iconStyle == SettingIconStyle.PLUMP_DUO) {
                listOf(
                    DiscoveryOption(
                        label = "全部",
                        icon = Icons.Default.Book,
                        plumpIcon = PlumpIcon.BookOpen,
                        activeColor = IsoluminantPalette.SkyIndigo,
                    ),
                    DiscoveryOption(
                        label = "已学",
                        icon = Icons.Default.CheckCircle,
                        plumpIcon = PlumpIcon.BadgeCheck,
                        activeColor = IsoluminantPalette.EmeraldGreen,
                    ),
                    DiscoveryOption(
                        label = "未学",
                        icon = Icons.Default.Error,
                        plumpIcon = PlumpIcon.Flame,
                        activeColor = IsoluminantPalette.WarmAmber,
                    ),
                )
            } else {
                listOf(
                    DiscoveryOption(
                        label = "全部",
                        icon = Icons.Default.Book,
                        activeColor = NemoIndigo,
                    ),
                    DiscoveryOption(
                        label = "已学",
                        icon = Icons.Default.CheckCircle,
                        activeColor = NemoSecondary,
                    ),
                    DiscoveryOption(
                        label = "未学",
                        icon = Icons.Default.Error,
                        activeColor = NemoOrange,
                    ),
                )
            }
        }
        val selectedIndex = when (filterState) {
            StudyFilter.ALL -> 0
            StudyFilter.LEARNED -> 1
            StudyFilter.UNLEARNED -> 2
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NemoChasingDotsLoader()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = uiState.error ?: "加载失败", color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (animatedBlurRadius > 0.dp) Modifier.blur(animatedBlurRadius) else Modifier
                    )
                    .layerBackdrop(backdrop),
                contentPadding = PaddingValues(
                    top = statusBarTop + 60.dp,
                    bottom = navBarBottom + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 搜索与筛选栏（移入列表作为第一项，随列表顺畅滚动）
                item(key = "discovery_bar") {
                    DiscoveryBar(
                        options = options,
                        selectedOptionIndex = selectedIndex,
                        onOptionSelected = { index ->
                            filterState = when (index) {
                                0 -> StudyFilter.ALL
                                1 -> StudyFilter.LEARNED
                                2 -> StudyFilter.UNLEARNED
                                else -> StudyFilter.ALL
                            }
                        },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        searchPlaceholder = "搜索：汉字 / 假名 / 释义",
                        style = DiscoveryBarStyle(
                            backgroundColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White,
                            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            activeTextStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // 2. 单词平铺列表
                if (currentWords.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyIcon: androidx.compose.ui.graphics.vector.ImageVector =
                                if (searchQuery.isNotEmpty()) Icons.Rounded.Search else Icons.Rounded.Inbox
                            val emptyPlumpIcon: PlumpIcon =
                                if (searchQuery.isNotEmpty()) PlumpIcon.SearchX else PlumpIcon.Globe
                            EmptyState(
                                message = if (searchQuery.isNotEmpty()) "未找到相关单词"
                                    else if (selectedLevel != "ALL") "当前等级暂无单词"
                                    else "该分类下暂无词汇",
                                icon = emptyIcon,
                                plumpIcon = emptyPlumpIcon
                            )
                        }
                    }
                } else {
                    items(currentWords, key = { "category_word_${it.id}" }) { word ->
                        Box(modifier = Modifier.animateListItem().padding(horizontal = 16.dp)) {
                            WordListItemPremium(
                                word = word,
                                onClick = { onNavigateToWordDetail(word.id) }
                            )
                        }
                    }
                }
            }
        }

        // 1.5 页面全屏高斯模糊景深遮罩 (对齐学习界面：12% 柔光暗化 + 200ms 淡入淡出，点击外部收起菜单)
        LibraryMenuScrimOverlay(
            visible = isMenuExpanded,
            onDismiss = { isMenuExpanded = false }
        )

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
        FloatingHeaderGradientContainer(backgroundColor = backgroundColor) {
            val titleText = if (selectedLevel == "ALL") {
                if (uiState.isLoading) categoryTitle else "$categoryTitle (${currentWords.size})"
            } else {
                "$categoryTitle · $selectedLevel (${currentWords.size})"
            }

            CommonHeader(
                title = titleText,
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                actions = {
                    LibraryLevelSelectionMenu(
                        expanded = isMenuExpanded,
                        onExpandedChange = { isMenuExpanded = it },
                        selectedLevel = selectedLevel,
                        onLevelSelected = { selectedLevel = it },
                        countsByLevel = countsByLevel,
                        backdrop = backdrop,
                        onScrollToTop = {
                            coroutineScope.launch { lazyListState.scrollToItem(0) }
                        }
                    )
                }
            )
        }

        // 2.5 悬浮回顶按钮（严格锁定 30/48 黄金比例：56.dp 按钮对应 35.dp 图标，纯净磨砂底色）
        LibraryScrollToTopButton(
            lazyListState = lazyListState,
            backdrop = backdrop,
            isMenuExpanded = isMenuExpanded,
            bottomPadding = navBarBottom + 20.dp
        )
    }
    }
}

@Composable
private fun WordListItemPremium(
    word: Word,
    onClick: () -> Unit
) {
    val iconStyle = LocalSettingIconStyle.current
    val avatarColors = if (iconStyle == SettingIconStyle.PLUMP_DUO) PlumpAvatarColors else Md3AvatarColors
    val colorIndex = kotlin.math.abs(word.id.hashCode()) % avatarColors.size
    val avatarColor = avatarColors[colorIndex]

    PremiumCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            val char = word.japanese.firstOrNull()?.toString() ?: "?"
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(avatarColor.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NotoSerifJP,
                        localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                    ),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.W900,
                    color = avatarColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.japanese,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = NotoSerifJP,
                        localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                    ),
                    fontWeight = FontWeight.W800,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                val secondaryAnnotatedText = buildAnnotatedString {
                    if (word.hiragana.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                fontFamily = NotoSerifJP,
                                localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                            )
                        ) {
                            append(word.hiragana)
                        }
                    }
                    if (word.hiragana.isNotEmpty() && word.chinese.isNotEmpty()) {
                        append(" · ")
                    }
                    if (word.chinese.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                fontFamily = NotoSerifSC,
                                localeList = androidx.compose.ui.text.intl.LocaleList("zh")
                            )
                        ) {
                            append(word.chinese)
                        }
                    }
                }

                Text(
                    text = secondaryAnnotatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            if (word.level.isNotBlank()) {
                val levelColor = getLevelColor(word.level)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(levelColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = word.level.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = levelColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by if (onClick != null) {
        val isPressed by interactionSource.collectIsPressedAsState()
        animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            label = "cardScale",
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White


    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,

        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 22.dp, isDark = isDark)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        content = { Column(modifier = Modifier.padding(16.dp), content = content) }
    )
}

@Composable
private fun EmptyState(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Inbox,
    plumpIcon: PlumpIcon? = null
) {
    val iconStyle = LocalSettingIconStyle.current
    val isPlumpDuo = iconStyle == SettingIconStyle.PLUMP_DUO
    val accentColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else MaterialTheme.colorScheme.surfaceVariant

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isPlumpDuo && plumpIcon != null) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                PlumpDuoIcon(
                    icon = plumpIcon,
                    color = IsoluminantPalette.SkyIndigo,
                    size = 56.dp
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun getLevelColor(level: String): Color {
    val iconStyle = LocalSettingIconStyle.current
    return if (iconStyle == SettingIconStyle.PLUMP_DUO) {
        when (level.uppercase()) {
            "N5" -> IsoluminantPalette.EmeraldGreen
            "N4" -> IsoluminantPalette.LakesideCyan
            "N3" -> IsoluminantPalette.SkyIndigo
            "N2" -> IsoluminantPalette.WarmAmber
            "N1" -> IsoluminantPalette.RoseRuby
            else -> IsoluminantPalette.SkyIndigo
        }
    } else {
        when (level.uppercase()) {
            "N5" -> NemoSecondary
            "N4" -> NemoCyan
            "N3" -> NemoPrimary
            "N2" -> NemoOrange
            "N1" -> IosColors.Pink
            else -> NemoPrimary
        }
    }
}

private val Md3AvatarColors = listOf(
    NemoPrimary,
    NemoOrange,
    NemoSecondary,
    NemoIndigo,
    NemoTeal,
    NemoPurple,
    IosColors.Pink,
    NemoCyan
)

private val PlumpAvatarColors = listOf(
    IsoluminantPalette.SkyIndigo,     // 255°
    IsoluminantPalette.LakesideCyan,  // 195°
    IsoluminantPalette.EmeraldGreen,  // 145°
    IsoluminantPalette.WarmAmber,     // 70°
    IsoluminantPalette.CoralFlame,    // 30°
    IsoluminantPalette.RoseRuby,      // 15°
    IsoluminantPalette.LilacViolet,   // 295°
    IsoluminantPalette.GlazeMint      // 175°
)
