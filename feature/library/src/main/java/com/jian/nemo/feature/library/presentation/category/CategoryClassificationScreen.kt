package com.jian.nemo.feature.library.presentation.category

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.NemoCategoryTheme
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.animation.rememberIsTransitionSettled
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.library.R
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay

private data class CategoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color
)

/**
 * 专项训练 / 专项词汇分类选择界面。
 */
@Composable
fun CategoryClassificationScreen(
    source: String = "practice",
    onNavigateBack: () -> Unit = {},
    onCategorySelected: (String, String) -> Unit = { _, _ -> },
    viewModel: CategoryClassificationViewModel = hiltViewModel()
) {
    val settingIconStyle by viewModel.settingIconStyle.collectAsStateWithLifecycle()

    CategoryClassificationContent(
        source = source,
        settingIconStyle = settingIconStyle,
        onNavigateBack = onNavigateBack,
        onCategorySelected = onCategorySelected
    )
}

@Composable
internal fun CategoryClassificationContent(
    source: String,
    settingIconStyle: SettingIconStyle,
    onNavigateBack: () -> Unit,
    onCategorySelected: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isSoftTranslucent = settingIconStyle == SettingIconStyle.SOFT_TRANSLUCENT
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    val title = when (source) {
        "practice" -> stringResource(R.string.lib_category_title_practice)
        "vocabulary" -> stringResource(R.string.lib_category_title_vocabulary)
        else -> stringResource(R.string.lib_category_title_default)
    }

    val nounTitle = stringResource(R.string.lib_category_item_noun_title)
    val nounSubtitle = stringResource(R.string.lib_category_item_noun_subtitle)
    val adjTitle = stringResource(R.string.lib_category_item_adj_title)
    val adjSubtitle = stringResource(R.string.lib_category_item_adj_subtitle)
    val verbTitle = stringResource(R.string.lib_category_item_verb_title)
    val verbSubtitle = stringResource(R.string.lib_category_item_verb_subtitle)
    val advTitle = stringResource(R.string.lib_category_item_adv_title)
    val advSubtitle = stringResource(R.string.lib_category_item_adv_subtitle)

    val basicItems = remember(
        nounTitle, nounSubtitle, adjTitle, adjSubtitle, verbTitle, verbSubtitle, advTitle, advSubtitle
    ) {
        listOf(
            CategoryItem("noun", nounTitle, nounSubtitle, Icons.AutoMirrored.Rounded.MenuBook, NemoCategoryTheme.getCategoryColor("noun")),
            CategoryItem("adj", adjTitle, adjSubtitle, Icons.Rounded.AutoAwesome, NemoCategoryTheme.getCategoryColor("adj")),
            CategoryItem("verb", verbTitle, verbSubtitle, Icons.AutoMirrored.Rounded.DirectionsRun, NemoCategoryTheme.getCategoryColor("verb")),
            CategoryItem("adv", advTitle, advSubtitle, Icons.Rounded.Tune, NemoCategoryTheme.getCategoryColor("adv"))
        )
    }

    val rentaiTitle = stringResource(R.string.lib_category_item_rentai_title)
    val rentaiSubtitle = stringResource(R.string.lib_category_item_rentai_subtitle)
    val conjTitle = stringResource(R.string.lib_category_item_conj_title)
    val conjSubtitle = stringResource(R.string.lib_category_item_conj_subtitle)
    val exclamTitle = stringResource(R.string.lib_category_item_exclam_title)
    val exclamSubtitle = stringResource(R.string.lib_category_item_exclam_subtitle)
    val particleTitle = stringResource(R.string.lib_category_item_particle_title)
    val particleSubtitle = stringResource(R.string.lib_category_item_particle_subtitle)

    val syntaxItems = remember(
        rentaiTitle, rentaiSubtitle, conjTitle, conjSubtitle, exclamTitle, exclamSubtitle, particleTitle, particleSubtitle
    ) {
        listOf(
            CategoryItem("rentai", rentaiTitle, rentaiSubtitle, Icons.Rounded.Link, NemoCategoryTheme.getCategoryColor("rentai")),
            CategoryItem("conj", conjTitle, conjSubtitle, Icons.AutoMirrored.Rounded.AltRoute, NemoCategoryTheme.getCategoryColor("conj")),
            CategoryItem("exclam", exclamTitle, exclamSubtitle, Icons.Rounded.Campaign, NemoCategoryTheme.getCategoryColor("exclam")),
            CategoryItem("particle", particleTitle, particleSubtitle, Icons.Rounded.Extension, NemoCategoryTheme.getCategoryColor("particle"))
        )
    }

    val prefixTitle = stringResource(R.string.lib_category_item_prefix_title)
    val prefixSubtitle = stringResource(R.string.lib_category_item_prefix_subtitle)
    val suffixTitle = stringResource(R.string.lib_category_item_suffix_title)
    val suffixSubtitle = stringResource(R.string.lib_category_item_suffix_subtitle)
    val expressionTitle = stringResource(R.string.lib_category_item_expression_title)
    val expressionSubtitle = stringResource(R.string.lib_category_item_expression_subtitle)
    val kataTitle = stringResource(R.string.lib_category_item_kata_title)
    val kataSubtitle = stringResource(R.string.lib_category_item_kata_subtitle)

    val expressionItems = remember(
        prefixTitle, prefixSubtitle, suffixTitle, suffixSubtitle, expressionTitle, expressionSubtitle, kataTitle, kataSubtitle
    ) {
        listOf(
            CategoryItem("prefix", prefixTitle, prefixSubtitle, Icons.AutoMirrored.Rounded.ArrowForward, NemoCategoryTheme.getCategoryColor("prefix")),
            CategoryItem("suffix", suffixTitle, suffixSubtitle, Icons.AutoMirrored.Rounded.ArrowBack, NemoCategoryTheme.getCategoryColor("suffix")),
            CategoryItem("expression", expressionTitle, expressionSubtitle, Icons.Rounded.FormatQuote, NemoCategoryTheme.getCategoryColor("expression")),
            CategoryItem("kata", kataTitle, kataSubtitle, Icons.Rounded.Language, NemoCategoryTheme.getCategoryColor("kata"))
        )
    }

    val backdrop = rememberLayerBackdrop()
    val isTransitionSettled = rememberIsTransitionSettled()
    val activeBackdrop = if (isTransitionSettled) backdrop else null
    val layoutDirection = LocalLayoutDirection.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val horizontalSafeInsets = WindowInsets.displayCutout.union(WindowInsets.navigationBars).asPaddingValues()
    val safeStart = horizontalSafeInsets.calculateStartPadding(layoutDirection)
    val safeEnd = horizontalSafeInsets.calculateEndPadding(layoutDirection)

    CompositionLocalProvider(LocalSettingIconStyle provides settingIconStyle) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop = backdrop, enabled = isTransitionSettled)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = safeStart + 16.dp,
                        end = safeEnd + 16.dp,
                        top = statusBarTop + 60.dp + 16.dp,
                        bottom = navBarBottom + 24.dp
                    )
            ) {
                SectionHeader(title = stringResource(R.string.lib_category_section_basic), delay = 0)
                CategoryGrid(
                    items = basicItems,
                    isDark = isDark,
                    isSoftTranslucent = isSoftTranslucent,
                    onItemClick = { category -> onCategorySelected(category.id, category.title) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                SectionHeader(title = stringResource(R.string.lib_category_section_syntax), delay = 100)
                CategoryGrid(
                    items = syntaxItems,
                    isDark = isDark,
                    isSoftTranslucent = isSoftTranslucent,
                    onItemClick = { category -> onCategorySelected(category.id, category.title) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                SectionHeader(title = stringResource(R.string.lib_category_section_expression), delay = 200)
                CategoryGrid(
                    items = expressionItems,
                    isDark = isDark,
                    isSoftTranslucent = isSoftTranslucent,
                    onItemClick = { category -> onCategorySelected(category.id, category.title) }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .drawWithCache {
                        val gradientBrush = Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor.copy(alpha = 0.95f),
                                backgroundColor.copy(alpha = 0.75f),
                                backgroundColor.copy(alpha = 0.35f),
                                backgroundColor.copy(alpha = 0f)
                            )
                        )
                        onDrawBehind {
                            drawRect(brush = gradientBrush)
                        }
                    }
                    .padding(bottom = 8.dp)
            ) {
                CommonHeader(
                    title = title,
                    onBack = onNavigateBack,
                    backgroundColor = Color.Transparent,
                    backdrop = activeBackdrop,
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
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (delay > 0) {
            delay(delay.toLong())
        }
        alphaAnim.animateTo(1f, tween(500))
    }

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = alphaAnim.value }
    )
}

@Composable
private fun CategoryGrid(
    items: List<CategoryItem>,
    isDark: Boolean,
    isSoftTranslucent: Boolean,
    onItemClick: (CategoryItem) -> Unit
) {
    val rows = remember(items) { items.chunked(2) }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    CategoryCard(
                        item = item,
                        isDark = isDark,
                        isSoftTranslucent = isSoftTranslucent,
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
    isDark: Boolean,
    isSoftTranslucent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val containerBgColor = if (isSoftTranslucent) item.iconColor.copy(alpha = 0.15f) else item.iconColor
    val iconTint = if (isSoftTranslucent) item.iconColor else Color.White

    Surface(
        onClick = onClick,
        shape = RoundedRectangle(24.dp),
        color = containerColor,
        modifier = modifier
            .heightIn(min = 100.dp)
            .softCardShadow(borderRadius = 24.dp, isDark = isDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
