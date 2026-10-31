package com.jian.nemo.feature.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.user.R

/**
 * 个人中心界面
 *
 * 提供设置、收藏、错题等功能入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToMistakes: () -> Unit = {},
    onNavigateToPartOfSpeech: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_profile_title_screen)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 用户信息卡片
            UserInfoCard()

            Spacer(modifier = Modifier.height(24.dp))

            // 功能列表
            Text(
                text = stringResource(R.string.user_profile_title_learning_tools),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .softCardShadow(borderRadius = 16.dp, isDark = isDark)
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Default.Favorite,
                        title = stringResource(R.string.user_profile_title_favorites),
                        subtitle = stringResource(R.string.user_profile_label_favorites_desc),
                        onClick = onNavigateToFavorites
                    )

                    HorizontalDivider()

                    ProfileMenuItem(
                        icon = Icons.Default.Error,
                        title = stringResource(R.string.user_profile_title_mistakes),
                        subtitle = stringResource(R.string.user_profile_label_mistakes_desc),
                        onClick = onNavigateToMistakes
                    )

                    HorizontalDivider()

                    ProfileMenuItem(
                        icon = Icons.Default.Category,
                        title = stringResource(R.string.user_profile_title_pos),
                        subtitle = stringResource(R.string.user_profile_label_pos_desc),
                        onClick = onNavigateToPartOfSpeech
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.user_profile_title_settings_section),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .softCardShadow(borderRadius = 16.dp, isDark = isDark)
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.user_profile_title_app_settings),
                        subtitle = stringResource(R.string.user_profile_label_app_settings_desc),
                        onClick = onNavigateToSettings
                    )

                    HorizontalDivider()

                    ProfileMenuItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.user_profile_title_about),
                        subtitle = "版本 1.0.0",
                        onClick = { /* TODO */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像占位符
            Surface(
                modifier = Modifier.size(64.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(R.string.user_profile_label_default_user_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.user_profile_label_default_cheer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
