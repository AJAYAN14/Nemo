package com.jian.nemo.feature.test.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.domain.model.ExplanationPayload
import com.jian.nemo.feature.test.R

/**
 * 解析入口：根据 payload 类型分发到单词/语法卡片。
 */
@Composable
fun QuestionExplanationCard(
    payload: ExplanationPayload,
    modifier: Modifier = Modifier
) {
    when (payload) {
        is ExplanationPayload.WordSummary -> WordExplanationCard(payload = payload, modifier = modifier)
        is ExplanationPayload.GrammarText -> GrammarExplanationCard(payload = payload, modifier = modifier)
    }
}

/**
 * 单词解析卡片：只显示汉字、假名、意思。
 */
@Composable
fun WordExplanationCard(
    payload: ExplanationPayload.WordSummary,
    modifier: Modifier = Modifier
) {
    ExplanationCardContainer(modifier = modifier) {
        ExplanationField(label = stringResource(R.string.test_explanation_label_kanji), value = payload.japanese)
        ExplanationField(label = stringResource(R.string.test_explanation_label_kana), value = payload.hiragana)
        ExplanationField(label = stringResource(R.string.test_explanation_label_meaning), value = payload.meaning)
    }
}

/**
 * 语法解析卡片：显示解析文本。
 */
@Composable
fun GrammarExplanationCard(
    payload: ExplanationPayload.GrammarText,
    modifier: Modifier = Modifier
) {
    ExplanationCardContainer(modifier = modifier) {
        Text(
            text = payload.text.trim().ifEmpty { "--" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

@Composable
private fun ExplanationField(
    label: String,
    value: String
) {
    val normalized = value.trim().ifEmpty { "--" }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Text(
        text = normalized,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}
