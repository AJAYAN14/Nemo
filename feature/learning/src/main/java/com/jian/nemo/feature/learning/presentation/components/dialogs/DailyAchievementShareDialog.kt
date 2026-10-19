package com.jian.nemo.feature.learning.presentation.components.dialogs

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.jian.nemo.core.ui.R as CoreUiR
import com.jian.nemo.feature.learning.R
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 今日学习成就打卡海报弹窗 (Daily Achievement Share Poster Dialog)
 *
 * 采用 Compose GraphicsLayer 无损超清截图技术，将纯粹的海报卡片导出为高保真 PNG 图片。
 * 特点：
 * 1. 背景全屏高斯虚化遮罩；
 * 2. 海报卡片下半部分固定纯白背景；
 * 3. 中文部分原样保持不变，真实显示打卡天数（已连续打卡 X 天）；
 * 4. 仅原英文替换为日文：量词替换为「語」，英文小标替换为日文名言「継続は力なり」；
 * 5. 官方品牌 Logo 垂直居中展示，正版 App 图标位于名字 Nemo 上方；
 * 6. 底部操作按钮轻盈浮空，不添加多余白色底座，彻底杜绝横向挤压。
 *
 * @param sessionTotalCount 今日掌握总词数
 * @param sessionMaxCombo 本次学习最高连击数
 * @param sessionDurationSeconds 学习总用时（秒）
 * @param dailyStreak 真实连续打卡天数
 * @param onDismissRequest 关闭对话框回调
 */
@Composable
fun DailyAchievementShareDialog(
    sessionTotalCount: Int,
    sessionMaxCombo: Int,
    sessionDurationSeconds: Long,
    dailyStreak: Int = 1,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePrimary = MaterialTheme.colorScheme.primary

    val graphicsLayer = rememberGraphicsLayer()
    var isSaving by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }

    val minutes = sessionDurationSeconds / 60
    val seconds = sessionDurationSeconds % 60
    val timeStr = if (minutes > 0) "${minutes}分${seconds}秒" else "${seconds}秒"

    val currentDateStr = remember {
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // 外层：半透明全屏深色景深遮罩（点击外部可关闭）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // 消费内部点击，防止误触发关闭
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 纯粹的海报卡片（包裹在 GraphicsLayer 中直角满幅录制，外层圆角展示）
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White, // 固定白底，保证导出图片底色纯白明亮
                    shadowElevation = 18.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                graphicsLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                                drawContent()
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        // 上半部分：高保真主题渐变卡片 (Hero Card)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            themePrimary,
                                            themePrimary.copy(alpha = 0.88f),
                                            Color(0xFF0F766E)
                                        )
                                    )
                                )
                                .padding(horizontal = 20.dp, vertical = 22.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 顶部行：真实打卡天数徽章与日期（中文表述保持不变）
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = "🔥", fontSize = 11.sp)
                                            Text(
                                                text = "已连续打卡 ${dailyStreak} 天",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Text(
                                        text = currentDateStr,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // 中部：今日掌握词汇大字（中文保持不变，单位换为日文量词：語）
                                Text(
                                    text = "今日掌握词汇",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "$sessionTotalCount",
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1.5).sp,
                                        color = Color.White,
                                        lineHeight = 48.sp
                                    )
                                    Text(
                                        text = "語",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // 下方指标分割线与数据（中文保持不变）
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ 最高连击: ${sessionMaxCombo} 次",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                    Text(
                                        text = "⏱ 专注用时: $timeStr",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                }
                            }
                        }

                        // 下半部分：固定纯白背景（Color.White）、中文名言 + 日文副标名言 + 项目正版 Logo 与名字（高度精简为 11.dp，绝对对齐）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 20.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "不积跬步，无以至千里",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "「継続は力なり」",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }

                            // 官方正版 Logo 与名字（纵向排列：图标在名字上方）
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(id = CoreUiR.drawable.ic_nemo_splash_logo),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Nemo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                    letterSpacing = (-0.3).sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 2. 底部操作栏（移到海报卡片外部，轻盈浮空横排，左右 1:1 分割）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 保存图片按钮
                OutlinedButton(
                    onClick = {
                        if (isSaving || isSharing) return@OutlinedButton
                        isSaving = true
                        scope.launch {
                            try {
                                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                val success = saveBitmapToGallery(context, bitmap)
                                Toast.makeText(
                                    context,
                                    if (success) "已保存海报到相册" else "保存失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (success) {
                                    onDismissRequest()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.16f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f)),
                    enabled = !isSaving && !isSharing
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "保存图片",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // 分享海报图片按钮（使用热力图同款 ic_share_ios，浮空主题色胶囊按钮）
                Button(
                    onClick = {
                        if (isSharing || isSaving) return@Button
                        isSharing = true
                        scope.launch {
                            try {
                                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                shareBitmapImage(context, bitmap)
                                onDismissRequest()
                            } catch (e: Exception) {
                                Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSharing = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                    enabled = !isSharing && !isSaving
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share_ios),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "分享海报",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. 底部操作栏下方居中放置轻量圆形半透明关闭按钮
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "关闭",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
}

/**
 * 将生成的超清海报 Bitmap 写入缓存并通过 FileProvider 调起原生系统图片分享
 */
private fun shareBitmapImage(context: Context, bitmap: Bitmap) {
    val dir = File(context.cacheDir, "share_posters")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "nemo_daily_achievement.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "分享今日学习成就"))
}

/**
 * 将超清海报 Bitmap 保存至系统相册
 */
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "Nemo_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Nemo"
                )
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } ?: return false
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val albumDir = File(picturesDir, "Nemo")
            if (!albumDir.exists()) albumDir.mkdirs()
            val file = File(albumDir, "Nemo_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
