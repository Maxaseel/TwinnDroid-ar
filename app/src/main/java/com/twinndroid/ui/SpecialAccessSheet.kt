package com.twinndroid.ui

import androidx.compose.ui.res.stringResource
import com.twinndroid.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.twinndroid.install.ShizukuInstaller
import com.twinndroid.install.SpecialAccess

/**
 * Lets the user grant a clone the Settings-page permissions it asks for. The clone's own buttons
 * usually open the original app's page, so these open the clone's page (or grant via Shizuku).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialAccessSheet(vm: MainViewModel, item: CloneItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shizuku = ShizukuInstaller.hasPermission()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text( stringResource(R.string.specialaccesssheet_special_access), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "${item.config.label} asks for these. Its own \"Open settings\" button opens the page for " +
                    "${item.config.sourceLabel} instead, so grant them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            item.access.forEachIndexed { i, (access, granted) ->
                if (i > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                        null,
                        tint = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(access.label)
                        Text(
                            if (granted) "Allowed" else "Not allowed yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!granted) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 34.dp, bottom = 4.dp)) {
                        if (shizuku) Button(onClick = { vm.grantAccess(item, access) }) { Text( stringResource(R.string.specialaccesssheet_grant)) }
                        OutlinedButton(onClick = { SpecialAccess.openSettings(context, item.config.clonePackage, access) }) {
                            Text( stringResource(R.string.specialaccesssheet_open_settings))
                        }
                    }
                }
            }
            if (!shizuku) {
                Spacer(Modifier.height(8.dp))
                Text(
                     stringResource(R.string.specialaccesssheet_tip_with_shizuku_authorized_in_setti),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
