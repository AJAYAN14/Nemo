package com.jian.nemo.core.ui.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word

// 通用等级头部组件
@Composable
fun LevelHeader(
    level: String,
    count: Int,
    isExpanded: Boolean,
    onClick: () -> Unit,
    containerColor: Color = Color.White, // 修正默认背景色
    itemType: String = "单词"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (itemType) {
                    "单词" -> "JLPT $level"
                    "语法" -> "JLPT $level"
                    else -> "JLPT $level"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$count ${if (itemType == "单词") "词" else "条"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "折叠" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ----------------------------------------------------------------
// 标签组件
// ----------------------------------------------------------------

// JLPT标签组件
@Composable
fun JlptTag(level: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = level,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ----------------------------------------------------------------
// 列表项组件
// ----------------------------------------------------------------

// 通用单词列表项组件
@Composable
fun WordListItem(
    word: Word,
    containerColor: Color,
    onClick: (() -> Unit)? = null
) {
    CommonWordCard(
        japanese = word.japanese,
        hiragana = word.hiragana,
        chinese = word.chinese,
        level = word.level,
        containerColor = containerColor,
        onClick = onClick
    )
}

// 通用语法列表项组件
@Composable
fun GrammarListItem(
    grammar: Grammar,
    containerColor: Color,
    onClick: (() -> Unit)? = null
) {
    CommonGrammarCard(
        grammar = grammar.grammar ?: "", // Nemo Grammar model nullable check
        explanation = grammar.getFirstExplanation() ?: "", // Nemo Grammar model nullable check
        containerColor = containerColor,
        isFavorite = grammar.isFavorite,
        onClick = onClick
    )
}

// 收藏单词列表项组件
@Composable
fun FavoriteWordItem(
    word: Word,
    cardColor: Color,
    onClick: (() -> Unit)? = null
) {
    CommonWordCard(
        japanese = word.japanese,
        hiragana = word.hiragana,
        chinese = word.chinese,
        level = word.level,
        containerColor = cardColor,
        onClick = onClick
    )
}

// 收藏语法列表项组件
@Composable
fun FavoriteGrammarItem(
    grammar: Grammar,
    cardColor: Color,
    onClick: (() -> Unit)? = null
) {
    CommonGrammarCard(
        grammar = grammar.grammar ?: "",
        explanation = grammar.getFirstExplanation() ?: "",
        containerColor = cardColor,
        isFavorite = true,
        onClick = onClick
    )
}

// 错词列表项组件
@Composable
fun WrongWordItem(
    word: Word,
    containerColor: Color,
    onClick: (() -> Unit)? = null
) {
    CommonWordCard(
        japanese = word.japanese,
        hiragana = word.hiragana,
        chinese = word.chinese,
        level = word.level,
        containerColor = containerColor,
        onClick = onClick
    )
}

// 通用单词卡片组件
@Composable
fun CommonWordCard(
    japanese: String,
    hiragana: String,
    chinese: String,
    level: String,
    containerColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp) // 添加底部间距
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = japanese,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = hiragana,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = chinese,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            JlptTag(level = level)
        }
    }
}

// 通用列表项组件（用于简单的标题+副标题结构）
@Composable
fun SimpleListItem(
    title: String,
    subtitle: String,
    containerColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp) // 添加底部间距
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 通用语法卡片组件
@Composable
fun CommonGrammarCard(
    grammar: String,
    explanation: String,
    containerColor: Color,
    isFavorite: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 语法显示，只显示一行，超出部分用省略号
                Text(
                    text = grammar,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 解释说明显示，只显示一行，超出部分用省略号
                Text(
                    text = explanation,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            // 显示收藏图标
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "已收藏",
                    tint = Color.Red,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
