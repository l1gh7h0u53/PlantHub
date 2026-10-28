package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Official Google Material Symbols Outlined 24dp Cannabis icon
 * Source: https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/cannabis.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50
 */
public val CannabisSymbol: ImageVector
    get() {
        if (_cannabis != null) {
            return _cannabis!!
        }
        _cannabis = ImageVector.Builder(
            name = "cannabis",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(11f, 22f)
                verticalLineTo(17.83f)
                quadTo(9.95f, 18.5f, 8.84f, 19f)
                reflectiveQuadTo(6.5f, 19.5f)
                quadToRelative(-1.57f, 0f, -2.96f, -0.71f)
                reflectiveQuadTo(1f, 17f)
                quadTo(1.93f, 16.15f, 2.99f, 15.51f)
                reflectiveQuadTo(5.28f, 14.63f)
                quadToRelative(-1.7f, -1.38f, -2.5f, -3.4f)
                reflectiveQuadTo(1.98f, 7f)
                quadToRelative(1.8f, 0.02f, 3.48f, 0.57f)
                reflectiveQuadTo(8.5f, 9.2f)
                quadToRelative(0f, -0.05f, 0f, -0.1f)
                reflectiveQuadTo(8.5f, 9f)
                quadTo(8.5f, 6.72f, 9.51f, 4.72f)
                reflectiveQuadTo(12f, 1f)
                quadToRelative(1.45f, 1.72f, 2.48f, 3.72f)
                reflectiveQuadTo(15.5f, 9f)
                quadToRelative(0f, 0.05f, -0.01f, 0.1f)
                reflectiveQuadTo(15.48f, 9.2f)
                quadToRelative(1.4f, -1.07f, 3.07f, -1.61f)
                reflectiveQuadTo(22.03f, 7f)
                quadToRelative(0f, 2.2f, -0.81f, 4.22f)
                reflectiveQuadToRelative(-2.51f, 3.4f)
                quadToRelative(1.23f, 0.25f, 2.29f, 0.89f)
                reflectiveQuadTo(22.98f, 17f)
                quadToRelative(-1.15f, 1.07f, -2.52f, 1.79f)
                reflectiveQuadTo(17.5f, 19.5f)
                quadToRelative(-1.25f, 0f, -2.36f, -0.5f)
                reflectiveQuadTo(13f, 17.83f)
                verticalLineTo(22f)
                horizontalLineTo(11f)
                close()

                moveTo(9.6f, 14.6f)
                quadTo(9.33f, 13.65f, 8.89f, 12.79f)
                reflectiveQuadTo(7.75f, 11.25f)
                quadTo(7.05f, 10.55f, 6.19f, 10.11f)
                quadTo(5.33f, 9.67f, 4.38f, 9.4f)
                quadToRelative(0.28f, 0.95f, 0.71f, 1.81f)
                quadToRelative(0.44f, 0.86f, 1.14f, 1.56f)
                quadToRelative(0.67f, 0.7f, 1.55f, 1.14f)
                reflectiveQuadTo(9.6f, 14.6f)
                close()

                moveTo(6.5f, 17.5f)
                quadToRelative(0.53f, 0f, 1.03f, -0.14f)
                reflectiveQuadTo(8.5f, 17f)
                quadTo(8.03f, 16.8f, 7.53f, 16.65f)
                reflectiveQuadTo(6.5f, 16.5f)
                reflectiveQuadTo(5.48f, 16.65f)
                reflectiveQuadTo(4.48f, 17f)
                quadToRelative(0.47f, 0.23f, 0.99f, 0.36f)
                reflectiveQuadTo(6.5f, 17.5f)
                close()

                moveTo(12f, 13.7f)
                quadToRelative(0.65f, -1.1f, 1.08f, -2.26f)
                quadTo(13.5f, 10.27f, 13.5f, 9f)
                quadToRelative(0f, -1.28f, -0.42f, -2.44f)
                reflectiveQuadTo(12f, 4.32f)
                quadTo(11.35f, 5.4f, 10.93f, 6.56f)
                quadTo(10.5f, 7.72f, 10.5f, 9f)
                quadToRelative(0f, 1.27f, 0.43f, 2.45f)
                reflectiveQuadTo(12f, 13.7f)
                close()

                moveToRelative(2.4f, 0.9f)
                quadToRelative(0.95f, -0.25f, 1.81f, -0.69f)
                reflectiveQuadToRelative(1.54f, -1.14f)
                quadToRelative(0.7f, -0.7f, 1.14f, -1.56f)
                reflectiveQuadTo(19.6f, 9.4f)
                quadToRelative(-0.95f, 0.28f, -1.81f, 0.71f)
                reflectiveQuadToRelative(-1.56f, 1.14f)
                quadToRelative(-0.7f, 0.67f, -1.14f, 1.54f)
                reflectiveQuadTo(14.4f, 14.6f)
                close()

                moveToRelative(3.1f, 2.9f)
                quadToRelative(0.53f, 0f, 1.03f, -0.14f)
                reflectiveQuadTo(19.5f, 17f)
                quadTo(19.03f, 16.8f, 18.53f, 16.65f)
                reflectiveQuadTo(17.5f, 16.5f)
                quadToRelative(-0.52f, 0f, -1.02f, 0.15f)
                reflectiveQuadToRelative(-1f, 0.35f)
                quadToRelative(0.47f, 0.23f, 0.99f, 0.36f)
                reflectiveQuadTo(17.5f, 17.5f)
                close()
            }
        }.build()
        return _cannabis!!
    }

private var _cannabis: ImageVector? = null
