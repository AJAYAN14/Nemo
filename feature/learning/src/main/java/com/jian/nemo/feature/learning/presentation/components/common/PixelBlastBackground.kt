package com.jian.nemo.feature.learning.presentation.components.common

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.jian.nemo.core.designsystem.theme.toHex

private const val PIXEL_BLAST_SHADER_URL = "file:///android_asset/shaders/pixel_blast.html"

/**
 * 原生 WebGL 2 PixelBlast 粒子流光波纹背景引擎
 * 完全对齐设计规范，采用统一 assets 资产与主题通信通道
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PixelBlastBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onControllerReady: ((triggerCenterBlast: () -> Unit) -> Unit)? = null
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val primaryHex = remember(primaryColor) { primaryColor.toHex() }
    val accentHex = remember(accentColor) { accentColor.toHex() }

    fun sendTheme(webView: WebView?) {
        webView?.evaluateJavascript(
            "if (window.setTheme) window.setTheme($isDark, '$primaryHex', '$accentHex');",
            null
        )
    }

    LaunchedEffect(isDark, primaryHex, accentHex) {
        sendTheme(webViewRef.value)
    }

    LaunchedEffect(isActive) {
        webViewRef.value?.let { wv ->
            if (isActive) {
                wv.onResume()
                wv.setOnTouchListener(null)
            } else {
                wv.onPause()
                wv.setOnTouchListener { _, _ -> false }
            }
        }
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
                        if (!isActive) {
                            view?.onPause()
                            view?.setOnTouchListener { _, _ -> false }
                        }
                        onControllerReady?.invoke {
                            view?.evaluateJavascript("if (window.triggerCenterBlast) window.triggerCenterBlast();", null)
                        }
                    }
                }
                loadUrl(PIXEL_BLAST_SHADER_URL)
                if (!isActive) {
                    onPause()
                    setOnTouchListener { _, _ -> false }
                }
                webViewRef.value = this
            }
        },
        update = { webView ->
            sendTheme(webView)
            if (isActive) {
                webView.onResume()
                webView.setOnTouchListener(null)
            } else {
                webView.onPause()
                webView.setOnTouchListener { _, _ -> false }
            }
        },
        onRelease = { webView ->
            webView.destroy()
        },
        modifier = modifier.fillMaxSize()
    )
}
