package com.jian.nemo.feature.library.presentation.expression

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.ui.component.common.NemoScaffold
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler
import com.jian.nemo.core.domain.model.Expression
import com.jian.nemo.core.domain.model.ExpressionCategory
import com.jian.nemo.core.domain.model.ExpressionExample
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader

// 6 大分类精致扁平色彩系统
private val CategoryOrange = Color(0xFFFF6B4A)
private val CategoryEmerald = Color(0xFF10B981)
private val CategoryPink = Color(0xFFEC4899)
private val CategoryCyan = Color(0xFF06B6D4)
private val CategoryAmber = Color(0xFFF59E0B)
private val CategoryPurple = Color(0xFFA855F7)

@Composable
private fun getCategoryColor(key: String): Color {
    return when (key) {
        "collocation" -> CategoryOrange
        "sentence_pattern" -> CategoryEmerald
        "idiom" -> CategoryPink
        "four_character_idiom" -> CategoryCyan
        "semi_fixed_template" -> CategoryAmber
        "collocation_group" -> CategoryPurple
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getCategoryShortName(key: String): String {
    return when (key) {
        "collocation" -> "固"
        "sentence_pattern" -> "句"
        "idiom" -> "惯"
        "four_character_idiom" -> "四"
        "semi_fixed_template" -> "框"
        "collocation_group" -> "群"
        else -> "语"
    }
}

@Composable
fun ExpressionScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: ExpressionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 物理返回键处理
    BackHandler(enabled = uiState.selectedCategory != null || onNavigateBack != null) {
        if (uiState.selectedCategory != null) {
            viewModel.selectCategory(null)
        } else {
            onNavigateBack?.invoke()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // 微色差底色 #EEF2FF 级底色
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
                        text = "数据加载出错了: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                AnimatedContent(
                    targetState = uiState.selectedCategory,
                    transitionSpec = {
                        if (targetState != null) {
                            // 从第一层到第二层：新页面从右侧滑入，旧页面从左侧滑出
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            // 从第二层到第一层：新页面从左侧滑入，旧页面从右侧滑出
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "ExpressionScreenTransition",
                    modifier = Modifier.fillMaxSize()
                ) { selectedCategory ->
                    if (selectedCategory == null) {
                        // 第一层：分类面板主导航
                        CategoryDashboard(
                            categories = uiState.categories,
                            onCategorySelect = { viewModel.selectCategory(it) },
                            onNavigateBack = onNavigateBack
                        )
                    } else {
                        // 第二层：词法细节浏览器
                        ExpressionBrowser(
                            category = selectedCategory,
                            uiState = uiState,
                            onBack = { viewModel.selectCategory(null) },
                            onSearchChange = { viewModel.changeSearchQuery(it) },
                            onLevelSelect = { viewModel.changeSelectedLevel(it) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 第一层：九宫格总览大厅 (UI/UX Pro Max 扁平卡片与灵动排版)
 */
@Composable
fun CategoryDashboard(
    categories: List<ExpressionCategory>,
    onCategorySelect: (ExpressionCategory) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    if (onNavigateBack != null) {
        NemoScaffold(
            title = "灵动词法库",
            onBack = onNavigateBack,
            backgroundColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 24.dp
                )
            ) {
                items(categories) { category ->
                    CategoryFlatCard(
                        category = category,
                        onClick = { onCategorySelect(category) }
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 响应式2列网格，大圆角 24.dp 扁平卡片展示
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(categories) { category ->
                    CategoryFlatCard(
                        category = category,
                        onClick = { onCategorySelect(category) }
                    )
                }
            }
        }
    }
}

/**
 * 极具扁平扁平美感的分类卡片 (Flat UI, 0.dp Elevation, 24.dp Round, 12.dp Squircle Icon)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFlatCard(
    category: ExpressionCategory,
    onClick: () -> Unit
) {
    val themeColor = getCategoryColor(category.categoryKey)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(1.35f)
            .softCardShadow(borderRadius = 24.dp, isDark = isDark),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        // 纯扁平化描边边界定义 (NEMO_UI_SPEC: Elevation = 0.dp)
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 12.dp 圆角 Squircle 徽章图标块
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryShortName(category.categoryKey),
                        color = themeColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                // 大分类标题
                Column {
                    Text(
                        text = category.levelName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = category.levelEnglish.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 分类定义精简描述
            Text(
                text = category.definition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                lineHeight = 16.sp
            )
            
            // 下方记忆口诀 Badge 药丸 & 词汇总数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 扁平进度条指示词条条数
                Surface(
                    color = themeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, themeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${category.items.size} 条表达",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }
                
                // 圆形进入箭头 (NEMO_UI_SPEC: 全局级圆角 Circle)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 第二层：词法细节浏览器
 */
@Composable
fun ExpressionBrowser(
    category: ExpressionCategory,
    uiState: ExpressionUiState,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onLevelSelect: (String) -> Unit
) {
    val themeColor = getCategoryColor(category.categoryKey)

    NemoScaffold(
        title = category.levelName,
        onBack = onBack,
        backgroundColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 记忆口诀横幅 (Banner Card) — 高质感扁平
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 口诀: ${category.formula}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 极具扁平美感的搜索框与JLPT过滤器
            FlatFilterBar(
                searchQuery = uiState.searchQuery,
            selectedLevel = uiState.selectedLevel,
            onSearchChange = onSearchChange,
            onLevelSelect = onLevelSelect
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 列表区域 (NEMO_UI_SPEC: items 垂直间距 20.dp)
        if (uiState.filteredExpressions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "没有符合过滤条件的表达词条",
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
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = uiState.filteredExpressions,
                    key = { it.id } // 绑定永久唯一字符 ID，极其流畅！
                ) { expression ->
                    ExpressionFlatCard(
                        expression = expression,
                        themeColor = themeColor
                    )
                }
            }
        }
    }
}
}

/**
 * 扁平化搜索与过滤器
 */
@Composable
fun FlatFilterBar(
    searchQuery: String,
    selectedLevel: String,
    onSearchChange: (String) -> Unit,
    onLevelSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 扁平极简搜索框 (0.dp 阴影，12.dp 圆角)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索表达、假名、释义...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // JLPT 药丸过滤器 (水平滑动)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val levels = listOf("All", "N5", "N4", "N3", "N2", "N1")
            levels.forEach { level ->
                val isSelected = selectedLevel == level
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLevelSelect(level) },
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = level,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 单个词法条目扁平卡片 (Flat Card: 24.dp圆角, 0.dp阴影, 1.dp细描边)
 */
@Composable
fun ExpressionFlatCard(
    expression: Expression,
    themeColor: Color
) {
    var isClozeRevealed by remember { mutableStateOf(false) }
    var isExamplesExpanded by remember { mutableStateOf(false) }
    
    val rotationState by animateFloatAsState(
        targetValue = if (isExamplesExpanded) 180f else 0f,
        label = "箭头旋转"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(), // 点击例句或隐显填空时平滑形变动效
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // 头部：等级药丸与自增字符串 ID
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
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

            // 核心日语原文 (大字重，字重 Black)
            Text(
                text = expression.japanese,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = themeColor
            )
            
            // 假名读音 (振假名方式提示)
            Text(
                text = "假名: ${expression.furigana}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 纯粹中文字义
            Text(
                text = expression.chinese,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 独立淡灰色提示字眼 (解耦后的 tip 字段)
            if (expression.tip.isNotBlank()) {
                Text(
                    text = "💡 提示: ${expression.tip}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 同义表达药丸组
            if (expression.synonyms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    expression.synonyms.forEach { syn ->
                        Surface(
                            color = themeColor.copy(alpha = 0.06f),
                            border = BorderStroke(0.5.dp, themeColor.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(6.dp)
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

            // ⚡ 挖空交互式卡片测试
            FlatClozeBox(
                clozeShow = expression.clozeShow,
                clozeAnswer = expression.clozeAnswers.firstOrNull() ?: "",
                isRevealed = isClozeRevealed,
                onToggleReveal = { isClozeRevealed = !isClozeRevealed },
                themeColor = themeColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 例句展开收起折叠面板 (NEMO_UI_SPEC: 项间距 20.dp，例句干净无 id)
            Column(modifier = Modifier.fillMaxWidth()) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExamplesExpanded = !isExamplesExpanded }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "查看双语例句 (${expression.examples.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "展收箭头",
                        modifier = Modifier.rotate(rotationState),
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

/**
 * 挖空填空测试卡片组件 (Flat 0.dp 阴影)
 */
@Composable
fun FlatClozeBox(
    clozeShow: String,
    clozeAnswer: String,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    themeColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggleReveal() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 切割 clozeShow 以将 {{input}} 渲染成交互式按钮
            val parts = clozeShow.split("{{input}}")
            
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
                
                // 交互填空胶囊药丸 (0.dp阴影, 扁平高对比度)
                Surface(
                    color = if (isRevealed) themeColor.copy(alpha = 0.15f) else themeColor,
                    shape = RoundedCornerShape(8.dp),
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

            // 右侧“小眼睛”扁平状态交互指示器
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRevealed) "👁" else "🙈",
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * 精美双语例句组件 (降维对齐，去除了多余的数字 id)
 */
@Composable
fun ExampleItem(
    index: Int,
    example: ExpressionExample,
    themeColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
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
                // 日语例句 (含假名标注文本，主色高亮)
                Text(
                    text = example.ja,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 中文例句 (淡文字色)
                Text(
                    text = example.zh,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
