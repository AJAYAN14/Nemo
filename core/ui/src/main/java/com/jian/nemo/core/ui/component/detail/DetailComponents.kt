package com.jian.nemo.core.ui.component.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoText
import com.jian.nemo.core.designsystem.theme.NemoTextLight
import androidx.compose.ui.res.stringResource
import com.jian.nemo.core.ui.R
import com.jian.nemo.core.ui.component.text.FuriganaText
import com.jian.nemo.core.designsystem.shape.RoundedRectangle

/**
 * 详情页头部组件（亮色主题）
 */
@Composable
fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    backgroundColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.core_ui_detail_cd_back),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = title,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 50.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 详情页头部组件（暗色主题）
 */
@Composable
fun DetailHeaderDark(
    title: String,
    onBack: () -> Unit,
    backgroundColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.core_ui_detail_cd_back),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = title,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 50.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 信息行组件（单个标签+内容）
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    isDarkTheme: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isDarkTheme) NemoTextLight.copy(alpha = 0.7f) else NemoTextLight
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else NemoText
        )
    }
}

/**
 * 信息行组件（支持多个标签+内容对）
 */
@Composable
fun InfoRow(
    infoItems: List<Pair<String, String>>,
    isDarkTheme: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        infoItems.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(16.dp))
            }
            InfoRow(label = label, value = value, isDarkTheme = isDarkTheme)
        }
    }
}

/**
 * 分段卡片组件
 */
@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        shape = RoundedRectangle(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NemoPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * 例句项组件
 */
@Composable
fun ExampleItem(
    sentence: String?,
    translation: String?,
    isDarkTheme: Boolean = false,
    index: Int? = null
) {
    if (!sentence.isNullOrBlank()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            val sentenceText = if (index != null) "${index + 1}. $sentence" else sentence!!
            FuriganaText(
                text = sentenceText,
                baseTextStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 24.sp
                ),
                baseTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else NemoText,
                furiganaTextSize = 9.sp,
                furiganaTextColor = if (isDarkTheme) NemoTextLight.copy(alpha = 0.7f) else NemoTextLight
            )
            translation?.let { trans ->
                if (trans.isNotBlank()) {
                    Text(
                        text = trans,
                        fontSize = 15.sp,
                        color = if (isDarkTheme) NemoTextLight.copy(alpha = 0.7f) else NemoTextLight,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 带项目符号的列表项组件
 */
@Composable
fun BulletListItem(
    text: String,
    isDarkTheme: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            fontSize = 15.sp,
            color = NemoPrimary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            fontSize = 15.sp,
            color = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else NemoText
        )
    }
}
