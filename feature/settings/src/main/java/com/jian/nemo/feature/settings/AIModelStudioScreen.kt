package com.jian.nemo.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.domain.repository.AIConfig
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.feature.settings.components.AIPlatformRegistry
import com.jian.nemo.feature.settings.components.CryptKeySocket
import com.jian.nemo.feature.settings.components.FloatingLiquidIsland
import com.jian.nemo.feature.settings.components.MatrixChipDock
import com.jian.nemo.feature.settings.components.PlatformItem
import com.jian.nemo.feature.settings.components.SlotModelPicker
import com.jian.nemo.feature.settings.components.SloshGaugeProbe
import com.jian.nemo.feature.settings.components.UnsavedChangesDialog
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 全新 AI 模型配置界面 (AI Model Studio)
 *
 * 全新重构架构：
 * 1. 深度融合 Nemo 标志性的「底层贯穿录制 + 悬浮液态玻璃顶栏 + 柔和多阶垂直渐变遮罩 (Backdrop Lens + Gradient Mask)」；
 * 2. 彻底推翻旧版单列办证表单与半屏弹窗，采用 Bento 模块化卡片（平台坞 + 老虎机滚轮 + API 密钥加密插槽 + 流体网络测速沙盘）；
 * 3. 悬浮操作岛使用全透明无底色液态玻璃外壳与彩色液态应用按钮（ColoredLiquidGlassButton）；
 * 4. 具备 BackHandler 拦截退出保护与未保存确认弹窗。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIModelStudioScreen(
    config: AIConfig,
    isNew: Boolean,
    isTesting: Boolean,
    testResult: AITestResult?,
    onConfigChange: (AIConfig) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = rememberLayerBackdrop()
    val backgroundColor = MaterialTheme.colorScheme.background
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 初始参数快照，用于比对是否有未保存修改
    val initialConfig = remember { config }
    val hasUnsavedChanges = remember(config, initialConfig) {
        config.name.trim() != initialConfig.name.trim() ||
        config.platform != initialConfig.platform ||
        config.model.trim() != initialConfig.model.trim() ||
        config.apiKey.trim() != initialConfig.apiKey.trim() ||
        config.baseUrl.trim() != initialConfig.baseUrl.trim()
    }

    var showExitConfirmation by remember { mutableStateOf(false) }

    // 拦截系统返回手势
    BackHandler(enabled = hasUnsavedChanges) {
        showExitConfirmation = true
    }

    // 当前选中的平台元数据
    val currentPlatform = remember(config.platform) {
        AIPlatformRegistry.find(config.platform)
    }

    // 检查核心配置是否齐备（API Key 与 Model ID 必填）
    val isReadyToDeploy = remember(config) {
        config.apiKey.isNotBlank() && config.model.isNotBlank()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. 底层页面滚动内容（被 backdrop 采样源录制，全屏贯穿至状态栏）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部弹性占位：未滑动时避开顶栏，向上滚动时平滑穿入顶栏遮罩与液态玻璃下方
            Spacer(modifier = Modifier.height(statusBarTop + 60.dp + 12.dp))

            // 模块 ①：服务商能量晶片坞 (Matrix Chip Dock)
            MatrixChipDock(
                selectedPlatform = config.platform,
                onPlatformSelected = { platform ->
                    val nextModel = platform.flagshipModels.firstOrNull() ?: ""
                    val nextUrl = platform.defaultUrl
                    val nextName = if (config.name.isBlank() || config.name.endsWith("配置") || config.name.endsWith("模型")) {
                        "${platform.name} 配置"
                    } else {
                        config.name
                    }
                    onConfigChange(
                        config.copy(
                            platform = platform.id,
                            model = nextModel,
                            baseUrl = nextUrl,
                            name = nextName
                        )
                    )
                }
            )

            // 模块 ②：旗舰模型老虎机滚轮选择舱 (Slot Picker + CodeSlots)
            SlotModelPicker(
                platform = currentPlatform,
                currentModel = config.model,
                onModelChange = { newModel ->
                    onConfigChange(config.copy(model = newModel))
                }
            )

            // 模块 ③：神经突触加密插槽 (Key & Gateway)
            CryptKeySocket(
                platform = currentPlatform,
                configName = config.name,
                onNameChange = { onConfigChange(config.copy(name = it)) },
                apiKey = config.apiKey,
                onApiKeyChange = { onConfigChange(config.copy(apiKey = it)) },
                baseUrl = config.baseUrl,
                onBaseUrlChange = { onConfigChange(config.copy(baseUrl = it)) }
            )

            // 模块 ④：神经共振蓄水池 (SloshGauge 连通性测试)
            SloshGaugeProbe(
                platform = currentPlatform,
                isTesting = isTesting,
                testResult = testResult,
                onStartTest = onTestConnection
            )

            // 底部预留悬浮操作岛和手势区安全高度
            Spacer(modifier = Modifier.height(navBarBottom + 96.dp))
        }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和多阶垂直渐变保护遮罩，读取 backdrop 物理透镜折射）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.98f),
                            backgroundColor.copy(alpha = 0.88f),
                            backgroundColor.copy(alpha = 0.55f),
                            backgroundColor.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 16.dp)
        ) {
            CommonHeader(
                title = if (isNew) "新建 AI 配置" else "编辑 AI 配置",
                onBack = {
                    if (hasUnsavedChanges) {
                        showExitConfirmation = true
                    } else {
                        onClose()
                    }
                },
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp
            )
        }

        // 3. 底部流体悬浮操作岛 (当参数就绪或存在未保存修改时以果冻弹簧滑出)
        FloatingLiquidIsland(
            visible = (hasUnsavedChanges || isNew) && isReadyToDeploy,
            onApply = onSave,
            backdrop = backdrop,
            accentColor = currentPlatform.brandColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottom + 8.dp)
        )

        // 4. 未保存退出确认弹窗
        if (showExitConfirmation) {
            UnsavedChangesDialog(
                onSaveAndExit = onSave,
                onDiscardChanges = onClose,
                onDismiss = { showExitConfirmation = false },
                accentColor = currentPlatform.brandColor
            )
        }
    }
}
