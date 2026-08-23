package com.jian.nemo.core.ui.component.avatar

import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.util.AvatarResult
import com.jian.nemo.core.ui.util.PresetAvatars
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 头像编辑对话框
 * 支持选择图片、拍照、删除头像
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current

        // 开启 Android 12+ 官方窗口级高斯毛玻璃 (Blur Behind)
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = 48
                    dimAmount = 0.20f
                }
            }
            onDispose {
                if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        window.attributes = window.attributes.apply {
                            blurBehindRadius = 0
                            dimAmount = 0f
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                }
                .background(Color.Black.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .clickable(enabled = false) {}, // 防止点击内容区关闭
                shape = RoundedCornerShape(24.dp), // 略微减小圆角以匹配扁平感
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "编辑头像",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.Black // 纯白背景下使用黑字
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.06f))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 2. Preview Section
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(124.dp)
                    ) {
                        // 扁平外环
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxSize()
                        ) {}
                        
                        // 头像图
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.dp, Color.White),
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

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (currentAvatarPath.isNullOrEmpty()) "尚未设置个性头像" else "当前的视觉识别符号",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // 3. Action Grid
                    val itemModifier = Modifier.weight(1f)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AvatarActionCard(
                            icon = Icons.Default.PhotoLibrary,
                            label = "相册",
                            containerColor = Color(0xFF007AFF), // 经典亮蓝
                            contentColor = Color(0xFF007AFF),
                            modifier = itemModifier,
                            onClick = { galleryLauncher.launch("image/*") }
                        )
                        AvatarActionCard(
                            icon = Icons.Default.CameraAlt,
                            label = "拍照",
                            containerColor = Color(0xFF34C759), // 鲜活绿色
                            contentColor = Color(0xFF34C759),
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
                            containerColor = Color(0xFFAF52DE), // 高级紫色
                            contentColor = Color(0xFFAF52DE),
                            modifier = itemModifier,
                            onClick = { showPresetSelector = true }
                        )
                        
                        if (!currentAvatarPath.isNullOrEmpty()) {
                            AvatarActionCard(
                                icon = Icons.Default.DeleteOutline,
                                label = "移除",
                                containerColor = Color(0xFFFF3B30), // 系统红
                                contentColor = Color(0xFFFF3B30),
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // ✅ 预设头像选择对话框
    if (showPresetSelector) {
        PresetAvatarSelectorDialog(
            onDismiss = { showPresetSelector = false },
            onPresetSelected = { preset ->
                // ✅ 优化方案：直接返回预设 ID 协议字符串，不再生成本地图片
                val presetProtocol = PresetAvatars.createPresetPath(preset.id)
                onAvatarChanged(presetProtocol)
                onDismiss()
            }
        )
    }
}

// savePresetAsImage 函数已移除 (本地渲染优化)


@Composable
private fun AvatarActionCard(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp), // 略微降低高度，更显干练
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        contentColor = contentColor,
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
                tint = containerColor // 使用原来的容器色作为图标色，保持识别度
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.Black.copy(alpha = 0.7f)
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
