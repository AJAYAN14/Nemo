package com.jian.nemo.core.ui.component.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 日语振假名组件 (Japanese Furigana Text)
 *
 * 特性：
 * 1. 支持 Group Ruby (整体注音)，完美支持"今日[きょう]"、"大人[おとな]"等熟字训。
 * 2. 简易禁则处理 (Kinsoku Shori)：防止句号、逗号单独出现在行首。
 * 3. 自动换行。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FuriganaText(
    text: String,
    modifier: Modifier = Modifier,
    baseTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    baseTextColor: Color = Color.Unspecified,
    furiganaTextSize: TextUnit = 10.sp,
    furiganaTextColor: Color = Color.Gray,
    furiganaFontFamily: FontFamily? = null
) {
    // 1. 解析
    val segments = remember(text) { parseFuriganaText(text) }

    // 2. 布局
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Bottom, // 底部对齐确保基线一致
        maxItemsInEachRow = Int.MAX_VALUE
    ) {
        segments.forEach { segment ->
            when (segment) {
                is FuriganaSegment.Plain -> {
                    // 【针对日语优化】处理普通文本的换行与禁则
                    val charList = segment.text.toList()
                    var i = 0
                    while (i < charList.size) {
                        val currentChar = charList[i]
                        val nextChar = charList.getOrNull(i + 1)
                        val shouldGlue = nextChar != null && isKinsokuChar(nextChar)

                        if (shouldGlue) {
                            val glueText = "$currentChar$nextChar"
                            PlainTextUnit(
                                text = glueText,
                                baseTextStyle = baseTextStyle,
                                baseTextColor = baseTextColor,
                                furiganaTextSize = furiganaTextSize
                            )
                            i += 2
                        } else {
                            PlainTextUnit(
                                text = currentChar.toString(),
                                baseTextStyle = baseTextStyle,
                                baseTextColor = baseTextColor,
                                furiganaTextSize = furiganaTextSize
                            )
                            i++
                        }
                    }
                }
                is FuriganaSegment.Annotated -> {
                    // 【日语优化】采用 Group Ruby (整体注音)
                    RubyUnit(
                        furigana = segment.furigana,
                        kanji = segment.kanji,
                        baseTextStyle = baseTextStyle,
                        baseTextColor = baseTextColor,
                        furiganaTextSize = furiganaTextSize,
                        furiganaTextColor = furiganaTextColor,
                        furiganaFontFamily = furiganaFontFamily
                    )
                }
            }
        }
    }
}

/**
 * 普通文本单元（不带振假名）
 * 使用固定高度的占位符确保与 RubyUnit 对齐
 */
@Composable
private fun PlainTextUnit(
    text: String,
    baseTextStyle: TextStyle,
    baseTextColor: Color,
    furiganaTextSize: TextUnit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // 空占位符，高度与振假名一致，确保基线对齐
        Box(
            modifier = Modifier.height(furiganaTextSize.value.dp + 10.dp)
        )
        // 文字
        Text(
            text = text,
            style = baseTextStyle,
            color = baseTextColor
        )
    }
}

/**
 * 带振假名的文本单元
 */
@Composable
private fun RubyUnit(
    furigana: String,
    kanji: String,
    baseTextStyle: TextStyle,
    baseTextColor: Color,
    furiganaTextSize: TextUnit,
    furiganaTextColor: Color,
    furiganaFontFamily: FontFamily?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(IntrinsicSize.Max) // 让振假名和汉字宽度取最大值
    ) {
        // 振假名 - 居中对齐
        Box(
            modifier = Modifier.height(furiganaTextSize.value.dp + 10.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = furigana,
                fontSize = furiganaTextSize,
                fontFamily = furiganaFontFamily,
                color = furiganaTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        }
        // 汉字 - 居中对齐
        Text(
            text = kanji,
            style = baseTextStyle,
            color = baseTextColor,
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------
// 辅助逻辑
// -------------------------------------------

sealed class FuriganaSegment {
    data class Plain(val text: String) : FuriganaSegment()
    data class Annotated(val kanji: String, val furigana: String) : FuriganaSegment()
}

/**
 * 判断是否为避头字符（不能出现在行首的字符）
 */
private fun isKinsokuChar(char: Char): Boolean {
    val kinsokuChars = "、。，．：；？！゛゜ー—‐／＼～｜…‥（）〔〕［］｛｝〈〉《》「」『』【】＋－±×÷＝＜＞∞°′″℃￥＄％＃＆＊＠§☆★○●◎◇"
    return kinsokuChars.contains(char)
}

fun parseFuriganaText(text: String): List<FuriganaSegment> {
    val result = mutableListOf<FuriganaSegment>()
    // 修正：只匹配汉字（CJK统一汉字 + 々）后跟 [假名]
    // 汉字范围: \u4E00-\u9FFF (基本汉字), \u3400-\u4DBF (扩展A), \u3005 (々)
    val pattern = Regex("""([\u4E00-\u9FFF\u3400-\u4DBF々]+)\[([^\]]+)]""")

    var lastIndex = 0
    pattern.findAll(text).forEach { matchResult ->
        val start = matchResult.range.first
        if (start > lastIndex) {
            result.add(FuriganaSegment.Plain(text.substring(lastIndex, start)))
        }
        val kanji = matchResult.groupValues[1]
        val furigana = matchResult.groupValues[2]
        result.add(FuriganaSegment.Annotated(kanji, furigana))
        lastIndex = matchResult.range.last + 1
    }

    if (lastIndex < text.length) {
        result.add(FuriganaSegment.Plain(text.substring(lastIndex)))
    }

    if (result.isEmpty() && text.isNotEmpty()) {
        result.add(FuriganaSegment.Plain(text))
    }

    return result
}
