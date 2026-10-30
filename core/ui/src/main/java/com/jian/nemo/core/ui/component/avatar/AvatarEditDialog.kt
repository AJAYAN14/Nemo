package com.jian.nemo.core.ui.component.avatar

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.modifier.softShadow
import com.jian.nemo.core.ui.util.AvatarResult
import com.jian.nemo.core.ui.util.PresetAvatars
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 头像编辑对话框 - 采用项目通用标准 NemoDialog
 * 支持选择图片、拍照、选择预设、删除头像
 */
@Composable
fun AvatarEditDialog(
    currentAvatarPath: String?,
    username: String,
    onDismiss: () -> Unit,
    onAvatarChanged: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPresetSelector by remember { mutableStateOf(false) }
    var tempImageFile by remember { mutableStateOf<File?>(null) }

    // 相册选择启动器（带错误提示）
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val result = saveImageToInternalStorage(context, selectedUri)
            when (result) {
                is AvatarResult.Success -> {
                    onAvatarChanged(result.path)
                    android.widget.Toast.makeText(context, result.message, android.widget.Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
                is AvatarResult.Error -> {
                    android.widget.Toast.makeText(context, result.getUserMessage(), android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 相机拍照启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageFile?.let { file ->
                if (file.exists()) {
                    onAvatarChanged(file.absolutePath)
                    onDismiss()
                }
            }
        }
    }

    // 创建临时文件用于相机拍照
    LaunchedEffect(Unit) {
        tempImageFile = createTempImageFile(context)
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val avatarInnerBorder = if (isDark) Color(0xFF2C2C2E) else Color.White
    val avatarOuterBorder = if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    val blueColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val greenColor = if (isDark) Color(0xFF30D158) else Color(0xFF34C759)
    val purpleColor = if (isDark) Color(0xFFBF5AF2) else Color(0xFFAF52DE)
    val redColor = if (isDark) Color(0xFFFF453A) else Color(0xFFFF3B30)

    NemoDialog(
        onDismissRequest = onDismiss,
        title = "编辑头像",
        dismissText = "取消",
        confirmText = null,
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 头像预览区域
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(124.dp)
                ) {
                    // 扁平外环
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, avatarOuterBorder),
                        modifier = Modifier.fillMaxSize()
                    ) {}

                    // 头像图
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(2.dp, avatarInnerBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(112.dp)
                    ) {
                        AvatarImage(
                            username = username,
                            avatarPath = currentAvatarPath,
                            size = 112.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (currentAvatarPath.isNullOrEmpty()) "尚未设置个性头像" else "当前的视觉识别符号",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 操作按钮网格
                val itemModifier = Modifier.weight(1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AvatarActionCard(
                        icon = Icons.Default.PhotoLibrary,
                        label = "相册",
                        containerColor = blueColor,
                        contentColor = blueColor,
                        modifier = itemModifier,
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                    AvatarActionCard(
                        icon = Icons.Default.CameraAlt,
                        label = "拍照",
                        containerColor = greenColor,
                        contentColor = greenColor,
                        modifier = itemModifier,
                        onClick = {
                            tempImageFile?.let { file ->
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                cameraLauncher.launch(uri)
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
                        icon = Icons.Default.Palette,
                        label = "预设",
                        containerColor = purpleColor,
                        contentColor = purpleColor,
                        modifier = itemModifier,
                        onClick = { showPresetSelector = true }
                    )

                    if (!currentAvatarPath.isNullOrEmpty()) {
                        AvatarActionCard(
                            icon = Icons.Default.DeleteOutline,
                            label = "移除",
                            containerColor = redColor,
                            contentColor = redColor,
                            modifier = itemModifier,
                            onClick = {
                                onAvatarChanged(null)
                                onDismiss()
                            }
                        )
                    } else {
                        // 占位保持布局对称
                        Box(modifier = itemModifier)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    )

    // 预设头像选择对话框
    if (showPresetSelector) {
        PresetAvatarSelectorDialog(
            onDismiss = { showPresetSelector = false },
            onPresetSelected = { preset ->
                val presetProtocol = PresetAvatars.createPresetPath(preset.id)
                onAvatarChanged(presetProtocol)
                onDismiss()
            }
        )
    }
}

@Composable
private fun AvatarActionCard(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .softShadow(
                color = containerColor.copy(alpha = 0.28f),
                borderRadius = 16.dp,
                blurRadius = 14.dp,
                offsetY = 6.dp
            )
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
        contentColor = Color.White,
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
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
        }
    }
}

/**
 * 将选择的图片保存到应用内部存储（带详细错误处理）
 */
private fun saveImageToInternalStorage(context: Context, uri: Uri): AvatarResult {
    return try {
        // 1. 检查存储空间
        val availableSpace = context.filesDir.freeSpace
        val requiredSpace = 5 * 1024 * 1024L // 5MB
        if (availableSpace < requiredSpace) {
            return AvatarResult.Error.StorageFull(
                availableSpace = availableSpace,
                requiredSpace = requiredSpace
            )
        }

        // 2. 打开输入流
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: return AvatarResult.Error.FileNotFound(
                path = uri.toString()
            )

        // 3. 检查文件大小
        val fileSize = inputStream.available().toLong()
        val maxSize = 20 * 1024 * 1024L // 20MB
        if (fileSize > maxSize) {
            inputStream.close()
            return AvatarResult.Error.ImageTooLarge(
                actualSize = fileSize,
                maxSize = maxSize
            )
        }

        // 4. 保存文件
        val fileName = "avatar_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)

        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        // 5. 验证文件已保存
        if (!file.exists()) {
            return AvatarResult.Error.SaveFailed(
                exception = Exception("文件保存后不存在")
            )
        }

        AvatarResult.Success(
            path = file.absolutePath,
            message = "头像已保存"
        )
    } catch (e: SecurityException) {
        AvatarResult.Error.PermissionDenied()
    } catch (e: java.io.FileNotFoundException) {
        AvatarResult.Error.FileNotFound(
            path = uri.toString()
        )
    } catch (e: Exception) {
        e.printStackTrace()
        AvatarResult.Error.Unknown(
            exception = e
        )
    }
}

/**
 * 创建临时图片文件用于相机拍照
 */
private fun createTempImageFile(context: Context): File? {
    return try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "temp_avatar_$timeStamp.jpg"
        File(context.cacheDir, fileName)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
