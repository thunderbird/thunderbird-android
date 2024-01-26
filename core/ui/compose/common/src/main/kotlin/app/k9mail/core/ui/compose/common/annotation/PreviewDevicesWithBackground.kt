package app.k9mail.core.ui.compose.common.annotation

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * A marker annotation for device previews with background.
 *
 * It's used to provide previews for a set of different devices and form factors.
 */
@Preview(name = "Small phone", device = "spec:shape=Normal,width=360,height=640,unit=dp,dpi=160", showBackground = true)
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(
    name = "Phone landscape",
    device = "spec:shape=Normal,width=891,height=411,unit=dp,dpi=420",
    showBackground = true,
)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
annotation class PreviewDevicesWithBackground
