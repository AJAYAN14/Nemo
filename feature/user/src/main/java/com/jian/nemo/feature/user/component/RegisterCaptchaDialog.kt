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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.designsystem.theme.CaptchaPalette
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.UserPalette
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
 * 算术验证码完整生成数据
 */
data class CaptchaData(
    val equationText: String,
    val answer: String,
    val chars: List<CaptchaChar>,
    val lines: List<CaptchaLine>,
    val dots: List<CaptchaDot>
)

private data class MathProblem(val num1: Int, val op: Char, val num2: Int, val answer: Int)

/**
 * 生成算术运算验证码数据（加、减、乘、除）
 */
fun generateCaptchaData(isDark: Boolean): CaptchaData {
    val operatorType = Random.nextInt(4)
    val problem = when (operatorType) {
        0 -> { // 加法：两数在 2..25 之间，避开加0
            val a = Random.nextInt(2, 26)
            val b = Random.nextInt(2, 26)
            MathProblem(a, '+', b, a + b)
        }
        1 -> { // 减法：大数减小数，差为正整数（避开减0和两数相等）
            val a = Random.nextInt(6, 31)
            val b = Random.nextInt(2, a)
            MathProblem(a, '-', b, a - b)
        }
        2 -> { // 乘法：严禁两位数乘两位数，避开 0 与 1
            if (Random.nextBoolean()) {
                // 模式 A：一位数 × 一位数 (2..9 × 2..9)
                val a = Random.nextInt(2, 10)
                val b = Random.nextInt(2, 10)
                MathProblem(a, '×', b, a * b)
            } else {
                // 模式 B：较小两位数 × 较小一位数 (11..19 × 2..5)
                val a = Random.nextInt(11, 20)
                val b = Random.nextInt(2, 6)
                MathProblem(a, '×', b, a * b)
            }
        }
        else -> { // 除法：严禁除以零，除数严格为一位数 (2..9)，被除数支持两位数除以一位数，严格整除
            val b = Random.nextInt(2, 10) // 除数 2..9，绝对不为 0
            val ans = Random.nextInt(2, 13) // 商 2..12
            val a = b * ans // 被除数，保证整除，且不出现两位数除以两位数
            MathProblem(a, '÷', b, ans)
        }
    }

    val equationStr = "${problem.num1} ${problem.op} ${problem.num2} = ?"
    val answerStr = problem.answer.toString()

    val colorPalette = if (isDark) CaptchaPalette.DarkColors else CaptchaPalette.LightColors

    val chars = equationStr.map { char ->
        CaptchaChar(
            char = char,
            color = when (char) {
                '?' -> if (isDark) CaptchaPalette.QuestionMarkDark else CaptchaPalette.QuestionMarkLight
                '=' -> if (isDark) CaptchaPalette.EqualSignDark else CaptchaPalette.EqualSignLight
                '+', '-', '×', '÷' -> if (isDark) CaptchaPalette.OperatorDark else CaptchaPalette.OperatorLight
                else -> colorPalette[Random.nextInt(colorPalette.size)]
            },
            rotation = if (char == ' ' || char == '=') 0f else (Random.nextFloat() * 12f - 6f),
            scale = if (char == ' ') 0.5f else (0.95f + Random.nextFloat() * 0.10f)
        )
    }

    val lines = (0..2).map {
        CaptchaLine(
            startXRatio = Random.nextFloat() * 0.25f,
            startYRatio = Random.nextFloat(),
            endXRatio = 0.75f + Random.nextFloat() * 0.25f,
            endYRatio = Random.nextFloat(),
            strokeWidth = 1.8f + Random.nextFloat() * 1.0f,
            color = colorPalette[Random.nextInt(colorPalette.size)].copy(alpha = if (isDark) 0.30f else 0.20f)
        )
    }

    val dots = (0..20).map {
        CaptchaDot(
            xRatio = Random.nextFloat(),
            yRatio = Random.nextFloat(),
            radius = 1.0f + Random.nextFloat() * 1.5f,
            color = colorPalette[Random.nextInt(colorPalette.size)].copy(alpha = if (isDark) 0.30f else 0.20f)
        )
    }

    return CaptchaData(
        equationText = equationStr,
        answer = answerStr,
        chars = chars,
        lines = lines,
        dots = dots
    )
}

/**
 * 使用 Jetpack Compose Canvas 原生绘制算术运算图形验证码
 */
@Composable
fun CaptchaCanvas(
    captchaData: CaptchaData,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDark) CaptchaPalette.BgDark else CaptchaPalette.BgLight
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

        // 3. 绘制算式字符（支持大字号居中渲染）
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
                    if (captchaChar.char != ' ') {
                        paint.textSize = (height * 0.52f) * captchaChar.scale
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
}

/**
 * 通用防刷算术运算验证码弹窗（同时支持注册与登录连续输错拦截）
 */
@Composable
fun RegisterCaptchaDialog(
    onDismissRequest: () -> Unit,
    onSuccess: () -> Unit,
    isDark: Boolean,
    title: String = "安全验证",
    description: String = "请计算图中算术式的结果，输入答案完成验证：",
    confirmText: String = "确认"
) {
    var captchaData by remember { mutableStateOf(generateCaptchaData(isDark)) }
    var inputCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val primaryTint = if (isDark) UserPalette.PrimaryGlassTintDark else UserPalette.PrimaryGlassTintLight
    val errorColor = if (isDark) IosColors.RedDark else IosColors.Red
    val descTextColor = if (isDark) Color.White.copy(alpha = 0.65f) else UserPalette.TextSlate500

    fun refreshCaptcha() {
        captchaData = generateCaptchaData(isDark)
        inputCode = ""
        errorMessage = null
    }

    fun submitVerification() {
        if (inputCode.isBlank()) {
            errorMessage = "请输入计算结果"
            return
        }
        if (inputCode.trim() == captchaData.answer) {
            errorMessage = null
            onSuccess()
        } else {
            errorMessage = "计算结果不正确，请重新计算"
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

            // 图形验证码 + 换一题操作栏
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
                        contentDescription = "换一题",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "换一题",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 数字计算结果输入框
            OutlinedTextField(
                value = inputCode,
                onValueChange = { newValue ->
                    if (newValue.length <= 5 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                        inputCode = newValue
                        if (errorMessage != null) errorMessage = null
                    }
                },
                placeholder = {
                    Text(
                        text = "请输入运算结果",
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
                    keyboardType = KeyboardType.Number,
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
                    unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else CaptchaPalette.UnfocusedBorderLight,
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
