package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.db.CircuitScheme
import com.example.lang.AppLanguage
import com.example.lang.Lang

@Composable
fun LoadDialog(
    lang: AppLanguage,
    schemes: List<CircuitScheme>, 
    cloudSchemes: List<String>,
    onDismiss: () -> Unit, 
    onLoad: (CircuitScheme) -> Unit, 
    onLoadCloud: (String) -> Unit,
    onDelete: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(16.dp), 
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Lang.t("load_dialog_title", lang), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                if (schemes.isEmpty() && cloudSchemes.isEmpty()) {
                    Text(Lang.t("schemes_empty", lang), modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (cloudSchemes.isNotEmpty()) {
                            item {
                                Text("NetAuth Cloud Saves", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(cloudSchemes) { fileName ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                                    horizontalArrangement = Arrangement.SpaceBetween, 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onLoadCloud(fileName) }
                                            .padding(8.dp)
                                    ) {
                                        Text(fileName, style = MaterialTheme.typography.bodyLarge)
                                        Text("Cloud Save", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { onLoadCloud(fileName) }) { 
                                        Icon(Icons.Default.CloudDownload, "Download") 
                                    }
                                }
                            }
                        }

                        if (schemes.isNotEmpty()) {
                            item {
                                Text("Local Saves", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(schemes) { scheme ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                                    horizontalArrangement = Arrangement.SpaceBetween, 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onLoad(scheme) }
                                            .padding(8.dp)
                                    ) {
                                        Text(scheme.name, style = MaterialTheme.typography.bodyLarge)
                                        Text("Size: ${scheme.width}x${scheme.height}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    IconButton(onClick = { onDelete(scheme.id) }) { 
                                        Icon(Icons.Default.Delete, "Delete") 
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { 
                    Text(Lang.t("close", lang)) 
                }
            }
        }
    }
}
