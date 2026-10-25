package com.jian.nemo.feature.settings.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.R as DesignR
import com.jian.nemo.core.domain.repository.AIConfig
import com.jian.nemo.core.ui.component.Capsule
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.feature.settings.AITestResult
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 界面专属中性高级调色板 (全面去 MD3 化，专为深浅色模式物理折射定制)
 */
object StudioThemePalette {
    fun cardBackground(isDark: Boolean) = if (isDark) Color(0xFF13171F) else Color(0xFFFFFFFF)
    fun cardBorder(isDark: Boolean) = if (isDark) Color(0x2294A3B8) else Color(0x140F172A)
    fun surfacePill(isDark: Boolean) = if (isDark) Color(0xFF1C222C) else Color(0xFFF1F5F9)
    fun surfacePillBorder(isDark: Boolean) = if (isDark) Color(0x2894A3B8) else Color(0x180F172A)
    fun textPrimary(isDark: Boolean) = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    fun textSecondary(isDark: Boolean) = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    fun textMuted(isDark: Boolean) = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    fun inputBorder(isDark: Boolean) = if (isDark) Color(0x2894A3B8) else Color(0x200F172A)
}

/**
 * 平台模型元数据定义
 */
data class PlatformItem(
    val id: String,
    val name: String,
    val tagline: String,
    val brandColor: Color,
    val iconRes: Int? = null,
    val iconVector: ImageVector? = null,
    val defaultUrl: String,
    val flagshipModels: List<String>
)

object AIPlatformRegistry {
    val platforms = listOf(
        // === 国产模型阵列（首字母字典序：DeepSeek -> 豆包 -> GLM -> Kimi -> Mimo -> Qwen）===
        PlatformItem(
            id = "deepseek",
            name = "DeepSeek",
            tagline = "深度求索 · V4 旗舰推理与高性价比",
            brandColor = Color(0xFF0E68FF),
            iconRes = DesignR.drawable.ic_deepseek,
            defaultUrl = "https://api.deepseek.com/v1",
            flagshipModels = listOf("deepseek-v4-pro", "deepseek-v4-flash")
        ),
        PlatformItem(
            id = "doubao",
            name = "豆包",
            tagline = "字节跳动 · Seed 2.1 旗舰与 Evolving 持续演进",
            brandColor = Color(0xFF00B4D8),
            iconRes = DesignR.drawable.ic_doubao,
            defaultUrl = "https://ark.cn-beijing.volces.com/api/v3",
            flagshipModels = listOf("doubao-seed-evolving", "doubao-seed-2-1-pro-260915", "doubao-seed-2-1-lite-260915", "doubao-seed-2-1-turbo-260628")
        ),
        PlatformItem(
            id = "glm",
            name = "智谱 GLM",
            tagline = "智谱 AI · GLM-5.3 旗舰推理与 Agent 编码",
            brandColor = Color(0xFF3762FF),
            iconRes = DesignR.drawable.ic_glm,
            defaultUrl = "https://open.bigmodel.cn/api/paas/v4",
            flagshipModels = listOf("glm-5.3", "glm-5.3-flash", "glm-5.2", "glm-5.1", "glm-4.7-flash")
        ),
        PlatformItem(
            id = "kimi",
            name = "Kimi",
            tagline = "Moonshot AI · K3 旗舰 1M 多模态与长程编程",
            brandColor = Color(0xFF007AFF),
            iconRes = DesignR.drawable.ic_kimi,
            defaultUrl = "https://api.moonshot.cn/v1",
            flagshipModels = listOf("kimi-k3", "kimi-k2.7-code", "kimi-k2.7-code-highspeed", "kimi-k2.6")
        ),
        PlatformItem(
            id = "mimo",
            name = "Mimo",
            tagline = "小米大模型 · V2.6 旗舰推理与端云协同",
            brandColor = Color(0xFFFF6A00),
            iconRes = DesignR.drawable.ic_mimo,
            defaultUrl = "https://api.xiaomimimo.com/v1",
            flagshipModels = listOf("mimo-v2.6-pro", "mimo-v2.6-flash", "mimo-v2.6-pro-ultraspeed")
        ),
        PlatformItem(
            id = "qwen",
            name = "通义千问",
            tagline = "阿里云百炼 · qwen3.8-max 旗舰与双轨生态",
            brandColor = Color(0xFF615CED),
            iconRes = DesignR.drawable.ic_qwen,
            defaultUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            flagshipModels = listOf("qwen3.8-max", "qwen3.8-flash", "qwen3.7-max", "qwen-turbo")
        ),

        // === 国外模型阵列（按 2026 顶级综合实力梯队：Claude -> OpenAI -> Gemini）===
        PlatformItem(
            id = "claude",
            name = "Claude",
            tagline = "Anthropic · 5.x 旗舰体系与自适应思考链",
            brandColor = Color(0xFFD97706),
            iconRes = DesignR.drawable.ic_claude,
            defaultUrl = "https://api.anthropic.com/v1",
            flagshipModels = listOf("claude-sonnet-5-5", "claude-opus-5-5", "claude-haiku-5-5", "claude-fable-5-1")
        ),
        PlatformItem(
            id = "openai",
            name = "OpenAI",
            tagline = "GPT-6 旗舰梯队 · Astra 深度推理与 Sol 智能体",
            brandColor = Color(0xFF10A37F),
            iconRes = DesignR.drawable.ic_openai,
            defaultUrl = "https://api.openai.com/v1",
            flagshipModels = listOf("gpt-6-astra", "gpt-6.1-sol", "gpt-6-luna", "chat-latest")
        ),
        PlatformItem(
            id = "gemini",
            name = "Gemini",
            tagline = "Google DeepMind · Argon 旗舰推理与百万上下文",
            brandColor = Color(0xFFAF52DE),
            iconRes = DesignR.drawable.ic_gemini,
            defaultUrl = "https://generativelanguage.googleapis.com/v1beta",
            flagshipModels = listOf("gemini-4-argon", "gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.1-pro-preview")
        ),
        PlatformItem(
            id = "custom",
            name = "自定义",
            tagline = "接入任意兼容 OpenAI 协议的接口与模型",
            brandColor = Color(0xFF8B5CF6),
            iconVector = Icons.Rounded.Cable,
            defaultUrl = "",
            flagshipModels = listOf(
                "MiniMax-Text-01",
                "abab6.5s-chat",
                "step-2-16k",
                "step-1v-8k",
                "yi-lightning",
                "yi-large",
                "deepseek-ai/DeepSeek-V3",
                "deepseek-ai/DeepSeek-R1",
                "llama-3.3-70b-instruct",
                "mistral-large-2411"
            )
        )
    )

    fun find(id: String): PlatformItem {
        return platforms.find { it.id == id } ?: platforms.first()
    }
}

/**
 * 模块 ①: 服务商能量晶片坞 (Matrix Chip Dock)
 *
 * 借鉴 portal_labs 与 Bento Grid 理念，将原本简陋的下拉框重构为具有全息呼吸微光与物理缩放的晶片磁吸阵列。
 */
@Composable
fun MatrixChipDock(
    selectedPlatform: String,
    onPlatformSelected: (PlatformItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(modifier = modifier.fillMaxWidth()) {
        val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlumpDuo) {
                        PlumpDuoIcon(
                            icon = PlumpIcon.Cpu,
                            color = IsoluminantPalette.WarmAmber,
                            size = 24.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 服务平台",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "滑动轻触切换",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AIPlatformRegistry.platforms, key = { it.id }) { item ->
                val isSelected = item.id == selectedPlatform
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.03f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                    label = "chip_scale"
                )

                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlatformSelected(item)
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) {
                        item.brandColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                    } else {
                        if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) item.brandColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.25f)
                    ),
                    modifier = Modifier
                        .width(136.dp)
                        .height(98.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconSize = when (item.id) {
                                "gemini" -> 27.dp
                                "qwen" -> 26.5.dp
                                else -> 24.dp
                            }
                            Box(
                                modifier = Modifier.size(34.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.iconRes != null) {
                                    Icon(
                                        painter = painterResource(id = item.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(iconSize),
                                        tint = Color.Unspecified
                                    )
                                } else if (item.iconVector != null) {
                                    Icon(
                                        imageVector = item.iconVector,
                                        contentDescription = null,
                                        modifier = Modifier.size(iconSize),
                                        tint = item.brandColor
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(item.brandColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) item.brandColor else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isSelected) "● 已挂载" else "点击挂载",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isSelected) item.brandColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 模块 ②: 旗舰模型老虎机滚轮选择舱 (Slot Picker + CodeSlots)
 *
 * 借鉴 portal_labs/slot_picker_interaction 与 react-bits/OptionWheel，
 * 彻底颠覆单调的输入框，支持齿轮机械阻尼拨动选型，并支持一键切换极客代码卡槽。
 */
@Composable
fun SlotModelPicker(
    platform: PlatformItem,
    currentModel: String,
    onModelChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var isCustomMode by remember(platform.id) {
        mutableStateOf(!platform.flagshipModels.contains(currentModel) && currentModel.isNotBlank())
    }

    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题栏与自由输入模式切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlumpDuo) {
                            PlumpDuoIcon(
                                icon = PlumpIcon.BrainCircuit,
                                color = IsoluminantPalette.EmeraldGreen,
                                size = 28.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                tint = platform.brandColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "选择模型",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = StudioThemePalette.textPrimary(isDark)
                        )
                        Text(
                            text = platform.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioThemePalette.textSecondary(isDark),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                }

                Surface(
                    onClick = {
                        isCustomMode = !isCustomMode
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCustomMode) platform.brandColor.copy(alpha = if (isDark) 0.20f else 0.12f) else StudioThemePalette.surfacePill(isDark),
                    border = BorderStroke(
                        0.5.dp,
                        if (isCustomMode) platform.brandColor else StudioThemePalette.surfacePillBorder(isDark)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCustomMode) Icons.Rounded.Edit else Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = if (isCustomMode) platform.brandColor else StudioThemePalette.textSecondary(isDark),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCustomMode) "推荐型号" else "手动输入",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isCustomMode) platform.brandColor else StudioThemePalette.textSecondary(isDark)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            AnimatedContent(
                targetState = isCustomMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 90)) + slideInVertically { height -> height / 2 }) togetherWith
                    (fadeOut(animationSpec = tween(90)) + slideOutVertically { height -> -height / 2 })
                },
                label = "model_mode_transition"
            ) { custom ->
                if (custom) {
                    // 极客自由代码输入模式 (CodeSlots)
                    Column {
                        OutlinedTextField(
                            value = currentModel,
                            onValueChange = onModelChange,
                            placeholder = {
                                Text(
                                    text = "输入自定义模型代号, 例如: ${platform.flagshipModels.firstOrNull() ?: "model-id"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = platform.brandColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                focusedContainerColor = if (isDark) Color(0xFF141720) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) Color(0xFF141720) else Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "提示：自定义模型需确保您的 API Key 具备该模型调用权限",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    // 机械老虎机 Slot Picker 滚轮列表
                    val models = platform.flagshipModels

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        models.forEachIndexed { index, modelName ->
                            val isSelected = currentModel == modelName || (currentModel.isBlank() && index == 0)

                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onModelChange(modelName)
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) {
                                    platform.brandColor.copy(alpha = if (isDark) 0.16f else 0.10f)
                                } else {
                                    if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
                                    else Color(0xFFF8FAFC)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.2.dp else 0.5.dp,
                                    color = if (isSelected) platform.brandColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) platform.brandColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                                ),
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = modelName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = if (isSelected) platform.brandColor else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = when {
                                                    modelName.contains("reasoner") || modelName.contains("opus") -> "满血深度逻辑推理 / 长思维链"
                                                    modelName.contains("mini") || modelName.contains("flash") -> "超低延迟极速响应 / 经济轻量"
                                                    else -> "通用全能旗舰主力模型"
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = platform.brandColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 模块 ③: 神经突触加密插槽 (Cryptographic Key Socket & Gateway)
 *
 * 借鉴 react-bits 智能插槽与剪贴板感知：
 * 1. 自动嗅探剪贴板并提供一键脉冲注入；
 * 2. 点阵流光密文展示与视网膜显隐切换；
 * 3. 官方网关与自定义网关快速切换。
 */
@Composable
fun CryptKeySocket(
    platform: PlatformItem,
    configName: String,
    onNameChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var passwordVisible by remember { mutableStateOf(false) }

    // 检查剪贴板中是否有可能是 API Key 的内容
    val clipboardText = remember { clipboardManager.getText()?.text ?: "" }
    val hasLikelyKeyInClipboard = remember(clipboardText, apiKey) {
        clipboardText.isNotBlank() &&
        (clipboardText.startsWith("sk-") || clipboardText.length > 20) &&
        clipboardText != apiKey
    }

    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 头部标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlumpDuo) {
                            PlumpDuoIcon(
                                icon = PlumpIcon.KeyRound,
                                color = IsoluminantPalette.LakesideCyan,
                                size = 28.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = platform.brandColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "安全凭证与网关",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "端到端密钥本地加密保护，绝不上报云端",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. 配置名称
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "配置名称",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = StudioThemePalette.textPrimary(isDark)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = configName,
                onValueChange = onNameChange,
                placeholder = {
                    Text(
                        text = "例如: ${platform.name} 极速引擎",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioThemePalette.textMuted(isDark)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = StudioThemePalette.textPrimary(isDark)
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = platform.brandColor,
                    unfocusedBorderColor = StudioThemePalette.inputBorder(isDark),
                    focusedContainerColor = StudioThemePalette.surfacePill(isDark),
                    unfocusedContainerColor = StudioThemePalette.surfacePill(isDark)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. API 访问密钥
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "API 访问密钥",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = StudioThemePalette.textPrimary(isDark)
                )

                // 剪贴板脉冲注入按钮
                if (hasLikelyKeyInClipboard) {
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onApiKeyChange(clipboardText)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = platform.brandColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, platform.brandColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = null,
                                tint = platform.brandColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "嗅探到密钥 · 一键注入",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = platform.brandColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                placeholder = {
                    Text(
                        text = "输入或粘贴 ${platform.name} API Key",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioThemePalette.textMuted(isDark)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = StudioThemePalette.textPrimary(isDark)
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (passwordVisible) "隐藏" else "显示",
                            tint = StudioThemePalette.textSecondary(isDark)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = platform.brandColor,
                    unfocusedBorderColor = StudioThemePalette.inputBorder(isDark),
                    focusedContainerColor = StudioThemePalette.surfacePill(isDark),
                    unfocusedContainerColor = StudioThemePalette.surfacePill(isDark)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. 服务网关端点 (Base URL)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "服务网关端点",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = StudioThemePalette.textPrimary(isDark)
                )

                // 一键恢复官方默认网关
                if (baseUrl.trim() != platform.defaultUrl.trim()) {
                    Text(
                        text = "恢复官方预设",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = platform.brandColor,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable { onBaseUrlChange(platform.defaultUrl) }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                placeholder = {
                    Text(
                        text = "例如: ${platform.defaultUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioThemePalette.textMuted(isDark)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = StudioThemePalette.textPrimary(isDark)
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = platform.brandColor,
                    unfocusedBorderColor = StudioThemePalette.inputBorder(isDark),
                    focusedContainerColor = StudioThemePalette.surfacePill(isDark),
                    unfocusedContainerColor = StudioThemePalette.surfacePill(isDark)
                )
            )
        }
    }
}

/**
 * 模块 ④: 神经共振蓄水池 (SloshGaugeProbe 连通性测试)
 *
 * 借鉴 react-bits/SloshGauge：
 * 将单调的按钮变成具有流体物理波浪的共振探测器，全流程展示 DNS ➔ TLS ➔ TTFT 握手延迟。
 */
@Composable
fun SloshGaugeProbe(
    platform: PlatformItem,
    isTesting: Boolean,
    testResult: AITestResult?,
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 动态波形过渡
    val infiniteTransition = rememberInfiniteTransition(label = "slosh_probe_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "probe_wave_phase"
    )

    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlumpDuo) {
                            PlumpDuoIcon(
                                icon = PlumpIcon.Gauge,
                                color = IsoluminantPalette.SkyIndigo,
                                size = 28.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Radar,
                                contentDescription = null,
                                tint = platform.brandColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "网络连通性测试",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "发送轻量 Ping 探测端点有效性与首字延迟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 测速交互卡片
            Surface(
                onClick = {
                    if (!isTesting) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartTest()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                color = when {
                    isTesting -> platform.brandColor.copy(alpha = 0.12f)
                    testResult != null && testResult.success -> Color(0xFF10A37F).copy(alpha = 0.12f)
                    testResult != null && !testResult.success -> Color(0xFFF43F5E).copy(alpha = 0.12f)
                    else -> if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f) else Color(0xFFF8FAFC)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isTesting -> platform.brandColor
                        testResult != null && testResult.success -> Color(0xFF10A37F)
                        testResult != null && !testResult.success -> Color(0xFFF43F5E)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // 若正在测试，绘制流体波浪背景
                    if (isTesting) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val width = size.width
                            val height = size.height
                            val wavePath = Path().apply {
                                moveTo(0f, height * 0.7f)
                                val points = 30
                                for (i in 0..points) {
                                    val x = (i.toFloat() / points) * width
                                    val y = (height * 0.7f) + sin(wavePhase + (i * 0.35f)) * 8.dp.toPx()
                                    lineTo(x, y)
                                }
                                lineTo(width, height)
                                lineTo(0f, height)
                                close()
                            }
                            drawPath(
                                path = wavePath,
                                color = platform.brandColor.copy(alpha = 0.15f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = platform.brandColor,
                                    strokeWidth = 2.5.dp
                                )
                            } else if (testResult != null) {
                                Icon(
                                    imageVector = if (testResult.success) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (testResult.success) Color(0xFF10A37F) else Color(0xFFF43F5E),
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.NetworkPing,
                                    contentDescription = null,
                                    tint = platform.brandColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Text(
                                    text = when {
                                        isTesting -> "正在测试连通性..."
                                        testResult != null && testResult.success -> "连接成功"
                                        testResult != null && !testResult.success -> "连接失败"
                                        else -> "测试连通性"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        testResult != null && testResult.success -> Color(0xFF10A37F)
                                        testResult != null && !testResult.success -> Color(0xFFF43F5E)
                                        else -> StudioThemePalette.textPrimary(isDark)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isTesting -> "正在验证 API 与模型响应..."
                                        testResult != null -> testResult.message
                                        else -> "验证当前密钥与端点可用性"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioThemePalette.textSecondary(isDark),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!isTesting) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = platform.brandColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                                border = BorderStroke(0.5.dp, platform.brandColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = if (testResult != null) "重测" else "探测",
                                    color = platform.brandColor,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
