package org.lsposed.lspatch.ui.component

import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import org.lsposed.lspatch.ui.theme.LSPTheme
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.interfaces.HoldDownInteraction
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppItem(
    modifier: Modifier = Modifier,
    icon: ImageBitmap,
    label: String,
    packageName: String,
    checked: Boolean? = null,
    rightIcon: (@Composable () -> Unit)? = null,
    additionalContent: (@Composable ColumnScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    if (checked != null && rightIcon != null)
        throw IllegalArgumentException("`checked` and `rightIcon` should not be both set")

    val holdDown = remember { mutableStateOf<HoldDownInteraction.HoldDown?>(null) }
    val currentOnClick by rememberUpdatedState(onClick)

    LaunchedEffect(holdDownState) {
        if (holdDownState) {
            val interaction = HoldDownInteraction.HoldDown()
            holdDown.value = interaction
            interactionSource.emit(interaction)
        } else {
            holdDown.value?.let { oldValue ->
                interactionSource.emit(HoldDownInteraction.Release(oldValue))
                holdDown.value = null
            }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (currentOnClick != null && enabled) {
                    Modifier.clickable(
                        indication = LocalIndication.current,
                        interactionSource = interactionSource,
                        onClick = { currentOnClick?.invoke() }
                    )
                } else Modifier
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            bitmap = icon,
            contentDescription = label,
            tint = Color.Unspecified
        )
        Column(
            modifier = Modifier.padding(start = 12.dp),
        ) {
            Text(
                color = MiuixTheme.colorScheme.onSurface,
                text = label,
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = packageName,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            additionalContent?.invoke(this)
        }
        if (checked != null) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
        }
        if (rightIcon != null) {
            rightIcon()
        }
    }
}

@Preview
@Composable
private fun AppItemPreview() {
    LSPTheme {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.setColor(MiuixTheme.colorScheme.primary.toArgb())
        AppItem(
            icon = shape.toBitmap().asImageBitmap(),
            label = "Sample App",
            packageName = "org.lsposed.sample",
        )
    }
}
