package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.lang.AppLanguage
import com.example.lang.Lang
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Telemetry
import com.example.ui.components.ToolButton
import java.util.Locale

object UIEngine {

    // High-Level Tab configuration
    enum class MasterTab(val ruLabel: String, val ukLabel: String, val enLabel: String, val icon: String, val categories: List<ComponentCategory>) {
        CORE("Базовые", "Базові", "Core & Power", "🔋", listOf(
            ComponentCategory.TOOLS,
            ComponentCategory.POWER,
            ComponentCategory.CONDUCTORS,
            ComponentCategory.SWITCHES
        )),
        IO("Органы/Датчики", "Органи/Датчики", "I/O & Sensors", "📡", listOf(
            ComponentCategory.SENSORS,
            ComponentCategory.OUTPUTS
        )),
        CHIPS("Логика/Чипы", "Логіка/Чіпи", "Logic & Chips", "🎛️", listOf(
            ComponentCategory.LOGIC,
            ComponentCategory.ANALOG_ICS,
            ComponentCategory.ADVANCED
        )),
        PHYSICS("Среды/Физика", "Середовища/Физика", "Fluids & Solids", "🏔️", listOf(
            ComponentCategory.HYDRAULICS,
            ComponentCategory.MATERIALS
        ));

        fun getLabel(lang: AppLanguage): String {
            return when (lang) {
                AppLanguage.RU -> ruLabel
                AppLanguage.UK -> ukLabel
                else -> enLabel
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopToolBar(
        lang: AppLanguage,
        isSimulationRunning: Boolean,
        timeMultiplier: Int,
        onToggleSimulation: () -> Unit,
        onClearGrid: () -> Unit,
        onSetTimeMultiplier: (Int) -> Unit,
        onShowWiki: () -> Unit,
        onShowSave: () -> Unit,
        onShowLoad: () -> Unit,
        onShowSettings: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .padding(16.dp)
                .widthIn(max = 600.dp)
                .fillMaxWidth(0.9f)
                .background(Color(0xDD1B1B26), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x3300FFCC), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onToggleSimulation) {
                    Icon(
                        if (isSimulationRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Toggle Simulation",
                        tint = Color(0xFF00FFCC)
                    )
                }
                IconButton(onClick = onClearGrid) {
                    Icon(Icons.Default.Delete, "Clear", tint = Color(0xFFFF5555))
                }

                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .background(Color(0xFF13131F), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x22FFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 5, 10, 20).forEach { mul ->
                        val isSelected = timeMultiplier == mul
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) Color(0xFF00FFCC) else Color.Transparent,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                                .clickable { onSetTimeMultiplier(mul) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mul}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF1E1E2E) else Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))

                IconButton(onClick = onShowWiki) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, "Wiki", tint = Color(0xFF00FFCC))
                }
                IconButton(onClick = onShowSave) {
                    Icon(Icons.Default.Save, "Save", tint = Color.White)
                }
                IconButton(onClick = onShowLoad) {
                    Icon(Icons.Default.FolderOpen, "Load", tint = Color.White)
                }
                IconButton(onClick = onShowSettings) {
                    Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                }
            }
        }
    }

    @Composable
    fun TelemetryHUD(
        lang: AppLanguage,
        telemetry: Telemetry,
        isSimulationRunning: Boolean,
        grid: Array<Array<com.example.model.GridComponent>>,
        deviceCores: Int,
        deviceMaxFreq: String,
        deviceCurrentFreq: String,
        deviceTotalRam: String,
        deviceAvailRam: String,
        appRamUsedByGame: String,
        telemetryTab: String,
        telemetryState: String,
        onTabChanged: (String) -> Unit,
        onStateChanged: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        if (telemetryState == "HIDDEN") {
            Box(
                modifier = modifier
                    .padding(top = 80.dp, end = 16.dp)
                    .background(Color(0xD2121215), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x9900FFCC), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .clickable { onStateChanged("FULL") }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Show Telemetry",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (lang == AppLanguage.RU) "ТЕЛЕМЕТРИЯ [ + ]" else if (lang == AppLanguage.UK) "ТЕЛЕМЕТРІЯ [ + ]" else "TELEMETRY [ + ]",
                        fontSize = 11.sp,
                        color = Color(0xFF00FFCC),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Box(
                modifier = modifier
                    .padding(top = 80.dp, end = 16.dp)
                    .width(if (telemetryState == "COMPACT") 180.dp else 235.dp)
                    .background(Color(0xD2121215), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x3300FFCC), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == AppLanguage.RU) {
                                if (telemetryState == "COMPACT") "ТЕЛЕМЕТР." else "ТЕЛЕМЕТРИЯ"
                            } else if (lang == AppLanguage.UK) {
                                if (telemetryState == "COMPACT") "ТЕЛЕМЕТР." else "ТЕЛЕМЕТРІЯ"
                            } else {
                                if (telemetryState == "COMPACT") "TELEMETRY" else "SYSTEM TELEMETRY"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF00FFCC),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { onStateChanged(if (telemetryState == "FULL") "COMPACT" else "FULL") },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = if (telemetryState == "FULL") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = Color(0xFF00FFCC),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(Modifier.width(2.dp))

                        IconButton(
                            onClick = { onStateChanged("HIDDEN") },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFFFF5555),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x22FFFFFF))

                    if (telemetryState == "COMPACT") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (lang == AppLanguage.RU) "Мощность" else if (lang == AppLanguage.UK) "Потужність" else "Power", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                            Text(text = String.format(Locale.US, "%.2f W", telemetry.totalPower), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // FULL view
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("ELECTR", "SIM", "PHONE").forEach { tab ->
                                val isSelected = telemetryTab == tab
                                val tabIcon = when (tab) {
                                    "ELECTR" -> "⚡"
                                    "SIM" -> "⚙️"
                                    else -> "📱"
                                }
                                val tabLabel = when (tab) {
                                    "ELECTR" -> if (lang == AppLanguage.RU) "Схема" else if (lang == AppLanguage.UK) "Схема" else "Circuit"
                                    "SIM" -> if (lang == AppLanguage.RU) "Движок" else if (lang == AppLanguage.UK) "Двигун" else "Engine"
                                    else -> if (lang == AppLanguage.RU) "Железо" else if (lang == AppLanguage.UK) "Залізо" else "Phone"
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) Color(0x2600FFCC) else Color(0x0AFFFFFF),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF00FFCC) else Color(0x11FFFFFF),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onTabChanged(tab) }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = tabIcon, fontSize = 10.sp)
                                        Text(
                                            text = tabLabel,
                                            fontSize = 7.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF00FFCC) else Color(0xFFAAAAAA)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0x22FFFFFF))

                        when (telemetryTab) {
                            "ELECTR" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (lang == AppLanguage.RU) "Общая мощность" else if (lang == AppLanguage.UK) "Загальна потужність" else "Total Power", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    Text(text = String.format(Locale.US, "%.2f W", telemetry.totalPower), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (lang == AppLanguage.RU) "Напряжение" else if (lang == AppLanguage.UK) "Напруга" else "Total Volt", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    Text(text = String.format(Locale.US, "%.1f V", telemetry.totalVoltage), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (lang == AppLanguage.RU) "Общий ток" else if (lang == AppLanguage.UK) "Загальний струм" else "Total Current", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    Text(text = String.format(Locale.US, "%.1f Ma", telemetry.totalCurrent), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (lang == AppLanguage.RU) "Коротк. замык." else if (lang == AppLanguage.UK) "Коротк. замик." else "Short Circuit", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    if (telemetry.isShortCircuit) {
                                        Text(text = "YES", fontSize = 10.sp, color = Color(0xFFFF3366), fontWeight = FontWeight.Bold)
                                    } else {
                                        Text(text = "NO", fontSize = 10.sp, color = Color(0xFF00FF00), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            "SIM" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val mcuCount = grid.sumOf { col -> col.count { it.type == ComponentType.MICROCONTROLLER } }
                                    val activeCpuCores = if (isSimulationRunning) (1 + mcuCount) else 1
                                    Text(text = if (lang == AppLanguage.RU) "Ядра процессора" else if (lang == AppLanguage.UK) "Ядра процесора" else "CPU Cores", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    Text(text = "$activeCpuCores Cores", fontSize = 10.sp, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val mcuCount = grid.sumOf { col -> col.count { it.type == ComponentType.MICROCONTROLLER } }
                                    val cpuFrequencyMhz = if (isSimulationRunning) {
                                        val baseFreq = 80.0f + (mcuCount * 40.0f)
                                        val drift = ((System.currentTimeMillis() % 500) - 250) / 1000f
                                        baseFreq + drift
                                    } else {
                                        0.0f
                                    }
                                    Text(text = if (lang == AppLanguage.RU) "Частота ЦП" else if (lang == AppLanguage.UK) "Частота процесора" else "CPU Clock", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    Text(text = if (cpuFrequencyMhz > 0f) String.format(Locale.US, "%.2f MHz", cpuFrequencyMhz) else "0.00 MHz", fontSize = 10.sp, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val mcuCount = grid.sumOf { col -> col.count { it.type == ComponentType.MICROCONTROLLER } }
                                    val ramUsageMb = if (isSimulationRunning) {
                                        15.4f + (mcuCount * 4.2f)
                                    } else {
                                        3.8f
                                    }
                                    Text(text = if (lang == AppLanguage.RU) "Память ОЗУ" else if (lang == AppLanguage.UK) "Пам'ять ОЗУ" else "Sim RAM", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    Text(text = String.format(Locale.US, "%.1f MB", ramUsageMb), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            "PHONE" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (lang == AppLanguage.RU) "Процессор" else if (lang == AppLanguage.UK) "Процесор" else "Phone CPU", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    Text(text = "$deviceCores Cores", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (lang == AppLanguage.RU) "Частота" else if (lang == AppLanguage.UK) "Частота" else "CPU Freq", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    Text(text = deviceCurrentFreq, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (lang == AppLanguage.RU) "Свободно ОЗУ" else if (lang == AppLanguage.UK) "Вільно ОЗУ" else "Avail RAM", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    Text(text = deviceAvailRam, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (lang == AppLanguage.RU) "ОЗУ Игры" else if (lang == AppLanguage.UK) "ОЗУ Гри" else "Game RAM", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                    Text(text = appRamUsedByGame, fontSize = 10.sp, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BottomToolBar(
        lang: AppLanguage,
        selectedCategory: ComponentCategory,
        selectedTool: ComponentType,
        onCategorySelected: (ComponentCategory) -> Unit,
        onToolSelected: (ComponentType) -> Unit
    ) {
        var isCollapsed by remember { mutableStateOf(false) }

        // Find which MasterTab belongs to the currently active category
        val currentMasterTab = remember(selectedCategory) {
            MasterTab.values().flatMap { master ->
                master.categories.map { cat -> cat to master }
            }.toMap()[selectedCategory] ?: MasterTab.CORE
        }

        var activeMasterTabState by remember { mutableStateOf(currentMasterTab) }

        // Sync activeMasterTabState with outer selectedCategory if it changes elsewhere
        LaunchedEffect(selectedCategory) {
            val matchingMaster = MasterTab.values().firstOrNull { it.categories.contains(selectedCategory) }
            if (matchingMaster != null) {
                activeMasterTabState = matchingMaster
            }
        }

        Column(
            modifier = Modifier
                .background(Color.Transparent)
                .fillMaxWidth()
        ) {
            // Core Master TabRow Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TabRow(
                        selectedTabIndex = activeMasterTabState.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[activeMasterTabState.ordinal]),
                                color = Color(0xFF00FFCC),
                                height = 3.dp
                            )
                        },
                        divider = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MasterTab.values().forEach { master ->
                            Tab(
                                selected = activeMasterTabState == master,
                                onClick = {
                                    activeMasterTabState = master
                                    isCollapsed = false
                                    // Set default subcategory of selected master group
                                    val firstSubCat = master.categories.firstOrNull()
                                    if (firstSubCat != null && selectedCategory != firstSubCat) {
                                        onCategorySelected(firstSubCat)
                                    }
                                },
                                text = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = master.icon, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = master.getLabel(lang),
                                            fontSize = 11.sp,
                                            fontWeight = if (activeMasterTabState == master) FontWeight.Bold else FontWeight.Medium,
                                            color = if (activeMasterTabState == master) Color(0xFF00FFCC) else Color(0xFFAAAAAA),
                                            maxLines = 1
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { isCollapsed = !isCollapsed },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Toggle Menu",
                        tint = Color(0xFF00FFCC)
                    )
                }
            }

            AnimatedVisibility(
                visible = !isCollapsed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Subcategory Navigation (Horizontal Chips to reduce clutter!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeMasterTabState.categories.forEach { category ->
                            val isSelected = selectedCategory == category
                            val categoryName = Lang.getCategoryName(category, lang)

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) Color(0xFF264F4A) else Color(0xFF20202E),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF00FFCC) else Color(0x22FFFFFF),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onCategorySelected(category) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                              ) {
                                  Text(
                                      text = categoryName,
                                      fontSize = 11.sp,
                                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                      color = if (isSelected) Color(0xFF00FFCC) else Color(0xFFCCCCCC)
                                  )
                              }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Display filtered component list inside clean responsive grid
                    val componentsInCategory = remember(selectedCategory) {
                        ComponentType.values().filter {
                            it.category == selectedCategory && !it.name.startsWith("INFINITE_")
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 72.dp),
                        modifier = Modifier
                            .height(180.dp)
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(componentsInCategory) { type ->
                            ToolButton(
                                icon = com.example.ui.getIconForType(type),
                                text = Lang.getComponentDisplayName(type, lang),
                                isSelected = selectedTool == type,
                                onClick = { onToolSelected(type) }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun WikiDialog(lang: AppLanguage, onDismiss: () -> Unit) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == AppLanguage.RU) "Вики Справочник" else "Вікі Довідник",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            WikiSection(
                                title = if (lang == AppLanguage.RU) "⚡ Энергия и Уран" else "⚡ Енергія та Уран",
                                items = listOf(
                                    WikiItem(
                                        name = if (lang == AppLanguage.RU) "Уран (Uranium)" else "Уран (Uranium)",
                                        desc = if (lang == AppLanguage.RU)
                                            "Радиоактивный тяжелый металл. При сильном нагреве, избыточном давлении или достижении критической массы уходит в стремительный термоядерный разгон. Выделяет гигантское количество тепла, испаряя воду в пар и расплавляя окружающие материалы в лаву!"
                                            else "Радіоактивний важкий метал. При сильному нагріванні, надмірному тиску або досягненні критичної маси переходить у стрімкий термоядерний розгін. Виділяє гігантську кількість тепла, випаровуючи воду в пару та розплавляючи навколишні матеріали в лаву!"
                                    ),
                                    WikiItem(
                                        name = if (lang == AppLanguage.RU) "Генератор пар-электричество" else "Генератор пара-електрика",
                                        desc = if (lang == AppLanguage.RU)
                                            "Превращает кинетическую энергию поднимающегося ПАРА сверху в электрический ток. Эффективно работает в увязке с нагревателями или критическим Ураном."
                                            else "Перетворює кінетичну енергію ПАРИ, що піднімається зверху, в електричний струм. Ефективно працює разом із нагрівачами або критичним Ураном."
                                    )
                                )
                            )
                        }

                        item {
                            WikiSection(
                                title = if (lang == AppLanguage.RU) "💧 Гидравлика, Давление и Трубы" else "💧 Гідравліка, Тиск та Труби",
                                items = listOf(
                                    WikiItem(
                                        name = if (lang == AppLanguage.RU) "Труба (Pipe)" else "Труба (Pipe)",
                                        desc = if (lang == AppLanguage.RU)
                                            "Герметичный проводник жидкостей. Трубы поддерживают гидравлическое давление! Способны перекачивать жидкости из источников высокого давления в резервуары низкого давления."
                                            else "Герметичний провідник рідин. Труби підтримують гідравлічний тиск! Здатні перекачувати рідини з джерел високого тиску в резервуари низького тиску."
                                    ),
                                    WikiItem(
                                        name = if (lang == AppLanguage.RU) "Жидкости (Fluids)" else "Рідини (Fluids)",
                                        desc = if (lang == AppLanguage.RU)
                                            "Вода, Лава, Нефть, Кислота, Слизь, Бензин. Обладают уникальными физическими свойствами текучести, вязкости, плотности и температуры."
                                            else "Вода, Лава, Нафта, Кислота, Слиз, Бензин. Мають унікальні фізичні властивості текучості, в'язкості, щільності та температури."
                                    )
                                )
                            )
                        }

                        item {
                            WikiSection(
                                title = if (lang == AppLanguage.RU) "🔥 Термодинамика и Фазовые переходы" else "🔥 Термодинаміка та Фазові переходи",
                                items = listOf(
                                    WikiItem(
                                        name = if (lang == AppLanguage.RU) "Пар и Конденсация" else "Пара та Конденсація",
                                        desc = if (lang == AppLanguage.RU)
                                            "Вода закипает и испаряется в пар при высоких температурах (более 100°C). Перегретый пар поднимается вверх. При соприкосновении с охладителями пар конденсируется обратно в капли воды!"
                                            else "Вода закипає і випаровується в пару при високих температурах (понад 100°C). Перегріта пара піднімається вгору. При зіткненні з охолоджувачами пара конденсується назад у краплі води!"
                                    ),
                                    WikiItem(
                                        name = if (lang == AppLanguage.RU) "Лава и Застывание" else "Лава та Застигання",
                                        desc = if (lang == AppLanguage.RU)
                                            "При контакте с водой лава бурно вскипает, образуя ПАР и превращаясь в твердый КАМЕНЬ, затыкая проходы."
                                            else "При контакті з водою лава бурхливо закипає, утворюючи ПАРУ та перетворюючись на твердий КАМІНЬ, забиваючи проходи."
                                    )
                                )
                            )
                        }

                        item {
                            WikiSection(
                                title = if (lang == AppLanguage.RU) "💨 Зеленая Энергия и Ветер" else "💨 Зелена Енергія та Вітер",
                                items = listOf(
                                    WikiItem(
                                        name = if (lang == AppLanguage.RU) "Ветряки (Wind Turbines)" else "Вітряки (Wind Turbines)",
                                        desc = if (lang == AppLanguage.RU)
                                            "Генерируют энергию на основе динамического обдува! Скорость ветра пропорциональна высоте (чем выше, тем сильнее). Вентиляторы (Fan), дующие в сторону ветряка, создают мощный искусственный поток ветра!"
                                            else "Генерують енергію на основі динамічного обдування! Скорость вітру пропорційна висоті (чим вище, тим сильний). Вентилятори (Fan), що дують у бік вітряка, створюють потужний штучний потік вітру!"
                                    ),
                                    WikiItem(
                                        name = if (lang == AppLanguage.RU) "Солнечные панели (Solar)" else "Сонячні панелі (Solar)",
                                        desc = if (lang == AppLanguage.RU)
                                            "Генерируют свет на открытом небе в верхних частях симулятора. В шахтах или закрытых пещерах их можно активировать искусственно, посветив на них светодиодом (LED)!"
                                            else "Генерують світло на відкритому небі у верхніх частинах симулятора. У шахтах або закритих печерах їх можна активувати штучно, посвітивши на них світлодіодом (LED)!"
                                    )
                                )
                            )
                        }

                        item {
                            WikiSection(
                                title = if (lang == AppLanguage.RU) "♾️ Бесконечные Источники" else "♾️ Нескінченні Джерела",
                                items = listOf(
                                    WikiItem(
                                        name = if (lang == AppLanguage.RU) "Двойной Клик по Инструменту" else "Подвійний Клік по Інструменту",
                                        desc = if (lang == AppLanguage.RU)
                                            "Нажмите дважды на любую жидкость или пар на нижней панели инструментов, чтобы переключить ее в режим спавна бесконечного источника! Нажмите дважды еще раз, чтобы выключить."
                                            else "Натисніть двічі на будь-яку рідину чи пару на нижній панелі інструментів, щоб перемкнути її на нескінченне джерело! Натисніть двічі ще раз, щоб вимкнути."
                                    )
                                )
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color(0xFF1E1E2E))
                    ) {
                        Text(if (lang == AppLanguage.RU) "Закрыть" else "Закрити", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    private fun WikiSection(title: String, items: List<WikiItem>) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3D)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2200FFCC))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FFCC))
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    Text(text = item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = item.desc, fontSize = 12.sp, color = Color(0xFFB0B0C0))
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    private data class WikiItem(val name: String, val desc: String)
}
