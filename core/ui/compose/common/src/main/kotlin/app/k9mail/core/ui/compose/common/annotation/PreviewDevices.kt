package app.k9mail.core.ui.compose.common.annotation

import androidx.compose.ui.tooling.preview.Preview

/**
 * A marker annotation for device previews.
 *
 * It's used to provide previews for a set of different devices and form factors.
 */
@Preview(name = "Small phone", device = "spec:width=360dp,height=640dp,dpi=160")
@Preview(name = "Phone", device = "spec:width=411dp,height=891dp,dpi=420")
@Preview(name = "Phone landscape", device = "spec:width=891dp,height=411dp,dpi=420")
@Preview(name = "Foldable", device = "spec:width=673dp,height=841dp,dpi=420")
@Preview(name = "Tablet", device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(name = "Desktop", device = "spec:width=1920dp,height=1080dp,dpi=160")
annotation class PreviewDevices
