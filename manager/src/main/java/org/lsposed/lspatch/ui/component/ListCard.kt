package org.lsposed.lspatch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.utils.CornerSmoothness
import top.yukonga.miuix.kmp.utils.G2RoundedCornerShape

@Composable
fun ListCard(
    modifier: Modifier = Modifier,
    index: Int,
    size: Int,
    content: @Composable () -> Unit
) {
    val cornerRadius = CardDefaults.CornerRadius
    val shape = remember(cornerRadius) {
        when {
            size == 1 -> G2RoundedCornerShape(16.dp)
            index == 0 -> G2RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = cornerRadius,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )

            index == size - 1 -> G2RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = cornerRadius,
                bottomEnd = cornerRadius
            )

            else -> RoundedCornerShape(0.dp)
        }
    }
    val clipShape = remember(cornerRadius) {
        when {
            size == 1 -> G2RoundedCornerShape(16.dp, CornerSmoothness.None)
            index == 0 -> G2RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = cornerRadius,
                bottomStart = 0.dp,
                bottomEnd = 0.dp,
                cornerSmoothness = CornerSmoothness.None
            )

            index == size - 1 -> G2RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = cornerRadius,
                bottomEnd = cornerRadius,
                cornerSmoothness = CornerSmoothness.None
            )

            else -> G2RoundedCornerShape(0.dp, CornerSmoothness.None)
        }
    }
    Box(
        modifier = modifier
            .background(color = CardDefaults.defaultColors().color, shape = shape)
            .clip(clipShape),
    ) {
        content()
    }
}
