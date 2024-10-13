@file:Suppress("MagicNumber")

package app.k9mail.feature.funding.googleplay.ui.contribution.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val GoldenHearthSunburst: ImageVector
    get() {
        if (goldenInstance != null) {
            return goldenInstance!!
        }
        goldenInstance = createInstance(
            name = "GoldenHearthSunburst",
            hearthColor = Color(0xFFFFC107),
            hearthOutline = Color(0xFFFFA500),
        )

        return goldenInstance!!
    }

val HearthSunburst: ImageVector
    get() {
        if (instance != null) {
            return instance!!
        }
        instance = createInstance(
            name = "HearthSunburst",
            hearthColor = Color(0xFFEF4444),
            hearthOutline = Color(0xFFB91C1C),
        )

        return instance!!
    }

@Suppress("LongMethod")
private fun createInstance(
    name: String,
    defaultWidth: Dp = 270.dp,
    defaultHeight: Dp = 204.dp,
    viewportWidth: Float = 270f,
    viewportHeight: Float = 204f,
    hearthColor: Color,
    hearthOutline: Color,
): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = defaultWidth,
        defaultHeight = defaultHeight,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
    ).apply {
        path(fill = SolidColor(hearthColor)) {
            moveTo(120.38f, 18.74f)
            curveTo(120.1f, 18.74f, 119.82f, 18.88f, 119.66f, 19.15f)
            curveTo(119.43f, 19.54f, 119.64f, 19.76f, 119.92f, 20.26f)
            curveTo(121.35f, 22.8f, 121.49f, 21.82f, 121.01f, 19.95f)
            curveTo(120.87f, 19.41f, 121.09f, 19.05f, 120.75f, 18.85f)
            curveTo(120.64f, 18.78f, 120.51f, 18.74f, 120.38f, 18.74f)
            close()
            moveTo(117.56f, 19.82f)
            curveTo(117.25f, 19.82f, 117.11f, 20.29f, 117.44f, 21.18f)
            curveTo(117.94f, 22.48f, 119.4f, 22.63f, 118.62f, 20.97f)
            curveTo(118.35f, 20.4f, 118.05f, 20.05f, 117.81f, 19.9f)
            curveTo(117.72f, 19.85f, 117.63f, 19.83f, 117.56f, 19.82f)
            close()
            moveTo(150.85f, 20.17f)
            curveTo(150.72f, 20.17f, 150.59f, 20.21f, 150.48f, 20.27f)
            curveTo(150.14f, 20.48f, 150.36f, 20.84f, 150.22f, 21.38f)
            curveTo(149.74f, 23.25f, 149.88f, 24.23f, 151.31f, 21.68f)
            curveTo(151.59f, 21.19f, 151.8f, 20.97f, 151.57f, 20.58f)
            curveTo(151.41f, 20.31f, 151.13f, 20.16f, 150.85f, 20.17f)
            close()
            moveTo(114.53f, 20.85f)
            curveTo(114.24f, 20.86f, 113.93f, 21.05f, 113.77f, 21.31f)
            curveTo(113.53f, 21.72f, 113.67f, 22.24f, 114.07f, 22.48f)
            curveTo(114.48f, 22.72f, 115.86f, 23.35f, 116.27f, 23.14f)
            curveTo(116.62f, 22.96f, 115.57f, 21.71f, 114.94f, 21.02f)
            curveTo(114.9f, 20.98f, 114.85f, 20.94f, 114.81f, 20.92f)
            curveTo(114.72f, 20.87f, 114.63f, 20.85f, 114.53f, 20.85f)
            close()
            moveTo(153.67f, 21.25f)
            curveTo(153.6f, 21.25f, 153.51f, 21.28f, 153.42f, 21.33f)
            curveTo(153.18f, 21.48f, 152.88f, 21.83f, 152.61f, 22.4f)
            curveTo(151.83f, 24.06f, 153.29f, 23.91f, 153.79f, 22.61f)
            curveTo(154.12f, 21.72f, 153.98f, 21.25f, 153.67f, 21.25f)
            close()
            moveTo(156.7f, 22.28f)
            curveTo(156.6f, 22.28f, 156.51f, 22.3f, 156.42f, 22.35f)
            curveTo(156.38f, 22.37f, 156.33f, 22.41f, 156.29f, 22.45f)
            curveTo(155.66f, 23.14f, 154.61f, 24.39f, 154.96f, 24.57f)
            curveTo(155.37f, 24.78f, 156.75f, 24.15f, 157.15f, 23.91f)
            curveTo(157.56f, 23.67f, 157.7f, 23.14f, 157.46f, 22.74f)
            curveTo(157.3f, 22.48f, 156.99f, 22.28f, 156.7f, 22.28f)
            close()
            moveTo(113.29f, 24.11f)
            curveTo(113.01f, 24.12f, 112.69f, 24.32f, 112.55f, 24.55f)
            curveTo(112.33f, 24.93f, 112.47f, 25.43f, 112.87f, 25.66f)
            curveTo(113.27f, 25.9f, 115.03f, 26f, 115.21f, 25.6f)
            curveTo(115.39f, 25.21f, 114.01f, 24.9f, 113.67f, 24.3f)
            curveTo(113.64f, 24.24f, 113.59f, 24.19f, 113.54f, 24.16f)
            curveTo(113.46f, 24.12f, 113.38f, 24.1f, 113.29f, 24.11f)
            close()
            moveTo(157.94f, 25.53f)
            curveTo(157.85f, 25.53f, 157.77f, 25.55f, 157.69f, 25.59f)
            curveTo(157.64f, 25.62f, 157.59f, 25.67f, 157.56f, 25.73f)
            curveTo(157.21f, 26.32f, 155.84f, 26.64f, 156.02f, 27.03f)
            curveTo(156.2f, 27.43f, 157.96f, 27.33f, 158.36f, 27.09f)
            curveTo(158.76f, 26.85f, 158.9f, 26.36f, 158.68f, 25.98f)
            curveTo(158.54f, 25.75f, 158.22f, 25.55f, 157.94f, 25.53f)
            close()
            moveTo(113.96f, 27.5f)
            curveTo(113.74f, 27.51f, 113.55f, 27.54f, 113.38f, 27.72f)
            curveTo(113.06f, 28.06f, 113.11f, 28.44f, 113.58f, 28.54f)
            curveTo(113.9f, 28.6f, 114.14f, 28.53f, 114.39f, 28.32f)
            curveTo(114.79f, 27.98f, 114.82f, 27.73f, 114.61f, 27.6f)
            curveTo(114.52f, 27.55f, 114.37f, 27.51f, 114.19f, 27.51f)
            curveTo(114.11f, 27.5f, 114.03f, 27.5f, 113.96f, 27.5f)
            close()
            moveTo(157.27f, 28.93f)
            curveTo(157.2f, 28.93f, 157.12f, 28.93f, 157.04f, 28.94f)
            curveTo(156.86f, 28.94f, 156.71f, 28.97f, 156.62f, 29.03f)
            curveTo(156.41f, 29.16f, 156.43f, 29.41f, 156.84f, 29.75f)
            curveTo(157.09f, 29.96f, 157.33f, 30.03f, 157.65f, 29.96f)
            curveTo(158.12f, 29.87f, 158.17f, 29.49f, 157.85f, 29.15f)
            curveTo(157.68f, 28.97f, 157.49f, 28.93f, 157.27f, 28.93f)
            close()
            moveTo(143.51f, 48.26f)
            curveTo(142.89f, 48.3f, 142.71f, 49.43f, 141.91f, 50.3f)
            curveTo(140.81f, 51.5f, 139.65f, 52.62f, 140.7f, 53.15f)
            curveTo(142.39f, 53.97f, 144.52f, 48.64f, 143.68f, 48.28f)
            curveTo(143.67f, 48.28f, 143.65f, 48.27f, 143.64f, 48.27f)
            curveTo(143.59f, 48.26f, 143.55f, 48.26f, 143.51f, 48.26f)
            close()
        }
        path(
            fill = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to hearthColor,
                    1f to hearthColor.copy(alpha = 0f),
                ),
                center = Offset(134.18f, 39.04f),
                radius = 155.24f,
            ),
            fillAlpha = 0.15f,
            strokeAlpha = 0.15f,
        ) {
            moveTo(10.03f, 9.37f)
            curveTo(-6.4f, 9.37f, -0.97f, 89.68f, 14.26f, 85.6f)
            lineTo(97.33f, 37.63f)
            curveTo(94.06f, 29.77f, 94.39f, 26.19f, 96.7f, 18.26f)
            lineTo(10.03f, 9.37f)
            close()
            moveTo(258.49f, 9.37f)
            lineTo(171.82f, 18.26f)
            curveTo(174.7f, 26.17f, 173.89f, 29.99f, 171.19f, 37.63f)
            lineTo(254.26f, 85.6f)
            curveTo(274.78f, 91.1f, 273.85f, 9.37f, 258.49f, 9.37f)
            close()
            moveTo(105.07f, 47.53f)
            lineTo(25.02f, 146.39f)
            curveTo(6.37f, 178.71f, 66.1f, 215.9f, 85.07f, 183.03f)
            lineTo(117.92f, 60.39f)
            lineTo(105.07f, 47.53f)
            close()
            moveTo(163.45f, 47.53f)
            lineTo(150.59f, 60.39f)
            lineTo(183.45f, 183.03f)
            curveTo(207.39f, 224.52f, 273.35f, 198.12f, 243.49f, 146.39f)
            lineTo(163.45f, 47.53f)
            close()
        }
        path(
            fill = SolidColor(hearthOutline),
            fillAlpha = 0.2f,
            strokeAlpha = 0.2f,
        ) {
            moveTo(161.54f, 39.01f)
            curveTo(164.61f, 35.95f, 166.16f, 32.4f, 166.16f, 28.11f)
            curveTo(166.16f, 23.82f, 164.61f, 19.38f, 161.54f, 16.31f)
            curveTo(158.47f, 13.25f, 154.47f, 11.71f, 150.46f, 11.71f)
            curveTo(146.46f, 11.71f, 142.45f, 14.33f, 139.38f, 17.39f)
            lineTo(135.69f, 21.08f)
            lineTo(132f, 17.39f)
            curveTo(128.93f, 14.33f, 124.92f, 11.71f, 120.92f, 11.71f)
            curveTo(116.91f, 11.71f, 112.91f, 13.25f, 109.84f, 16.31f)
            curveTo(106.77f, 19.38f, 105.25f, 23.82f, 105.25f, 28.11f)
            curveTo(105.25f, 32.4f, 106.77f, 35.95f, 109.84f, 39.01f)
            curveTo(117.43f, 46.42f, 124.46f, 53.41f, 131.63f, 61.22f)
            curveTo(132.65f, 62.24f, 134.17f, 63.25f, 135.69f, 63.25f)
            curveTo(137.21f, 63.25f, 138.73f, 62.24f, 139.75f, 61.22f)
            curveTo(146.77f, 53.28f, 154.04f, 46.51f, 161.54f, 39.01f)
            close()
        }
        path(fill = SolidColor(hearthOutline)) {
            moveTo(120.92f, 9.37f)
            curveTo(116.32f, 9.37f, 111.69f, 11.15f, 108.18f, 14.66f)
            curveTo(104.58f, 18.25f, 102.91f, 23.27f, 102.91f, 28.11f)
            curveTo(102.91f, 32.96f, 104.75f, 37.24f, 108.18f, 40.68f)
            curveTo(108.19f, 40.68f, 108.19f, 40.69f, 108.2f, 40.69f)
            curveTo(115.78f, 48.09f, 122.78f, 55.04f, 129.9f, 62.8f)
            curveTo(129.93f, 62.83f, 129.95f, 62.85f, 129.98f, 62.88f)
            curveTo(131.27f, 64.16f, 133.13f, 65.59f, 135.69f, 65.59f)
            curveTo(138.25f, 65.59f, 140.12f, 64.16f, 141.41f, 62.88f)
            curveTo(141.44f, 62.84f, 141.47f, 62.81f, 141.5f, 62.77f)
            curveTo(148.43f, 54.94f, 155.66f, 48.2f, 163.2f, 40.68f)
            curveTo(166.62f, 37.25f, 168.5f, 32.97f, 168.5f, 28.11f)
            curveTo(168.5f, 23.25f, 166.78f, 18.25f, 163.2f, 14.66f)
            curveTo(159.68f, 11.15f, 155.06f, 9.37f, 150.46f, 9.37f)
            curveTo(145.43f, 9.37f, 141.05f, 12.42f, 137.73f, 15.74f)
            lineTo(135.69f, 17.77f)
            lineTo(133.66f, 15.74f)
            curveTo(130.34f, 12.42f, 125.94f, 9.37f, 120.92f, 9.37f)
            close()
            moveTo(120.92f, 14.06f)
            curveTo(123.9f, 14.06f, 127.53f, 16.24f, 130.34f, 19.05f)
            lineTo(134.03f, 22.74f)
            curveTo(134.47f, 23.18f, 135.07f, 23.43f, 135.69f, 23.43f)
            curveTo(136.31f, 23.43f, 136.9f, 23.18f, 137.34f, 22.74f)
            lineTo(141.04f, 19.05f)
            curveTo(143.86f, 16.24f, 147.47f, 14.06f, 150.46f, 14.06f)
            curveTo(153.86f, 14.06f, 157.26f, 15.35f, 159.88f, 17.97f)
            curveTo(162.43f, 20.52f, 163.82f, 24.38f, 163.82f, 28.11f)
            curveTo(163.82f, 31.84f, 162.59f, 34.65f, 159.88f, 37.35f)
            curveTo(152.46f, 44.77f, 145.18f, 51.56f, 138.09f, 59.56f)
            curveTo(137.36f, 60.3f, 136.17f, 60.91f, 135.69f, 60.91f)
            curveTo(135.21f, 60.91f, 134.03f, 60.3f, 133.29f, 59.56f)
            curveTo(126.1f, 51.74f, 119.07f, 44.74f, 111.49f, 37.35f)
            curveTo(108.79f, 34.65f, 107.6f, 31.85f, 107.6f, 28.11f)
            curveTo(107.6f, 24.37f, 108.95f, 20.51f, 111.49f, 17.97f)
            curveTo(114.12f, 15.35f, 117.51f, 14.06f, 120.92f, 14.06f)
            close()
        }
    }.build()
}

private var goldenInstance: ImageVector? = null
private var instance: ImageVector? = null
