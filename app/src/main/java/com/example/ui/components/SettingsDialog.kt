package com.example.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lang.AppLanguage
import com.example.lang.Lang
import com.example.viewmodel.SimulatorViewModel
import com.example.engine.JavaModManager
import com.example.engine.JavaModEngine
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ContentCopy
import java.io.File

@Composable
fun SettingsDialog(
    lang: AppLanguage,
    onChangeLang: (AppLanguage) -> Unit,
    onDismiss: () -> Unit, 
    onResize: (Int, Int) -> Unit, 
    exportScheme: () -> String, 
    importScheme: (String) -> Unit,
    allocatedRamGb: Int,
    allocatedCores: Int,
    clockMhz: Int,
    canvasStyle: String,
    onHardwareUpdate: (ramGb: Int, cores: Int, clockMhz: Int, canvasStyle: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    // Auto-detect actual hardware capabilities of this physical phone
    val physicalCores = remember { SimulatorViewModel.getPhysicalCpuCores() }
    val physicalRamGb = remember { SimulatorViewModel.getMaxPhysicalRamGb() }
    
    // Toggle for per-core allocation vs global allocation
    var allocatePerCore by remember { mutableStateOf(true) }

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Lang.t("settings_dialog_title", lang), 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                // Modern tab division
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(if (lang == AppLanguage.RU) "Общие" else if (lang == AppLanguage.UK) "Загальні" else "General", style = MaterialTheme.typography.labelLarge) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(if (lang == AppLanguage.RU) "Оформление" else if (lang == AppLanguage.UK) "Вигляд" else "Aesthetics", style = MaterialTheme.typography.labelLarge) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(if (lang == AppLanguage.RU) "Железо" else if (lang == AppLanguage.UK) "Залізо" else "Hardware", style = MaterialTheme.typography.labelLarge) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text(if (lang == AppLanguage.RU) "Java Моды" else if (lang == AppLanguage.UK) "Java Моди" else "Java Mods", style = MaterialTheme.typography.labelLarge) }
                    )
                }

                // Scrollable container for selected tab content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // General & Grid Settings
                            Text(
                                text = Lang.t("language", lang), 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
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
                            
                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                text = Lang.t("grid_size", lang), 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = if (lang == AppLanguage.RU) "Внимание: изменение размера очистит текущую схему!" else if (lang == AppLanguage.UK) "Увага: зміна розміру поля очистить поточну схему!" else "Warning: Resizing will reset your current circuit diagram!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val sizeOptions = listOf(
                                    Triple(8, 8, if (lang == AppLanguage.RU) "Малый прототип (8x8)" else "Small Prototype (8x8)"),
                                    Triple(16, 16, if (lang == AppLanguage.RU) "Стандартный мобильный (16x16)" else "Mobile Standard (16x16)"),
                                    Triple(32, 32, if (lang == AppLanguage.RU) "Планшетный расширенный (32x32)" else "Tablet Expansive (32x32)"),
                                    Triple(64, 64, if (lang == AppLanguage.RU) "Огромная плата (64x64)" else "Massive Blueprint (64x64)"),
                                    Triple(128, 128, if (lang == AppLanguage.RU) "Материнская плата L1 (128x128)" else "Motherboard Level 1 (128x128)")
                                )
                                sizeOptions.forEach { (w, h, label) ->
                                    Button(
                                        onClick = { onResize(w, h) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Text(label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                        
                        1 -> { // Aesthetics / Canvas theme selection / Scheme sharing
                            Text(
                                text = Lang.t("canvas_style", lang), 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val styles = listOf(
                                    Triple("dark_neon", Lang.t("canvas_classic", lang), Color(0xFF1E1E2E)),
                                    Triple("blueprint", Lang.t("canvas_blueprint", lang), Color(0xFF0F172A)),
                                    Triple("oled_black", Lang.t("canvas_oled", lang), Color(0xFF000000))
                                )
                                styles.forEach { (styleKey, styleName, bgCol) ->
                                    val isSelected = canvasStyle == styleKey
                                    OutlinedButton(
                                        onClick = { onHardwareUpdate(allocatedRamGb, allocatedCores, clockMhz, styleKey) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = if (isSelected) {
                                            ButtonDefaults.outlinedButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        } else {
                                            ButtonDefaults.outlinedButtonColors()
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(bgCol)
                                                .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = styleName, 
                                            style = MaterialTheme.typography.bodyMedium, 
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(24.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                text = if (lang == AppLanguage.RU) "Управление чертежами" else "Project Blueprint Sharing", 
                                style = MaterialTheme.typography.titleMedium, 
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { exportLauncher.launch("schematic.esshim") }, 
                                    modifier = Modifier.weight(1f)
                                ) { 
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (lang == AppLanguage.RU) "Экспорт в файл" else "Export File", fontSize = 12.sp) 
                                }
                                OutlinedButton(
                                    onClick = { importLauncher.launch(arrayOf("*/*")) }, 
                                    modifier = Modifier.weight(1f)
                                ) { 
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (lang == AppLanguage.RU) "Импорт .esshim" else "Import File", fontSize = 12.sp) 
                                }
                            }
                        }
                        
                        2 -> { // Hardware Throttling Limits
                            // Auto-detection status card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (lang == AppLanguage.RU) "Характеристики вашего телефона:" else "Physical Phone Auto-Detection:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (lang == AppLanguage.RU) 
                                            "• Количество ядер процессора: $physicalCores\n• Объем выделенной памяти JVM: $physicalRamGb ГБ" 
                                            else 
                                            "• Physical core capacity: $physicalCores Cores\n• Available JVM memory heap: $physicalRamGb GB",
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { onHardwareUpdate(physicalRamGb, physicalCores, 2400, canvasStyle) },
                                        modifier = Modifier.align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (lang == AppLanguage.RU) "Загрузить реальные лимиты" else "Use Real HW Limits", fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            // Cores Limit slider
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = Lang.t("simulated_cores", lang), 
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Slider(
                                        value = allocatedCores.toFloat(),
                                        onValueChange = { onHardwareUpdate(allocatedRamGb, it.toInt(), clockMhz, canvasStyle) },
                                        valueRange = 1f..physicalCores.coerceAtLeast(4).toFloat(),
                                        steps = (physicalCores.coerceAtLeast(4) - 2)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("$allocatedCores ${if (lang == AppLanguage.RU) "Ядер" else "Cores"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = if (allocatedCores <= 1) "Heavy Queue Delay (+15ms)" else "Sync Dispatch",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (allocatedCores <= 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            // Dynamic allocation type switcher
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.RU) "Выделять по ядрам (поядерно)" else "Specify Core Frequency Allocation",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Switch(
                                    checked = allocatePerCore,
                                    onCheckedChange = { allocatePerCore = it }
                                )
                            }
                            
                            // MHz Allocator core Slider
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (allocatePerCore) {
                                            if (lang == AppLanguage.RU) "Тактовая частота на ядро (МГц)" else "Clock Frequency Per Core (MHz)"
                                        } else {
                                            Lang.t("simulated_mhz", lang)
                                        }, 
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Slider(
                                        value = clockMhz.toFloat(),
                                        onValueChange = { onHardwareUpdate(allocatedRamGb, allocatedCores, it.toInt(), canvasStyle) },
                                        valueRange = 500f..4000f,
                                        steps = 35
                                    )
                                    
                                    val totalBudgetMhz = if (allocatePerCore) allocatedCores * clockMhz else clockMhz
                                    val estimatedStepsAllowed = (totalBudgetMhz / 220).coerceIn(1, 20)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("$clockMhz MHz", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = if (allocatePerCore) "${if (lang == AppLanguage.RU) "Всего в сумме:" else "Total Clock Pool:"} $totalBudgetMhz MHz" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                            
                            // Real CPU power enforcement feedback card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val totalBudgetMhz = if (allocatePerCore) allocatedCores * clockMhz else clockMhz
                                    val estimatedStepsAllowed = (totalBudgetMhz / 220).coerceIn(1, 20)
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (lang == AppLanguage.RU) "Лимит производительности:" else "Enforced Physical Computational Limits:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (lang == AppLanguage.RU)
                                            "Тяжелые детали ограничены вашими лимитами. Больше выделенного объема расчет НЕ ПОЙДЕТ. Текущая скорость физики будет аппаратно снижена до: $estimatedStepsAllowed тиков/кадр."
                                            else
                                            "Severe calculations are hardware-throttled. The emulator will strictly NOT exceed chosen clock boundary. Current simulated speed capped to: $estimatedStepsAllowed cycle-ticks.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            
                            // RAM Allocation Limit Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = Lang.t("simulated_ram", lang), 
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Slider(
                                        value = allocatedRamGb.toFloat(),
                                        onValueChange = { onHardwareUpdate(it.toInt(), allocatedCores, clockMhz, canvasStyle) },
                                        valueRange = 1f..physicalRamGb.coerceAtLeast(8).toFloat(),
                                        steps = (physicalRamGb.coerceAtLeast(8) - 2)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("$allocatedRamGb GB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = if (allocatedRamGb <= 2) "Low Memory Throttled" else "RAM Verified Balanced",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (allocatedRamGb <= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            JavaModdingTab(lang)
                        }
                    }
                }
            }
        },
        confirmButton = { 
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) { 
                Text(
                    text = Lang.t("close", lang),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ) 
            } 
        }
    )
}

@Composable
fun JavaModdingTab(lang: AppLanguage) {
    val context = LocalContext.current
    var scriptCode by remember { mutableStateOf(JavaModEngine.getCurrentModScript()) }
    var activeModName by remember { mutableStateOf(JavaModEngine.getActiveModName()) }
    var errorStatus by remember { mutableStateOf(JavaModEngine.getLastParsingErrors()) }
    
    var benchmarkResult by remember { mutableStateOf("") }
    var isBenchmarking by remember { mutableStateOf(false) }
    
    val logs = remember { mutableStateListOf<String>().apply { 
        add("[Java Core Compiler] Ready to interpret ESMS code.")
        add("[Java Core Compiler] Current active mod: " + JavaModEngine.getActiveModName())
    } }

    val applyScript = {
        JavaModEngine.setCurrentModScript(scriptCode)
        val success = JavaModEngine.compileAndApplyModScript(scriptCode)
        activeModName = JavaModEngine.getActiveModName()
        errorStatus = JavaModEngine.getLastParsingErrors()
        
        logs.add("[JVM Compile] Tick: " + System.currentTimeMillis() % 1000)
        if (success) {
            logs.add("[SUCCESS] Applied Mod \"" + activeModName + "\" successfully!")
            logs.add("  - Water color: " + JavaModEngine.waterColorHex)
            logs.add("  - Vortex state: " + (if (JavaModEngine.vortexActive) "ACTIVE" else "INACTIVE") + " at (" + JavaModEngine.vortexCenterX + ", " + JavaModEngine.vortexCenterY + ")")
        } else {
            logs.add("[ERROR] " + errorStatus)
        }
        Unit
    }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(
            text = if (lang == AppLanguage.RU) "Редактор ESMS Модов (Ядро Java)" else if (lang == AppLanguage.UK) "Редактор ESMS Модів (Ядро Java)" else "Java ESMS Code Compiler",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = if (lang == AppLanguage.RU) 
                "Напишите код мода на новом синтаксисе ESMS. Модификации выполняются на лету прямо на виртуальной машине Java."
                else
                "Write custom mods in ESM Script syntax. Code updates execute instantly inside the high-octane Java Host Virtual Engine.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom script input area (Monospace, dark theme)
        OutlinedTextField(
            value = scriptCode,
            onValueChange = { scriptCode = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF0F1E1A), RoundedCornerShape(8.dp)),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Color(0xFF00FF99),
                fontSize = 11.sp
            ),
            label = { Text("ESMS Script Source Code", fontSize = 10.sp) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF334433)
            )
        )

        Spacer(Modifier.height(8.dp))

        // Actions Row: Compile, Export, Import
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = applyScript,
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (lang == AppLanguage.RU) "Запустить" else "Compile", fontSize = 11.sp, maxLines = 1)
            }

            Button(
                onClick = {
                    val esmFile = JavaModEngine.packAndSaveEsm(context, activeModName)
                    if (esmFile != null) {
                        logs.add("[EXPORT] Saved mod archives renamed as: " + esmFile.name)
                        logs.add("  - Path: " + esmFile.absolutePath)
                    } else {
                        logs.add("[EXPORT ERROR] Failed to compress script ZIP container as .esm")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (lang == AppLanguage.RU) "В .esm (ZIP)" else "Export .esm", fontSize = 10.sp, maxLines = 1)
            }

            Button(
                onClick = {
                    // Try to load any existing .esm file in directory or default
                    val dir = context.getExternalFilesDir("mods") ?: context.cacheDir
                    val files = dir.listFiles { _, name -> name.endsWith(".esm") }
                    if (files != null && files.isNotEmpty()) {
                        val target = files[0]
                        val success = JavaModEngine.unpackAndLoadEsm(context, target)
                        if (success) {
                            scriptCode = JavaModEngine.getCurrentModScript()
                            activeModName = JavaModEngine.getActiveModName()
                            errorStatus = JavaModEngine.getLastParsingErrors()
                            logs.add("[IMPORT] Successfully decompressed and loaded " + target.name)
                        } else {
                            logs.add("[IMPORT ERROR] Failed to parse mod.esms script inside " + target.name)
                        }
                    } else {
                        logs.add("[IMPORT] No .esm files found. Export a mod first!")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text(if (lang == AppLanguage.RU) "Загрузить .esm" else "Import .esm", fontSize = 10.sp, maxLines = 1)
            }
        }

        // Live Diagnostic Metrics compiled from active ESM variables
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Engine State - Compiled Parameters",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active Mod: $activeModName", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("Water Custom Color: ${JavaModEngine.waterColorHex}", fontSize = 10.sp)
                        Text("Brick Custom Color: ${JavaModEngine.brickColorHex}", fontSize = 10.sp)
                    }
                    Column {
                        Text("Vortex Whirlpool: " + (if (JavaModEngine.vortexActive) "ENABLED" else "DISABLED"), fontSize = 10.sp)
                        Text("Vortex Center: (${JavaModEngine.vortexCenterX}, ${JavaModEngine.vortexCenterY})", fontSize = 10.sp)
                        Text("Vortex Range/Force: ${JavaModEngine.vortexRadius} / ${JavaModEngine.vortexStrength}", fontSize = 10.sp)
                    }
                }
            }
        }

        // JVM Benchmark Actions Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isBenchmarking = true
                    val timeMs = JavaModManager.runModSystemBenchmark()
                    benchmarkResult = if (lang == AppLanguage.RU) "JVM: 100k ур. рещено за %.3f мс".format(timeMs) else "JVM: 100k eq. solved in %.3f ms".format(timeMs)
                    isBenchmarking = false
                    logs.add("[BENCHMARK] Sin/Cos 100,000 iterations ran in " + String.format("%.3f", timeMs) + " ms")
                },
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isBenchmarking) "..." else if (lang == AppLanguage.RU) "Тест скорости Java" else "Hardware Benchmarks", fontSize = 11.sp, maxLines = 1)
            }

            Button(
                onClick = {
                    logs.clear()
                    logs.add("[Console Logs Cleared]")
                },
                modifier = Modifier.weight(0.8f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outlineVariant, contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text(if (lang == AppLanguage.RU) "Очистить" else "Clear Logs", fontSize = 11.sp, maxLines = 1)
            }
        }

        if (benchmarkResult.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
                Text(
                    text = benchmarkResult,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Java Micro-console Logger Window
        Text(
            text = if (lang == AppLanguage.RU) "Лог модов ядра (Java Console)" else "Java Compiler Engine Console Log",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    logs.forEach { logLine ->
                        Text(
                            text = logLine,
                            color = when {
                                logLine.contains("[ERROR]") -> Color(0xFFFF5555)
                                logLine.contains("[SUCCESS]") -> Color(0xFF00FFCC)
                                logLine.contains("[EXPORT]") || logLine.contains("[IMPORT]") -> Color(0xFFFF9900)
                                else -> Color(0xFFCCCCCC)
                            },
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
