package com.jian.nemo.core.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.jian.nemo.core.domain.model.User
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

// 定义颜色常量
private val LearningCardBackgroundDark = Color(0xFF2c2c2c)

/**
 * Nemo应用底部导航栏 (悬浮纯色胶囊版)
 *
 * 包含4个主要Tab：学习、进度、测试、个人
 * 采用悬浮胶囊布局设计，背景纯色，深浅自适应。
 * 选中项的背景改为弹簧横向滑动的胶囊指示器，体验极其高级。
 */
@Composable
fun NemoBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    hazeState: HazeState? = null,
    user: User? = null
) {
    // 根据主题判断深色/浅色模式
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5
    
    // 胶囊容器背景色设置 (纯色实心设计)
    // 浅色模式下：纯白色带半透明，深色模式下：深灰带半透明
    val containerColor = if (isDarkTheme) 
        LearningCardBackgroundDark.copy(alpha = 0.45f)
    else 
        Color.White.copy(alpha = 0.65f)
        
    // 边框描边颜色：为胶囊提供细微的轮廓感
    val borderColor = if (isDarkTheme)
        Color.White.copy(alpha = 0.12f)
    else
        Color.White.copy(alpha = 0.45f)

    // 用于跟踪当前选中项的位置和宽度
    var selectedOffset by remember { mutableStateOf(0.dp) }
    var selectedWidth by remember { mutableStateOf(0.dp) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = slideOutVertically { it } + fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.12f),
                        ambientColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(CircleShape)
                    .then(
                        if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = CircleShape) else Modifier
                    )
                    // 拦截在此胶囊体上尚未被子条目消费的底层点击事件，防止穿透
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    // 纯色背景填充
                    .background(containerColor)
                    // 悬浮胶囊的外侧微描边
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp) // 内部留白宽度
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                contentAlignment = Alignment.CenterStart
            ) {
                // 1. 横向滑动背景滑块 (Sliding Background Capsule)
                if (selectedWidth > 0.dp) {
                    val animOffset by animateDpAsState(
                        targetValue = selectedOffset,
                        animationSpec = spring(
                            dampingRatio = 0.65f, // 精美的弹簧回弹阻尼
                            stiffness = 150f      // 高刚度，快速灵动跟随
                        ),
                        label = "navIndicatorOffset"
                    )
                    val animWidth by animateDpAsState(
                        targetValue = selectedWidth,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = 150f
                        ),
                        label = "navIndicatorWidth"
                    )
                    
                    Box(
                        modifier = Modifier
                            .offset(x = animOffset)
                            .width(animWidth)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                // 2. Tab选项Row列表
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem.entries.forEach { item ->
                        val isSelected = currentRoute == item.route
                        CapsuleNavItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = { onNavigate(item.route) },
                            isDarkTheme = isDarkTheme,
                            user = user,
                            onPositioned = { offset, width ->
                                if (isSelected) {
                                    selectedOffset = offset
                                    selectedWidth = width
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个胶囊导航项，支持平滑缩放与选中背景横向伸缩切换
 */
@Composable
private fun CapsuleNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    user: User? = null,
    onPositioned: (Dp, Dp) -> Unit
) {
    // 交互无默认波纹效果，模拟 HTML 手感
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    val density = LocalDensity.current
    
    // 动态适配深浅模式对应选项卡的激活颜色与图标文字配色
    val activeContentColor = Color.White // 选中内容统一使用白色
    val inactiveContentColor = if (isDarkTheme) Color(0xFFA1A1AA) else Color(0xFF9CA3AF)

    // 文字/图标渐变动画
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeContentColor else inactiveContentColor,
        animationSpec = tween(300, easing = FastOutSlowInEasing), 
        label = "content_color"
    )

    Box(
        modifier = Modifier
            // 实时感知自己在父容器中的相对位置，并将选中状态下的坐标传递给外层
            .onGloballyPositioned { coordinates ->
                if (isSelected) {
                    val x = coordinates.positionInParent().x
                    val w = coordinates.size.width
                    with(density) {
                        onPositioned(x.toDp(), w.toDp())
                    }
                }
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onClick()
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (item == BottomNavItem.SETTINGS && user != null) {
                // 已登录且是“个人”Tab，显示头像
                AvatarImage(
                    username = user.username,
                    avatarPath = user.avatarUrl,
                    size = 24.dp,
                    borderWidth = if (isSelected) 1.dp else 0.dp,
                    borderColor = Color.White.copy(alpha = 0.5f)
                )
            } else {
                // 默认显示 Icon
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 选中文本内容的横向展开动画
            AnimatedVisibility(
                visible = isSelected,
                enter = expandHorizontally(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                ),
                exit = shrinkHorizontally(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                )
            ) {
                Row {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.title,
                        color = contentColor,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 底部导航栏 Tab 项
 */
enum class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    LEARNING(
        route = "learning",
        title = "学习",
        icon = Icons.AutoMirrored.Rounded.MenuBook
    ),
    PROGRESS(
        route = "progress",
        title = "进度",
        icon = Icons.Rounded.BarChart
    ),
    TEST(
        route = "test",
        title = "测试",
        icon = Icons.Rounded.Interests
    ),
    SETTINGS(
        route = "settings",
        title = "个人",
        icon = Icons.Rounded.AccountCircle
    )
}
