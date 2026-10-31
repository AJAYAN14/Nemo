package com.jian.nemo.feature.library.presentation.category

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.NemoCategoryColors
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 词性分类主题颜色
 */
data class CategoryThemeColor(
    val containerColor: Color,
    val contentColor: Color
)

/**
 * 根据当前主题获取分类颜色
 */
@Composable
private fun getCategoryTheme(
    lightContainer: Color,
    lightContent: Color,
    darkContainer: Color,
    darkContent: Color
): CategoryThemeColor {
    // 保持原来的颜色逻辑用于Icon
    val isDark = MaterialTheme.colorScheme.surface.luminance < 0.5
    return CategoryThemeColor(
        containerColor = if (isDark) darkContainer else lightContainer,
        contentColor = if (isDark) darkContent else lightContent
    )
}

/**
 * 专项训练 - 分类选择界面
 *
 * 风格统一：
 * - Background: Solid (MaterialTheme.colorScheme.background)
 * - Card: Premium Card (White/SurfaceContainer + Shadow + 26dp Rounded)
 * - Icon: Squircle (Rounded 14dp)
 * - Typography: ExtraBold Titles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryClassificationScreen(
    source: String = "practice",
    onNavigateBack: () -> Unit = {},
    onCategorySelected: (String, String) -> Unit = { _, _ -> },
    viewModel: CategoryClassificationViewModel = hiltViewModel()
) {
    val settingIconStyle by viewModel.settingIconStyle.collectAsState()
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    // 根据来源显示不同的标题
    val title = when (source) {
        "practice" -> "专项训练"
        "vocabulary" -> "专项词汇"
        else -> "专项分类"
    }

    CompositionLocalProvider(LocalSettingIconStyle provides settingIconStyle) {
        Scaffold(
            topBar = {
                com.jian.nemo.core.ui.component.common.CommonHeader(
                    title = title,
                    onBack = onNavigateBack,
                    backgroundColor = backgroundColor // Header 背景与页面一致
                )
            },
            containerColor = backgroundColor
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // 1. 基础词性类
            SectionHeader(title = "基础词性类", delay = 0)
            CategoryGrid(
                items = listOf(
                    CategoryItem(
                        id = "noun",
                        title = "名词类",
                        subtitle = "名词、代词等",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        plumpIcon = PlumpIcon.BookOpen,
                        isoluminantColor = IsoluminantPalette.SkyIndigo,
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardNounBgLight, NemoCategoryColors.CardNounTextLight,
                            NemoCategoryColors.CardNounBgDark, NemoCategoryColors.CardNounTextDark
                        ) }
                    ),
                    CategoryItem(
                        id = "adj",
                        title = "形容词类",
                        subtitle = "い形、な形形容词",
                        icon = Icons.Rounded.AutoAwesome,
                        plumpIcon = PlumpIcon.Sparkles,
                        isoluminantColor = IsoluminantPalette.WarmAmber,
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardAdjIBgLight, NemoCategoryColors.CardAdjITextLight,
                            NemoCategoryColors.CardAdjIBgDark, NemoCategoryColors.CardAdjITextDark
                        ) }
                    ),
                    CategoryItem(
                        id = "verb",
                        title = "动词类",
                        subtitle = "自动/他动/自他動词",
                        icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                        plumpIcon = PlumpIcon.Activity,
                        isoluminantColor = IsoluminantPalette.EmeraldGreen,
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardVerbBgLight, NemoCategoryColors.CardVerbTextLight,
                            NemoCategoryColors.CardVerbBgDark, NemoCategoryColors.CardVerbTextDark
                        ) }
                    ),
                    CategoryItem(
                        id = "adv",
                        title = "副词",
                        subtitle = "修饰用言词汇",
                        icon = Icons.Rounded.Tune,
                        plumpIcon = PlumpIcon.SlidersHorizontal,
                        isoluminantColor = IsoluminantPalette.LakesideCyan,
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardAdvBgLight, NemoCategoryColors.CardAdvTextLight,
                            NemoCategoryColors.CardAdvBgDark, NemoCategoryColors.CardAdvTextDark
                        ) }
                    )
                ),
                onItemClick = { category ->
                    onCategorySelected(category.id, category.title)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 构词·句法功能类
            SectionHeader(title = "構词·句法功能类", delay = 100)
            CategoryGrid(
                items = listOf(
                    CategoryItem(
                        id = "rentai",
                        title = "连体词",
                        subtitle = "直接修饰体言",
                        icon = Icons.Rounded.Link,
                        plumpIcon = PlumpIcon.Link2,
                        isoluminantColor = IsoluminantPalette.SeaMarine,
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardRentaiBgLight, NemoCategoryColors.CardRentaiTextLight,
                            NemoCategoryColors.CardRentaiBgDark, NemoCategoryColors.CardRentaiTextDark
                        ) }
                    ),
                    CategoryItem(
                        id = "conj",
                        title = "接続词",
                        subtitle = "连接句子成分",
                        icon = Icons.AutoMirrored.Rounded.AltRoute,
                        plumpIcon = PlumpIcon.Workflow,
                        isoluminantColor = IsoluminantPalette.fromOklch(85f),
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardConjBgLight, NemoCategoryColors.CardConjTextLight,
                            NemoCategoryColors.CardConjBgDark, NemoCategoryColors.CardConjTextDark
                        ) }
                    ),
                    CategoryItem(
                        id = "exclam",
                        title = "感叹词",
                        subtitle = "表达情感语气",
                        icon = Icons.Rounded.Campaign,
                        plumpIcon = PlumpIcon.Megaphone,
                        isoluminantColor = IsoluminantPalette.CoralFlame,
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardIdiomBgLight, NemoCategoryColors.CardIdiomTextLight,
                            NemoCategoryColors.CardIdiomBgDark, NemoCategoryColors.CardIdiomTextDark
                        ) }
                    ),
                    CategoryItem(
                        id = "particle",
                        title = "助词",
                        subtitle = "语法功能标记",
                        icon = Icons.Rounded.Extension,
                        plumpIcon = PlumpIcon.Puzzle,
                        isoluminantColor = IsoluminantPalette.LilacViolet,
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardFixBgLight, NemoCategoryColors.CardFixTextLight,
                            NemoCategoryColors.CardFixBgDark, NemoCategoryColors.CardFixTextDark
                        ) }
                    )
                ),
                onItemClick = { category ->
                    onCategorySelected(category.id, category.title)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 构词·表达用法类
            SectionHeader(title = "構词·表达用法类", delay = 200)
            CategoryGrid(
                items = listOf(
                    CategoryItem(
                        id = "prefix",
                        title = "接头词",
                        subtitle = "词语前置构成",
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        plumpIcon = PlumpIcon.ArrowRight,
                        isoluminantColor = IsoluminantPalette.RoseRuby,
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardKataBgLight, NemoCategoryColors.CardKataTextLight,
                            NemoCategoryColors.CardKataBgDark, NemoCategoryColors.CardKataTextDark
                        ) }
                    ),
                    CategoryItem(
                        id = "suffix",
                        title = "接尾词",
                        subtitle = "词语后置构成",
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        plumpIcon = PlumpIcon.ArrowLeft,
                        isoluminantColor = IsoluminantPalette.GlazeMint,
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardSoundBgLight, NemoCategoryColors.CardSoundTextLight,
                            NemoCategoryColors.CardSoundBgDark, NemoCategoryColors.CardSoundTextDark
                        ) }
                    ),
                    CategoryItem(
                        id = "expression",
                        title = "表达·固定句型",
                        subtitle = "习惯表达方式",
                        icon = Icons.Rounded.FormatQuote,
                        plumpIcon = PlumpIcon.Quote,
                        isoluminantColor = IsoluminantPalette.fromOklch(45f),
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardKeigoBgLight, NemoCategoryColors.CardKeigoTextLight,
                            NemoCategoryColors.CardKeigoBgDark, NemoCategoryColors.CardKeigoTextDark
                        ) }
                    ),
                    CategoryItem(
                        id = "kata",
                        title = "外来语",
                        subtitle = "片假名借词体系",
                        icon = Icons.Rounded.Language,
                        plumpIcon = PlumpIcon.Globe,
                        isoluminantColor = IsoluminantPalette.OceanBlue,
                        colors = { getCategoryTheme(
                            NemoCategoryColors.CardAdjNaBgLight, NemoCategoryColors.CardAdjNaTextLight,
                            NemoCategoryColors.CardAdjNaBgDark, NemoCategoryColors.CardAdjNaTextDark
                        ) }
                    )
                ),
                onItemClick = { category ->
                    onCategorySelected(category.id, category.title)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

@Composable
private fun SectionHeader(
    title: String,
    delay: Int = 0,
    modifier: Modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        launch { alpha.animateTo(1f, tween(500)) }
    }

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha.value)
    )
}

@Composable
private fun CategoryGrid(
    items: List<CategoryItem>,
    onItemClick: (CategoryItem) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    CategoryCard(
                        item = item,
                        modifier = Modifier.weight(1f),
                        onClick = { onItemClick(item) }
                    )
                }
                // 如果只有1个item，添加一个空的Spacer来保持布局
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    item: CategoryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val themeColors = item.colors()
    val isDark = MaterialTheme.colorScheme.background.luminance < 0.5f // Use standard check

    val containerColor = themeColors.containerColor
    val borderColor = if (isDark) themeColors.contentColor.copy(alpha = 0.2f) else themeColors.contentColor.copy(alpha = 0.12f)
    val iconContainerColor = if (isDark) themeColors.contentColor.copy(alpha = 0.15f) else Color.White

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = modifier
            .height(76.dp)
            .softCardShadow(borderRadius = 20.dp, isDark = isDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon Container：根据全局设置区分 Plump Duo 与 MD3
            val iconStyle = LocalSettingIconStyle.current
            val isPlumpDuo = iconStyle == SettingIconStyle.PLUMP_DUO

            if (isPlumpDuo) {
                // Plump Duo 规范：42dp 纯透明容器，居中展示 28dp 双层微质感矢量图标，无沉重方块背景
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlumpDuoIcon(
                        icon = item.plumpIcon,
                        color = item.isoluminantColor,
                        size = 28.dp
                    )
                }
            } else {
                // 原生 Material 3 规范：Squircle 浅色容器与单色描边图标
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = themeColors.contentColor
                    )
                }
            }

            // Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface, // Clean text color
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 分类项数据类
 */
private data class CategoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val plumpIcon: PlumpIcon,
    val isoluminantColor: Color,
    val colors: @Composable () -> CategoryThemeColor
)

// 扩展属性：获取Color的亮度
private val Color.luminance: Float
    get() = (0.299f * red + 0.587f * green + 0.114f * blue) // Keep for local usage
