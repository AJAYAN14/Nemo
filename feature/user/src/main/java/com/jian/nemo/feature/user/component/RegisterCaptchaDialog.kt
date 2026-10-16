package com.jian.nemo.feature.user.component

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.NemoDialog
import kotlin.random.Random

/**
 * 验证码单个字符绘制配置
 */
data class CaptchaChar(
    val char: Char,
    val color: Color,
    val rotation: Float,
    val scale: Float
)

/**
 * 验证码干扰线
 */
data class CaptchaLine(
    val startXRatio: Float,
    val startYRatio: Float,
    val endXRatio: Float,
    val endYRatio: Float,
    val strokeWidth: Float,
    val color: Color
)

/**
 * 验证码干扰噪点
 */
data class CaptchaDot(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val color: Color
)

/**
 * 验证码完整生成数据
 */
data class CaptchaData(
    val text: String,
    val chars: List<CaptchaChar>,
    val lines: List<CaptchaLine>,
    val dots: List<CaptchaDot>
)

/**
 * 生成 6 位随机验证码数据（数字与字母强制组合，排除易混淆字符 0/O, 1/I/L）
 */
fun generateCaptchaData(isDark: Boolean): CaptchaData {
    val digits = "23456789"
    val letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    val allCandidates = digits + letters
    val totalLength = 6

    // 确保包含数字加字母组合：至少 2 位数字，至少 2 位字母，剩余 2 位从混合池随机抽取
    val selectedChars = mutableListOf<Char>()
    repeat(2) {
        selectedChars.add(digits[Random.nextInt(digits.length)])
    }
    repeat(2) {
        selectedChars.add(letters[Random.nextInt(letters.length)])
    }
    repeat(totalLength - 4) {
        selectedChars.add(allCandidates[Random.nextInt(allCandidates.length)])
    }
    selectedChars.shuffle()

    val darkColors = listOf(
        Color(0xFF60A5FA),
        Color(0xFF34D399),
        Color(0xFFF472B6),
        Color(0xFFFBBF24),
        Color(0xFFA78BFA),
        Color(0xFF38BDF8),
        Color(0xFF4ADE80)
    )

    val lightColors = listOf(
        Color(0xFF1D4ED8),
        Color(0xFF047857),
        Color(0xFFBE185D),
        Color(0xFFB45309),
        Color(0xFF6D28D9),
        Color(0xFF0369A1),
        Color(0xFF15803D)
    )

    val colorPalette = if (isDark) darkColors else lightColors

    val chars = selectedChars.map { char ->
        CaptchaChar(
            char = char,
            color = colorPalette[Random.nextInt(colorPalette.size)],
            rotation = Random.nextFloat() * 32f - 16f, // -16° ~ +16°，排布更整齐
            scale = 0.92f + Random.nextFloat() * 0.16f
        )
    }

    val lines = (0..3).map {
        CaptchaLine(
            startXRatio = Random.nextFloat() * 0.25f,
            startYRatio = Random.nextFloat(),
            endXRatio = 0.75f + Random.nextFloat() * 0.25f,
            endYRatio = Random.nextFloat(),
            strokeWidth = 2.2f + Random.nextFloat() * 1.8f,
            color = colorPalette[Random.nextInt(colorPalette.size)].copy(alpha = if (isDark) 0.5f else 0.38f)
        )
    }

    val dots = (0..45).map {
        CaptchaDot(
            xRatio = Random.nextFloat(),
            yRatio = Random.nextFloat(),
            radius = 1.2f + Random.nextFloat() * 2.2f,
            color = colorPalette[Random.nextInt(colorPalette.size)].copy(alpha = if (isDark) 0.45f else 0.35f)
        )
    }

    return CaptchaData(
        text = selectedChars.joinToString(""),
        chars = chars,
        lines = lines,
        dots = dots
    )
}

/**
 * 使用 Jetpack Compose Canvas 原生绘制 6 位图形验证码
 */
@Composable
fun CaptchaCanvas(
    captchaData: CaptchaData,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDark) Color(0xFF1E1E24) else Color(0xFFEEF2F6)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        val width = size.width
        val height = size.height

        // 1. 绘制噪点
        captchaData.dots.forEach { dot ->
            drawCircle(
                color = dot.color,
                radius = dot.radius,
                center = Offset(dot.xRatio * width, dot.yRatio * height)
            )
        }

        // 2. 绘制干扰线
        captchaData.lines.forEach { line ->
            drawLine(
                color = line.color,
                start = Offset(line.startXRatio * width, line.startYRatio * height),
                end = Offset(line.endXRatio * width, line.endYRatio * height),
                strokeWidth = line.strokeWidth
            )
        }

        // 3. 绘制 6 位文字（每个字符独立旋转、倾斜与着色）
        val charCount = captchaData.chars.size
        if (charCount > 0) {
            val cellWidth = width / (charCount + 1)
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                val paint = Paint().apply {
                    isAntiAlias = true
                    isFakeBoldText = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }

                captchaData.chars.forEachIndexed { index, captchaChar ->
                    paint.textSize = (height * 0.46f) * captchaChar.scale
                    paint.color = captchaChar.color.toArgb()
                    val fontMetrics = paint.fontMetrics
                    val x = cellWidth * (index + 0.90f)
                    val y = (height - fontMetrics.bottom - fontMetrics.top) / 2

                    nativeCanvas.save()
                    nativeCanvas.rotate(captchaChar.rotation, x, y)
                    val charStr = captchaChar.char.toString()
                    val textWidth = paint.measureText(charStr)
                    nativeCanvas.drawText(charStr, x - (textWidth / 2f), y, paint)
                    nativeCanvas.restore()
                }
            }
        }
    }
}

/**
 * 通用防刷图形验证码弹窗（同时支持注册与登录连续输错拦截）
 */
@Composable
fun RegisterCaptchaDialog(
    onDismissRequest: () -> Unit,
    onSuccess: () -> Unit,
    isDark: Boolean,
    title: String = "安全验证",
    description: String = "请根据下方图片输入 6 位验证码，防止恶意操作：",
    confirmText: String = "确认"
) {
    var captchaData by remember { mutableStateOf(generateCaptchaData(isDark)) }
    var inputCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val primaryTint = if (isDark) Color(0xFF2563EB) else Color(0xFF0E68FF)
    val errorColor = if (isDark) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val descTextColor = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF64748B)

    fun refreshCaptcha() {
        captchaData = generateCaptchaData(isDark)
        inputCode = ""
        errorMessage = null
    }

    fun submitVerification() {
        if (inputCode.isBlank()) {
            errorMessage = "请输入图形验证码"
            return
        }
        if (inputCode.trim().equals(captchaData.text, ignoreCase = true)) {
            errorMessage = null
            onSuccess()
        } else {
            errorMessage = "验证码错误，请重新输入"
            refreshCaptcha()
        }
    }

    NemoDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        confirmText = confirmText,
        dismissText = "取消",
        confirmButtonColor = primaryTint,
        onConfirm = { submitVerification() },
        onDismiss = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = description,
                fontSize = 13.sp,
                color = descTextColor,
                lineHeight = 18.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            // 图形验证码 + 换一张操作栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CaptchaCanvas(
                    captchaData = captchaData,
                    isDark = isDark,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable { refreshCaptcha() }
                )

                OutlinedButton(
                    onClick = { refreshCaptcha() },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = primaryTint
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "换一张",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "换一张",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 6 位验证码输入框
            OutlinedTextField(
                value = inputCode,
                onValueChange = { newValue ->
                    if (newValue.length <= 6) {
                        inputCode = newValue.uppercase()
                        if (errorMessage != null) errorMessage = null
                    }
                },
                placeholder = {
                    Text(
                        text = "请输入 6 位验证码",
                        fontSize = 14.sp,
                        color = descTextColor.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = descTextColor,
                        modifier = Modifier.size(19.dp)
                    )
                },
                singleLine = true,
                isError = errorMessage != null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        submitVerification()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryTint,
                    unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0),
                    cursorColor = primaryTint
                )
            )

            // 错误信息提示
            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = errorColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
