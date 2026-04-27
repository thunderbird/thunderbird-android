package net.thunderbird.feature.thundermail.thunderbird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import kotlin.math.max
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider

private const val LIGHT_BACKGROUND_WIDTH = 1013f
private const val LIGHT_BACKGROUND_HEIGHT = 893f

/**
 * Provides a [BrandBackgroundModifierProvider] that applies a custom Thunderbird-themed
 * brand background to a [Modifier].
 *
 * @return A [BrandBackgroundModifierProvider] containing the modifier with the applied
 * brand background and decorative layers.
 */
@Suppress("MagicNumber", "LongMethod")
@Composable
internal fun Modifier.thunderbirdBrandLight(): Modifier {
    val layer = rememberGraphicsLayer()
    return this then Modifier
        .background(Color(0xFFF0F8FF))
        .drawWithCache {
            onDrawBehind {
                val scale = max(size.width / LIGHT_BACKGROUND_WIDTH, size.height / LIGHT_BACKGROUND_HEIGHT)
                layer.apply {
                    record {
                        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero) {
                            withTransform(
                                transformBlock = {
                                    clipPath(
                                        path = Path().apply {
                                            addRect(
                                                Rect(
                                                    left = 1f,
                                                    top = 0f,
                                                    right = LIGHT_BACKGROUND_WIDTH,
                                                    bottom = LIGHT_BACKGROUND_HEIGHT,
                                                ),
                                            )
                                        },
                                    )
                                },
                            ) {
                                drawPath(
                                    path = Path().apply {
                                        moveTo(x = 1013.0f, y = 0.0f)
                                        lineTo(x = 0.0f, y = 0.0f)
                                        relativeLineTo(dx = 0f, dy = 893.0f)
                                        relativeLineTo(dx = 1013.0f, dy = 0.0f)
                                        close()
                                    },
                                    brush = Brush.radialGradient(
                                        0f to Color(0xFFFFFFFF),
                                        0.25f to Color(0xFAFDFEFF),
                                        0.39f to Color(0xF0F6FCFF),
                                        0.51f to Color(0xE1EBF9FF),
                                        0.62f to Color(0xCADBF4FF),
                                        0.71f to Color(0xB0C6EEFF),
                                        0.8f to Color(0x93ADE6FF),
                                        0.88f to Color(0x728FDDFF),
                                        0.96f to Color(0x536DD3FF),
                                        1f to Color(0x8057CDFF),
                                        center = Offset(x = 506.327f, y = 446.697f),
                                        radius = 769.4702f,
                                    ),
                                    alpha = 0.35f,
                                )
                                drawPath(
                                    path = Path().apply {
                                        moveTo(x = 128.52f, y = 2.52f)
                                        cubicTo(
                                            x1 = 29.68f,
                                            y1 = 116.55f,
                                            x2 = -5.15f,
                                            y2 = 297.64f,
                                            x3 = 33.4f,
                                            y3 = 454.0f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 38.56f,
                                            dy1 = 156.37f,
                                            dx2 = 145.57f,
                                            dy2 = 284.51f,
                                            dx3 = 274.63f,
                                            dy3 = 343.61f,
                                        )
                                        relativeQuadraticTo(dx1 = 277.24f, dy1 = 51.42f, dx2 = 405.55f, dy2 = -9.88f)
                                        relativeQuadraticTo(dx1 = 236.8f, dy1 = -173.84f, dx2 = 313.48f, dy2 = -309.4f)
                                        relativeCubicTo(
                                            dx1 = 7.78f,
                                            dy1 = 52.08f,
                                            dx2 = 15.61f,
                                            dy2 = 104.62f,
                                            dx3 = 14.0f,
                                            dy3 = 157.47f,
                                        )
                                        relativeQuadraticTo(dx1 = -13.42f, dy1 = 106.73f, dx2 = -41.22f, dy2 = 148.31f)
                                        relativeCubicTo(
                                            dx1 = -38.44f,
                                            dy1 = 57.51f,
                                            dx2 = -101.23f,
                                            dy2 = 83.72f,
                                            dx3 = -161.63f,
                                            dy3 = 101.28f,
                                        )
                                        relativeCubicTo(
                                            dx1 = -97.0f,
                                            dy1 = 28.2f,
                                            dx2 = -197.02f,
                                            dy2 = 41.4f,
                                            dx3 = -296.94f,
                                            dy3 = 41.47f,
                                        )
                                        relativeCubicTo(
                                            dx1 = -120.43f,
                                            dy1 = 0.07f,
                                            dx2 = -243.62f,
                                            dy2 = -19.94f,
                                            dx3 = -350.17f,
                                            dy3 = -85.95f,
                                        )
                                        cubicTo(
                                            x1 = 54.55f,
                                            y1 = 756.3f,
                                            x2 = -47.56f,
                                            y2 = 590.57f,
                                            x3 = -57.2f,
                                            y3 = 409.5f,
                                        )
                                        cubicTo(
                                            x1 = -66.88f,
                                            y1 = 228.42f,
                                            x2 = 24.81f,
                                            y2 = 40.9f,
                                            x3 = 168.91f,
                                            y3 = -24.17f,
                                        )
                                        relativeCubicTo(
                                            dx1 = -5.28f,
                                            dy1 = -2.26f,
                                            dx2 = -11.9f,
                                            dy2 = -6.2f,
                                            dx3 = -40.4f,
                                            dy3 = 26.69f,
                                        )
                                    },
                                    brush = Brush.radialGradient(
                                        0f to Color(0xFF7F2FEE),
                                        1f to Color(0xFFFFFFFF),
                                        center = Offset(x = 27.465f, y = 439.832f),
                                        radius = 845.98315f,
                                    ),
                                    alpha = 0.15f,
                                )
                                drawPath(
                                    path = Path().apply {
                                        moveTo(x = 97.29f, y = 7.77f)
                                        cubicTo(
                                            x1 = 26.39f,
                                            y1 = 81.6f,
                                            x2 = 2.59f,
                                            y2 = 207.43f,
                                            x3 = 30.23f,
                                            y3 = 313.92f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 27.62f,
                                            dy1 = 106.49f,
                                            dx2 = 101.45f,
                                            dy2 = 192.0f,
                                            dx3 = 188.63f,
                                            dy3 = 235.54f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 30.53f,
                                            dy1 = 15.24f,
                                            dx2 = 62.63f,
                                            dy2 = 25.81f,
                                            dx3 = 93.26f,
                                            dy3 = 40.78f,
                                        )
                                        relativeQuadraticTo(dx1 = 60.4f, dy1 = 35.05f, dx2 = 81.3f, dy2 = 65.33f)
                                        relativeCubicTo(
                                            dx1 = -53.6f,
                                            dy1 = -51.47f,
                                            dx2 = -125.22f,
                                            dy2 = -66.4f,
                                            dx3 = -193.01f,
                                            dy3 = -82.77f,
                                        )
                                        relativeCubicTo(
                                            dx1 = -67.8f,
                                            dy1 = -16.37f,
                                            dx2 = -139.45f,
                                            dy2 = -38.93f,
                                            dx3 = -185.44f,
                                            dy3 = -99.73f,
                                        )
                                        cubicTo(
                                            x1 = -22.0f,
                                            y1 = 424.19f,
                                            x2 = -36.9f,
                                            y2 = 355.33f,
                                            x3 = -30.81f,
                                            y3 = 290.33f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 6.1f,
                                            dy1 = -65.0f,
                                            dx2 = 31.8f,
                                            dy2 = -126.1f,
                                            dx3 = 66.95f,
                                            dy3 = -176.78f,
                                        )
                                        quadraticTo(x1 = 115.62f, y1 = 21.9f, x2 = 162.32f, y2 = -13.63f)
                                        relativeCubicTo(
                                            dx1 = -18.25f,
                                            dy1 = -0.64f,
                                            dx2 = -39.24f,
                                            dy2 = -5.47f,
                                            dx3 = -65.03f,
                                            dy3 = 21.4f,
                                        )
                                    },
                                    brush = Brush.radialGradient(
                                        0f to Color(0xFF56CCFF),
                                        1f to Color(0x1A8957FF),
                                        center = Offset(x = 84.244f, y = 367.127f),
                                        radius = 274.41757f,
                                    ),
                                    alpha = 0.3f,
                                )
                                drawPath(
                                    path = Path().apply {
                                        moveTo(x = 182.95f, y = -48.97f)
                                        cubicTo(
                                            x1 = 117.15f,
                                            y1 = -19.2f,
                                            x2 = 57.4f,
                                            y2 = 31.4f,
                                            x3 = 17.08f,
                                            y3 = 99.41f,
                                        )
                                        relativeQuadraticTo(dx1 = -60.15f, dy1 = 153.77f, dx2 = -49.2f, dy2 = 235.66f)
                                        cubicTo(
                                            x1 = -21.15f,
                                            y1 = 416.96f,
                                            x2 = 22.0f,
                                            y2 = 493.4f,
                                            x3 = 84.54f,
                                            y3 = 531.69f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 57.38f,
                                            dy1 = 35.14f,
                                            dx2 = 124.84f,
                                            dy2 = 36.88f,
                                            dx3 = 187.42f,
                                            dy3 = 56.17f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 72.36f,
                                            dy1 = 22.31f,
                                            dx2 = 139.86f,
                                            dy2 = 69.97f,
                                            dx3 = 185.98f,
                                            dy3 = 139.2f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 46.11f,
                                            dy1 = 69.21f,
                                            dx2 = 69.48f,
                                            dy2 = 160.42f,
                                            dx3 = 59.28f,
                                            dy3 = 244.39f,
                                        )
                                        relativeCubicTo(
                                            dx1 = -50.3f,
                                            dy1 = -83.36f,
                                            dx2 = -125.0f,
                                            dy2 = -145.98f,
                                            dx3 = -208.44f,
                                            dy3 = -174.74f,
                                        )
                                        relativeCubicTo(
                                            dx1 = -51.16f,
                                            dy1 = -17.64f,
                                            dx2 = -105.17f,
                                            dy2 = -22.9f,
                                            dx3 = -155.52f,
                                            dy3 = -43.55f,
                                        )
                                        relativeCubicTo(
                                            dx1 = -93.1f,
                                            dy1 = -38.22f,
                                            dx2 = -168.13f,
                                            dy2 = -130.14f,
                                            dx3 = -200.54f,
                                            dy3 = -239.61f,
                                        )
                                        relativeCubicTo(
                                            dx1 = -32.42f,
                                            dy1 = -109.47f,
                                            dx2 = -22.84f,
                                            dy2 = -234.19f,
                                            dx3 = 22.18f,
                                            dy3 = -337.33f,
                                        )
                                        cubicTo(
                                            x1 = 19.89f,
                                            y1 = 73.1f,
                                            x2 = 98.97f,
                                            y2 = -7.88f,
                                            x3 = 182.95f,
                                            y3 = -48.97f,
                                        )
                                    },
                                    brush = Brush.radialGradient(
                                        0f to Color(0xFF56CCFF),
                                        1f to Color(0x1A8957FF),
                                        center = Offset(x = 93.804f, y = 532.139f),
                                        radius = 393.89847f,
                                    ),
                                    alpha = 0.3f,
                                )
                                drawPath(
                                    path = Path().apply {
                                        moveTo(x = 3.27f, y = 157.03f)
                                        relativeCubicTo(
                                            dx1 = -17.55f,
                                            dy1 = 147.28f,
                                            dx2 = 50.42f,
                                            dy2 = 303.37f,
                                            dx3 = 162.3f,
                                            dy3 = 372.79f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 56.13f,
                                            dy1 = 34.82f,
                                            dx2 = 120.15f,
                                            dy2 = 48.72f,
                                            dx3 = 177.05f,
                                            dy3 = 81.78f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 65.01f,
                                            dy1 = 37.76f,
                                            dx2 = 119.2f,
                                            dy2 = 100.7f,
                                            dx3 = 152.1f,
                                            dy3 = 176.65f,
                                        )
                                        relativeQuadraticTo(dx1 = 44.33f, dy1 = 164.5f, dx2 = 32.08f, dy2 = 248.49f)
                                        relativeCubicTo(
                                            dx1 = -5.75f,
                                            dy1 = -75.3f,
                                            dx2 = -14.06f,
                                            dy2 = -150.91f,
                                            dx3 = -34.42f,
                                            dy3 = -222.62f,
                                        )
                                        quadraticTo(x1 = 438.83f, y1 = 674.27f, x2 = 389.8f, y2 = 625.37f)
                                        relativeCubicTo(
                                            dx1 = -53.72f,
                                            dy1 = -53.57f,
                                            dx2 = -122.7f,
                                            dy2 = -80.97f,
                                            dx3 = -180.9f,
                                            dy3 = -127.57f,
                                        )
                                        relativeCubicTo(
                                            dx1 = -55.5f,
                                            dy1 = -44.44f,
                                            dx2 = -100.69f,
                                            dy2 = -106.52f,
                                            dx3 = -129.42f,
                                            dy3 = -177.87f,
                                        )
                                        cubicTo(
                                            x1 = 51.55f,
                                            y1 = 250.61f,
                                            x2 = 39.32f,
                                            y2 = 170.0f,
                                            x3 = 57.53f,
                                            y3 = 96.35f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 18.2f,
                                            dy1 = -73.64f,
                                            dx2 = 70.73f,
                                            dy2 = -137.67f,
                                            dx3 = 135.3f,
                                            dy3 = -148.7f,
                                        )
                                    },
                                    brush = Brush.radialGradient(
                                        0f to Color(0xFF56CCFF),
                                        1f to Color(0x1A8957FF),
                                        center = Offset(x = 145.745f, y = 567.859f),
                                        radius = 385.8429f,
                                    ),
                                    alpha = 0.3f,
                                )
                                drawPath(
                                    path = Path().apply {
                                        moveTo(x = 743.95f, y = 901.72f)
                                        relativeCubicTo(
                                            dx1 = 41.82f,
                                            dy1 = -13.57f,
                                            dx2 = 81.32f,
                                            dy2 = -35.4f,
                                            dx3 = 120.58f,
                                            dy3 = -57.12f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 33.04f,
                                            dy1 = -18.28f,
                                            dx2 = 66.28f,
                                            dy2 = -36.7f,
                                            dx3 = 95.9f,
                                            dy3 = -61.78f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 29.61f,
                                            dy1 = -25.08f,
                                            dx2 = 55.72f,
                                            dy2 = -57.5f,
                                            dx3 = 69.73f,
                                            dy3 = -97.13f,
                                        )
                                        relativeCubicTo(
                                            dx1 = -0.43f,
                                            dy1 = 40.56f,
                                            dx2 = -16.39f,
                                            dy2 = 79.54f,
                                            dx3 = -39.67f,
                                            dy3 = 109.47f,
                                        )
                                        relativeQuadraticTo(dx1 = -53.46f, dy1 = 51.46f, dx2 = -85.08f, dy2 = 67.73f)
                                        relativeCubicTo(
                                            dx1 = -54.83f,
                                            dy1 = 28.2f,
                                            dx2 = -115.18f,
                                            dy2 = 41.48f,
                                            dx3 = -161.46f,
                                            dy3 = 38.83f,
                                        )
                                    },
                                    brush = Brush.radialGradient(
                                        0f to Color(0xFF7F2FEE),
                                        1f to Color(0xFFFFFFFF),
                                        center = Offset(x = 766.183f, y = 791.421f),
                                        radius = 253.09709f,
                                    ),
                                    alpha = 0.1f,
                                )
                                drawPath(
                                    path = Path().apply {
                                        moveTo(x = 242.7f, y = -4.84f)
                                        cubicTo(
                                            x1 = 184.23f,
                                            y1 = 37.01f,
                                            x2 = 142.66f,
                                            y2 = 107.56f,
                                            x3 = 122.9f,
                                            y3 = 184.6f,
                                        )
                                        relativeQuadraticTo(dx1 = -18.8f, dy1 = 160.1f, dx2 = -4.5f, dy2 = 238.79f)
                                        relativeCubicTo(
                                            dx1 = 22.81f,
                                            dy1 = 125.4f,
                                            dx2 = 79.71f,
                                            dy2 = 241.94f,
                                            dx3 = 160.34f,
                                            dy3 = 328.34f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 80.62f,
                                            dy1 = 86.4f,
                                            dx2 = 184.6f,
                                            dy2 = 142.25f,
                                            dx3 = 292.94f,
                                            dy3 = 157.37f,
                                        )
                                        cubicTo(
                                            x1 = 387.03f,
                                            y1 = 928.7f,
                                            x2 = 195.0f,
                                            y2 = 835.86f,
                                            x3 = 80.27f,
                                            y3 = 664.67f,
                                        )
                                        cubicTo(
                                            x1 = -34.47f,
                                            y1 = 493.5f,
                                            x2 = -65.73f,
                                            y2 = 248.21f,
                                            x3 = 6.79f,
                                            y3 = 47.66f,
                                        )
                                        cubicTo(
                                            x1 = 16.7f,
                                            y1 = 20.2f,
                                            x2 = 29.02f,
                                            y2 = -7.24f,
                                            x3 = 49.0f,
                                            y3 = -25.6f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 36.65f,
                                            dy1 = -33.67f,
                                            dx2 = 88.3f,
                                            dy2 = -28.56f,
                                            dx3 = 134.46f,
                                            dy3 = -21.79f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 14.0f,
                                            dy1 = 2.06f,
                                            dx2 = 28.59f,
                                            dy2 = 4.36f,
                                            dx3 = 40.27f,
                                            dy3 = 13.65f,
                                        )
                                        relativeCubicTo(
                                            dx1 = 11.7f,
                                            dy1 = 9.29f,
                                            dx2 = 19.35f,
                                            dy2 = 27.86f,
                                            dx3 = 18.97f,
                                            dy3 = 28.9f,
                                        )
                                    },
                                    brush = Brush.radialGradient(
                                        0f to Color(0xFF56CCFF),
                                        1f to Color(0xFFFFFFFF),
                                        center = Offset(x = 269.775f, y = 429.667f),
                                        radius = 530.3197f,
                                    ),
                                    alpha = 0.25f,
                                )
                            }
                        }
                    }
                    renderEffect = BlurEffect(1f, 1f)
                    alpha = 0.35f
                }
                drawLayer(layer)
            }
        }
}

@PreviewScreenSizes
@Composable
private fun Preview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .thunderbirdBrandLight(),
    )
}
