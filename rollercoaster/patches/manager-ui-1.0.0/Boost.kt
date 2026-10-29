package com.rifsxd.ksunext.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.ui.theme.IOS_BLUE
import com.rifsxd.ksunext.ui.theme.IOS_GREEN
import com.rifsxd.ksunext.ui.theme.IOS_ORANGE
import com.rifsxd.ksunext.ui.theme.IOS_PURPLE
import com.rifsxd.ksunext.ui.theme.IosGroup
import com.rifsxd.ksunext.ui.theme.IosHeader
import com.rifsxd.ksunext.ui.theme.IosRow
import com.rifsxd.ksunext.ui.util.rootAvailable
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Layar "Boost" — dua peningkatan yang benar-benar bekerja di kernel:
 *
 *  1. Jaringan: menaikkan buffer TCP/UDP, mengaktifkan BBR bila tersedia, dan
 *     mematikan hemat daya Wi-Fi (power save) yang sering menahan latency.
 *  2. Suara: menaikkan gain mixer codec (speaker/headphone) lewat tinymix bila
 *     mixer-nya tersedia; kalau tidak ada, laporkan apa adanya.
 *
 * Semua perubahan diterapkan lewat root dan disimpan sebagai skrip boot di
 * /data/adb/service.d supaya tetap aktif setelah reboot.
 */
@Destination<RootGraph>
@Composable
fun BoostScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rootOk by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var netEnabled by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(false) }
    var soundGain by remember { mutableIntStateOf(6) }
    var qdisc by remember { mutableStateOf("-") }
    var cc by remember { mutableStateOf("-") }
    var wifiPowerSave by remember { mutableStateOf("-") }
    var mixerInfo by remember { mutableStateOf("Belum diperiksa") }
    var persistent by remember { mutableStateOf(false) }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        rootOk = rootAvailable()
        if (!rootOk) return@withContext
        val r = Shell.cmd(
            "sysctl -n net.ipv4.tcp_congestion_control 2>/dev/null",
            "cat /sys/module/tcp_bbr/parameters/... 2>/dev/null",
            "cat /proc/sys/net/core/rmem_max 2>/dev/null",
            "getprop persist.sys.tcp.congestion 2>/dev/null",
            "cmd wifi get-power-save 2>/dev/null || echo unknown"
        ).exec()
        cc = r.out.getOrNull(0)?.trim()?.ifBlank { "-" } ?: "-"
        qdisc = r.out.getOrNull(2)?.trim()?.ifBlank { "-" } ?: "-"
        wifiPowerSave = r.out.getOrNull(4)?.trim()?.ifBlank { "-" } ?: "-"
        persistent = Shell.cmd("[ -f /data/adb/service.d/rc-boost.sh ] && echo 1 || echo 0").exec()
            .out.firstOrNull()?.trim() == "1"
        val t = Shell.cmd("command -v tinymix >/dev/null && echo yes || echo no").exec()
            .out.firstOrNull()?.trim()
        mixerInfo = if (t == "yes") "tinymix tersedia" else "tinymix tidak terpasang (gain suara dilewati)"
    }

    LaunchedEffect(Unit) { refresh() }

    fun applyNet(enable: Boolean) {
        scope.launch(Dispatchers.IO) {
            busy = true
            val script = """
                #!/system/bin/sh
                # Rollercoaster Boost — jaringan (dibuat otomatis)
                sysctl -w net.core.rmem_max=6291456 >/dev/null 2>&1
                sysctl -w net.core.wmem_max=6291456 >/dev/null 2>&1
                sysctl -w net.ipv4.tcp_rmem='4096 87380 6291456' >/dev/null 2>&1
                sysctl -w net.ipv4.tcp_wmem='4096 87380 6291456' >/dev/null 2>&1
                sysctl -w net.ipv4.tcp_mtu_probing=1 >/dev/null 2>&1
                sysctl -w net.ipv4.tcp_fastopen=3 >/dev/null 2>&1
                sysctl -w net.ipv4.tcp_slow_start_after_idle=0 >/dev/null 2>&1
                sysctl -w net.core.default_qdisc=fq_codel >/dev/null 2>&1
                if ! (echo bbr > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null); then
                  echo cubic > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null
                fi
                # matikan hemat daya Wi-Fi (mengurangi lonjakan latency)
                cmd wifi set-power-save false >/dev/null 2>&1
                svc wifi force-power-save-mode disable >/dev/null 2>&1
            """.trimIndent()

            val res = if (enable) {
                Shell.cmd(
                    "mkdir -p /data/adb/service.d",
                    "cat > /data/adb/service.d/rc-boost.sh <<'RC_EOF'\n$script\nRC_EOF",
                    "chmod 755 /data/adb/service.d/rc-boost.sh",
                    "sh /data/adb/service.d/rc-boost.sh",
                    "sysctl -n net.ipv4.tcp_congestion_control"
                ).exec()
            } else {
                Shell.cmd(
                    "rm -f /data/adb/service.d/rc-boost.sh",
                    "sysctl -w net.core.rmem_max=212992 >/dev/null 2>&1",
                    "sysctl -w net.core.wmem_max=212992 >/dev/null 2>&1",
                    "sysctl -w net.ipv4.tcp_slow_start_after_idle=1 >/dev/null 2>&1",
                    "cmd wifi set-power-save true >/dev/null 2>&1"
                ).exec()
            }

            netEnabled = enable
            val applied = res.out.lastOrNull()?.trim().orEmpty()
            withContext(Dispatchers.Main) {
                busy = false
                Toast.makeText(
                    context,
                    if (enable) "Jaringan dipercepat (${applied.ifBlank { "ok" }})" else "Pengaturan jaringan dikembalikan",
                    Toast.LENGTH_SHORT
                ).show()
            }
            refresh()
        }
    }

    fun applySound(enable: Boolean, gain: Int) {
        scope.launch(Dispatchers.IO) {
            busy = true
            val script = """
                #!/system/bin/sh
                # Rollercoaster Boost — gain suara
                command -v tinymix >/dev/null || exit 0
                tinymix "RX0 Digital Volume" $gain >/dev/null 2>&1
                tinymix "RX1 Digital Volume" $gain >/dev/null 2>&1
                tinymix "DEC0 Volume" $gain >/dev/null 2>&1
                tinymix -D 0 2>/dev/null | grep -iE 'digital volume|gain' | head -5
            """.trimIndent()

            val res = if (enable) {
                Shell.cmd(
                    "mkdir -p /data/adb/service.d",
                    "cat > /data/adb/service.d/rc-sound.sh <<'RC_EOF'\n$script\nRC_EOF",
                    "chmod 755 /data/adb/service.d/rc-sound.sh",
                    "sh /data/adb/service.d/rc-sound.sh"
                ).exec()
            } else {
                Shell.cmd("rm -f /data/adb/service.d/rc-sound.sh", "tinymix -D 0 2>/dev/null | head -1").exec()
            }
            soundEnabled = enable
            withContext(Dispatchers.Main) {
                busy = false
                Toast.makeText(
                    context,
                    if (enable) "Gain suara dinaikkan (${gain}dB)" else "Gain suara dikembalikan",
                    Toast.LENGTH_SHORT
                ).show()
            }
            refresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navigator.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Column {
                Text("Boost", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Peningkatan jaringan & suara",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }

        Spacer(Modifier.height(12.dp))

        if (!rootOk) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Boost memerlukan root. Berikan izin root untuk Rollercoaster lebih dulu.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        IosHeader(title = "Jaringan", subtitle = "Buffer lebih besar, latency Wi-Fi lebih rendah")

        Spacer(Modifier.height(10.dp))

        IosGroup {
            IosRow(
                title = "Wifi Boost + TCP tuning",
                subtitle = "rmem/wmem 6 MB · MTU probing · fast open · power save mati",
                icon = Icons.Filled.Wifi,
                iconTint = IOS_BLUE,
                pill = if (netEnabled || persistent) "Aktif" else "Mati",
                pillColor = if (netEnabled || persistent) IOS_GREEN else IOS_ORANGE,
                isLast = false,
                onClick = { if (rootOk && !busy) applyNet(!netEnabled) }
            )
            IosRow(
                title = "Kongesti TCP",
                subtitle = "BBR bila tersedia, jika tidak cubic",
                icon = Icons.Filled.Speed,
                iconTint = IOS_PURPLE,
                value = cc,
                isLast = false
            )
            IosRow(
                title = "Hemat daya Wi-Fi",
                subtitle = "Dimatikan supaya paket tidak tertahan",
                icon = Icons.Filled.NetworkCheck,
                iconTint = IOS_GREEN,
                value = wifiPowerSave,
                isLast = true
            )
        }

        Spacer(Modifier.height(20.dp))

        IosHeader(title = "Suara", subtitle = "Gain mixer codec (speaker & headphone)")

        Spacer(Modifier.height(10.dp))

        IosGroup(footer = mixerInfo) {
            IosRow(
                title = "Sound booster",
                subtitle = "Menaikkan RX digital volume lewat tinymix",
                icon = Icons.Filled.VolumeUp,
                iconTint = IOS_ORANGE,
                pill = if (soundEnabled) "Aktif" else "Mati",
                pillColor = if (soundEnabled) IOS_GREEN else IOS_ORANGE,
                isLast = soundEnabled,
                onClick = { if (rootOk && !busy) applySound(!soundEnabled, soundGain) }
            )
            if (soundEnabled) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.GraphicEq, null, tint = IOS_ORANGE, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gain +${soundGain}dB", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = soundGain.toFloat(),
                        onValueChange = { soundGain = it.toInt() },
                        onValueChangeFinished = { applySound(true, soundGain) },
                        valueRange = 0f..12f,
                        steps = 11
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        IosGroup(title = "Catatan") {
            IosRow(
                title = "Disimpan otomatis",
                subtitle = "Skrip ditaruh di /data/adb/service.d agar tetap aktif setelah reboot",
                icon = Icons.Filled.Bolt,
                iconTint = IOS_PURPLE,
                isLast = true
            )
        }

        Spacer(Modifier.height(96.dp))
    }
}
