package com.jian.nemo.feature.user.component

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val KANA_WAVE_SHADER_URL = "file:///android_asset/shaders/kana_wave.html"

/**
 * 顶部假名流动天幕背景
 * 基于 WebGL2 渲染引擎（包含 4 重 RenderPass、双向高斯模糊辉光与胶片颗粒）
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KanaWaveBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(isDark) {
        webViewRef.value?.evaluateJavascript("if (window.setTheme) window.setTheme($isDark);", null)
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
                        view?.evaluateJavascript("if (window.setTheme) window.setTheme($isDark);", null)
                    }
                }
                loadUrl(KANA_WAVE_SHADER_URL)
                webViewRef.value = this
            }
        },
        update = { webView ->
            webView.evaluateJavascript("if (window.setTheme) window.setTheme($isDark);", null)
        },
        onRelease = { webView ->
            webView.destroy()
        },
        modifier = modifier.fillMaxSize()
    )
}
