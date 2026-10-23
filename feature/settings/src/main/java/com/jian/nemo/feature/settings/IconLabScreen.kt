package com.jian.nemo.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.ui.component.common.CommonHeader

/**
 * 图标设计方案模型（支持无限拓展，4列网格自动排布与换行）
 */
data class IconLabScheme(
    val id: String,
    val name: String,
    val subtitle: String,
    val shapeTag: String,
    val renderIcon: @Composable (color: Color, icon: ImageVector) -> Unit
)

/**
 * 图标实验室屏幕 (Icon Lab Screen)
 *
 * 采用 4 列方格（Grid）布局，展示不同设计质感的图标方案；
 * 方格内的图标严格保持 42.dp 列表规格，不铺满方格；
 * 底部提供真实列表项（SquircleListItem）渲染预览与调色盘切换。
 */
@Composable
fun IconLabScreen(
    onNavigateBack: () -> Unit
) {
    // 调色盘选项
    val palette = remember {
        listOf(
            "经典蓝" to NemoPrimary,
            "活力橙" to Color(0xFFFF9500),
            "薄荷绿" to Color(0xFF34C759),
            "丁香紫" to Color(0xFF5856D6),
            "珊瑚粉" to Color(0xFFFF2D55)
        )
    }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    val activeColor = palette[selectedColorIndex].second

    // 测试图标切换
    val testIcons = remember {
        listOf(
            Icons.Rounded.DateRange,
            Icons.AutoMirrored.Rounded.TrendingUp,
            Icons.Rounded.LocalFireDepartment,
            Icons.Rounded.Psychology,
            Icons.Rounded.School
        )
    }
    var selectedIconIndex by remember { mutableIntStateOf(0) }
    val activeIcon = testIcons[selectedIconIndex]

    // 方案定义列表（当前 4 个，未来扩展只需在列表中添加元素，4 列网格将自动换行排布）
    val schemes = remember {
        listOf(
            IconLabScheme(
                id = "ios_gradient",
                name = "iOS 微渐变",
                subtitle = "135°饱满双色渐变，纯白对比清晰",
                shapeTag = "Squircle 12dp",
                renderIcon = { color, icon ->
                    IosGradientIcon(color = color, icon = icon)
                }
            ),
            IconLabScheme(
                id = "frost_glass",
                name = "透光微玻璃",
                subtitle = "半透明底 + 顶部1dp渐变高光描边",
                shapeTag = "超圆角 14dp",
                renderIcon = { color, icon ->
                    FrostGlassIcon(color = color, icon = icon)
                }
            ),
            IconLabScheme(
                id = "bento_dualtone",
                name = "Bento 双阶",
                subtitle = "柔和低透底 + 局部光斑微层次",
                shapeTag = "Squircle 12dp",
                renderIcon = { color, icon ->
                    BentoDualToneIcon(color = color, icon = icon)
                }
            ),
            IconLabScheme(
                id = "radial_tactile",
                name = "柔光微立体",
                subtitle = "中心径向扩散光晕 + 浮雕微高光",
                shapeTag = "圆形 Circle",
                renderIcon = { color, icon ->
                    RadialTactileIcon(color = color, icon = icon)
                }
            )
        )
    }

    var selectedSchemeIndex by remember { mutableIntStateOf(0) }
    val currentScheme = schemes[selectedSchemeIndex]

    Scaffold(
        topBar = {
            CommonHeader(
                title = "图标设计实验室",
                onBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==========================================
            // 1. 顶部控制条：主题色选择 & 测试图标切换
            // ==========================================
            item(span = { GridItemSpan(4) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "4 列方案对比（第一行）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "点击方格切换下方真实列表预览，图标尺寸严格对标列表 42.dp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // 颜色选择行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "基色:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        palette.forEachIndexed { index, (name, color) ->
                            val isSelected = selectedColorIndex == index
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorIndex = index }
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 图标样式切换
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "图标:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        testIcons.forEachIndexed { index, icon ->
                            val isSelected = selectedIconIndex == index
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { selectedIconIndex = index }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. 4 列方案方格 (首行展示 4 款方案)
            // ==========================================
            itemsIndexed(schemes) { index, scheme ->
                val isSelected = selectedSchemeIndex == index
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1f,
                    animationSpec = tween(150),
                    label = "cellScale"
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        activeColor.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { selectedSchemeIndex = index }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 方案编号标签
                        Text(
                            text = "0${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 图标容器：严格 42.dp，不撑满方格
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            scheme.renderIcon(activeColor, activeIcon)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 方案名称
                        Text(
                            text = scheme.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        // 形状标签
                        Text(
                            text = scheme.shapeTag,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 3. 选中方案的详细参数说明卡片
            // ==========================================
            item(span = { GridItemSpan(4) }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            currentScheme.renderIcon(activeColor, activeIcon)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentScheme.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = activeColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = currentScheme.shapeTag,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = activeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentScheme.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 4. 模拟真实列表项环境预览 (1:1 还原真实列表场景)
            // ==========================================
            item(span = { GridItemSpan(4) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "真实列表项环境渲染效果",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "模拟设置页或统计仪表盘中的实际使用场景：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // 列表项 1：当前选中的新方案
                            MockListItem(
                                title = "每日学习日历与活动目标",
                                subtitle = "当前方案：${currentScheme.name} (${currentScheme.shapeTag})",
                                color = activeColor,
                                icon = activeIcon,
                                iconRenderer = currentScheme.renderIcon
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                thickness = 0.5.dp
                            )

                            // 列表项 2：项目原版的对照组 (Old Style)
                            MockListItem(
                                title = "原始方案对照项 (旧版)",
                                subtitle = "15% 透明度底块 + 同色单色图标 (现状)",
                                color = activeColor,
                                icon = activeIcon,
                                iconRenderer = { color, icon ->
                                    LegacyOldIcon(color = color, icon = icon)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 四种图标方案具体实现 (严格 42.dp 背景，22.dp 图标，不撑满)
// ============================================================================

/**
 * 方案 1：iOS 经典微渐变底 + 纯白高对比图标
 * 特点：45° 线性渐变饱满底色，纯白图标，清晰耐看，类似 Apple 设置/健身风格
 */
@Composable
fun IosGradientIcon(
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            color,
            color.copy(alpha = 0.82f)
        ),
        start = Offset(0f, 0f),
        end = Offset(100f, 100f)
    )

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 方案 2：透光微玻璃风 (Frost Glass)
 * 特点：半透明材质底色 + 顶部 1dp 渐变高光描边 + 高亮主题色图标
 */
@Composable
fun FrostGlassIcon(
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            color.copy(alpha = 0.55f),
            color.copy(alpha = 0.15f)
        )
    )

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.14f))
            .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 方案 3：Bento 双色阶 (Dual-Tone Depth)
 * 特点：外层低饱和柔底 + 内部对角线微光斑几何层叠，打破平面呆板
 */
@Composable
fun BentoDualToneIcon(
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.09f))
            .drawBehind {
                // 右上角局部光斑小层
                drawCircle(
                    color = color.copy(alpha = 0.14f),
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.85f, size.height * 0.15f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 方案 4：柔光微立体胶囊 (Radial Glow Tactile)
 * 特点：正圆形，中心径向扩散光晕，顶部微浮雕质感
 */
@Composable
fun RadialTactileIcon(
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val radialBrush = Brush.radialGradient(
        colors = listOf(
            color.copy(alpha = 0.28f),
            color.copy(alpha = 0.08f)
        )
    )

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(radialBrush)
            .border(
                width = 0.8.dp,
                color = color.copy(alpha = 0.25f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 旧版对照组 (用于并在列表中直观对比区别)
 */
@Composable
fun LegacyOldIcon(
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 模拟列表项组件
 */
@Composable
private fun MockListItem(
    title: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    iconRenderer: @Composable (color: Color, icon: ImageVector) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            iconRenderer(color, icon)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(14.dp)
        )
    }
}
