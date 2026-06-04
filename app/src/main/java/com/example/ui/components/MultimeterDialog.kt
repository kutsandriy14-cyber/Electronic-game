package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.lang.AppLanguage
import com.example.lang.Lang
import com.example.model.ComponentType
import com.example.model.GridComponent

@Composable
fun MultimeterDialog(
    lang: AppLanguage,
    component: GridComponent, 
    coords: Pair<Int, Int>, 
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Lang.t("multimeter", lang).uppercase(), style = MaterialTheme.typography.titleMedium, color = Color(0xFF00BCD4), letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                val displayName = Lang.getComponentDisplayName(component.type, lang)
                Text("Component: $displayName", style = MaterialTheme.typography.bodyLarge)
                Text("Position: X: ${coords.first} Y: ${coords.second}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (component.type == ComponentType.EMPTY) {
                    Text("Empty space", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Power State", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(if (component.isPowered) "POWERED" else "OFF", style = MaterialTheme.typography.bodyLarge, color = if (component.isPowered) Color(0xFF4CAF50) else Color(0xFFE53935))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Logic State", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(if (component.logicState) "HIGH (1)" else "LOW (0)", style = MaterialTheme.typography.bodyLarge, color = if (component.logicState) Color(0xFF2196F3) else Color(0xFF9E9E9E))
                        }
                    }
                    val isStorage = component.type in listOf(ComponentType.BATTERY, ComponentType.BATTERY_PACK, ComponentType.COIN_CELL, ComponentType.CAPACITOR)
                    if (isStorage) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (component.type == ComponentType.CAPACITOR) "Capacitor Charge" else "Battery Charge", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        val maxCap = if (component.type == ComponentType.CAPACITOR) component.voltage * 1000f else com.example.engine.RenderEngine.getMaxCap(component)
                        val doubleMaxCap = if (maxCap > 0f) maxCap else 1f
                        val fraction = (component.charge / doubleMaxCap).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                        )
                        val displayStr = if (component.type == ComponentType.CAPACITOR) {
                            String.format(java.util.Locale.US, "%.1f uC", component.charge)
                        } else {
                            String.format(java.util.Locale.US, "%.0f / %.0f mAh", component.charge.coerceAtLeast(0f), maxCap)
                        }
                        Text(displayStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ELECTRICAL DIAGNOSTICS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Voltage Drop", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(String.format(java.util.Locale.US, "%.2f V", component.voltage), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(if (component.current >= 1000f) String.format(java.util.Locale.US, "%.2f A", component.current / 1000f) else String.format(java.util.Locale.US, "%.1f mA", component.current), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Resistance", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(if (component.resistance >= 1000f) String.format(java.util.Locale.US, "%.1f kΩ", component.resistance / 1000f) else String.format(java.util.Locale.US, "%.1f Ω", component.resistance), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Power Consumption", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            val powerMw = component.voltage * component.current
                            Text(if (powerMw >= 1000f) String.format(java.util.Locale.US, "%.2f W", powerMw / 1000f) else String.format(java.util.Locale.US, "%.1f mW", powerMw), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }

                    if (component.isOverloaded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Overloaded! Needs repair.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { 
                        Text(Lang.t("close", lang)) 
                    }
                }
            }
        }
    }
}
