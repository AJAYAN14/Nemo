package com.jian.nemo.core.ui.component.dialog

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import com.jian.nemo.core.designsystem.component.PremiumStepper

/**
 * 今日加餐模式
 */
enum class BonusMode {
    ReviewBacklog, // 消化积压 (推荐)
    NewItems       // 学习新词
}

/**
 * 今日加餐配置弹窗 (BonusStudyBottomSheet)
 * 放置在 core:ui 组件库中，供统计仪表盘、结算页与全局设置共享调用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusStudyBottomSheet(
    onDismissRequest: () -> Unit,
    onConfirmBonus: (Int, BonusMode) -> Unit,
    modifier: Modifier = Modifier,
    initialQuantity: Int = 10,
    initialMode: BonusMode = BonusMode.ReviewBacklog,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedQuantity by remember { mutableIntStateOf(initialQuantity) }
    var selectedMode by remember { mutableStateOf(initialMode) }
    val presetOptions = listOf(10, 20, 30)

    com.jian.nemo.core.ui.component.sheet.NemoModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题与副标题
            Text(
                text = "今日加餐",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "不修改常规每日目标，仅针对今天追加自选微会话",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 模式选择 Choice Chips 区域
            Text(
                text = "加餐目标模式",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val modeList = listOf(
                    BonusMode.ReviewBacklog to "消化积压 (推荐)",
                    BonusMode.NewItems to "学习新词"
                )
                modeList.forEach { (mode, title) ->
                    val isSelected = selectedMode == mode
                    val chipBgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        label = "ModeChipBgColor"
                    )
                    val chipTextColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "ModeChipTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(chipBgColor)
                            .border(
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedMode = mode
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = chipTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 快捷预设 Choice Chips 选项组
            Text(
                text = "快捷预设数量",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                presetOptions.forEach { preset ->
                    val isSelected = selectedQuantity == preset
                    val chipBgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        label = "ChipBgColor"
                    )
                    val chipTextColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "ChipTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(chipBgColor)
                            .border(
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedQuantity = preset
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$preset",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = chipTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 自定义微调控件区域
            Text(
                text = "自定义数量微调",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            // 重写版动画 Stepper 控件
            PremiumStepper(
                value = selectedQuantity,
                onValueChange = { selectedQuantity = it },
                min = 1,
                max = 200,
                step = 1
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 开始加餐提交按钮
            val confirmButtonText = when (selectedMode) {
                BonusMode.ReviewBacklog -> "开始消化积压 (+$selectedQuantity)"
                BonusMode.NewItems -> "开始学习新词 (+$selectedQuantity)"
            }

            Button(
                onClick = {
                    onConfirmBonus(selectedQuantity, selectedMode)
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = confirmButtonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 兼容单参数回调的重载函数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusStudyBottomSheet(
    onDismissRequest: () -> Unit,
    onConfirmBonus: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialQuantity: Int = 10,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    BonusStudyBottomSheet(
        onDismissRequest = onDismissRequest,
        onConfirmBonus = { count, _ -> onConfirmBonus(count) },
        modifier = modifier,
        initialQuantity = initialQuantity,
        initialMode = BonusMode.ReviewBacklog,
        sheetState = sheetState
    )
}
