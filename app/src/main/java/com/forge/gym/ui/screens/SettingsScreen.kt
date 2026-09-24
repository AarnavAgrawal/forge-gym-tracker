package com.forge.gym.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.forge.gym.data.WeightUnit
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.components.ConfirmDialog
import com.forge.gym.ui.components.DetailTopBar
import com.forge.gym.ui.components.FilterPill
import com.forge.gym.ui.components.ForgeCard
import com.forge.gym.ui.components.SegmentedTabs
import com.forge.gym.ui.components.Stepper
import com.forge.gym.ui.rememberSettings
import com.forge.gym.ui.theme.Forge
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(nav: NavController) {
    val c = LocalContainer.current
    val settings = rememberSettings()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val json = c.backup.export()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
                }
            }
            Toast.makeText(context, if (result.isSuccess) "Backup saved" else "Export failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    Column(Modifier.fillMaxSize().background(Forge.Bg)) {
        DetailTopBar("Settings", onBack = { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsGroup("Units") {
                SegmentedTabs(
                    listOf("Pounds (lb)", "Kilograms (kg)"),
                    if (settings.unit == WeightUnit.LB) 0 else 1,
                    { i -> c.settings.update { it.copy(unit = if (i == 0) WeightUnit.LB else WeightUnit.KG) } },
                    indicator = Forge.Lime, selectedText = Forge.OnLime,
                )
            }
            SettingsGroup("Rest timer") {
                Text("Default rest between sets", style = MaterialTheme.typography.bodyMedium, color = Forge.Text2)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(30, 60, 90, 120, 150, 180, 240, 300)) { s ->
                        FilterPill(if (s < 60) "${s}s" else if (s % 60 == 0) "${s / 60}m" else "${s / 60}:${(s % 60).toString().padStart(2, '0')}", s == settings.restSeconds) {
                            c.settings.update { it.copy(restSeconds = s) }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-start after each set", style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
                        Text("Vibrates when rest is over", style = MaterialTheme.typography.bodySmall, color = Forge.Text2)
                    }
                    Switch(
                        settings.autoRest, { v -> c.settings.update { it.copy(autoRest = v) } },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Forge.Lime, checkedThumbColor = Forge.OnLime,
                            uncheckedTrackColor = Forge.Surface3, uncheckedThumbColor = Forge.Text2, uncheckedBorderColor = Forge.Surface3,
                        ),
                    )
                }
            }
            SettingsGroup("Weekly goal") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Workouts per week", style = MaterialTheme.typography.titleSmall, color = Forge.Text1, modifier = Modifier.weight(1f))
                    Stepper(settings.weeklyGoal, { v -> c.settings.update { it.copy(weeklyGoal = v) } }, min = 1, max = 7)
                }
            }
            SettingsGroup("Data") {
                Text(
                    "Everything is stored only on this phone. Export a backup before switching phones or reinstalling.",
                    style = MaterialTheme.typography.bodySmall, color = Forge.Text2,
                )
                Spacer(Modifier.height(8.dp))
                SettingsRow(Icons.Rounded.Upload, "Export backup", "Save all data as a JSON file") {
                    exportLauncher.launch("forge-backup-${LocalDate.now()}.json")
                }
                SettingsRow(Icons.Rounded.Download, "Import backup", "Replace current data with a backup file") {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
            }
            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FORGE", style = MaterialTheme.typography.titleLarge, color = Forge.Lime)
                Text("Version 1.0  ·  Built for lifting", style = MaterialTheme.typography.bodySmall, color = Forge.Text3)
            }
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            "Import backup?", "This replaces ALL current workouts, routines, exercises, and body weight entries with the backup's contents.", "Import",
            onConfirm = {
                scope.launch {
                    val result = runCatching {
                        val json = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                        }
                        c.backup.import(json)
                    }
                    Toast.makeText(context, if (result.isSuccess) "Backup restored" else "Import failed: not a valid Forge backup", Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { pendingImport = null },
            destructive = true,
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    ForgeCard(Modifier.fillMaxWidth()) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Forge.Lime)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Forge.Surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Forge.Text1, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Forge.Text2)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Forge.Text3)
    }
}
