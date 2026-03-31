package com.jian.nemo.feature.test.presentation.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.common.NemoGooeyToggle

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant, // Standard Grey Title
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp, top = 8.dp) // Aligned padding
    )
}

@Composable
fun PremiumGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    val shadowElevation = if (isDark) 2.dp else 10.dp
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.03f)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            content = content
        )
    }
}

@Composable
fun PremiumSettingRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Elegant Grey
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PremiumSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp), // Increased padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium, // Upgraded from bodyLarge
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        NemoGooeyToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}
