package com.jian.nemo.core.ui.component.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import kotlinx.coroutines.launch

/**
 * 智能分段回顶悬浮按钮 (Liquid 风格)
 *
 * 智能判定当前滚动位置并逐级向上回退：
 * 1. 处于语法区域（下方）时：点击先平滑滚动到「语法板块」顶部 (index = intermediateTargetIndex, 如 3)
 * 2. 处于语法顶部或单词区域时：点击平滑滚动到「单词板块」顶部 (index = wordTargetIndex, 如 1)
 * 3. 处于单词顶部或已接近最顶时：点击平滑滚动到「页面最顶部」 (index = topTargetIndex, 如 0)
 * 4. 处于最顶部时：按钮自动淡出隐藏
 *
 * 支持快速连点/双击，每次点击均顺畅向上跳跃一级，彻底避免在当前索引原地踏步。
 *
 * @param listState 绑定的列表滚动状态
 * @param intermediateTargetIndex 中间目标索引（语法板块起始索引，默认 3）
 * @param wordTargetIndex 单词板块索引（默认 1）
 * @param topTargetIndex 页面最顶部索引（默认 0）
 * @param modifier 修饰符
 */
@Composable
fun TwoStageScrollToTopButton(
    listState: LazyListState,
    intermediateTargetIndex: Int = 3,
    wordTargetIndex: Int = 1,
    topTargetIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > topTargetIndex || listState.firstVisibleItemScrollOffset > 50
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        LiquidButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    val currentIndex = listState.firstVisibleItemIndex
                    val currentOffset = listState.firstVisibleItemScrollOffset

                    // 判断当前是否在语法区域下方（大于语法索引，或在语法项但有偏移量）
                    val isBelowGrammar = currentIndex > intermediateTargetIndex ||
                            (currentIndex == intermediateTargetIndex && currentOffset > 30)

                    // 判断当前是否在单词区域下方（大于单词索引，或在单词项但有偏移量）
                    val isBelowWord = currentIndex > wordTargetIndex ||
                            (currentIndex == wordTargetIndex && currentOffset > 30)

                    when {
                        isBelowGrammar -> {
                            // 第一段：滚动到语法板块
                            listState.animateScrollToItem(intermediateTargetIndex)
                        }
                        isBelowWord -> {
                            // 第二段：滚动到单词板块
                            listState.animateScrollToItem(wordTargetIndex)
                        }
                        else -> {
                            // 第三段：滚动到最顶部
                            listState.animateScrollToItem(topTargetIndex)
                        }
                    }
                }
            },
            backgroundColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            elevation = 8.dp,
            useSoftShadow = true,
            modifier = Modifier.size(54.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "返回顶部",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * 常规单段式回顶悬浮按钮 (Liquid 风格)
 *
 * @param listState 绑定的列表滚动状态
 * @param targetIndex 目标索引，默认为 0
 * @param modifier 修饰符
 */
@Composable
fun ScrollToTopButton(
    listState: LazyListState,
    targetIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > targetIndex || listState.firstVisibleItemScrollOffset > 50
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        LiquidButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    listState.animateScrollToItem(targetIndex)
                }
            },
            backgroundColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            elevation = 8.dp,
            useSoftShadow = true,
            modifier = Modifier.size(54.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "返回顶部",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
