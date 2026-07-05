package com.jian.nemo.feature.user.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GoogleIcon: ImageVector
    get() {
        if (_googleIcon != null) {
            return _googleIcon!!
        }
        _googleIcon = ImageVector.Builder(
            name = "GoogleIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFBBC05)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(213.1976f, 515.482f)
                curveToRelative(0f, -33.485f, 5.632f, -65.588f, 15.616f, -95.693f)
                lineTo(53.4536f, 287.155f)
                arcTo(511.053f, 511.053f, 0f, false, false, 0.0016f, 515.482f)
                curveToRelative(0f, 82.048f, 19.2f, 159.436f, 53.35f, 228.147f)
                lineTo(228.6606f, 610.765f)
                curveToRelative(-10.24f, -30.72f, -15.437f, -62.9f, -15.463f, -95.283f)
            }
            path(
                fill = SolidColor(Color(0xFFEA4335)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(521.2426f, 210.944f)
                arcToRelative(303.36f, 303.36f, 0f, false, true, 191.872f, 67.891f)
                lineToRelative(151.629f, -149.913f)
                curveTo(772.3536f, 49.306f, 653.9006f, 0.077f, 521.2436f, 0.077f)
                curveToRelative(-205.978f, 0f, -383.028f, 116.659f, -467.79f, 287.078f)
                lineTo(228.9416f, 419.79f)
                curveToRelative(40.397f, -121.498f, 155.597f, -208.87f, 292.352f, -208.87f)
            }
            path(
                fill = SolidColor(Color(0xFF34A853)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(523.4706f, 813.235f)
                curveToRelative(-137.473f, 0f, -253.236f, -86.81f, -293.889f, -207.565f)
                lineTo(53.3256f, 737.51f)
                curveToRelative(85.172f, 169.37f, 263.092f, 285.261f, 470.144f, 285.261f)
                curveToRelative(127.719f, 0f, 249.677f, -44.39f, 341.248f, -127.641f)
                lineTo(697.3706f, 768.46f)
                curveToRelative(-47.18f, 29.108f, -106.65f, 44.8f, -173.952f, 44.8f)
            }
            path(
                fill = SolidColor(Color(0xFF4285F4)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(1023.4376f, 510.618f)
                curveToRelative(0f, -30.26f, -4.787f, -62.874f, -11.904f, -93.133f)
                horizontalLineTo(523.4186f)
                verticalLineToRelative(197.888f)
                horizontalLineToRelative(280.935f)
                curveToRelative(-14.004f, 67.481f, -52.224f, 119.347f, -106.957f, 153.088f)
                lineTo(864.7176f, 895.13f)
                curveToRelative(96.18f, -87.373f, 158.72f, -217.55f, 158.72f, -384.512f)
            }
        }.build()
        return _googleIcon!!
    }

private var _googleIcon: ImageVector? = null
