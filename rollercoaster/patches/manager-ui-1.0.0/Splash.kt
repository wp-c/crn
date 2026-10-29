package com.rifsxd.ksunext.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.theme.IOS_BLUE
import com.rifsxd.ksunext.ui.theme.IOS_PURPLE
import kotlinx.coroutines.delay

/**
 * Splash screen Rollercoaster.
 *
 * Urutan animasi (total ± 1,6 detik):
 *  1. latar gradien biru→ungu muncul lembut,
 *  2. logo membesar dengan pegas (0,7 → 1,0) sambil berputar sedikit,
 *  3. nama aplikasi naik + memudar masuk,
 *  4. subjudul muncul,
 *  5. layar memudar keluar (ditangani AnimatedContent di MainActivity).
 */
@Composable
fun RollercoasterSplash(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(0.7f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-12f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(18f) }
    val subAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(320))
        logoScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
        logoRotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
        titleAlpha.animateTo(1f, tween(260))
        titleOffset.animateTo(0f, tween(320))
        subAlpha.animateTo(1f, tween(260))
        delay(520)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        IOS_BLUE,
                        IOS_PURPLE,
                        Color(0xFF1C1C1E)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.16f),
                modifier = Modifier
                    .size(132.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .graphicsLayer { rotationZ = logoRotation.value }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.ic_rollercoaster_logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = "Rollercoaster",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .graphicsLayer { translationY = titleOffset.value }
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Kernel kustom untuk miatoll",
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subAlpha.value)
            )
        }

        Text(
            text = "graziemille",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp)
                .alpha(subAlpha.value)
        )
    }
}
