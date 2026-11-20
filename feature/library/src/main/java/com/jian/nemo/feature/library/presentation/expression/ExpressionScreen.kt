package com.jian.nemo.feature.library.presentation.expression

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.jian.nemo.core.ui.component.NemoTextField
import com.jian.nemo.core.ui.component.NemoTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.ExpressionCategoryPalette
import com.jian.nemo.core.domain.model.Expression
import com.jian.nemo.core.domain.model.ExpressionCategory
import com.jian.nemo.core.domain.model.ExpressionExample
import com.jian.nemo.core.ui.animation.rememberIsTransitionSettled
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.library.R
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
private fun getCategoryColor(key: String, isDark: Boolean): Color {
    return when (key) {
        "collocation" -> if (isDark) ExpressionCategoryPalette.OrangeDark else ExpressionCategoryPalette.OrangeLight
        "sentence_pattern" -> if (isDark) ExpressionCategoryPalette.EmeraldDark else ExpressionCategoryPalette.EmeraldLight
        "idiom" -> if (isDark) ExpressionCategoryPalette.PinkDark else ExpressionCategoryPalette.PinkLight
        "four_character_idiom" -> if (isDark) ExpressionCategoryPalette.CyanDark else ExpressionCategoryPalette.CyanLight
        "semi_fixed_template" -> if (isDark) ExpressionCategoryPalette.AmberDark else ExpressionCategoryPalette.AmberLight
        "collocation_group" -> if (isDark) ExpressionCategoryPalette.PurpleDark else ExpressionCategoryPalette.PurpleLight
        else -> MaterialTheme.colorScheme.primary
    }
}

@StringRes
private fun getCategoryShortNameRes(key: String): Int {
    return when (key) {
        "collocation" -> R.string.library_expression_label_short_collocation
        "sentence_pattern" -> R.string.library_expression_label_short_sentence_pattern
        "idiom" -> R.string.library_expression_label_short_idiom
        "four_character_idiom" -> R.string.library_expression_label_short_four_character_idiom
        "semi_fixed_template" -> R.string.library_expression_label_short_semi_fixed_template
        "collocation_group" -> R.string.library_expression_label_short_collocation_group
        else -> R.string.library_expression_label_short_default
    }
}

private val ExpressionLevelOptions = listOf(
    ExpressionUiState.LEVEL_ALL,
    "N5",
    "N4",
    "N3",
    "N2",
    "N1"
)

@Composable
fun ExpressionScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: ExpressionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    ExpressionContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onNavigateBack = onNavigateBack,
        onSelectCategory = viewModel::selectCategory,
        onSearchChange = viewModel::changeSearchQuery,
        onLevelSelect = viewModel::changeSelectedLevel,
        modifier = modifier
    )
}

@Composable
internal fun ExpressionContent(
    uiState: ExpressionUiState,
    searchQuery: String,
    onNavigateBack: (() -> Unit)?,
    onSelectCategory: (ExpressionCategory?) -> Unit,
    onSearchChange: (String) -> Unit,
    onLevelSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = uiState.selectedCategory != null) {
        onSelectCategory(null)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                NemoChasingDotsLoader(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.lib_expression_error_load_failed, uiState.error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                AnimatedContent(
                    targetState = uiState.selectedCategory,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "ExpressionScreenTransition",
                    modifier = Modifier.fillMaxSize()
                ) { selectedCategory ->
                    if (selectedCategory == null) {
                        CategoryDashboard(
                            categories = uiState.categories,
                            onCategorySelect = onSelectCategory,
                            onNavigateBack = onNavigateBack
                        )
                    } else {
                        ExpressionBrowser(
                            category = selectedCategory,
                            uiState = uiState,
                            searchQuery = searchQuery,
                            onBack = { onSelectCategory(null) },
                            onSearchChange = onSearchChange,
                            onLevelSelect = onLevelSelect
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDashboard(
    categories: List<ExpressionCategory>,
    onCategorySelect: (ExpressionCategory) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val backdrop = rememberLayerBackdrop()
    val isTransitionSettled = rememberIsTransitionSettled()
    val activeBackdrop = if (isTransitionSettled) backdrop else null
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val layoutDirection = LocalLayoutDirection.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val horizontalSafeInsets = WindowInsets.displayCutout.union(WindowInsets.navigationBars).asPaddingValues()
    val safeStart = horizontalSafeInsets.calculateStartPadding(layoutDirection)
    val safeEnd = horizontalSafeInsets.calculateEndPadding(layoutDirection)
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop = backdrop, enabled = isTransitionSettled),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = safeStart + 16.dp,
                end = safeEnd + 16.dp,
                top = if (onNavigateBack != null) statusBarTop + 60.dp + 16.dp else statusBarTop + 16.dp,
                bottom = navBarBottom + 24.dp
            )
        ) {
            items(
                items = categories,
                key = { it.categoryKey },
                contentType = { "expression_category" }
            ) { category ->
                CategoryFlatCard(
                    category = category,
                    isDark = isDark,
                    onClick = { onCategorySelect(category) }
                )
            }
        }

        if (onNavigateBack != null) {
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
                    title = stringResource(R.string.lib_expression_title),
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
fun CategoryFlatCard(
    category: ExpressionCategory,
    onClick: () -> Unit,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val themeColor = getCategoryColor(category.categoryKey, isDark)
    val englishSubtitle = remember(category.levelEnglish) {
        category.levelEnglish.replace("_", " ")
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .defaultMinSize(minHeight = 148.dp)
            .softCardShadow(borderRadius = 24.dp, isDark = isDark),
        shape = RoundedRectangle(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedRectangle(12.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(getCategoryShortNameRes(category.categoryKey)),
                        color = themeColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.levelName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = englishSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = category.definition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = themeColor.copy(alpha = 0.12f),
                    shape = RoundedRectangle(12.dp),
                    border = BorderStroke(0.5.dp, themeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = stringResource(R.string.lib_expression_count_format, category.items.size),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressionBrowser(
    category: ExpressionCategory,
    uiState: ExpressionUiState,
    searchQuery: String,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onLevelSelect: (String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val themeColor = getCategoryColor(category.categoryKey, isDark)
    val backdrop = rememberLayerBackdrop()
    val isTransitionSettled = rememberIsTransitionSettled()
    val activeBackdrop = if (isTransitionSettled) backdrop else null
    val layoutDirection = LocalLayoutDirection.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding()
    val horizontalSafeInsets = WindowInsets.displayCutout.union(WindowInsets.navigationBars).asPaddingValues()
    val safeStart = horizontalSafeInsets.calculateStartPadding(layoutDirection)
    val safeEnd = horizontalSafeInsets.calculateEndPadding(layoutDirection)
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop = backdrop, enabled = isTransitionSettled)
                .padding(
                    start = safeStart + 16.dp,
                    end = safeEnd + 16.dp,
                    top = statusBarTop + 60.dp + 16.dp
                )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedRectangle(12.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.lib_expression_formula_format, category.formula),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FlatFilterBar(
                searchQuery = searchQuery,
                selectedLevel = uiState.selectedLevel,
                onSearchChange = onSearchChange,
                onLevelSelect = onLevelSelect
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.filteredExpressions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = bottomInset + 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.lib_expression_empty_filter),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = bottomInset + 24.dp)
                ) {
                    items(
                        items = uiState.filteredExpressions,
                        key = { it.id },
                        contentType = { "expression_card" }
                    ) { expression ->
                        ExpressionFlatCard(
                            expression = expression,
                            themeColor = themeColor
                        )
                    }
                }
            }
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
                title = category.levelName,
                onBack = onBack,
                backgroundColor = Color.Transparent,
                backdrop = activeBackdrop,
                backButtonSize = 48.dp
            )
        }
    }
}

@Composable
fun FlatFilterBar(
    searchQuery: String,
    selectedLevel: String,
    onSearchChange: (String) -> Unit,
    onLevelSelect: (String) -> Unit
) {
    val allLabel = stringResource(R.string.lib_word_list_tab_all)

    Column(modifier = Modifier.fillMaxWidth()) {
        NemoTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.lib_expression_search_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(com.jian.nemo.core.ui.R.string.core_ui_discovery_cd_close)
                        )
                    }
                }
            },
            shape = RoundedRectangle(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpressionLevelOptions.forEach { level ->
                val isSelected = selectedLevel.equals(level, ignoreCase = true)
                val displayLabel = if (level == ExpressionUiState.LEVEL_ALL) allLabel else level
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedRectangle(8.dp))
                        .clickable { onLevelSelect(level) },
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedRectangle(8.dp)
                ) {
                    Text(
                        text = displayLabel,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpressionFlatCard(
    expression: Expression,
    themeColor: Color
) {
    var isClozeRevealed by rememberSaveable(expression.id) { mutableStateOf(false) }
    var isExamplesExpanded by rememberSaveable(expression.id) { mutableStateOf(false) }

    val rotationState = animateFloatAsState(
        targetValue = if (isExamplesExpanded) 180f else 0f,
        label = stringResource(R.string.library_expression_label_arrow_rotation)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedRectangle(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedRectangle(6.dp)
                ) {
                    Text(
                        text = expression.level,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = expression.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = expression.japanese,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = themeColor
            )

            Text(
                text = stringResource(R.string.lib_expression_furigana_format, expression.furigana),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = expression.chinese,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (expression.tip.isNotBlank()) {
                Text(
                    text = stringResource(R.string.lib_expression_tip_format, expression.tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (expression.synonyms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    expression.synonyms.forEach { syn ->
                        Surface(
                            color = themeColor.copy(alpha = 0.06f),
                            border = BorderStroke(0.5.dp, themeColor.copy(alpha = 0.2f)),
                            shape = RoundedRectangle(6.dp)
                        ) {
                            Text(
                                text = syn,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = themeColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FlatClozeBox(
                clozeShow = expression.clozeShow,
                clozeAnswer = expression.clozeAnswers.firstOrNull().orEmpty(),
                isRevealed = isClozeRevealed,
                onToggleReveal = { isClozeRevealed = !isClozeRevealed },
                themeColor = themeColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedRectangle(8.dp))
                        .clickable { isExamplesExpanded = !isExamplesExpanded }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.lib_expression_examples_expand_format, expression.examples.size),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.lib_expression_expand_arrow_cd),
                        modifier = Modifier.graphicsLayer { rotationZ = rotationState.value },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = isExamplesExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        expression.examples.forEachIndexed { index, ex ->
                            ExampleItem(
                                index = index + 1,
                                example = ex,
                                themeColor = themeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlatClozeBox(
    clozeShow: String,
    clozeAnswer: String,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    themeColor: Color
) {
    val parts = remember(clozeShow) { clozeShow.split("{{input}}") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(12.dp))
            .clickable { onToggleReveal() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (parts.isNotEmpty()) {
                    Text(
                        text = parts[0],
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = if (isRevealed) themeColor.copy(alpha = 0.15f) else themeColor,
                    shape = RoundedRectangle(8.dp),
                    border = if (isRevealed) BorderStroke(1.dp, themeColor) else null,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = if (isRevealed) " $clozeAnswer " else "  ?  ",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isRevealed) themeColor else Color.White
                    )
                }

                if (parts.size > 1) {
                    Text(
                        text = parts[1],
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRevealed) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = themeColor
                )
            }
        }
    }
}

@Composable
fun ExampleItem(
    index: Int,
    example: ExpressionExample,
    themeColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(12.dp)
    ) {
        Row {
            Text(
                text = "$index. ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = themeColor
            )
            Column {
                Text(
                    text = example.ja,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = example.zh,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
