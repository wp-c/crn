package com.rifsxd.ksunext.ui.screen

import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.PowerManager
import android.system.Os
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.rifsxd.ksunext.ui.theme.IosDivider
import com.rifsxd.ksunext.ui.theme.IosAppear
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.dergoogler.mmrl.ui.component.text.TextRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.*
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.theme.ORANGE
import com.rifsxd.ksunext.ui.theme.IosGroup
import com.rifsxd.ksunext.ui.theme.IosRow
import com.rifsxd.ksunext.ui.theme.IosPill
import com.rifsxd.ksunext.ui.theme.IosHeader
import com.rifsxd.ksunext.ui.theme.IosQuickTile
import com.rifsxd.ksunext.ui.theme.IosIconBadge
import com.rifsxd.ksunext.ui.theme.IOS_BLUE
import com.rifsxd.ksunext.ui.theme.IOS_GREEN
import com.rifsxd.ksunext.ui.theme.IOS_ORANGE
import com.rifsxd.ksunext.ui.theme.IOS_PURPLE
import com.rifsxd.ksunext.ui.theme.IOS_TEAL
import com.rifsxd.ksunext.ui.util.*
import com.rifsxd.ksunext.ui.webui.WebUIActivity
import com.rifsxd.ksunext.ui.util.restartActivity
import com.rifsxd.ksunext.ui.util.module.LatestVersionInfo
import com.rifsxd.ksunext.ui.viewmodel.ModuleViewModel
import com.rifsxd.ksunext.ui.LocalNavBarEnabled
import com.rifsxd.ksunext.ui.LocalScrollState 
import com.rifsxd.ksunext.ui.screen.BottomBarDestination
import com.rifsxd.ksunext.ui.trackScroll 
import com.rifsxd.ksunext.ui.rememberScrollConnection
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val kernelVersion = getKernelVersion()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val isManager = Natives.isManager
    val fullFeatured = Natives.isFullFeatured()
    val ksuVersion = if (isManager) Natives.version else null
    val ksuVersionTag = if (isManager) Natives.getVersionTag() else null
    val kernelUAPIVersion = if (isManager) Natives.kernelUAPIVersion else null
    val managerUAPIVersion = Natives.managerUAPIVersion

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)
    
    // Get scroll state for bottom bar tracking
    val bottomBarScrollState = LocalScrollState.current

    val scrollState = LocalScrollState.current
    val navBarEnabled = LocalNavBarEnabled.current
    val isNavBarHidden = (scrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp
    
    // Create scroll connection for bottom bar
    val bottomBarScrollConnection = if (bottomBarScrollState != null) {
        rememberScrollConnection(
            isScrollingDown = bottomBarScrollState.isScrollingDown,
            scrollOffset = bottomBarScrollState.scrollOffset,
            previousScrollOffset = bottomBarScrollState.previousScrollOffset,
            threshold = 30f
        )
    } else null

    Scaffold(
        topBar = {
            TopBar(
                kernelVersion,
                ksuVersion,
                onInstallClick = {
                    navigator.navigate(InstallScreenDestination)
                },
                onSettingsClick = {
                    navigator.navigate(SettingScreenDestination) {
                        popUpTo(NavGraphs.root.startRoute) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                navBarEnabled = LocalNavBarEnabled.current?.value ?: true,
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                // Chain scroll connections - bottom bar tracking first, then topbar behavior
                .let { modifier ->
                    if (bottomBarScrollConnection != null) {
                        modifier
                            .nestedScroll(bottomBarScrollConnection)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                    } else {
                        modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp)
                .padding(bottom = navBarPadding + 24.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val lkmMode = ksuVersion?.let {
                Natives.isLkmMode
            }

            // --- data ringkas untuk beranda ---
            val unameRelease = remember { Os.uname().release }
            val rootOk = remember { rootAvailable() }
            val superUserCount = remember { getSuperuserCount() }
            val moduleCount = remember { getModuleCount() }

            fun goTab(direction: com.ramcosta.composedestinations.spec.DirectionDestinationSpec) {
                navigator.navigate(direction) {
                    popUpTo(NavGraphs.root.startRoute) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            // ---- kartu utama: keadaan kernel sekarang ----
            // Judul besar tidak dipakai: sudah ada app bar "Rollercoaster" di atas,
            // dulu jadi dobel dan memakan ruang.
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigator.navigate(InstallScreenDestination) }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        IosIconBadge(
                            icon = Icons.Filled.Memory,
                            tint = IOS_BLUE,
                            size = 42.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ios_kernel_active),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = unameRelease,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    // chip di baris sendiri agar tidak menabrak teks versi kernel
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IosPill(
                            text = if (ksuVersion != null) stringResource(R.string.ios_rooted)
                            else stringResource(R.string.ios_no_root),
                            color = if (ksuVersion != null) IOS_GREEN else IOS_ORANGE
                        )
                        IosPill(text = "Linux " + kernelVersion.toString(), color = IOS_BLUE)
                        if (ksuVersion != null) {
                            IosPill(text = "KSU " + ksuVersion, color = IOS_PURPLE)
                            IosPill(
                                text = if (lkmMode == true) "LKM" else "Built-in",
                                color = IOS_TEAL
                            )
                        }
                    }
                }
            }

            // ---- penjelasan singkat kalau root belum aktif ----
            if (!fullFeatured) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = IOS_ORANGE.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        IosIconBadge(icon = Icons.Filled.Info, tint = IOS_ORANGE, size = 30.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.ios_root_missing_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.ios_root_missing_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ---- ringkasan dalam bentuk daftar (gaya iOS Settings) ----
            IosGroup(title = stringResource(R.string.ios_status_title)) {
                IosRow(
                    icon = Icons.Filled.Memory,
                    iconTint = IOS_BLUE,
                    title = stringResource(R.string.ios_kernel_version),
                    value = unameRelease.substringBefore("-openela").ifEmpty { unameRelease },
                )
                IosRow(
                    icon = Icons.Filled.VerifiedUser,
                    iconTint = IOS_PURPLE,
                    title = stringResource(R.string.ios_manager),
                    subtitle = ksuVersionTag,
                    value = ksuVersion?.toString() ?: "-",
                )
                IosRow(
                    icon = Icons.Filled.AdminPanelSettings,
                    iconTint = IOS_GREEN,
                    title = stringResource(R.string.ios_root),
                    value = if (rootOk) stringResource(R.string.ios_yes) else stringResource(R.string.ios_no),
                    pill = if (rootOk) stringResource(R.string.ios_active) else null,
                    pillColor = IOS_GREEN,
                )
                IosRow(
                    icon = Icons.Filled.Shield,
                    iconTint = IOS_TEAL,
                    title = stringResource(R.string.ios_hiding),
                    subtitle = stringResource(R.string.ios_hiding_desc),
                    value = if (ksuVersion != null) stringResource(R.string.ios_on) else stringResource(R.string.ios_off),
                    isLast = true
                )
            }

            // ---- peringatan versi (fungsional, tetap dipertahankan) ----
            val currentVersionCode = getManagerVersion(context).second
            val requiresNewKernel = isManager && kernelUAPIVersion != null && managerUAPIVersion > kernelUAPIVersion
            val requiresNewManager = isManager && kernelUAPIVersion != null && managerUAPIVersion < kernelUAPIVersion
            val canInstallKernelUpdate = lkmMode == true && !Natives.isLateLoadMode

            if (requiresNewKernel) {
                WarningCard(
                    stringResource(
                        id = if (lkmMode == true) R.string.require_kernel_version else R.string.require_kernel_version_gki,
                        kernelUAPIVersion!!,
                        managerUAPIVersion
                    ),
                    onClick = if (lkmMode == true) {
                        { navigator.navigate(InstallScreenDestination) }
                    } else null
                )
            }

            if (requiresNewManager) {
                WarningCard(
                    stringResource(
                        id = R.string.require_manager_version,
                        managerUAPIVersion,
                        kernelUAPIVersion!!
                    )
                )
            }

            val showLkmUpdate = isManager && lkmMode == true && Natives.isLkmBundled && ksuVersion?.toLong() != currentVersionCode && !requiresNewKernel && !requiresNewManager

            if (showLkmUpdate) {
                WarningCard(
                    message = stringResource(R.string.home_lkm_update_available),
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { navigator.navigate(InstallScreenDestination) }
                )
            }

            if (ksuVersion != null && !rootAvailable()) {
                WarningCard(
                    stringResource(id = R.string.grant_root_failed),
                    onClick = { restartActivity(context) }
                )
            }

            // ---- aksi cepat ----
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IosQuickTile(
                        title = stringResource(R.string.ios_modules),
                        subtitle = if (!fullFeatured) stringResource(R.string.ios_needs_root)
                        else if (moduleCount == 1) stringResource(R.string.ios_one_item)
                        else stringResource(R.string.ios_n_items, moduleCount),
                        icon = Icons.Filled.Layers,
                        tint = IOS_BLUE,
                        modifier = Modifier.weight(1f)
                    ) { goTab(ModuleScreenDestination) }

                    IosQuickTile(
                        title = stringResource(R.string.ios_root_apps),
                        subtitle = if (!fullFeatured) stringResource(R.string.ios_needs_root)
                        else if (superUserCount == 1) stringResource(R.string.ios_one_item)
                        else stringResource(R.string.ios_n_items, superUserCount),
                        icon = Icons.Filled.AdminPanelSettings,
                        tint = IOS_GREEN,
                        modifier = Modifier.weight(1f)
                    ) { goTab(SuperUserScreenDestination) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IosQuickTile(
                        title = stringResource(R.string.ios_install_kernel),
                        subtitle = stringResource(R.string.ios_install_kernel_desc),
                        icon = Icons.Filled.SystemUpdate,
                        tint = IOS_ORANGE,
                        modifier = Modifier.weight(1f)
                    ) { navigator.navigate(InstallScreenDestination) }

                    IosQuickTile(
                        title = stringResource(R.string.ios_settings),
                        subtitle = stringResource(R.string.ios_settings_desc),
                        icon = Icons.Filled.Settings,
                        tint = IOS_TEAL,
                        modifier = Modifier.weight(1f)
                    ) { goTab(SettingScreenDestination) }
                }
            }

            val checkUpdate =
                LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getBoolean("check_update", true)
            if (checkUpdate) {
                UpdateCard()
            }

            IosAppear(delayMillis = 90) { InfoCard(autoExpand = developerOptionsEnabled) }
            IosAppear(delayMillis = 140) { IssueReportCard() }
            Spacer(Modifier)
        }
    }
}

@Composable
private fun SuperuserCard(onClick: (() -> Unit)? = null) {
    val count = getSuperuserCount()
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (count <= 1) {
                        stringResource(R.string.home_superuser_count_singular)
                    } else {
                        stringResource(R.string.home_superuser_count_plural)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(onClick: (() -> Unit)? = null) {
    val count = getModuleCount()
    val moduleViewModel: ModuleViewModel = viewModel()

    val moduleUpdateCount = remember(moduleViewModel.moduleList) {
        moduleViewModel.moduleList.count { module ->
            module.enabled && (moduleViewModel.checkUpdate(module).first.isNotEmpty())
        }
    }

    // State machine: 0 = nothing, 1 = show "+ Update!", 2 = show "+ X"
    var step by remember { mutableStateOf(0) }

    LaunchedEffect(moduleUpdateCount) {
        if (moduleUpdateCount > 0) {
            step = 1
            delay(1200) // show "+ Update!" for a moment
            step = 2
        } else {
            step = 0
        }
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (count <= 1) {
                        stringResource(R.string.home_module_count_singular)
                    } else {
                        stringResource(R.string.home_module_count_plural)
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (moduleUpdateCount > 0) {
                        Spacer(Modifier.width(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Keep the "|" static
                            Text(
                                text = "|",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.width(4.dp))

                            // Animate only the right-side text
                            AnimatedContent(
                                targetState = step,
                                transitionSpec = {
                                    slideInHorizontally { -it } + fadeIn() togetherWith
                                            slideOutHorizontally { it } + fadeOut()
                                }
                            ) { target ->
                                when (target) {
                                    1 -> Text(
                                        text = stringResource(id = R.string.home_module_update_available),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    2 -> Text(
                                        text = buildAnnotatedString {
                                            append(moduleUpdateCount.toString())
                                            append("*")
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateCard() {
    val context = LocalContext.current
    val latestVersionInfo = LatestVersionInfo()
    
    var preferSpoofed by remember { mutableStateOf(false) }
    
    val newVersion by produceState(initialValue = latestVersionInfo, key1 = preferSpoofed) {
        value = withContext(Dispatchers.IO) {
            checkNewVersion(preferSpoofed)
        }
    }

    val currentVersionCode = getManagerVersion(context).second
    val newVersionCode = newVersion.versionCode
    val newVersionUrl = newVersion.downloadUrl
    val changelog = newVersion.changelog
    val newVersionTag = newVersion.versionTag

    val uriHandler = LocalUriHandler.current
    val title = stringResource(id = R.string.module_changelog)
    val updateText = stringResource(id = R.string.module_update)

    AnimatedVisibility(
        visible = newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        ElevatedCard(
            modifier = Modifier.clickable {
                if (changelog.isEmpty()) {
                    uriHandler.openUri(newVersionUrl)
                } else {
                    updateDialog.showConfirm(
                        title = title,
                        content = changelog,
                        markdown = true,
                        confirm = updateText
                    )
                }
            },
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Update,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(end = 20.dp)
                    )
                    Text(
                        text = if (!newVersionTag.isNullOrEmpty()) {
                            stringResource(id = R.string.new_version_available, newVersionTag, newVersionCode)
                        } else {
                            stringResource(id = R.string.new_version_available, "", newVersionCode)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = R.string.select_build_type),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!preferSpoofed) {
                            FilledTonalButton(
                                onClick = { preferSpoofed = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(stringResource(id = R.string.main), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { preferSpoofed = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.69f),
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(stringResource(id = R.string.main), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        
                        if (preferSpoofed) {
                            FilledTonalButton(
                                onClick = { preferSpoofed = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(stringResource(id = R.string.spoofed), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { preferSpoofed = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.69f),
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(stringResource(id = R.string.spoofed), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RebootDropdownItem(@StringRes id: Int, reason: String = "") {
    DropdownMenuItem(text = {
        Text(stringResource(id))
    }, onClick = {
        reboot(reason)
    })
}

// @Composable
// fun getSeasonalIcon(): ImageVector {
//     val month = Calendar.getInstance().get(Calendar.MONTH) // 0-11 for January-December
//     return when (month) {
//         Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> Icons.Filled.AcUnit // Winter
//         Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Icons.Filled.Spa // Spring
//         Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Icons.Filled.WbSunny // Summer
//         Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> Icons.Filled.Forest // Fall
//         else -> Icons.Filled.Whatshot // Fallback icon
//     }
// }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    kernelVersion: KernelVersion,
    ksuVersion: Int?,
    onInstallClick: () -> Unit,
    onSettingsClick: () -> Unit,
    navBarEnabled: Boolean,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    var isSpinning by remember { mutableStateOf(false) }
    var rotationTarget by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = rotationTarget,
        animationSpec = tween(
            durationMillis = 1400,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        finishedListener = {
            isSpinning = false
        }
    )

    val moduleViewModel: ModuleViewModel = viewModel()
    
    val webUILauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    val context = LocalContext.current
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isSpinning) {
                        isSpinning = true
                        rotationTarget += 360f * 6
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rollercoaster_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 4.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
        },
        actions = {
            if (!navBarEnabled) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(id = R.string.settings)
                    )
                }
            }

            if (ksuVersion != null) {
                IconButton(onClick = onInstallClick) {
                    Icon(
                        imageVector = Icons.Filled.Archive,
                        contentDescription = stringResource(id = R.string.install)
                    )
                }
            }

            if (ksuVersion != null) {
                var showDropdown by remember { mutableStateOf(false) }
                IconButton(onClick = {
                    showDropdown = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = stringResource(id = R.string.reboot)
                    )

                    DropdownMenu(expanded = showDropdown, onDismissRequest = {
                        showDropdown = false
                    }) {
                        RebootDropdownItem(id = R.string.reboot)
                        RebootDropdownItem(id = R.string.reboot_userspace, reason = "soft-reboot")

                        val pm =
                            LocalContext.current.getSystemService(Context.POWER_SERVICE) as PowerManager?
                        @Suppress("DEPRECATION")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
                            RebootDropdownItem(id = R.string.reboot_userspace, reason = "userspace")
                        }
                        RebootDropdownItem(id = R.string.reboot_recovery, reason = "recovery")
                        RebootDropdownItem(id = R.string.reboot_bootloader, reason = "bootloader")
                        RebootDropdownItem(id = R.string.reboot_download, reason = "download")
                        RebootDropdownItem(id = R.string.reboot_edl, reason = "edl")
                    }
                }
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}


@Composable
private fun StatusCard(
    kernelVersionParam: KernelVersion,
    ksuVersionParam: Int?,
    uapiVerParam: Int? = null,
    lkmModeParam: Boolean?,
    moduleUpdateCount: Int = 0,
    ksuVersionTagParam: String? = null,
    onClickInstall: () -> Unit = {}
) {
    val context = LocalContext.current

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = run {
            if (ksuVersionParam != null) MaterialTheme.colorScheme.primary
            else if (kernelVersionParam.isGKI()) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer
        })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (ksuVersionParam == null) {
                        onClickInstall()
                    }
                }
                .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                ksuVersionParam != null -> {
                    val workingMode = if (lkmModeParam == true || lkmModeParam == false) {
                        val mode = if (lkmModeParam == true) "LKM" else "BUILT-IN"
                        "$mode (" + kernelVersionParam.getKernelType() + ")"
                    } else kernelVersionParam.getKernelType()

                    Icon(
                        imageVector = Icons.Filled.Mood,
                        contentDescription = null
                    )
                    Column(
                        modifier = Modifier.padding(start = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val labelStyle = LabelItemDefaults.style
                        TextRow(
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LabelItem(
                                        icon = if (Natives.isSafeMode) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.Security,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.VerifiedUser,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        text = {
                                            Text(
                                                text = workingMode,
                                                style = labelStyle.textStyle.copy(
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                )
                                            )
                                        },
                                        style = LabelItemDefaults.style.copy(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                    if (isSuCompatDisabled()) {
                                        LabelItem(
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Warning,
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    contentDescription = null
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.sucompat_disabled),
                                                    style = labelStyle.textStyle.copy(
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    )
                                                )
                                            },
                                            style = LabelItemDefaults.style.copy(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                        )
                                    }
                                    if (Natives.isLateLoadMode) {
                                        LabelItem(
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Warning,
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    contentDescription = null
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.jailbreak_mode),
                                                    style = labelStyle.textStyle.copy(
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                    )
                                                )
                                            },
                                            style = LabelItemDefaults.style.copy(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_working),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val ksuVer = ksuVersionParam ?: 0
                        val uapiVer = uapiVerParam ?: 0
                        val tag = if (!ksuVersionTagParam.isNullOrEmpty()) ksuVersionTagParam else "v0.0.0"
                        val versionText = stringResource(
                            R.string.home_working_version,
                            tag,
                            "$ksuVer-$uapiVer"
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = versionText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (lkmModeParam == true && !Natives.isLkmBundled) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.home_lkm_custom),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                kernelVersionParam.isGKI() -> {
                    Icon(Icons.Filled.AutoFixHigh, null)
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(
                            text = stringResource(R.string.home_not_installed),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_click_to_install),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    Icon(Icons.Filled.MoodBad, null)
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(
                            text = stringResource(R.string.home_failure),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_failure_tip),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WarningCard(
    message: String, color: Color = MaterialTheme.colorScheme.error, onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = color
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SentimentDissatisfied,
                contentDescription = null,
                modifier = Modifier.padding(end = 20.dp)
            )
            Text(
                text = message, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private data class HomeInfoSnapshot(
    val managerVersion: Pair<String, Long> = "" to 0L,
    val managerUAPIVersion: Int = 0,
    val managerAppId: Int = 0,
    val hookMode: String? = null,
    val metaModule: String? = null,
    val metaInfo: ModuleViewModel.ModuleInfo? = null,
    val suSFS: String? = null,
    val suSFSVersion: String? = null,
    val suSFSVariant: String? = null,
    val zygiskEnabled: Boolean = false,
    val zygiskInfo: ModuleViewModel.ModuleInfo? = null,
    val unameRelease: String = "",
    val unameMachine: String = "",
    val seccompStatus: String = "Unavailable",
)

private fun buildHomeInfoSnapshot(
    context: Context,
    ksuVersion: Int?,
    moduleList: List<ModuleViewModel.ModuleInfo>,
): HomeInfoSnapshot {
    val managerVersion = getManagerVersion(context)
    val managerUAPIVersion = Natives.managerUAPIVersion
    val managerAppId = Natives.getManagerAppid()
    val hookMode = if (ksuVersion == null) null else {
        Natives.getHookMode().takeUnless { it.isNullOrBlank() } ?: "Unavailable"
    }
    val metaModule = if (ksuVersion == null) null else getMetaModule()
    val metaInfo = if (metaModule == null) null else moduleList.firstOrNull { it.isMetaModule }
    val suSFS = if (ksuVersion == null) null else getSuSFS()
    val suSFSVersion = if (suSFS == "Supported" && ksuVersion != null) getSuSFSVersion() else null
    val suSFSVariant = if (suSFS == "Supported" && ksuVersion != null) getSuSFSVariant() else null
    val zygiskEnabled = if (ksuVersion == null) false else Natives.isZygiskEnabled()
    val zygiskInfo = if (!zygiskEnabled) null else moduleList.firstOrNull { it.isZygisk && it.enabled }
    val uname = kotlin.runCatching { Os.uname() }.getOrNull()
    val statusInt = kotlin.runCatching { Os.prctl(21, 0, 0, 0, 0) }.getOrDefault(-1)
    val seccompStatus = when (statusInt) {
        -1 -> "Unavailable"
        0 -> "Disabled"
        1 -> "Strict"
        2 -> "Filter"
        else -> "Unknown"
    }

    return HomeInfoSnapshot(
        managerVersion = managerVersion,
        managerUAPIVersion = managerUAPIVersion,
        managerAppId = managerAppId,
        hookMode = hookMode,
        metaModule = metaModule,
        metaInfo = metaInfo,
        suSFS = suSFS,
        suSFSVersion = suSFSVersion,
        suSFSVariant = suSFSVariant,
        zygiskEnabled = zygiskEnabled,
        zygiskInfo = zygiskInfo,
        unameRelease = uname?.release.orEmpty(),
        unameMachine = uname?.machine.orEmpty(),
        seccompStatus = seccompStatus,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun InfoCard(autoExpand: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    val isManager = remember { Natives.isManager }
    val ksuVersion = remember(isManager) { if (isManager) Natives.version else null }

    var expanded by rememberSaveable { mutableStateOf(false) }
    val developerOptionsEnabled = remember(prefs) { prefs.getBoolean("enable_developer_options", false) }

    LaunchedEffect(autoExpand) {
        if (autoExpand) expanded = true
    }

    val moduleViewModel: ModuleViewModel = viewModel()
    var homeInfo by remember { mutableStateOf(HomeInfoSnapshot()) }

    LaunchedEffect(ksuVersion, moduleViewModel.moduleList) {
        homeInfo = withContext(Dispatchers.IO) {
            buildHomeInfoSnapshot(context, ksuVersion, moduleViewModel.moduleList)
        }
    }

    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    // Salin nilai baris saat ditekan lama — dengan getaran halus sebagai umpan balik.
    fun copyRow(label: String, value: String) {
        clipboard.setText(AnnotatedString(value))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        Toast.makeText(context, "$label disalin", Toast.LENGTH_SHORT).show()
    }

    val metaInfo = homeInfo.metaInfo
    val metaDetail = if (metaInfo != null) "${metaInfo.name} · ${metaInfo.version}" else null

    Column(modifier = Modifier.fillMaxWidth()) {
        // ---- ringkasan utama ------------------------------------------------
        IosGroup(title = "Info sistem") {
            InfoRow(
                title = "Versi Manager",
                value = "${homeInfo.managerVersion.first} (${homeInfo.managerVersion.second})",
                subtitle = if (developerOptionsEnabled) "UID ${homeInfo.managerAppId}" else "Antarmuka Rollercoaster",
                icon = Icons.Filled.AutoAwesomeMotion,
                iconTint = IOS_BLUE,
                onCopy = {
                    copyRow(
                        "Versi Manager",
                        "${homeInfo.managerVersion.first} (${homeInfo.managerVersion.second}-${homeInfo.managerUAPIVersion})"
                    )
                }
            )

            if (ksuVersion != null) {
                InfoRow(
                    title = "Mode hook",
                    value = homeInfo.hookMode ?: stringResource(R.string.unavailable),
                    subtitle = "Cara kernel menangkap permintaan root",
                    icon = Icons.Filled.Phishing,
                    iconTint = IOS_PURPLE,
                    pill = if (homeInfo.hookMode != null) "Aktif" else null,
                    pillColor = IOS_GREEN,
                    onCopy = { copyRow("Mode hook", homeInfo.hookMode ?: "-") }
                )

                InfoRow(
                    title = "Metamodul",
                    value = when {
                        homeInfo.metaModule == "Installed" && metaInfo != null && !metaInfo.enabled ->
                            stringResource(R.string.disabled)
                        homeInfo.metaModule == "Installed" -> stringResource(R.string.installed)
                        else -> stringResource(R.string.home_not_installed)
                    },
                    subtitle = metaDetail ?: "OverlayFS untuk memasang modul",
                    icon = Icons.Filled.SettingsSuggest,
                    iconTint = IOS_ORANGE,
                    pill = if (homeInfo.metaModule == "Installed") "Siap" else null,
                    pillColor = IOS_GREEN,
                    onCopy = {
                        copyRow("Metamodul", "${homeInfo.metaModule} ${metaDetail ?: ""}".trim())
                    }
                )

                if (homeInfo.suSFS == "Supported") {
                    InfoRow(
                        title = "SuFS",
                        value = homeInfo.suSFSVersion ?: stringResource(R.string.unavailable),
                        subtitle = "Menyembunyikan jejak root · ${homeInfo.suSFSVariant ?: "-"}",
                        icon = painterResource(R.drawable.ic_sus),
                        iconTint = IOS_GREEN,
                        pill = "Didukung",
                        pillColor = IOS_GREEN,
                        onCopy = {
                            copyRow("SuFS", "${homeInfo.suSFSVersion} (${homeInfo.suSFSVariant})")
                        }
                    )
                }

                if (homeInfo.zygiskEnabled) {
                    InfoRow(
                        title = "Zygisk",
                        value = stringResource(R.string.enabled),
                        subtitle = homeInfo.zygiskInfo?.let { "${it.name} · ${it.version}" },
                        icon = Icons.Filled.Vaccines,
                        iconTint = IOS_PURPLE,
                        pill = "Aktif",
                        pillColor = IOS_GREEN,
                        onCopy = { copyRow("Zygisk", "${homeInfo.zygiskInfo?.name} ${homeInfo.zygiskInfo?.version}") }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- detail perangkat (dapat dilipat) -------------------------------
        IosGroup(title = "Perangkat") {
            IosRow(
                title = "Kernel",
                value = homeInfo.unameMachine,
                subtitle = homeInfo.unameRelease,
                icon = painterResource(R.drawable.ic_linux),
                iconTint = IOS_BLUE,
                showChevron = true,
                isLast = !expanded,
                onClick = {
                    expanded = !expanded
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(120))
            ) {
                Column {
                    InfoRow(
                        title = "Android",
                        value = "${Build.VERSION.RELEASE}",
                        subtitle = "API ${Build.VERSION.SDK_INT}",
                        icon = Icons.Filled.Android,
                        iconTint = IOS_GREEN,
                        isLast = false,
                        onCopy = { copyRow("Android", "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})") }
                    )
                    InfoRow(
                        title = "Arsitektur",
                        value = Build.SUPPORTED_ABIS.firstOrNull() ?: "-",
                        subtitle = Build.SUPPORTED_ABIS.joinToString(", "),
                        icon = Icons.Filled.Memory,
                        iconTint = IOS_BLUE,
                        isLast = false,
                        onCopy = { copyRow("ABI", Build.SUPPORTED_ABIS.joinToString(", ")) }
                    )
                    val selinux = getSELinuxStatus()
                    InfoRow(
                        title = "SELinux",
                        value = selinux,
                        subtitle = if (selinux.equals("Enforcing", true))
                            "Pembatasan sistem aktif" else "Pembatasan sistem longgar",
                        icon = Icons.Filled.Security,
                        iconTint = IOS_ORANGE,
                        isLast = false,
                        onCopy = { copyRow("SELinux", selinux) }
                    )
                    InfoRow(
                        title = "Seccomp",
                        value = homeInfo.seccompStatus,
                        subtitle = "Pembatas syscall di kernel",
                        icon = Icons.Filled.LocalPolice,
                        iconTint = IOS_PURPLE,
                        isLast = true,
                        onCopy = { copyRow("Seccomp", homeInfo.seccompStatus) }
                    )
                }
            }
        }

        Text(
            text = "Tekan lama sebuah baris untuk menyalin nilainya.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp)
        )
    }
}

/**
 * Satu baris info sistem: ikon berwarna, judul + nilai, keterangan kecil,
 * pil status opsional, dan salin-nilai lewat tekan lama.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InfoRow(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: Any? = null,
    iconTint: Color = IOS_BLUE,
    pill: String? = null,
    pillColor: Color = IOS_GREEN,
    isLast: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onCopy?.invoke() },
                onLongClick = { onCopy?.invoke() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosIconBadge(icon = icon, tint = iconTint)
            Spacer(Modifier.width(12.dp))
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
            if (value.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.sizeIn(maxWidth = 140.dp)
                )
            }
        }
        if (!isLast) IosDivider()
    }
}


@Composable
fun IssueReportCard() {
    val uriHandler = LocalUriHandler.current
    val githubIssueUrl = stringResource(R.string.issue_report_github_link)
    val telegramUrl = stringResource(R.string.issue_report_telegram_link)

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.issue_report_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_report_body),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_report_body_2),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = { uriHandler.openUri(githubIssueUrl) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = stringResource(R.string.issue_report_github),
                    )
                }
                IconButton(onClick = { uriHandler.openUri(telegramUrl) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_telegram),
                        contentDescription = stringResource(R.string.issue_report_telegram),
                    )
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
fun handleDynamicShortcuts(context: Context, moduleConfigs: List <Pair<ModuleViewModel.ModuleInfo, Int>>) {
    ShortcutManagerCompat.removeAllDynamicShortcuts(context)

    moduleConfigs.forEach { (module, iconRes) ->
        val shortcut = ShortcutInfoCompat.Builder(context, module.id)
            .setShortLabel(module.name)
            .setLongLabel(module.name)
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setCategories(setOf(ShortcutInfo.SHORTCUT_CATEGORY_CONVERSATION))
            .setIntent(
                Intent(context, WebUIActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = "kernelsu://webui/${module.id}".toUri()
                    putExtra("id", module.id)
                    putExtra("name", module.name)
                }
            )
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }
}

fun getManagerVersion(context: Context): Pair<String, Long> {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)!!
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return Pair(packageInfo.versionName!!, versionCode)
}

@Preview
@Composable
private fun StatusCardPreview() {
    Column {
        StatusCard(KernelVersion(5, 10, 101), 1, 1, null)
        StatusCard(KernelVersion(5, 10, 101), 20000, 1, true)
        StatusCard(KernelVersion(5, 10, 101), null, null, true)
        StatusCard(KernelVersion(4, 10, 101), null, null, false)
    }
}

@Preview
@Composable
private fun WarningCardPreview() {
    Column {
        WarningCard(message = "Warning message")
        WarningCard(
            message = "Warning message",
            MaterialTheme.colorScheme.outlineVariant,
            onClick = {})
    }
}
