package com.jian.nemo.core.ui.component.avatar

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.UiPalette
import com.jian.nemo.core.ui.R
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.modifier.softShadow
import com.jian.nemo.core.ui.util.PresetAvatars

/**
 * 头像编辑对话框
 *
 * 采用纯受控状态设计，仅负责分发相册、拍照、预设与移除意图，所有 I/O 与持久化由上层状态机完成。
 */
@Composable
fun AvatarEditDialog(
    currentAvatarPath: String?,
    username: String,
    isProcessing: Boolean,
    pendingCameraLaunchUri: String?,
    onDismiss: () -> Unit,
    onGalleryImageSelected: (String) -> Unit,
    onGalleryPickerUnavailable: () -> Unit,
    onCameraCaptureClick: () -> Unit,
    onCameraLaunchConsumed: () -> Unit,
    onCameraLaunchFailed: () -> Unit,
    onCameraCaptureResult: (Boolean) -> Unit,
    onPresetSelected: (String) -> Unit,
    onRemoveAvatar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPresetSelector by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (!isProcessing && uri != null) {
            onGalleryImageSelected(uri.toString())
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        onCameraCaptureResult(success)
    }

    LaunchedEffect(pendingCameraLaunchUri) {
        val launchUri = pendingCameraLaunchUri ?: return@LaunchedEffect
        onCameraLaunchConsumed()
        try {
            cameraLauncher.launch(Uri.parse(launchUri))
        } catch (_: ActivityNotFoundException) {
            onCameraLaunchFailed()
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val avatarInnerBorder = if (isDark) UiPalette.AvatarInnerBorderDark else Color.White
    val avatarOuterBorder = if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    val blueColor = if (isDark) UiPalette.AvatarSystemBlueDark else UiPalette.AvatarSystemBlueLight
    val greenColor = if (isDark) UiPalette.AvatarSystemGreenDark else UiPalette.AvatarSystemGreenLight
    val purpleColor = if (isDark) UiPalette.AvatarSystemPurpleDark else UiPalette.AvatarSystemPurpleLight
    val redColor = if (isDark) UiPalette.AvatarSystemRedDark else UiPalette.AvatarSystemRedLight

    NemoDialog(
        onDismissRequest = {
            if (!isProcessing) {
                onDismiss()
            }
        },
        title = stringResource(R.string.core_ui_avatar_edit_dialog_title),
        dismissText = stringResource(R.string.core_ui_dialog_btn_cancel),
        confirmText = null,
        onDismiss = {
            if (!isProcessing) {
                onDismiss()
            }
        },
        isLoading = isProcessing,
        content = {
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(124.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, avatarOuterBorder),
                        modifier = Modifier.fillMaxSize()
                    ) {}

                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(2.dp, avatarInnerBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(112.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AvatarImage(
                                username = username,
                                avatarPath = currentAvatarPath,
                                size = 112.dp
                            )
                            if (isProcessing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    NemoChasingDotsLoader(
                                        size = 24.dp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when {
                        isProcessing -> stringResource(R.string.core_ui_avatar_edit_hint_processing)
                        currentAvatarPath.isNullOrEmpty() -> stringResource(R.string.core_ui_avatar_edit_hint_empty)
                        else -> stringResource(R.string.core_ui_avatar_edit_hint_current)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                val itemModifier = Modifier.weight(1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AvatarActionCard(
                        icon = Icons.Rounded.PhotoLibrary,
                        label = stringResource(R.string.core_ui_avatar_edit_action_gallery),
                        containerColor = blueColor,
                        enabled = !isProcessing,
                        modifier = itemModifier,
                        onClick = {
                            if (!isProcessing) {
                                try {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } catch (_: ActivityNotFoundException) {
                                    onGalleryPickerUnavailable()
                                }
                            }
                        }
                    )
                    AvatarActionCard(
                        icon = Icons.Rounded.CameraAlt,
                        label = stringResource(R.string.core_ui_avatar_edit_action_camera),
                        containerColor = greenColor,
                        enabled = !isProcessing,
                        modifier = itemModifier,
                        onClick = {
                            if (!isProcessing) {
                                onCameraCaptureClick()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AvatarActionCard(
                        icon = Icons.Rounded.Palette,
                        label = stringResource(R.string.core_ui_avatar_edit_action_preset),
                        containerColor = purpleColor,
                        enabled = !isProcessing,
                        modifier = itemModifier,
                        onClick = {
                            if (!isProcessing) {
                                showPresetSelector = true
                            }
                        }
                    )

                    if (!currentAvatarPath.isNullOrEmpty()) {
                        AvatarActionCard(
                            icon = Icons.Rounded.Delete,
                            label = stringResource(R.string.core_ui_avatar_edit_action_remove),
                            containerColor = redColor,
                            enabled = !isProcessing,
                            modifier = itemModifier,
                            onClick = {
                                if (!isProcessing) {
                                    onRemoveAvatar()
                                }
                            }
                        )
                    } else {
                        Box(modifier = itemModifier)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    )

    if (showPresetSelector && !isProcessing) {
        PresetAvatarSelectorDialog(
            onDismiss = { showPresetSelector = false },
            onPresetSelected = { preset ->
                showPresetSelector = false
                val presetProtocol = PresetAvatars.createPresetPath(preset.id)
                onPresetSelected(presetProtocol)
            }
        )
    }
}

@Composable
private fun AvatarActionCard(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val resolvedContainerColor = if (enabled) containerColor else containerColor.copy(alpha = 0.45f)
    val resolvedContentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.7f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .softShadow(
                color = containerColor.copy(alpha = if (enabled) 0.28f else 0.12f),
                borderRadius = 16.dp,
                blurRadius = 14.dp,
                offsetY = 6.dp
            )
            .height(72.dp),
        shape = RoundedRectangle(16.dp),
        color = resolvedContainerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
        contentColor = resolvedContentColor,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = resolvedContentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = resolvedContentColor
            )
        }
    }
}
