package com.rifsxd.ksunext.ui.theme

import androidx.compose.foundation.background
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Kumpulan komponen "iOS clean" yang dipakai ulang di semua layar.
 * Tujuannya: satu bahasa visual — kartu membulat, pemisah tipis, ikon petak
 * berwarna, dan teks yang mudah dibaca orang awam.
 */

// -----------------------------------------------------------------------------
//  Judul besar (large title) + subjudul
// -----------------------------------------------------------------------------
@Composable
fun IosHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -----------------------------------------------------------------------------
//  Grup kartu bergaya iOS (settings-style), dengan judul kecil & catatan kaki
// -----------------------------------------------------------------------------
@Composable
fun IosGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
            )
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) { content() }
        }
        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
            )
        }
    }
}

/** Garis pemisah tipis dengan indentasi seperti iOS. */
@Composable
fun IosDivider(indent: androidx.compose.ui.unit.Dp = 56.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = indent),
        thickness = 0.6.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

// -----------------------------------------------------------------------------
//  Satu baris di dalam grup
// -----------------------------------------------------------------------------
@Composable
fun IosRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: Any? = null,
    iconTint: Color = IOS_BLUE,
    pill: String? = null,
    pillColor: Color = IOS_GREEN,
    showChevron: Boolean = false,
    isLast: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        // Efek tekan ala iOS: baris menyusut 2% dengan pegas,
                        // lalu kembali. Tidak ada efek ripple besar.
                        Modifier.iosPressable { onClick() }
                    } else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                IosIconBadge(icon = icon, tint = iconTint)
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (pill != null) {
                Spacer(Modifier.width(8.dp))
                IosPill(text = pill, color = pillColor)
            }
            if (value != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (showChevron) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (!isLast) IosDivider()
    }
}

/** Ikon petak membulat khas iOS Settings. */
@Composable
fun IosIconBadge(
    icon: Any?,
    tint: Color = IOS_BLUE,
    size: androidx.compose.ui.unit.Dp = 30.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(tint, RoundedCornerShape(size / 3.4f)),
        contentAlignment = Alignment.Center
    ) {
        when (icon) {
            is ImageVector -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.58f)
            )
            is androidx.compose.ui.graphics.painter.Painter -> Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.58f)
            )
        }
    }
}

/** Pil status kecil (mis. "Aktif", "Siap"). */
@Composable
fun IosPill(text: String, color: Color = IOS_GREEN) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

// -----------------------------------------------------------------------------
//  Petak aksi cepat (2 kolom)
// -----------------------------------------------------------------------------
@Composable
fun IosQuickTile(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    tint: Color = IOS_BLUE,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            IosIconBadge(icon = icon, tint = tint, size = 34.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
//  Efek tekan ala iOS (spring, tanpa ripple besar)
// -----------------------------------------------------------------------------
@Composable
fun Modifier.iosPressable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "iosPressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
}

// -----------------------------------------------------------------------------
//  Muncul lembut (fade + naik sedikit) — dipakai untuk kartu agar halaman
//  terasa hidup tanpa berlebihan.
// -----------------------------------------------------------------------------
@Composable
fun IosAppear(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(320),
        label = "iosAppearAlpha"
    )
    val offset by animateFloatAsState(
        targetValue = if (visible) 0f else 14f,
        animationSpec = androidx.compose.animation.core.tween(320),
        label = "iosAppearOffset"
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offset
        }
    ) { content() }
}
