package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.functional.Battery

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
                    var showPresetsDialog by remember { mutableStateOf(false) }
                    
                    if (showPresetsDialog) {
                        PresetLibraryDialog(
                            lang = lang,
                            onDismissOnSelection = { code ->
                                textData = code
                                showPresetsDialog = false
                            },
                            onDismissRequest = { showPresetsDialog = false }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(Lang.t("advanced_memory", lang), style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Format: cores=X|mhz=Y|mem_kb=Z", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        
                        Button(
                            onClick = { showPresetsDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when(lang) {
                                    AppLanguage.RU -> "Библиотека"
                                    AppLanguage.UK -> "Бібліотека"
                                    else -> "Library"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
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
                            maxV * (component.charge / com.example.engine.TabletRender.getMaxCap(component))
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
                    if (!component.isOverloaded && (Battery.isBattery(component.type) || component.type == ComponentType.CAPACITOR)) {
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

data class ScriptPreset(
    val titleEn: String,
    val titleRu: String,
    val titleUa: String,
    val descEn: String,
    val descRu: String,
    val descUa: String,
    val categoryEn: String,
    val categoryRu: String,
    val categoryUa: String,
    val code: String
)

val scriptPresets = listOf(
    ScriptPreset(
        titleEn = "Repeater / Pass-through (Top -> Right)",
        titleRu = "Повторитель (Сверху -> Вправо)",
        titleUa = "Повторювач (Зверху -> Вправо)",
        descEn = "Reads top pin in(0) and outputs to right pin out(1).",
        descRu = "Принимает сигнал сверху (in(0)) и выдает вправо (out(1)).",
        descUa = "Приймає сигнал зверху (in(0)) та видає вправо (out(1)).",
        categoryEn = "Conveyance",
        categoryRu = "Передача",
        categoryUa = "Передача",
        code = "-- Repeater Top -> Right\nif in(0) == 1 then out(1, 1)\nif in(0) == 0 then out(1, 0)"
    ),
    ScriptPreset(
        titleEn = "Repeater / Pass-through (Left -> Right)",
        titleRu = "Повторитель (Слева -> Вправо)",
        titleUa = "Повторювач (Зліва -> Вправо)",
        descEn = "Reads left pin in(3) and outputs to right pin out(1).",
        descRu = "Принимает сигнал слева (in(3)) и выдает вправо (out(1)).",
        descUa = "Приймає сигнал зліва (in(3)) та видає вправо (out(1)).",
        categoryEn = "Conveyance",
        categoryRu = "Передача",
        categoryUa = "Передача",
        code = "-- Repeater Left -> Right\nif in(3) == 1 then out(1, 1)\nif in(3) == 0 then out(1, 0)"
    ),
    ScriptPreset(
        titleEn = "Signal Splitter (Left -> All Outputs)",
        titleRu = "Делитель сигнала (Слева -> На все выходы)",
        titleUa = "Поділювач сигналу (Зліва -> На всі виходи)",
        descEn = "Splits Left input (in(3)) and repeats it to Top (0), Right (1) and Bottom (2) pins.",
        descRu = "Берет сигнал слева (in(3)) и дублирует его вверх (0), вправо (1) и вниз (2).",
        descUa = "Бере сигнал зліва (in(3)) та дублює його вгору (0), вправо (1) та вниз (2).",
        categoryEn = "Conveyance",
        categoryRu = "Передача",
        categoryUa = "Передача",
        code = "-- Splitter: Left -> Top, Right, Bottom\nif in(3) == 1 then out(0, 1)\nif in(3) == 1 then out(1, 1)\nif in(3) == 1 then out(2, 1)\nif in(3) == 0 then out(0, 0)\nif in(3) == 0 then out(1, 0)\nif in(3) == 0 then out(2, 0)"
    ),
    ScriptPreset(
        titleEn = "Logical AND Gate",
        titleRu = "Логическое И (AND)",
        titleUa = "Логічне ТА (AND)",
        descEn = "Output Right (1) is 1 only if Top (0) AND Left (3) are both 1.",
        descRu = "Сигнал справа (1) появится только если есть сигнал сверху (0) И слева (3).",
        descUa = "Сигнал праворуч (1) з'явиться тільки якщо є сигнал зверху (0) ТА зліва (3).",
        categoryEn = "Logic Gates",
        categoryRu = "Логика",
        categoryUa = "Логіка",
        code = "-- Logical AND Gate\nout(1, 0)\nif in(0) == 1 then out(1, in(3))"
    ),
    ScriptPreset(
        titleEn = "Logical OR Gate",
        titleRu = "Логическое ИЛИ (OR)",
        titleUa = "Логічне АБО (OR)",
        descEn = "Output Right (1) is 1 if Top (0) OR Left (3) is 1.",
        descRu = "Сигнал справа (1), если есть сигнал сверху (0) ИЛИ слева (3).",
        descUa = "Сигнал праворуч (1), якщо є сигнал зверху (0) АБО зліва (3).",
        categoryEn = "Logic Gates",
        categoryRu = "Логика",
        categoryUa = "Логіка",
        code = "-- Logical OR Gate\nout(1, 0)\nif in(0) == 1 then out(1, 1)\nif in(3) == 1 then out(1, 1)"
    ),
    ScriptPreset(
        titleEn = "Logical XOR Gate",
        titleRu = "Исключающее ИЛИ (XOR)",
        titleUa = "Виключне АБО (XOR)",
        descEn = "Output Right (1) is 1 if Top (0) and Left (3) values differ.",
        descRu = "Сигнал справа (1) появится только если входные сигналы сверху (0) и слева (3) разные.",
        descUa = "Сигнал праворуч (1) з'явиться тільки якщо вхідні сигнали зверху (0) та зліва (3) відрізняються.",
        categoryEn = "Logic Gates",
        categoryRu = "Логика",
        categoryUa = "Логіка",
        code = "-- Logical XOR Gate\nout(1, 0)\nif in(0) != in(3) then out(1, 1)"
    ),
    ScriptPreset(
        titleEn = "Logical NOT Gate (Inverter)",
        titleRu = "Инвертор НЕ (NOT)",
        titleUa = "Інвертор НІ (NOT)",
        descEn = "Inverts Top pin in(0) input and outputs to Right pin out(1).",
        descRu = "Инвертирует сигнал сверху (in(0)) и подает его вправо (out(1)).",
        descUa = "Інвертує сигнал зверху (in(0)) та подає його вправо (out(1)).",
        categoryEn = "Logic Gates",
        categoryRu = "Логика",
        categoryUa = "Логіка",
        code = "-- Logical NOT Gate\nif in(0) == 1 then out(1, 0)\nif in(0) == 0 then out(1, 1)"
    ),
    ScriptPreset(
        titleEn = "SR Latch Memory",
        titleRu = "SR-Триггер памяти",
        titleUa = "SR-Тригер пам'яті",
        descEn = "Top pin (0) acts as SET, Left pin (3) as RESET. Output is Right (1).",
        descRu = "Верх (0) – SET (сохранить 1), Лево (3) – RESET (сбросить в 0). Выход праворуч (1).",
        descUa = "Верх (0) – SET (зберегти 1), Ліво (3) – RESET (скинути в 0). Вихід праворуч (1).",
        categoryEn = "Memory",
        categoryRu = "Память",
        categoryUa = "Пам'ять",
        code = "-- SR Latch (Set/Reset)\nif in(0) == 1 then out(1, 1)\nif in(3) == 1 then out(1, 0)"
    ),
    ScriptPreset(
        titleEn = "D Latch Memory",
        titleRu = "D-Защёлка памяти",
        titleUa = "D-Засувка пам'яті",
        descEn = "Saves Data from Left (3) whenever Enable on Top (0) is active.",
        descRu = "Запоминает сигнал слева (3), пока подан сигнал сверху (0).",
        descUa = "Запам'ятовує сигнал зліва (3), доки подано сигнал зверху (0).",
        categoryEn = "Memory",
        categoryRu = "Память",
        categoryUa = "Пам'ять",
        code = "-- D Latch\nif in(0) == 1 then out(1, in(3))"
    ),
    ScriptPreset(
        titleEn = "Smart Interlock Override",
        titleRu = "Умная аварийная автоблокировка",
        titleUa = "Розумне аварійне автоблокування",
        descEn = "Passes Top (0) to Right (1), but instantly shuts down if Left (3) override goes active.",
        descRu = "Пропускает сигнал сверху (0) вправо (1), но моментально отключает, если слева (3) подан сигнал тревоги.",
        descUa = "Пропускає сигнал зверху (0) вправо (1), але миттєво вимикає, якщо зліва (3) подано сигнал тривоги.",
        categoryEn = "Automation",
        categoryRu = "Автоматика",
        categoryUa = "Автоматика",
        code = "-- Smart Safety Interlock\nif in(3) == 1 then out(1, 0)\nif in(3) == 0 then out(1, in(0))"
    )
)

@Composable
fun PresetLibraryDialog(
    lang: AppLanguage,
    onDismissOnSelection: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                val title = when(lang) {
                    AppLanguage.RU -> "Библиотека скриптов МК"
                    AppLanguage.UK -> "Бібліотека скриптів МК"
                    else -> "MCU Script Library"
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val categorized = scriptPresets.groupBy { 
                        when(lang) {
                            AppLanguage.RU -> it.categoryRu
                            AppLanguage.UK -> it.categoryUa
                            else -> it.categoryEn
                        }
                    }
                    
                    categorized.forEach { (category, presets) ->
                        item {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }
                        
                        items(presets) { preset ->
                            val pTitle = when(lang) {
                                AppLanguage.RU -> preset.titleRu
                                AppLanguage.UK -> preset.titleUa
                                else -> preset.titleEn
                            }
                            val pDesc = when(lang) {
                                AppLanguage.RU -> preset.descRu
                                AppLanguage.UK -> preset.descUa
                                else -> preset.descEn
                            }
                            
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = pTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = pDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Code Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = Color(0xFF1E1E1E),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = preset.code,
                                            color = Color(0xFFA9B7C6),
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        val btnText = when(lang) {
                                            AppLanguage.RU -> "Загрузить"
                                            AppLanguage.UK -> "Завантажити"
                                            else -> "Load"
                                        }
                                        Button(
                                            onClick = { onDismissOnSelection(preset.code) },
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(btnText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val closeText = when(lang) {
                        AppLanguage.RU -> "Закрыть"
                        AppLanguage.UK -> "Закрити"
                        else -> "Close"
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text(closeText)
                    }
                }
            }
        }
    }
}
