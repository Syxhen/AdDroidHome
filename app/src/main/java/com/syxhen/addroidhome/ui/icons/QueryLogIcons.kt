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
public val dashboard: ImageVector
  get() {
    if (_dashboard != null) {
      return _dashboard!!
    }
    _dashboard =
      ImageVector.Builder(
          name = "dashboard",
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
            moveTo(13f, 9f)
            verticalLineTo(3f)
            horizontalLineToRelative(8f)
            verticalLineTo(9f)
            horizontalLineTo(13f)
            close()
            moveTo(3f, 13f)
            verticalLineTo(3f)
            horizontalLineToRelative(8f)
            verticalLineTo(13f)
            horizontalLineTo(3f)
            close()
            moveToRelative(10f, 8f)
            verticalLineTo(11f)
            horizontalLineToRelative(8f)
            verticalLineTo(21f)
            horizontalLineTo(13f)
            close()
            moveTo(3f, 21f)
            verticalLineTo(15f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(6f)
            horizontalLineTo(3f)
            close()
          }
        }
        .build()
    return _dashboard!!
  }

private var _dashboard: ImageVector? = null

@Suppress("CheckReturnValue")
public val receipt_long: ImageVector
  get() {
    if (_receipt_long != null) {
      return _receipt_long!!
    }
    _receipt_long =
      ImageVector.Builder(
          name = "receipt_long",
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
            moveTo(6f, 22f)
            quadTo(4.75f, 22f, 3.88f, 21.13f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(16f)
            horizontalLineTo(6f)
            verticalLineTo(2f)
            lineTo(7.5f, 3.5f)
            lineTo(9f, 2f)
            lineToRelative(1.5f, 1.5f)
            lineTo(12f, 2f)
            lineToRelative(1.5f, 1.5f)
            lineTo(15f, 2f)
            lineToRelative(1.5f, 1.5f)
            lineTo(18f, 2f)
            lineToRelative(1.5f, 1.5f)
            lineTo(21f, 2f)
            verticalLineTo(19f)
            quadToRelative(0f, 1.25f, -0.88f, 2.13f)
            reflectiveQuadTo(18f, 22f)
            horizontalLineTo(6f)
            close()
            moveTo(18f, 20f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(19f, 19.43f, 19f, 19f)
            verticalLineTo(5f)
            horizontalLineTo(8f)
            verticalLineTo(16f)
            horizontalLineToRelative(9f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.43f, 0.29f, 0.71f)
            reflectiveQuadTo(18f, 20f)
            close()
            moveTo(9f, 9f)
            verticalLineTo(7f)
            horizontalLineToRelative(6f)
            verticalLineTo(9f)
            horizontalLineTo(9f)
            close()
            moveToRelative(0f, 3f)
            verticalLineTo(10f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(2f)
            horizontalLineTo(9f)
            close()
            moveTo(17f, 9f)
            quadTo(16.58f, 9f, 16.29f, 8.71f)
            reflectiveQuadTo(16f, 8f)
            quadTo(16f, 7.57f, 16.29f, 7.29f)
            reflectiveQuadTo(17f, 7f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18f, 8f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(17f, 9f)
            close()
            moveToRelative(0f, 3f)
            quadToRelative(-0.43f, 0f, -0.71f, -0.29f)
            quadTo(16f, 11.43f, 16f, 11f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(17f, 10f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18f, 11f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(17f, 12f)
            close()
          }
        }
        .build()
    return _receipt_long!!
  }

private var _receipt_long: ImageVector? = null

@Suppress("CheckReturnValue")
public val search: ImageVector
  get() {
    if (_search != null) {
      return _search!!
    }
    _search =
      ImageVector.Builder(
          name = "search",
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
            moveTo(19.6f, 21f)
            lineTo(13.3f, 14.7f)
            quadToRelative(-0.75f, 0.6f, -1.72f, 0.95f)
            reflectiveQuadTo(9.5f, 16f)
            quadTo(6.78f, 16f, 4.89f, 14.11f)
            quadTo(3f, 12.23f, 3f, 9.5f)
            quadTo(3f, 6.77f, 4.89f, 4.89f)
            reflectiveQuadTo(9.5f, 3f)
            reflectiveQuadToRelative(4.61f, 1.89f)
            reflectiveQuadTo(16f, 9.5f)
            quadToRelative(0f, 1.1f, -0.35f, 2.07f)
            reflectiveQuadTo(14.7f, 13.3f)
            lineTo(21f, 19.6f)
            lineTo(19.6f, 21f)
            close()
            moveTo(9.5f, 14f)
            quadToRelative(1.88f, 0f, 3.19f, -1.31f)
            reflectiveQuadTo(14f, 9.5f)
            reflectiveQuadTo(12.69f, 6.31f)
            reflectiveQuadTo(9.5f, 5f)
            reflectiveQuadTo(6.31f, 6.31f)
            reflectiveQuadTo(5f, 9.5f)
            reflectiveQuadToRelative(1.31f, 3.19f)
            reflectiveQuadTo(9.5f, 14f)
            close()
          }
        }
        .build()
    return _search!!
  }

private var _search: ImageVector? = null

@Suppress("CheckReturnValue")
public val close: ImageVector
  get() {
    if (_close != null) {
      return _close!!
    }
    _close =
      ImageVector.Builder(
          name = "close",
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
            moveTo(6.4f, 19f)
            lineTo(5f, 17.6f)
            lineTo(10.6f, 12f)
            lineTo(5f, 6.4f)
            lineTo(6.4f, 5f)
            lineTo(12f, 10.6f)
            lineTo(17.6f, 5f)
            lineTo(19f, 6.4f)
            lineTo(13.4f, 12f)
            lineTo(19f, 17.6f)
            lineTo(17.6f, 19f)
            lineTo(12f, 13.4f)
            lineTo(6.4f, 19f)
            close()
          }
        }
        .build()
    return _close!!
  }

private var _close: ImageVector? = null

@Suppress("CheckReturnValue")
public val block: ImageVector
  get() {
    if (_block != null) {
      return _block!!
    }
    _block =
      ImageVector.Builder(
          name = "block",
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
            moveTo(8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            close()
            moveTo(12f, 20f)
            quadToRelative(1.35f, 0f, 2.6f, -0.44f)
            reflectiveQuadTo(16.9f, 18.3f)
            lineTo(5.7f, 7.1f)
            quadTo(4.88f, 8.15f, 4.44f, 9.4f)
            reflectiveQuadTo(4f, 12f)
            quadToRelative(0f, 3.35f, 2.33f, 5.68f)
            reflectiveQuadTo(12f, 20f)
            close()
            moveToRelative(6.3f, -3.1f)
            quadToRelative(0.82f, -1.05f, 1.26f, -2.3f)
            reflectiveQuadTo(20f, 12f)
            quadTo(20f, 8.65f, 17.68f, 6.32f)
            reflectiveQuadTo(12f, 4f)
            quadTo(10.65f, 4f, 9.4f, 4.44f)
            reflectiveQuadTo(7.1f, 5.7f)
            lineTo(18.3f, 16.9f)
            close()
            moveTo(12f, 12f)
            close()
          }
        }
        .build()
    return _block!!
  }

private var _block: ImageVector? = null

@Suppress("CheckReturnValue")
public val check_circle: ImageVector
  get() {
    if (_check_circle != null) {
      return _check_circle!!
    }
    _check_circle =
      ImageVector.Builder(
          name = "check_circle",
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
            moveTo(10.6f, 16.6f)
            lineTo(17.65f, 9.55f)
            lineToRelative(-1.4f, -1.4f)
            lineTo(10.6f, 13.8f)
            lineTo(7.75f, 10.95f)
            lineToRelative(-1.4f, 1.4f)
            lineTo(10.6f, 16.6f)
            close()
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
          }
        }
        .build()
    return _check_circle!!
  }

private var _check_circle: ImageVector? = null
