package com.jian.nemo.feature.library.presentation.category

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NemoCategoryTheme
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.library.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 分类项数据模型
 */
private data class CategoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color
)

/**
 * 专项训练 / 专项词汇 - 分类选择界面
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

    val title = when (source) {
        "practice" -> stringResource(R.string.lib_category_title_practice)
        "vocabulary" -> stringResource(R.string.lib_category_title_vocabulary)
        else -> stringResource(R.string.lib_category_title_default)
    }

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalSettingIconStyle provides settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = statusBarTop + 60.dp + 16.dp,
                        bottom = navBarBottom + 24.dp
                    )
            ) {
                // 1. 基础词性类
                SectionHeader(title = stringResource(R.string.lib_category_section_basic), delay = 0)
                CategoryGrid(
                    items = listOf(
                        CategoryItem(
                            id = "noun",
                            title = stringResource(R.string.lib_category_item_noun_title),
                            subtitle = stringResource(R.string.lib_category_item_noun_subtitle),
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            iconColor = NemoCategoryTheme.getCategoryColor("noun")
                        ),
                        CategoryItem(
                            id = "adj",
                            title = stringResource(R.string.lib_category_item_adj_title),
                            subtitle = stringResource(R.string.lib_category_item_adj_subtitle),
                            icon = Icons.Rounded.AutoAwesome,
                            iconColor = NemoCategoryTheme.getCategoryColor("adj")
                        ),
                        CategoryItem(
                            id = "verb",
                            title = stringResource(R.string.lib_category_item_verb_title),
                            subtitle = stringResource(R.string.lib_category_item_verb_subtitle),
                            icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                            iconColor = NemoCategoryTheme.getCategoryColor("verb")
                        ),
                        CategoryItem(
                            id = "adv",
                            title = stringResource(R.string.lib_category_item_adv_title),
                            subtitle = stringResource(R.string.lib_category_item_adv_subtitle),
                            icon = Icons.Rounded.Tune,
                            iconColor = NemoCategoryTheme.getCategoryColor("adv")
                        )
                    ),
                    onItemClick = { category ->
                        onCategorySelected(category.id, category.title)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. 构词·句法功能类
                SectionHeader(title = stringResource(R.string.lib_category_section_syntax), delay = 100)
                CategoryGrid(
                    items = listOf(
                        CategoryItem(
                            id = "rentai",
                            title = stringResource(R.string.lib_category_item_rentai_title),
                            subtitle = stringResource(R.string.lib_category_item_rentai_subtitle),
                            icon = Icons.Rounded.Link,
                            iconColor = NemoCategoryTheme.getCategoryColor("rentai")
                        ),
                        CategoryItem(
                            id = "conj",
                            title = stringResource(R.string.lib_category_item_conj_title),
                            subtitle = stringResource(R.string.lib_category_item_conj_subtitle),
                            icon = Icons.AutoMirrored.Rounded.AltRoute,
                            iconColor = NemoCategoryTheme.getCategoryColor("conj")
                        ),
                        CategoryItem(
                            id = "exclam",
                            title = stringResource(R.string.lib_category_item_exclam_title),
                            subtitle = stringResource(R.string.lib_category_item_exclam_subtitle),
                            icon = Icons.Rounded.Campaign,
                            iconColor = NemoCategoryTheme.getCategoryColor("exclam")
                        ),
                        CategoryItem(
                            id = "particle",
                            title = stringResource(R.string.lib_category_item_particle_title),
                            subtitle = stringResource(R.string.lib_category_item_particle_subtitle),
                            icon = Icons.Rounded.Extension,
                            iconColor = NemoCategoryTheme.getCategoryColor("particle")
                        )
                    ),
                    onItemClick = { category ->
                        onCategorySelected(category.id, category.title)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3. 构词·表达用法类
                SectionHeader(title = stringResource(R.string.lib_category_section_expression), delay = 200)
                CategoryGrid(
                    items = listOf(
                        CategoryItem(
                            id = "prefix",
                            title = stringResource(R.string.lib_category_item_prefix_title),
                            subtitle = stringResource(R.string.lib_category_item_prefix_subtitle),
                            icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            iconColor = NemoCategoryTheme.getCategoryColor("prefix")
                        ),
                        CategoryItem(
                            id = "suffix",
                            title = stringResource(R.string.lib_category_item_suffix_title),
                            subtitle = stringResource(R.string.lib_category_item_suffix_subtitle),
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            iconColor = NemoCategoryTheme.getCategoryColor("suffix")
                        ),
                        CategoryItem(
                            id = "expression",
                            title = stringResource(R.string.lib_category_item_expression_title),
                            subtitle = stringResource(R.string.lib_category_item_expression_subtitle),
                            icon = Icons.Rounded.FormatQuote,
                            iconColor = NemoCategoryTheme.getCategoryColor("expression")
                        ),
                        CategoryItem(
                            id = "kata",
                            title = stringResource(R.string.lib_category_item_kata_title),
                            subtitle = stringResource(R.string.lib_category_item_kata_subtitle),
                            icon = Icons.Rounded.Language,
                            iconColor = NemoCategoryTheme.getCategoryColor("kata")
                        )
                    ),
                    onItemClick = { category ->
                        onCategorySelected(category.id, category.title)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
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
                    .padding(bottom = 8.dp)
            ) {
                CommonHeader(
                    title = title,
                    onBack = onNavigateBack,
                    backgroundColor = Color.Transparent,
                    backdrop = backdrop,
                    backButtonSize = 48.dp
                )
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
    val containerBgColor = if (isSoftTranslucent) item.iconColor.copy(alpha = 0.15f) else item.iconColor
    val iconTint = if (isSoftTranslucent) item.iconColor else Color.White

    Surface(
        onClick = onClick,
        shape = RoundedRectangle(24.dp),
        color = containerColor,
        modifier = modifier
            .height(100.dp)
            .softCardShadow(borderRadius = 24.dp, isDark = isDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(containerBgColor, RoundedRectangle(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = iconTint
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}
