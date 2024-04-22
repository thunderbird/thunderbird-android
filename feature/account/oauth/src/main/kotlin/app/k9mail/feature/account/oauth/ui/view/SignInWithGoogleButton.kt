package app.k9mail.feature.account.oauth.ui.view

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.k9mail.feature.account.oauth.R
import androidx.compose.material.Button as MaterialButton

/**
 * A sign in with Google button, following the Google Branding Guidelines.
 *
 * @see [Google Branding Guidelines](https://developers.google.com/identity/branding-guidelines)
 */
@Suppress("LongMethod")
@Composable
fun SignInWithGoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLight: Boolean = MaterialTheme.colors.isLight,
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            contentColor = getTextColor(isLight),
            backgroundColor = getSurfaceColor(isLight),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = getBorderColor(isLight),
        ),
        contentPadding = PaddingValues(all = 0.dp),
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing,
                    ),
                )
                .padding(
                    end = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = Color.White,
            ) {
                Icon(
                    modifier = Modifier
                        .padding(8.dp),
                    painter = painterResource(
                        id = R.drawable.account_oauth_ic_google_logo,
                    ),
                    contentDescription = "Google logo",
                    tint = Color.Unspecified,
                )
            }
            Spacer(modifier = Modifier.requiredWidth(8.dp))
            Text(
                text = stringResource(
                    id = R.string.account_oauth_sign_in_with_google_button,
                ),
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    letterSpacing = 1.25.sp,
                ),
            )
        }
    }
}

@Suppress("MagicNumber")
private fun getBorderColor(isLight: Boolean): Color {
    return if (isLight) {
        Color(0x87000000)
    } else {
        Color(0xFF4285F4)
    }
}

@Suppress("MagicNumber")
private fun getSurfaceColor(isLight: Boolean): Color {
    return if (isLight) {
        Color(0xFFFFFFFF)
    } else {
        Color(0xFF4285F4)
    }
}

@Suppress("MagicNumber")
private fun getTextColor(isLight: Boolean): Color {
    return if (isLight) {
        Color(0x87000000)
    } else {
        Color(0xFFFFFFFF)
    }
}
