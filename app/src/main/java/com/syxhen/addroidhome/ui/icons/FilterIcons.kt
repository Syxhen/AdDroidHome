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
public val filter_list: ImageVector
  get() {
    if (_filter_list != null) {
      return _filter_list!!
    }
    _filter_list =
      ImageVector.Builder(
          name = "filter_list",
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
            moveTo(10f, 18f)
            verticalLineTo(16f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(2f)
            horizontalLineTo(10f)
            close()
            moveTo(6f, 13f)
            verticalLineTo(11f)
            horizontalLineTo(18f)
            verticalLineToRelative(2f)
            horizontalLineTo(6f)
            close()
            moveTo(3f, 8f)
            verticalLineTo(6f)
            horizontalLineTo(21f)
            verticalLineTo(8f)
            horizontalLineTo(3f)
            close()
          }
        }
        .build()
    return _filter_list!!
  }

private var _filter_list: ImageVector? = null

@Suppress("CheckReturnValue")
public val add: ImageVector
  get() {
    if (_add != null) {
      return _add!!
    }
    _add =
      ImageVector.Builder(
          name = "add",
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
            moveTo(11f, 13f)
            horizontalLineTo(5f)
            verticalLineTo(11f)
            horizontalLineToRelative(6f)
            verticalLineTo(5f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(6f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(2f)
            horizontalLineTo(13f)
            verticalLineToRelative(6f)
            horizontalLineTo(11f)
            verticalLineTo(13f)
            close()
          }
        }
        .build()
    return _add!!
  }

private var _add: ImageVector? = null

@Suppress("CheckReturnValue")
public val delete: ImageVector
  get() {
    if (_delete != null) {
      return _delete!!
    }
    _delete =
      ImageVector.Builder(
          name = "delete",
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
            moveTo(7f, 21f)
            quadTo(6.18f, 21f, 5.59f, 20.41f)
            reflectiveQuadTo(5f, 19f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            verticalLineTo(4f)
            horizontalLineTo(9f)
            verticalLineTo(3f)
            horizontalLineToRelative(6f)
            verticalLineTo(4f)
            horizontalLineToRelative(5f)
            verticalLineTo(6f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(17f, 21f)
            horizontalLineTo(7f)
            close()
            moveTo(17f, 6f)
            horizontalLineTo(7f)
            verticalLineTo(19f)
            horizontalLineTo(17f)
            verticalLineTo(6f)
            close()
            moveTo(9f, 17f)
            horizontalLineToRelative(2f)
            verticalLineTo(8f)
            horizontalLineTo(9f)
            verticalLineToRelative(9f)
            close()
            moveToRelative(4f, 0f)
            horizontalLineToRelative(2f)
            verticalLineTo(8f)
            horizontalLineTo(13f)
            verticalLineToRelative(9f)
            close()
            moveTo(7f, 6f)
            verticalLineTo(19f)
            verticalLineTo(6f)
            close()
          }
        }
        .build()
    return _delete!!
  }

private var _delete: ImageVector? = null

@Suppress("CheckReturnValue")
public val save: ImageVector
  get() {
    if (_save != null) {
      return _save!!
    }
    _save =
      ImageVector.Builder(
          name = "save",
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
            moveTo(21f, 7f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(17f)
            lineToRelative(4f, 4f)
            close()
            moveTo(19f, 7.85f)
            lineTo(16.15f, 5f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            horizontalLineTo(19f)
            verticalLineTo(7.85f)
            close()
            moveToRelative(-4.88f, 9.28f)
            quadTo(15f, 16.25f, 15f, 15f)
            reflectiveQuadTo(14.13f, 12.88f)
            reflectiveQuadTo(12f, 12f)
            reflectiveQuadTo(9.88f, 12.88f)
            reflectiveQuadTo(9f, 15f)
            reflectiveQuadToRelative(0.88f, 2.13f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadToRelative(2.13f, -0.88f)
            close()
            moveTo(6f, 10f)
            horizontalLineToRelative(9f)
            verticalLineTo(6f)
            horizontalLineTo(6f)
            verticalLineToRelative(4f)
            close()
            moveTo(5f, 7.85f)
            verticalLineTo(19f)
            verticalLineTo(5f)
            verticalLineTo(7.85f)
            close()
          }
        }
        .build()
    return _save!!
  }

private var _save: ImageVector? = null
