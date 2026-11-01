package com.jian.nemo.feature.learning.presentation.ai.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jian.nemo.core.designsystem.theme.LearningPalette
import com.jian.nemo.core.designsystem.theme.toHex
import com.jian.nemo.feature.learning.R

private const val AURORA_SHADER_URL = "file:///android_asset/shaders/ai_aurora.html"

/**
 * React Bits 官方 Aurora 原型预设色彩库
 */
data class AuroraPreset(
    val id: String,
    @StringRes val nameRes: Int,
    val color1: Color,
    val color2: Color,
    val color3: Color
)

val AuroraPresets = listOf(
    // 1. 经典极光 (Classic Neon) - 官方标志性电光紫与霓虹青绿
    AuroraPreset(
        id = "classic",
        nameRes = R.string.learning_ai_aurora_label_preset_classic,
        color1 = LearningPalette.AuroraClassic1,
        color2 = LearningPalette.AuroraClassic2,
        color3 = LearningPalette.AuroraClassic1
    ),
    // 2. 霓虹天际 (Nordic Sky) - React Bits 官方精选 (深紫/樱花洋红/晴空青)
    AuroraPreset(
        id = "nordic",
        nameRes = R.string.learning_ai_aurora_label_preset_nordic,
        color1 = LearningPalette.AuroraNordic1,
        color2 = LearningPalette.AuroraNordic2,
        color3 = LearningPalette.AuroraNordic3
    ),
    // 3. 极昼破晓 (Polar Dawn) - 皇家深蓝/晨曦柔粉/破晓赤红
    AuroraPreset(
        id = "polar_dawn",
        nameRes = R.string.learning_ai_aurora_label_preset_polar_dawn,
        color1 = LearningPalette.AuroraPolar1,
        color2 = LearningPalette.AuroraPolar2,
        color3 = LearningPalette.AuroraPolar3
    ),
    // 4. 冰川青碧 (Glacier Turquoise) - 冰川天蓝/薄荷翡翠/琉璃靛蓝
    AuroraPreset(
        id = "glacier",
        nameRes = R.string.learning_ai_aurora_label_preset_glacier,
        color1 = LearningPalette.AuroraGlacier1,
        color2 = LearningPalette.AuroraGlacier2,
        color3 = LearningPalette.AuroraGlacier3
    ),
    // 5. 暮色紫霞 (Dusk Sunset) - 绚丽玫红/梦幻罗兰/深海蔚蓝
    AuroraPreset(
        id = "dusk",
        nameRes = R.string.learning_ai_aurora_label_preset_dusk,
        color1 = LearningPalette.AuroraDusk1,
        color2 = LearningPalette.AuroraDusk2,
        color3 = LearningPalette.AuroraDusk3
    )
)

/**
 * AI 思考极光背景组件
 * 采用统一 assets 资产管理与运行时动态颜色通信
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AIAuroraBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    preset: AuroraPreset? = null,
    containerBg: Color? = null,
    height: Dp? = null
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    fun sendTheme(webView: WebView?) {
        val stopsArg = if (preset != null) {
            "['${preset.color1.toHex()}', '${preset.color2.toHex()}', '${preset.color3.toHex()}']"
        } else "null"
        webView?.evaluateJavascript("if (window.setTheme) window.setTheme($isDark, $stopsArg);", null)
    }

    LaunchedEffect(isDark, preset) {
        sendTheme(webViewRef.value)
    }

    val finalModifier = if (height != null) {
        modifier.height(height)
    } else {
        modifier.fillMaxSize()
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    allowFileAccess = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        sendTheme(view)
                    }
                }
                loadUrl(AURORA_SHADER_URL)
                webViewRef.value = this
            }
        },
        update = { webView ->
            sendTheme(webView)
        },
        onRelease = { webView ->
            webView.destroy()
        },
        modifier = finalModifier
    )
}
