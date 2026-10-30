package com.jian.nemo.core.ui.component.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nemo 统一规范的圆底对勾单选/多选指示器 (实心圆底白勾)
 *
 * - 选中状态：主题色（或自定义高亮色）实心圆底 + 白色 Check 对勾
 * - 未选状态：灰色轮廓空心圆圈（无对勾，灰色外边框，内部透明）
 *
 * @param checked 是否选中
 * @param modifier 外部修饰符
 * @param onCheckedChange 选中状态切换回调，为空时作为展示指示器使用，不消费点击事件
 * @param checkedColor 选中时的填充与边框颜色（默认 MaterialTheme 主题色）
 * @param uncheckedBorderColor 未选中时的轮廓边框颜色
 * @param size 控件整体外径（默认 22.dp，阅读理解等紧凑场景可设为 18.dp）
 * @param checkSize 内部白色对勾图标尺寸（默认 14.dp，紧凑场景可设为 12.dp）
 * @param borderWidth 未选中时的边框线宽（默认 1.5.dp）
 */
@Composable
fun NemoRoundCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedBorderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
    size: Dp = 22.dp,
    checkSize: Dp = 14.dp,
    borderWidth: Dp = 1.5.dp
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (checked) checkedColor else Color.Transparent,
        label = "nemoRoundCheckboxBg"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (checked) checkedColor else uncheckedBorderColor,
        label = "nemoRoundCheckboxBorder"
    )

    val interactionModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            role = Role.Checkbox,
            onValueChange = onCheckedChange
        )
    } else {
        Modifier.semantics {
            contentDescription = if (checked) "已选中" else "未选中"
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .then(interactionModifier)
            .background(animatedBgColor, CircleShape)
            .border(
                width = borderWidth,
                color = animatedBorderColor,
                shape = CircleShape
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn() + scaleIn(spring(dampingRatio = 0.65f, stiffness = 400f)),
            exit = fadeOut() + scaleOut()
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(checkSize)
            )
        }
    }
}

@Preview(name = "NemoRoundCheckbox - Light Mode", showBackground = true)
@Composable
private fun NemoRoundCheckboxPreview() {
    MaterialTheme {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NemoRoundCheckbox(checked = true)
                Spacer(modifier = Modifier.width(16.dp))
                NemoRoundCheckbox(checked = false)
            }
        }
    }
}

@Preview(name = "NemoRoundCheckbox - Dark Mode", showBackground = true)
@Composable
private fun NemoRoundCheckboxDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NemoRoundCheckbox(checked = true)
                Spacer(modifier = Modifier.width(16.dp))
                NemoRoundCheckbox(checked = false)
            }
        }
    }
}
