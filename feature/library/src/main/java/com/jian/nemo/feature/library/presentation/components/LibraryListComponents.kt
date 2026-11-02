package com.jian.nemo.feature.library.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.jian.nemo.feature.library.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.designsystem.theme.NotoSerifSC
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.common.NemoMorphMenu
import com.jian.nemo.core.ui.component.common.NemoMorphMenuItem
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBar
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBarStyle
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryOption
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.library.presentation.list.StudyFilter
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch

/**
 * 词库列表通用 - 56dp 纯净磨砂回顶悬浮按钮
 *
 * 严格锁定 30/48 黄金比例（56.dp 按钮对应 35.dp 图标），纯净无杂色磨砂底 + 透镜物理折射
 */
@Composable
fun BoxScope.LibraryScrollToTopButton(
    lazyListState: LazyListState,
    backdrop: Backdrop?,
    isMenuExpanded: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 6
        }
    }

    AnimatedVisibility(
        visible = showScrollToTop && !isMenuExpanded,
        enter = fadeIn(spring()) + scaleIn(spring(dampingRatio = 0.75f)),
        exit = fadeOut(tween(150)) + scaleOut(tween(150)),
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = bottomPadding)
    ) {
        FrostedLiquidGlassButton(
            onClick = {
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            },
            shape = CircleShape,
            backdrop = backdrop,
            hasLens = backdrop != null,
            contentPadding = PaddingValues(0.dp),
            isInteractive = true,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                painter = painterResource(id = com.jian.nemo.core.ui.R.drawable.ic_scroll_to_top),
                contentDescription = stringResource(R.string.lib_components_scroll_to_top_cd),
                modifier = Modifier.size(35.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 词库列表通用 - 等级筛选 NemoMorphMenu
 *
 * 48dp 液态玻璃透镜按钮，专属几何图标，全部等级与 N5~N1 纯文字对齐、选中打勾、无震动
 * 遵循单一官方原生图标规范与标准设计调色盘 IosColors。
 */
@Composable
fun LibraryLevelSelectionMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    countsByLevel: Map<String, Int>,
    backdrop: Backdrop?,
    onScrollToTop: () -> Unit
) {
    NemoMorphMenu(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        buttonSize = 48.dp,
        menuWidth = 200.dp,
        enableHaptic = false,
        backdrop = backdrop,
        iconPainter = painterResource(id = com.jian.nemo.core.ui.R.drawable.ic_level_all),
        contentDescription = stringResource(R.string.lib_components_select_level_cd)
    ) {
        val allCount = countsByLevel.values.sum()
        val isAllSelected = selectedLevel == "ALL"

        NemoMorphMenuItem(
            text = stringResource(R.string.lib_components_all_levels_count_format, allCount),
            leadingIcon = Icons.Rounded.School,
            iconColor = IosColors.Blue,
            trailingIcon = if (isAllSelected) Icons.Rounded.Check else null,
            onClick = {
                onLevelSelected("ALL")
                close()
                onScrollToTop()
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        val levelsList = listOf(
            "N5" to IosColors.Green,
            "N4" to IosColors.Cyan,
            "N3" to IosColors.Indigo,
            "N2" to IosColors.Purple,
            "N1" to IosColors.Red
        )
        levelsList.forEach { (level, levelColor) ->
            val count = countsByLevel[level] ?: 0
            val isSelected = selectedLevel.equals(level, ignoreCase = true)
            NemoMorphMenuItem(
                text = stringResource(R.string.lib_components_level_count_format, level, count),
                leadingIcon = Icons.Rounded.School,
                iconColor = levelColor,
                trailingIcon = if (isSelected) Icons.Rounded.Check else null,
                onClick = {
                    onLevelSelected(level)
                    close()
                    onScrollToTop()
                }
            )
        }
    }
}


/**
 * 词库列表通用 - 页面全屏手势遮罩
 *
 * 纯透明背景拦截外部点击平滑收起菜单，保持底层窗口清晰
 */
@Composable
fun LibraryMenuScrimOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
}

/**
 * 词库列表通用 - 悬浮顶栏柔和垂直渐变保护遮罩容器
 */
@Composable
fun FloatingHeaderGradientContainer(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.95f),
                        backgroundColor.copy(alpha = 0.75f),
                        backgroundColor.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
            .padding(bottom = 8.dp),
        content = content
    )
}

val AvatarPalette = listOf(
    IosColors.Blue,
    IosColors.Orange,
    IosColors.Green,
    IosColors.Indigo,
    IosColors.Teal,
    IosColors.Purple,
    IosColors.Pink,
    IosColors.Cyan
)

@Composable
fun getLevelColor(level: String): Color {
    return when (level.uppercase()) {
        "N5" -> IosColors.Green
        "N4" -> IosColors.Cyan
        "N3" -> IosColors.Blue
        "N2" -> IosColors.Orange
        "N1" -> IosColors.Pink
        else -> IosColors.Blue
    }
}

/**
 * 词库列表通用 - 22dp G2 连续平滑圆角卡片容器
 */
@Composable
fun PremiumCard(
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
        shape = RoundedRectangle(22.dp),
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

/**
 * 词库列表通用 - 标准双轨单容器单词卡片列表项
 */
@Composable
fun WordListItemPremium(
    word: Word,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconStyle = LocalSettingIconStyle.current
    val colorIndex = kotlin.math.abs(word.id.hashCode()) % AvatarPalette.size
    val avatarColor = AvatarPalette[colorIndex]
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
    val containerBg = if (isSoftTranslucent) avatarColor.copy(alpha = 0.15f) else avatarColor
    val contentColor = if (isSoftTranslucent) avatarColor else Color.White

    PremiumCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(containerBg, RoundedRectangle(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = word.japanese.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NotoSerifJP,
                        localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                    ),
                    fontWeight = FontWeight.W900,
                    color = contentColor
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
                val levelBg = if (isSoftTranslucent) levelColor.copy(alpha = 0.15f) else levelColor
                val levelText = if (isSoftTranslucent) levelColor else Color.White
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedRectangle(8.dp))
                        .background(levelBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = word.level.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = levelText
                    )
                }
            }
        }
    }
}

/**
 * 词库列表通用 - 搜索与分类（全部/已学/未学）胶囊栏
 */
@Composable
fun LibraryDiscoveryBar(
    selectedFilter: StudyFilter,
    onFilterSelected: (StudyFilter) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val tabAllText = stringResource(R.string.lib_word_list_tab_all)
    val tabLearnedText = stringResource(R.string.lib_word_list_tab_learned)
    val tabUnlearnedText = stringResource(R.string.lib_word_list_tab_unlearned)

    val options = remember(tabAllText, tabLearnedText, tabUnlearnedText) {
        listOf(
            DiscoveryOption(
                label = tabAllText,
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                activeColor = IosColors.Indigo,
            ),
            DiscoveryOption(
                label = tabLearnedText,
                icon = Icons.Rounded.CheckCircle,
                activeColor = IosColors.Green,
            ),
            DiscoveryOption(
                label = tabUnlearnedText,
                icon = Icons.Rounded.HourglassEmpty,
                activeColor = IosColors.Orange,
            ),
        )
    }

    val selectedIndex = when (selectedFilter) {
        StudyFilter.ALL -> 0
        StudyFilter.LEARNED -> 1
        StudyFilter.UNLEARNED -> 2
    }

    DiscoveryBar(
        options = options,
        selectedOptionIndex = selectedIndex,
        onOptionSelected = { index ->
            val newFilter = when (index) {
                0 -> StudyFilter.ALL
                1 -> StudyFilter.LEARNED
                2 -> StudyFilter.UNLEARNED
                else -> StudyFilter.ALL
            }
            onFilterSelected(newFilter)
        },
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchPlaceholder = searchPlaceholder,
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
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
