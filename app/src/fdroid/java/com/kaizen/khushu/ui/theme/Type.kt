package com.kaizen.khushu.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kaizen.khushu.R

val ScheherazadeNew = FontFamily(
    Font(resId = R.font.scheherazade_new_regular, weight = FontWeight.Normal),
    Font(resId = R.font.scheherazade_new_bold, weight = FontWeight.Bold),
)

val BeVietnamPro = FontFamily(
    Font(resId = R.font.be_vietnam_pro_regular, weight = FontWeight.Normal),
    Font(resId = R.font.be_vietnam_pro_medium, weight = FontWeight.Medium),
    Font(resId = R.font.be_vietnam_pro_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.be_vietnam_pro_bold, weight = FontWeight.Bold),
)

val Typography = createKhushuTypography(
    beVietnamPro = BeVietnamPro,
    scheherazadeNew = ScheherazadeNew,
)
