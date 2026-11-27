package com.syxhen.addroidhome.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.syxhen.addroidhome.R
import com.syxhen.addroidhome.ui.theme.AdDroidHomeTheme

/**
 * Renders the AdDroid Home logo using Material 3 tonal colors (`primaryContainer` +
 * `onPrimaryContainer`) so it adapts seamlessly to Light, Dark, and Dynamic Color themes.
 */
@Composable
fun AppLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    shape: Shape = CircleShape,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_monochrome),
            contentDescription = "AdDroid Home logo",
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
            modifier = Modifier
                .fillMaxSize()
                .scale(ADAPTIVE_VIEWPORT_SCALE),
        )
    }
}

private const val ADAPTIVE_VIEWPORT_SCALE = 108f / 72f

@Preview(name = "App Icon M3 Light", showBackground = true)
@Composable
private fun AppIconShowcaseLightPreview() {
    AdDroidHomeTheme(darkTheme = false, dynamicColor = false) {
        Surface {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_background),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(ADAPTIVE_VIEWPORT_SCALE),
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(ADAPTIVE_VIEWPORT_SCALE),
                        )
                    }
                    Text("Launcher", style = MaterialTheme.typography.labelMedium)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppLogoBadge(size = 96.dp, shape = CircleShape)
                    Text("M3 Circle", style = MaterialTheme.typography.labelMedium)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppLogoBadge(size = 96.dp, shape = RoundedCornerShape(24.dp))
                    Text("M3 Squircle", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Preview(name = "App Icon M3 Dark", showBackground = true)
@Composable
private fun AppIconShowcaseDarkPreview() {
    AdDroidHomeTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppLogoBadge(size = 96.dp, shape = CircleShape)
                    Text("M3 Circle", style = MaterialTheme.typography.labelMedium)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppLogoBadge(size = 96.dp, shape = RoundedCornerShape(24.dp))
                    Text("M3 Squircle", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
