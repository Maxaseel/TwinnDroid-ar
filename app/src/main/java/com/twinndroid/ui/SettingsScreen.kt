package com.twinndroid.ui

import androidx.compose.ui.res.stringResource
import com.twinndroid.R

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinndroid.BuildConfig
import com.twinndroid.data.BackupManager
import com.twinndroid.engine.SigningKeys
import com.twinndroid.install.ShizukuInstaller
import rikka.shizuku.Shizuku

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var useShizuku by remember { mutableStateOf(vm.settings.useShizuku) }
    var autoUpdate by remember { mutableStateOf(vm.settings.autoUpdate) }
    var fingerprint by remember { mutableStateOf(runCatching { SigningKeys.fingerprint(context) }.getOrDefault("?")) }

    DisposableEffect(Unit) {
        val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> tick++ }
        val binderListener = Shizuku.OnBinderReceivedListener { tick++ }
        val deadListener = Shizuku.OnBinderDeadListener { tick++ }
        Shizuku.addRequestPermissionResultListener(permListener)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(deadListener)
        onDispose {
            Shizuku.removeRequestPermissionResultListener(permListener)
            Shizuku.removeBinderReceivedListener(binderListener)
            Shizuku.removeBinderDeadListener(deadListener)
        }
    }

    val exportKey = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-pkcs12")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { SigningKeys.export(context, it) }
        }.onFailure { vm.message = "Export failed: ${it.message}" }
    }
    val importKey = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)!!.use { SigningKeys.import(context, it) }
            fingerprint = SigningKeys.fingerprint(context)
        }.onFailure { vm.message = "Not a valid TwinnDroid key: ${it.message}" }
    }

    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { BackupManager.export(context, it) }
        }.onSuccess { vm.message = "Backup saved." }.onFailure { vm.message = "Backup failed: ${it.message}" }
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)!!.use { BackupManager.restore(context, it) }
            fingerprint = SigningKeys.fingerprint(context)
            useShizuku = vm.settings.useShizuku
            autoUpdate = vm.settings.autoUpdate
        }.onSuccess { vm.message = "Backup restored. Your clones are back." }.onFailure { vm.message = "Restore failed: ${it.message}" }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                title = { Text( stringResource(R.string.settingsscreen_settings)) },
                navigationIcon = { IconButton(onClick = { vm.screen = Screen.Home }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val running = remember(tick) { ShizukuInstaller.isRunning() }
            val granted = remember(tick) { ShizukuInstaller.hasPermission() }

            SettingsCard("Appearance") {
                Text( stringResource(R.string.settingsscreen_themes_custom_colours_text_size_and), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.screen = Screen.Appearance }) { Text( stringResource(R.string.settingsscreen_open_appearance_settings)) }
            }

            SettingsCard("Shizuku (optional)") {
                Text(
                    when {
                        granted -> "✓ Ready — clones install without a confirmation dialog."
                        running -> "Shizuku is running but TwinnDroid isn't authorized yet."
                        else -> "Not running. Install Shizuku and start it with Wireless debugging (no root needed) for prompt-free installs and background auto-updates."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                when {
                    running && !granted -> Button(onClick = ShizukuInstaller::requestPermission) { Text( stringResource(R.string.settingsscreen_authorize_twinndroid)) }
                    !running -> OutlinedButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
                    }) { Text( stringResource(R.string.settingsscreen_get_shizuku)) }
                }
                ToggleRow("Use Shizuku for installs", "Falls back to Android's installer when unavailable.", useShizuku) {
                    useShizuku = it; vm.settings.useShizuku = it
                }
            }

            SettingsCard("Updates") {
                Text(
                     stringResource(R.string.settingsscreen_twinndroid_checks_every_few_hours_wh) +
                        "Rebuilding keeps the clone's data and login because it's signed with the same key.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                ToggleRow("Auto-update in background", "Rebuilds and installs silently. Requires Shizuku.", autoUpdate, enabled = granted || autoUpdate) {
                    autoUpdate = it; vm.settings.autoUpdate = it
                }
            }

            SettingsCard("Signing key") {
                Text(
                     stringResource(R.string.settingsscreen_all_clones_are_signed_with_this_key),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text( stringResource(R.string.settingsscreen_sha_256), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fingerprint, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportKey.launch("twinndroid-key.p12") }) { Text( stringResource(R.string.settingsscreen_export)) }
                    OutlinedButton(onClick = { importKey.launch(arrayOf("*/*")) }) { Text( stringResource(R.string.settingsscreen_import)) }
                }
            }

            SettingsCard("Backup & restore") {
                Text(
                     stringResource(R.string.settingsscreen_saves_your_clone_list_signing_key_im) +
                        "reinstalling TwinnDroid or on a new phone and your clones keep updating.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportBackup.launch("TwinnDroid-backup.zip") }) { Text( stringResource(R.string.settingsscreen_back_up)) }
                    OutlinedButton(onClick = { importBackup.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }) { Text( stringResource(R.string.settingsscreen_restore)) }
                }
            }

            SettingsCard("About") {
                Text("TwinnDroid ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                     stringResource(R.string.settingsscreen_clones_are_real_separately_installed) +
                        "custom permissions inside the APK, applies your name/icon/permission tweaks, then re-signs it. " +
                        "Nothing runs inside TwinnDroid and no root is needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    TwinnCard {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
