package com.syxhen.addroidhome.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val gpp_maybe: ImageVector
  get() {
    if (_gpp_maybe != null) {
      return _gpp_maybe!!
    }
    _gpp_maybe =
      ImageVector.Builder(
          name = "gpp_maybe",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.71f, 15.71f)
            quadTo(13f, 15.43f, 13f, 15f)
            reflectiveQuadTo(12.71f, 14.29f)
            reflectiveQuadTo(12f, 14f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(11f, 15f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveTo(11f, 12f)
            horizontalLineToRelative(2f)
            verticalLineTo(7f)
            horizontalLineTo(11f)
            verticalLineToRelative(5f)
            close()
            moveToRelative(1f, 10f)
            quadTo(8.53f, 21.13f, 6.26f, 18.01f)
            reflectiveQuadTo(4f, 11.1f)
            verticalLineTo(5f)
            lineTo(12f, 2f)
            lineToRelative(8f, 3f)
            verticalLineToRelative(6.1f)
            quadToRelative(0f, 3.8f, -2.26f, 6.91f)
            reflectiveQuadTo(12f, 22f)
            close()
          }
        }
        .build()
    return _gpp_maybe!!
  }

private var _gpp_maybe: ImageVector? = null
