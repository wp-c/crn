package com.rifsxd.ksunext.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.DeveloperScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.BuildConfig
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.theme.IOS_BLUE
import com.rifsxd.ksunext.ui.theme.IOS_GREEN
import com.rifsxd.ksunext.ui.theme.IOS_ORANGE
import com.rifsxd.ksunext.ui.theme.IOS_PURPLE
import com.rifsxd.ksunext.ui.theme.IosGroup
import com.rifsxd.ksunext.ui.theme.IosRow

/**
 * Halaman Tentang — satu tempat untuk:
 *  - identitas aplikasi & versi,
 *  - daftar kontributor (desain ulang: avatar, nama, peran, tombol Dukung),
 *  - pintu masuk ke alat pengembang,
 *  - lisensi & tautan sumber.
 *
 * Menggantikan popup Tentang yang lama (penyebab menu tumpang tindih) dan
 * menggantikan halaman Kredit sepenuhnya.
 */
private data class AboutContributor(
    val name: String,
    val handle: String,
    val role: String,
    val initials: String,
    val color: Color,
    val url: String
)

private val aboutContributors = listOf(
    AboutContributor("Rifat Azad", "rifsxd", "Pengembang utama", "RA", IOS_BLUE, "https://github.com/rifsxd"),
    AboutContributor("Weishu", "tiann", "Pembuat KernelSU", "T", IOS_PURPLE, "https://github.com/tiann"),
    AboutContributor("fatalcoder524", "fatalcoder524", "Pemelihara antarmuka", "F", IOS_GREEN, "https://github.com/fatalcoder524"),
    AboutContributor("pershoot", "pershoot", "Pemelihara backend", "P", IOS_ORANGE, "https://github.com/persh00t-roms"),
    AboutContributor("Max", "maxsteeel", "Pemelihara versi lama", "M", Color(0xFF8E8E93), "https://github.com/maxsteeel")
)

private fun openLink(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Destination<RootGraph>
@Composable
fun AboutScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val kernelVersion = remember { runCatching { com.rifsxd.ksunext.Natives.version }.getOrNull() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // --- bilah atas -----------------------------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navigator.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                text = "Tentang",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        // --- kartu identitas aplikasi --------------------------------------
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = IOS_BLUE,
                    modifier = Modifier.size(64.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_rollercoaster_logo),
                        contentDescription = "Ikon Rollercoaster",
                        modifier = Modifier
                            .padding(10.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Rollercoaster",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manager ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (kernelVersion != null) {
                        Text(
                            text = "Kernel KSU $kernelVersion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- kontributor (desain baru) -------------------------------------
        IosGroup(
            title = "Kontributor",
            footer = "Terima kasih kepada mereka yang membuat root di kernel mungkin."
        ) {
            aboutContributors.forEachIndexed { index, c ->
                ContributorRow(
                    contributor = c,
                    isLast = index == aboutContributors.lastIndex,
                    onSupport = { openLink(context, c.url) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- pengembang -----------------------------------------------------
        IosGroup(title = "Pengembang") {
            IosRow(
                title = "Alat pengembang",
                subtitle = "Log, perintah root, dan diagnostik kernel",
                icon = Icons.Filled.Build,
                iconTint = IOS_PURPLE,
                showChevron = true,
                onClick = { navigator.navigate(DeveloperScreenDestination) }
            )
            IosRow(
                title = "Sumber kode",
                subtitle = "KernelSU-Next di GitHub",
                icon = Icons.Filled.Terminal,
                iconTint = IOS_BLUE,
                showChevron = true,
                isLast = true,
                onClick = { openLink(context, "https://github.com/KernelSU-Next/KernelSU-Next") }
            )
        }

        Spacer(Modifier.height(20.dp))

        // --- lisensi --------------------------------------------------------
        IosGroup(title = "Lisensi") {
            IosRow(
                title = "GPL-2.0",
                subtitle = "Rollercoaster Manager adalah turunan KernelSU-Next",
                icon = Icons.Filled.Security,
                iconTint = IOS_GREEN,
                isLast = true
            )
        }

        Spacer(Modifier.height(22.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Dibuat oleh graziemille",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Rollercoaster • miatoll / joyeuse",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(96.dp))
    }
}

/**
 * Baris kontributor: avatar bulat berisi inisial, nama + peran, dan tombol
 * "Dukung" berbentuk pil — meniru tata letak pada gambar acuan.
 */
@Composable
private fun ContributorRow(
    contributor: AboutContributor,
    isLast: Boolean,
    onSupport: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(contributor.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contributor.initials,
                    color = contributor.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (contributor.name == contributor.handle) contributor.name
                    else "${contributor.name} (${contributor.handle})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = contributor.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onSupport() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Dukung",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 74.dp),
                thickness = androidx.compose.ui.unit.Dp.Hairline,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}
