package com.jian.nemo.feature.test.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import com.jian.nemo.core.designsystem.theme.NemoDanger
import com.jian.nemo.core.designsystem.theme.NemoDangerDark
import com.jian.nemo.core.designsystem.theme.TestResultPalette
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuButton
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuDivider
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuItem
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuOverlay
import com.jian.nemo.core.ui.component.common.NemoNativeLiquidMenuTrigger
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.menu.records.GlassMenuItemRecord
import com.kyant.backdrop.Backdrop
import androidx.compose.ui.graphics.lerp

/**
 * 测试题目菜单状态数据
 */
data class TestMenuData(
    val isFavorite: Boolean,
    val itemId: Int,
    val onToggleFavorite: (Int, Boolean) -> Unit,
    val onPause: () -> Unit
)

val LocalTestMenuData = androidx.compose.runtime.compositionLocalOf<androidx.compose.runtime.MutableState<TestMenuData?>?> { null }

/**
 * 全局共享的活跃原生测试顶栏菜单项清单
 */
internal val ActiveNativeTestMenuItems = mutableStateOf<List<GlassMenuItemRecord>>(emptyList())

/**
 * 全局共享的活跃原生测试顶栏菜单选项动作分发回调
 */
internal val ActiveNativeTestMenuActionHandler = mutableStateOf<((String) -> Unit)?>(null)

/**
 * 全局共享的原生测试顶栏菜单展开状态
 */
internal val ActiveNativeTestMenuOpenState = mutableStateOf(false)

/**
 * 测试界面液态悬浮菜单浮层
 */
@Composable
fun TestMenuOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    isFavorite: Boolean,
    itemId: Int,
    onToggleFavorite: (Int, Boolean) -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    blurRadius: Dp = 12.dp,
    refractionHeightRatio: Float = 0.10f,
    refractionAmountRatio: Float = 0.20f
) {
    val favoriteTitle = if (isFavorite) {
        stringResource(R.string.test_header_menu_unfavorite)
    } else {
        stringResource(R.string.test_header_menu_favorite)
    }
    val pauseTitle = stringResource(R.string.test_header_menu_pause)

    val nativeItems = remember(itemId, isFavorite, favoriteTitle, pauseTitle) {
        val list = mutableListOf<GlassMenuItemRecord>()
        if (itemId != 0) {
            list.add(
                GlassMenuItemRecord(
                    id = "favorite",
                    title = favoriteTitle,
                    icon = "heart",
                    checked = isFavorite
                )
            )
            list.add(GlassMenuItemRecord(separator = true))
        }
        list.add(
            GlassMenuItemRecord(
                id = "pause",
                title = pauseTitle,
                icon = "pause"
            )
        )
        list
    }

    SideEffect {
        ActiveNativeTestMenuItems.value = nativeItems
        ActiveNativeTestMenuActionHandler.value = { id ->
            onDismiss()
            when (id) {
                "favorite" -> onToggleFavorite(itemId, !isFavorite)
                "pause" -> onPause()
            }
        }
    }
}

/**
 * 测试头部中间内容：倒计时显示（液态按钮容器）
 *
 * 供 CommonHeader 的 centerContent 插槽使用。
 * 当时间比例进入最后 30% 时，背景色与文字颜色在同红色系中平滑渐变。
 */
@Composable
fun TestHeaderCenterContent(
    timeLimitSeconds: Int,
    timeRemainingSeconds: Int,
    backdrop: Backdrop? = null
) {
    if (timeLimitSeconds <= 0) return

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val defaultBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.35f)
    val defaultTextColor = MaterialTheme.colorScheme.onBackground
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.6f)

    // 计算剩余时间比例 (0.0f ~ 1.0f)
    val remainingRatio = (timeRemainingSeconds.toFloat() / timeLimitSeconds.toFloat()).coerceIn(0f, 1f)

    // 触发阈值：最后 30% (即 remainingRatio <= 0.3f 时启动渐变)
    val threshold = 0.3f
    val alertFraction = if (remainingRatio < threshold) {
        ((threshold - remainingRatio) / threshold).coerceIn(0f, 1f)
    } else {
        0f
    }

    // 同色系目标颜色 (Red Tonal Scale：浅红半透背景 + 深红文字，兼顾警报与宝石通透质感)
    val targetBg = if (isDarkTheme) TestResultPalette.WrongCardBgDark.copy(alpha = 0.45f) else TestResultPalette.WrongCardBgLight.copy(alpha = 0.65f)
    val targetTextColor = if (isDarkTheme) NemoDangerDark else NemoDanger

    // 动态平滑插值
    val animatedBgColor = lerp(defaultBg, targetBg, alertFraction)
    val animatedTextColor = lerp(defaultTextColor, targetTextColor, alertFraction)

    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60

    FrostedLiquidGlassButton(
        onClick = { /* 倒计时按钮不触发操作 */ },
        backdrop = backdrop,
        surfaceColor = animatedBgColor,
        shape = CircleShape,
        border = BorderStroke(0.5.dp, borderColor),
        isInteractive = true,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            color = animatedTextColor,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * 测试头部右侧操作区：液态悬浮菜单触发按钮
 */
@Composable
fun TestHeaderActions(
    word: Word?,
    grammar: Grammar? = null,
    onToggleFavorite: (Int, Boolean) -> Unit,
    onPause: () -> Unit,
    backdrop: Backdrop? = null,
    buttonSize: Dp = 48.dp,
    content: (@Composable () -> Unit)? = null
) {
    val isFavorite = word?.isFavorite == true || grammar?.isFavorite == true
    val itemId = word?.id ?: grammar?.id ?: 0

    val menuExpandedState = LocalTestMenuExpanded.current
    val menuDataState = LocalTestMenuData.current

    // 同步当前题目的菜单状态数据至宿主页面
    LaunchedEffect(isFavorite, itemId, onToggleFavorite, onPause) {
        menuDataState?.value = TestMenuData(
            isFavorite = isFavorite,
            itemId = itemId,
            onToggleFavorite = onToggleFavorite,
            onPause = onPause
        )
    }

    var internalExpanded by remember { mutableStateOf(false) }
    val expanded = menuExpandedState?.value ?: internalExpanded
    val setExpanded: (Boolean) -> Unit = { expandedValue ->
        if (menuExpandedState != null) {
            menuExpandedState.value = expandedValue
        } else {
            internalExpanded = expandedValue
        }
    }

    val items = ActiveNativeTestMenuItems.value
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    if (items.isNotEmpty()) {
        NemoNativeLiquidMenuTrigger(
            items = items,
            buttonSize = buttonSize,
            colorScheme = if (isDarkTheme) "dark" else "light",
            menuWidth = 260.dp,
            onSelect = { item, _ ->
                ActiveNativeTestMenuActionHandler.value?.invoke(item.id)
            },
            onOpenChange = { open ->
                ActiveNativeTestMenuOpenState.value = open
                if (open != expanded) {
                    setExpanded(open)
                }
            },
            content = content
        )
    } else {
        NemoLiquidMenuButton(
            isExpanded = expanded,
            onClick = { setExpanded(!expanded) },
            backdrop = null,
            buttonSize = buttonSize,
            content = content
        )
    }
}
