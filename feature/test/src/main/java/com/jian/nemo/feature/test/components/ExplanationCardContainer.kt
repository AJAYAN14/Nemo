package com.jian.nemo.feature.test.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 可复用解析卡片容器。
 */
@Composable
fun ExplanationCardContainer(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.test_explanation_container_title),
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        val scrollState = rememberScrollState()

        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 220.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                content()
            }
        }
    }
}
