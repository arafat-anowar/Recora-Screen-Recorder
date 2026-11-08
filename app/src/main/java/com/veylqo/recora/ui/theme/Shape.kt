package com.veylqo.recora.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardCornerRadius = 22.dp
val PillCornerRadius = 50.dp
val IconTileRadius = 14.dp
val SheetCornerRadius = 26.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(CardCornerRadius),
    extraLarge = RoundedCornerShape(SheetCornerRadius)
)
