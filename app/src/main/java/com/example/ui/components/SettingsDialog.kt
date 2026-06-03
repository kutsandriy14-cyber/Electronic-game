package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lang.AppLanguage
import com.example.lang.Lang

@Composable
fun SettingsDialog(
    lang: AppLanguage,
    onChangeLang: (AppLanguage) -> Unit,
    onDismiss: () -> Unit, 
    onResize: (Int, Int) -> Unit, 
    exportScheme: () -> String, 
    importScheme: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(exportScheme().toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val text = input.bufferedReader().use { reader -> reader.readText() }
                    importScheme(text)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Lang.t("settings_dialog_title", lang), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Language Selection Section
                Text(Lang.t("language", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.values().forEach { appLang ->
                        val isSelected = appLang == lang
                        OutlinedButton(
                            onClick = { onChangeLang(appLang) },
                            modifier = Modifier.weight(1f),
                            colors = if (isSelected) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text(appLang.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                Text("Project Integration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { 
                    exportLauncher.launch("blueprint.esshim")
                }, modifier = Modifier.fillMaxWidth()) { 
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export to .esshim file") 
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { 
                    importLauncher.launch(arrayOf("*/*"))
                }, modifier = Modifier.fillMaxWidth()) { 
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import .esshim file") 
                }
                Spacer(Modifier.height(24.dp))
                
                Text(Lang.t("grid_size", lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text("Warning: Resizing clears your current circuit!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                
                Button(onClick = { onResize(8, 8) }, modifier = Modifier.fillMaxWidth()) { Text("Small Prototype (8x8)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(16, 16) }, modifier = Modifier.fillMaxWidth()) { Text("Mobile Standard (16x16)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(32, 32) }, modifier = Modifier.fillMaxWidth()) { Text("Tablet Expansive (32x32)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(64, 64) }, modifier = Modifier.fillMaxWidth()) { Text("Massive Blueprint (64x64)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(128, 128) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Motherboard Level 1 (128x128)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(256, 256) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Motherboard Level 2 (256x256)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onResize(512, 512) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("City Grid Level (512x512)") }
            }
        },
        confirmButton = { 
            TextButton(onClick = onDismiss) { 
                Text(Lang.t("close", lang)) 
            } 
        }
    )
}
