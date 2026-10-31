package com.jian.nemo.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import coil.compose.AsyncImage
import com.jian.nemo.core.designsystem.theme.UiPalette

/**
 * 可复用的头像组件
 * 支持图片头像和基于用户名的文字头像
 *
 * @param username 用户名，用于生成文字头像
 * @param avatarPath 头像图片路径，如果为空则显示文字头像
 * @param size 头像大小
 * @param borderWidth 边框宽度，默认为0
 * @param borderColor 边框颜色
 * @param padding 头像与边框之间的内边距
 * @param useGradientBorder 是否使用渐变色边框
 * @param shape 头像形状，默认为圆形
 * @param modifier 修饰符
 */
@Composable
fun AvatarImage(
    username: String,
    avatarPath: String? = null,
    size: Dp = 48.dp,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    padding: Dp = 0.dp,
    useGradientBorder: Boolean = false,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    // 生成基于用户名的颜色
    val backgroundColor = remember(username) {
        generateColorFromUsername(username)
    }

    // 获取首字母
    val initial = remember(username) {
        username.takeIf { it.isNotEmpty() }?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }

    // 解析预设头像
    val presetAvatar = remember(avatarPath) {
        // 使用完整路径作为 key (avatarPath)
        com.jian.nemo.core.ui.util.PresetAvatars.getPresetId(avatarPath)?.let {
             com.jian.nemo.core.ui.util.PresetAvatars.getById(it)
        }
    }

    val isImageAvatar = !avatarPath.isNullOrEmpty() && !com.jian.nemo.core.ui.util.PresetAvatars.isPreset(avatarPath)

    // 计算渐变边框的绘制参数
    val gradientColors = remember {
        UiPalette.AvatarGlowGradient
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (useGradientBorder && borderWidth > 0.dp) {
                    Modifier.drawBehind {
                        val borderWidthPx = borderWidth.toPx()
                        val radius = size.toPx() / 2f
                        val center = Offset(radius, radius)

                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = gradientColors,
                                center = center
                            ),
                            radius = radius - borderWidthPx / 2f,
                            style = Stroke(width = borderWidthPx)
                        )
                    }
                } else if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, borderColor, shape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        val safeSize: Dp = size - (borderWidth * 2) - (padding * 2)
        val innerSize = safeSize * 0.4f
        
        Box(
            modifier = Modifier
                .size(width = safeSize, height = safeSize)
                .clip(shape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(width = innerSize, height = innerSize),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                presetAvatar != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .drawBehind {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = presetAvatar.colors,
                                        start = Offset(0f, 0f),
                                        end = Offset(this.size.width, this.size.height)
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                         presetAvatar.emoji?.let { emoji ->
                             Text(
                                 text = emoji,
                                 fontSize = (innerSize.value).sp,
                                 textAlign = TextAlign.Center
                             )
                         }
                    }
                }
                isImageAvatar -> {
                    val request = coil.request.ImageRequest.Builder(context)
                        .data(avatarPath)
                        .crossfade(true)
                        .diskCacheKey(avatarPath?.substringBefore("?"))
                        .memoryCacheKey(avatarPath?.substringBefore("?"))
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()

                    var isError by remember { mutableStateOf(false) }

                    if (isError) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                fontSize = (innerSize.value).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics {
                                    this.contentDescription = "用户 $username 的默认头像，显示首字母 $initial (图片加载失败)"
                                }
                            )
                        }
                    } else {
                        AsyncImage(
                            model = request,
                            contentDescription = "用户 $username 的个人头像图片",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape),
                            contentScale = ContentScale.Crop,
                            onState = { state ->
                                if (state is coil.compose.AsyncImagePainter.State.Error) {
                                    isError = true
                                }
                            }
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = (innerSize.value).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics {
                                this.contentDescription = "用户 $username 的默认头像，显示首字母 $initial"
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 根据用户名生成固定的颜色
 * 使用哈希算法确保同一用户名总是生成相同的颜色
 */
private fun generateColorFromUsername(username: String): Color {
    val colors = UiPalette.DefaultAvatarPalette
    val hash = username.hashCode()
    val index = kotlin.math.abs(hash) % colors.size
    return colors[index]
}
