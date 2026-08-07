package com.ludoven.adbtool.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.Surface
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.ui.mac.bodyMedium
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedbackToast(message: MsgContent?) {
    if (message == null) return

    val text = when (message) {
        is MsgContent.Resource -> stringResource(message.stringResource, *message.args.toTypedArray())
        is MsgContent.Text -> message.text
    }
    if (text.isBlank()) return

    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(focusable = false)
    ) {
        Surface(
            shape = RoundedCornerShape(UiTokens.RadiusLarge),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .padding(bottom = UiTokens.SpaceXXLarge)
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(
                    horizontal = UiTokens.SpaceXLarge,
                    vertical = UiTokens.SpaceMedium
                ),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
