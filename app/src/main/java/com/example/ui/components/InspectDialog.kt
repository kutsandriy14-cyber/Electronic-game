package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.lang.AppLanguage
import com.example.lang.Lang
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.GridComponent

@Composable
fun InspectDialog(
    lang: AppLanguage,
    component: GridComponent, 
    onDismiss: () -> Unit, 
    onSave: (String, Boolean) -> Unit
) {
    var textData by remember { mutableStateOf(component.extraData) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                val displayName = Lang.getComponentDisplayName(component.type, lang)
                Text("${Lang.t("inspect", lang)}: $displayName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (component.type == ComponentType.MICROCONTROLLER) {
                    Text(Lang.t("advanced_memory", lang), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Format: cores=X|mhz=Y|mem_kb=Z", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    OutlinedTextField(
                        value = textData,
                        onValueChange = { textData = it },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        placeholder = { Text("Example: cores=2|mhz=16|mem_kb=1024\n\n-- SSM Script engine\n-- out(pin, val)\n-- Pin: 0(Top) 1(Right) 2(Bot) 3(Left)\n-- log(Hello World)\n-- if in(0) == 1 then out(1, 1)\n\nif 1==1 then log(Started)\nout(1, 1)") }
                    )
                } else if (component.type == ComponentType.MEMORY_RAM || component.type == ComponentType.MEMORY_ROM) {
                    Text(Lang.t("advanced_memory", lang), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Configurable properties (pipe separated)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    OutlinedTextField(
                        value = textData,
                        onValueChange = { textData = it },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        placeholder = { Text(if(component.type == ComponentType.MEMORY_RAM) "mem_kb=1024" else "mem_kb=16384") }
                    )
                } else if (component.type == ComponentType.MONITOR_OLED || component.type == ComponentType.CRT_MONITOR) {
                    Text(Lang.t("edit_properties", lang), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Format: display=TextToDisplay", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    OutlinedTextField(
                        value = textData,
                        onValueChange = { textData = it },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        placeholder = { Text("e.g. display=Hello") }
                    )
                } else {
                    Text(Lang.t("edit_properties", lang), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Format: key=value|key=value\n(v=voltage, c=capacity, r=resistance)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    OutlinedTextField(
                        value = textData,
                        onValueChange = { textData = it },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        placeholder = { Text("e.g. v=5|c=1000 or r=330") }
                    )
                    if (component.type in listOf(ComponentType.BATTERY, ComponentType.BATTERY_PACK, ComponentType.COIN_CELL, ComponentType.INFINITE_BATTERY)) {
                        Spacer(modifier=Modifier.height(8.dp))
                        val currentV = if (component.charge >= 0f) {
                            val maxV = textData.split("|").find { it.startsWith("v=") }?.substring(2)?.toFloatOrNull() ?: 9f
                            maxV * (component.charge / com.example.engine.PhoneRender.getMaxCap(component))
                        } else 0f
                        Text("${Lang.t("live_state", lang)}: ${String.format(java.util.Locale.US, "%.1f", component.charge.coerceAtLeast(0f))} mAh / ${String.format(java.util.Locale.US, "%.2f", currentV)} V", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (component.isOverloaded) {
                        Spacer(modifier=Modifier.height(8.dp))
                        Text("STATUS: OVERLOADED/BURNED OUT", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier=Modifier.height(4.dp))
                        Button(onClick = { onSave(textData, true) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { 
                            Text(Lang.t("recharge_repair", lang)) 
                        }
                    }
                }
                
                // Live physical readouts: Temperature and local Pressure
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Temperature: ${String.format(java.util.Locale.US, "%.1f", component.temperature)} °C",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (component.temperature > 1000f) Color(0xFFFF5722) else Color.White
                    )
                    Text(
                        text = "Pressure: ${String.format(java.util.Locale.US, "%.1f", component.pressure)} kPa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (component.pressure > 100f) Color(0xFFFF5722) else Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (component.charge >= 0f && !component.isOverloaded && component.type.category == ComponentCategory.POWER && component.type != ComponentType.AC_SOURCE && component.type != ComponentType.GENERATOR && component.type != ComponentType.SOLAR_PANEL) {
                        TextButton(onClick = { onSave(textData, true) }) { 
                            Text(Lang.t("recharge_repair", lang).substringBefore("/").substringBefore("&").trim()) 
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = onDismiss) { 
                        Text(Lang.t("cancel", lang)) 
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(textData, false) }) { 
                        Text(Lang.t("apply", lang)) 
                    }
                }
            }
        }
    }
}
