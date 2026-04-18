package com.kaizen.khushu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaizen.khushu.ui.theme.ScheherazadeNew

fun Int.toArabicIndic() = this.toString().map { c ->
    when (c) {
        '0' -> '٠'; '1' -> '١'; '2' -> '٢'; '3' -> '٣'; '4' -> '٤'
        '5' -> '٥'; '6' -> '٦'; '7' -> '٧'; '8' -> '٨'; '9' -> '٩'
        else -> c
    }
}.joinToString("")

@Composable
fun AyahEndMarker(number: Int, fg: Color) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // The ornamental Ayah end symbol
        Text(
            text = "\u06DD",
            fontSize = 21.sp,
            lineHeight = 32.sp,
            softWrap = false,
            color = fg.copy(alpha = 0.6f),
            fontFamily = ScheherazadeNew,
            textAlign = TextAlign.Center
        )
        // The number inside
        Text(
            text = number.toArabicIndic(),
            fontSize = 12.sp,
            color = fg,
            fontFamily = ScheherazadeNew,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = 2.dp)
        )
    }
}
