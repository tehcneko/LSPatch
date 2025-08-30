package org.lsposed.lspatch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text

@Composable
public fun StatusTag(label: String, backgroundColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .background(
                color = backgroundColor.copy(alpha = 0.8f),
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            text = label,
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}