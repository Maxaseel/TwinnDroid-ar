package com.twinndroid.ui

import androidx.compose.ui.res.stringResource

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.twinndroid.R
import com.twinndroid.install.ShizukuInstaller
import com.twinndroid.ui.theme.LocalTwinnExtras

/** First tab: the user's clones. */
@Composable
fun ClonesPage(vm: MainViewModel, onNewClone: () -> Unit) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf<CloneItem?>(null) }
    var accessFor by remember { mutableStateOf<String?>(null) }
    val canInstall = context.packageManager.canRequestPackageInstalls() ||
        (vm.settings.useShizuku && ShizukuInstaller.hasPermission())

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            HomeHeader(
                vm.clones.size,
                vm.clones.count { it.updateAvailable },
                onAppearance = { vm.screen = Screen.Appearance },
                onSettings = { vm.screen = Screen.Settings },
            )
        }

        if (!canInstall) item(key = "warn") {
            WarningCard(
                "TwinnDroid needs permission to install apps",
                "Allow it once, or set up Shizuku in Settings for prompt-free installs.",
                "Allow",
                Modifier.animateItem(),
            ) {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
                )
            }
        }

        if (vm.clones.isEmpty()) item(key = "empty") { EmptyState(onNewClone) }

        items(vm.clones, key = { it.config.id }) { item ->
            CloneCard(
                item = item,
                modifier = Modifier.animateItem(
                    fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
                ),
                onOpen = { vm.launchIntent(item.config.clonePackage)?.let(context::startActivity) },
                onBuild = { vm.rebuild(item.config) },
                onEdit = { vm.edit(item.config) },
                onUninstall = {
                    context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${item.config.clonePackage}")))
                },
                onDelete = { confirmDelete = item },
                onAccess = { accessFor = item.config.id },
            )
        }
    }

    accessFor?.let { id ->
        vm.clones.firstOrNull { it.config.id == id }?.let { item -> SpecialAccessSheet(vm, item) { accessFor = null } }
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Remove ${item.config.label}?") },
            text = {
                Text(
                    if (item.installedVersion != null) "This removes it from TwinnDroid only. The installed clone stays on your device; uninstall it separately if you want it gone."
                    else "This removes the clone settings from TwinnDroid.",
                )
            },
            confirmButton = { TextButton(onClick = { vm.delete(item.config); confirmDelete = null }) { Text( stringResource(R.string.homescreen_remove)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text( stringResource(R.string.homescreen_cancel)) } },
        )
    }
}

@Composable
private fun EmptyState(onNewClone: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(LocalTwinnExtras.current.headerBrush), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(120.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text( stringResource(R.string.homescreen_no_clones_yet), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
             stringResource(R.string.homescreen_make_a_second_copy_of_an_app_with_it),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onNewClone) { Text( stringResource(R.string.homescreen_make_your_first_clone)) }
    }
}

@Composable
fun WarningCard(title: String, body: String, action: String, modifier: Modifier = Modifier, onAction: () -> Unit) {
    TwinnCard(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        container = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun CloneCard(
    item: CloneItem,
    modifier: Modifier,
    onOpen: () -> Unit,
    onBuild: () -> Unit,
    onEdit: () -> Unit,
    onUninstall: () -> Unit,
    onDelete: () -> Unit,
    onAccess: () -> Unit,
) {
    val c = item.config
    var menu by remember { mutableStateOf(false) }
    TwinnCard(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.icon != null) {
                    Image(item.icon, null, Modifier.size(52.dp))
                } else {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        c.clonePackage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    StatusChip(item)
                    if (item.missingAccess.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Needs ${item.missingAccess.joinToString { it.label.lowercase() }} · tap to grant",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAccess).padding(vertical = 2.dp),
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text( stringResource(R.string.homescreen_edit)) }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; onEdit() })
                        DropdownMenuItem(text = { Text( stringResource(R.string.homescreen_rebuild_reinstall)) }, leadingIcon = { Icon(Icons.Rounded.Build, null) }, onClick = { menu = false; onBuild() })
                        if (item.installedVersion != null) {
                            DropdownMenuItem(text = { Text( stringResource(R.string.homescreen_uninstall_clone)) }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { menu = false; onUninstall() })
                        }
                        if (item.access.isNotEmpty()) {
                            DropdownMenuItem(text = { Text( stringResource(R.string.homescreen_special_access)) }, leadingIcon = { Icon(Icons.Rounded.AdminPanelSettings, null) }, onClick = { menu = false; onAccess() })
                        }
                        DropdownMenuItem(text = { Text( stringResource(R.string.homescreen_remove_from_list)) }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    item.updateAvailable -> FilledTonalButton(onClick = onBuild) {
                        Icon(Icons.Rounded.SystemUpdate, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text( stringResource(R.string.homescreen_update))
                    }
                    item.installedVersion == null -> FilledTonalButton(onClick = onBuild) {
                        Icon(Icons.Rounded.Build, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text( stringResource(R.string.homescreen_build_install))
                    }
                }
                if (item.installedVersion != null) {
                    OutlinedButton(onClick = onOpen) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text( stringResource(R.string.homescreen_open))
                    }
                }
                OutlinedButton(onClick = onEdit) { Text( stringResource(R.string.homescreen_customize)) }
            }
        }
    }
}

@Composable
private fun StatusChip(item: CloneItem) {
    val (text, color) = when {
        item.updateAvailable -> "Update available" to MaterialTheme.colorScheme.tertiary
        item.installedVersion != null -> "Installed · ${item.config.builtVersionName ?: item.installedVersion}" to MaterialTheme.colorScheme.secondary
        else -> "Not installed" to MaterialTheme.colorScheme.outline
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        Text(
            "  ·  from ${item.config.sourceLabel}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
