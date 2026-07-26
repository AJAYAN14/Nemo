package com.jian.nemo.feature.library.presentation.list

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A premium Discovery Bar composable that uses robust morphing containers to switch
 * between search and discovery states without layout overflows.
 *
 * Corresponds to the Flutter [DiscoveryBar] widget from portal_labs.
 *
 * @param options The list of discovery options to display.
 * @param selectedOptionIndex The currently active selected option index.
 * @param onOptionSelected Callback when a discovery option is selected.
 * @param searchQuery The search query from parent state.
 * @param onSearchQueryChange Callback when the search text is modified.
 * @param searchPlaceholder The placeholder text for the search input.
 * @param style The style configuration for the discovery bar.
 * @param modifier Modifier to be applied to the bar.
 */
@Composable
fun DiscoveryBar(
    options: List<DiscoveryOption>,
    selectedOptionIndex: Int,
    onOptionSelected: ((Int) -> Unit)? = null,
    searchQuery: String,
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = "Search",
    style: DiscoveryBarStyle = DiscoveryBarStyle(),
    modifier: Modifier = Modifier,
) {
    // If there is initially a search query, start in searching mode.
    var isSearching by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    var searchText by remember { mutableStateOf(searchQuery) }
    var localSelectedIndex by remember { mutableIntStateOf(selectedOptionIndex) }
    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current

    val spacing = 8.dp

    // Sync external index changes
    LaunchedEffect(selectedOptionIndex) {
        localSelectedIndex = selectedOptionIndex
    }

    // Sync external query changes (e.g. cleared by reset)
    LaunchedEffect(searchQuery) {
        if (searchQuery != searchText) {
            searchText = searchQuery
            if (searchQuery.isNotEmpty()) {
                isSearching = true
            }
        }
    }

    // Auto-focus soft keyboard when entering search mode
    LaunchedEffect(isSearching) {
        if (isSearching) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore if layout not ready
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(style.height),
    ) {
        val totalWidth = maxWidth
        val circleSize = style.height
        val pillWidth = totalWidth - circleSize - spacing

        // Animated widths for the two morphing containers
        val searchWidth by animateDpAsState(
            targetValue = if (isSearching) pillWidth else circleSize,
            animationSpec = spring(
                dampingRatio = style.mainSpringDampingRatio,
                stiffness = style.mainSpringStiffness,
            ),
            label = "searchWidth",
        )
        val discoveryWidth by animateDpAsState(
            targetValue = if (isSearching) circleSize else pillWidth,
            animationSpec = spring(
                dampingRatio = style.mainSpringDampingRatio,
                stiffness = style.mainSpringStiffness,
            ),
            label = "discoveryWidth",
        )

        // Left: Morphing Search Unit (Anchored Left)
        MorphingContainer(
            width = searchWidth,
            height = style.height,
            backgroundColor = style.backgroundColor,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            TapPulseWrapper(
                onTap = if (!isSearching) {
                    {
                        isSearching = true
                        if (style.enableHaptics) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    }
                } else null,
                active = !isSearching,
                fullArea = true,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = style.textStyle.color,
                        modifier = Modifier.size(style.searchIconSize),
                    )

                    // Search input: only visible when searching
                    val inputAlpha by animateFloatAsState(
                        targetValue = if (isSearching) 1f else 0f,
                        animationSpec = tween(durationMillis = 400),
                        label = "inputAlpha",
                    )
                    if (isSearching) {
                        BasicTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                onSearchQueryChange?.invoke(it)
                            },
                            singleLine = true,
                            textStyle = style.activeTextStyle,
                            cursorBrush = SolidColor(style.activeTextStyle.color),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.padding(start = 8.dp),
                                ) {
                                    if (searchText.isEmpty()) {
                                        Text(
                                            text = searchPlaceholder,
                                            style = style.textStyle.copy(
                                                color = style.textStyle.color.copy(alpha = 0.3f),
                                            ),
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { alpha = inputAlpha }
                                .focusRequester(focusRequester),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Right: Morphing Discovery Unit (Anchored Right)
        MorphingContainer(
            width = discoveryWidth,
            height = style.height,
            backgroundColor = style.backgroundColor,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            AnimatedContent(
                targetState = isSearching,
                transitionSpec = {
                    (fadeIn(tween(400)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(
                            dampingRatio = style.selectorSpringDampingRatio,
                            stiffness = style.selectorSpringStiffness,
                        ),
                    )).togetherWith(
                        fadeOut(tween(120)) + scaleOut(
                            targetScale = 0.8f,
                            animationSpec = tween(120),
                        ),
                    )
                },
                label = "discoveryContent",
            ) { searching ->
                if (searching) {
                    // Close (X) icon button
                    TapPulseWrapper(
                        onTap = {
                            isSearching = false
                            searchText = ""
                            onSearchQueryChange?.invoke("")
                            if (style.enableHaptics) {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        },
                        fullArea = true,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = style.textStyle.color,
                            modifier = Modifier.size(style.searchIconSize),
                        )
                    }
                } else {
                    // Options selector
                    OptionsSelector(
                        options = options,
                        selectedIndex = localSelectedIndex,
                        style = style,
                        onOptionSelected = { index ->
                            localSelectedIndex = index
                            onOptionSelected?.invoke(index)
                            if (style.enableHaptics) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * A morphing container with rounded corners and shadow, matching the Flutter
 * `_buildMorphingContainer` method.
 */
@Composable
private fun MorphingContainer(
    width: Dp,
    height: Dp,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(height / 2)
    val isDark = backgroundColor.luminance() < 0.5f
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .softCardShadow(borderRadius = height / 2, isDark = isDark)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * The options selector that displays available discovery options with a sliding
 * indicator. Corresponds to the Flutter `_buildOptionsSelector` method.
 */
@Composable
private fun OptionsSelector(
    options: List<DiscoveryOption>,
    selectedIndex: Int,
    style: DiscoveryBarStyle,
    onOptionSelected: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val containerWidth = maxWidth
        val optionWidth = containerWidth / options.size
        val indicatorPadding = 4.dp

        // Sliding indicator background
        val indicatorOffset by animateDpAsState(
            targetValue = optionWidth * selectedIndex + indicatorPadding,
            animationSpec = spring(
                dampingRatio = style.selectorSpringDampingRatio,
                stiffness = style.selectorSpringStiffness,
            ),
            label = "indicatorOffset",
        )

        // Animated indicator background color
        val currentActiveColor = options[selectedIndex.coerceIn(options.indices)].activeColor
        val indicatorBgColor by animateColorAsState(
            targetValue = currentActiveColor.copy(alpha = 0.12f),
            animationSpec = tween(durationMillis = 300),
            label = "indicatorBgColor",
        )

        // Indicator background
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(optionWidth - indicatorPadding * 2)
                .fillMaxHeight()
                .padding(vertical = indicatorPadding)
                .clip(RoundedCornerShape(style.height / 2))
                .background(indicatorBgColor),
        )

        // Option items row
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index

                // Animated icon color
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) option.activeColor
                    else style.inactiveColor.copy(alpha = 0.6f),
                    animationSpec = tween(durationMillis = 300),
                    label = "iconColor_$index",
                )

                // Animated text style
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) option.activeColor
                    else style.textStyle.color,
                    animationSpec = tween(durationMillis = 300),
                    label = "textColor_$index",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    TapPulseWrapper(
                        onTap = { onOptionSelected(index) },
                        fullArea = true,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (option.icon != null) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(style.iconSize),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = option.label,
                                style = TextStyle(
                                    fontSize = style.textStyle.fontSize,
                                    fontWeight = if (isSelected) FontWeight.W700
                                    else FontWeight.W500,
                                    color = textColor,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
