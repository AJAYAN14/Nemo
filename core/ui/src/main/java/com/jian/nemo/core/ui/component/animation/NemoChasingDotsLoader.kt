package com.jian.nemo.core.ui.component.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * NemoChasingDotsLoader
 *
 * 追逐点加载动画 (形变动画)
 * 使用谷歌官方 Material 3 Expressive 提供的 LoadingIndicator。
 *
 * @param modifier 修饰符
 * @param size 容器大小，默认 45.dp
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NemoChasingDotsLoader(
    modifier: Modifier = Modifier,
    size: Dp = 45.dp,
    duration: Int = 2000 // 保留参数兼容性，但 LoadingIndicator 内部自带动画节奏
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
