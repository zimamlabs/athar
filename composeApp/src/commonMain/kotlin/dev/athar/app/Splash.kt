package dev.athar.app

import dev.athar.ui.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color(0xFF0E0E0C)
private val Cream = Color(0xFFF5F1E8)
private val Orange = Color(0xFFFF5A1F)

/** The opening screen: the mark eases in, then the name and "by Zimam" slide up apart from it. About 1.4 s. */
@Composable
fun Splash(onDone: () -> Unit) {
    val mark = remember { Animatable(0f) }
    val name = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { mark.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        launch { delay(600); name.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        delay(1400)
        onDone()
    }
    Column(Modifier.fillMaxSize().background(Ink), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BrandMark(Modifier.size(150.dp).alpha(mark.value).graphicsLayer { val k = 0.86f + 0.14f * mark.value; scaleX = k; scaleY = k })
        Spacer(Modifier.height(32.dp))
        Text(Brand.NAME, Modifier.alpha(name.value).offset(y = ((1f - name.value) * 14f).dp), color = Cream, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("by ${Brand.MAKER}", Modifier.alpha(name.value).offset(y = ((1f - name.value) * 14f).dp), color = Orange, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    }
}
