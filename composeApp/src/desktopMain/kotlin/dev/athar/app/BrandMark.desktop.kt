package dev.athar.app

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

@Composable
actual fun BrandMark(modifier: Modifier) = Image(painterResource("mark.png"), null, modifier)
