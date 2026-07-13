package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.CircuitRepository
import com.example.db.CircuitScheme
import com.example.engine.CircuitEngine
import com.example.lang.AppLanguage
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.Direction
import com.example.model.GridComponent
import com.example.model.Telemetry
import com.example.functional.Physical
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.delay

data class SimulatorState(
    val grid: Array<Array<GridComponent>>,
    val width: Int = 16,
    val height: Int = 16,
    val telemetry: Telemetry = Telemetry(),
    val selectedTool: ComponentType = ComponentType.WIRE_ANY,
    val selectedCategory: ComponentCategory = ComponentCategory.CONDUCTORS,
    val showLoadDialog: Boolean = false,
    val showSaveDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val logs: List<String> = emptyList(), // For SSM scripts logs
    val inspectCoordinates: Pair<Int, Int>? = null, // Open inspect dialog for this cell
    val multimeterCoordinates: Pair<Int, Int>? = null, // Open multimeter tooltip for this cell
    val isEasterEggActive: Boolean = false,
    val isSimulationRunning: Boolean = true,
    val simulationTick: Long = 0,
    val appLanguage: AppLanguage = AppLanguage.EN,
    val timeMultiplier: Int = 1, // Acceleration from 1 to 20
    val allocatedRamGbytes: Int = 4, // Simulated RAM limit (1 to 16 GB)
    val allocatedCores: Int = 4,      // Simulated CPU Core count (1 to 8)
    val clockMhz: Int = 2400,         // Simulated CPU Clock Speed throttling (500 to 3600 MHz)
    val canvasStyle: String = "dark_neon", // Canvas aesthetic theme: "dark_neon", "blueprint", "oled_black"
    val brushSize: Int = 1, // Brush size to draw with (1x1, 3x3, 5x5, etc.)
    val alertMessage: String? = null,
    val cloudSchemes: List<String> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimulatorState

        if (simulationTick != other.simulationTick) return false
        if (isSimulationRunning != other.isSimulationRunning) return false
        if (selectedTool != other.selectedTool) return false
        if (selectedCategory != other.selectedCategory) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (telemetry != other.telemetry) return false
        if (showLoadDialog != other.showLoadDialog) return false
        if (showSaveDialog != other.showSaveDialog) return false
        if (showSettingsDialog != other.showSettingsDialog) return false
        if (inspectCoordinates != other.inspectCoordinates) return false
        if (multimeterCoordinates != other.multimeterCoordinates) return false
        if (isEasterEggActive != other.isEasterEggActive) return false
        if (appLanguage != other.appLanguage) return false
        if (timeMultiplier != other.timeMultiplier) return false
        if (allocatedRamGbytes != other.allocatedRamGbytes) return false
        if (allocatedCores != other.allocatedCores) return false
        if (clockMhz != other.clockMhz) return false
        if (canvasStyle != other.canvasStyle) return false
        if (brushSize != other.brushSize) return false
        
        // Deep equals last because it is very expensive
        if (!grid.contentDeepEquals(other.grid)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + telemetry.hashCode()
        result = 31 * result + selectedTool.hashCode()
        result = 31 * result + selectedCategory.hashCode()
        result = 31 * result + showLoadDialog.hashCode()
        result = 31 * result + showSaveDialog.hashCode()
        result = 31 * result + showSettingsDialog.hashCode()
        result = 31 * result + (inspectCoordinates?.hashCode() ?: 0)
        result = 31 * result + (multimeterCoordinates?.hashCode() ?: 0)
        result = 31 * result + isEasterEggActive.hashCode()
        result = 31 * result + isSimulationRunning.hashCode()
        result = 31 * result + simulationTick.hashCode()
        result = 31 * result + appLanguage.hashCode()
        result = 31 * result + timeMultiplier.hashCode()
        result = 31 * result + allocatedRamGbytes
        result = 31 * result + allocatedCores
        result = 31 * result + clockMhz
        result = 31 * result + canvasStyle.hashCode()
        result = 31 * result + brushSize
        return result
    }
}

class SimulatorViewModel(private val repository: CircuitRepository) : ViewModel() {

    private val engine = CircuitEngine()

    private val _uiState = MutableStateFlow(
        SimulatorState(grid = Array(16) { Array(16) { GridComponent() } })
    )
    val uiState: StateFlow<SimulatorState> = _uiState.asStateFlow()

    val savedSchemes = repository.allSchemes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    companion object {
        fun getMaxPhysicalRamGb(): Int {
            return try {
                val runtime = Runtime.getRuntime()
                val heapMaxGb = (runtime.maxMemory() / (1024L * 1024L * 1024L)).toInt().coerceIn(1, 48)
                if (heapMaxGb == 0) {
                    val heapMaxMb = (runtime.maxMemory() / (1024L * 1024L)).toInt()
                    when {
                        heapMaxMb <= 256 -> 2
                        heapMaxMb <= 512 -> 4
                        heapMaxMb <= 1024 -> 8
                        else -> 12
                    }
                } else {
                    heapMaxGb * 2
                }
            } catch (e: Exception) {
                4
            }
        }

        fun getPhysicalCpuCores(): Int {
            return try {
                Runtime.getRuntime().availableProcessors().coerceIn(1, 16)
            } catch (e: Exception) {
                4
            }
        }
    }

    init {
        val detectedCores = getPhysicalCpuCores()
        val detectedRam = getMaxPhysicalRamGb()
        _uiState.update { current ->
            current.copy(
                allocatedCores = detectedCores,
                allocatedRamGbytes = detectedRam,
                clockMhz = 2400
            )
        }
        startSimulationLoop()
    }

    fun getShareableString(): String {
        val state = _uiState.value
        return "esshim://" + engine.serializeGrid(state.grid, state.width, state.height)
    }

    fun importShareableString(data: String) {
        try {
            val actualData = if (data.startsWith("esshim://")) data.substring(9) else data
            val newGrid = engine.deserializeGrid(actualData, _uiState.value.width, _uiState.value.height)
            _uiState.update { it.copy(grid = newGrid, showSettingsDialog = false) }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun startSimulationLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val state = _uiState.value
                var elapsed = 0L
                if (state.isSimulationRunning) {
                    val startTime = System.currentTimeMillis()
                    
                    // Physical CPU limits "если я виделил столько то то больше етого лимита не пойдет"
                    // Each step standardly consumes 200 MHz multiplier cycles.
                    // Max computed steps per cycle is bottlenecked by state.allocatedCores * state.clockMhz.
                    val maxAllowedSteps = ((state.allocatedCores * state.clockMhz) / 220).coerceIn(1, 20)
                    val stepsCount = minOf(state.timeMultiplier, maxAllowedSteps)
                    
                    var currentGrid = state.grid
                    var currentTelemetry = state.telemetry
                    val accumulatedLogs = mutableListOf<String>()
                    var currentTick = state.simulationTick
                    
                    for (step in 0 until stepsCount) {
                        val simResult = engine.calculatePower(
                            currentGrid, 
                            state.width, 
                            state.height, 
                            currentTick,
                            ramGb = state.allocatedRamGbytes,
                            cores = state.allocatedCores,
                            clockMhz = state.clockMhz
                        )
                        currentGrid = simResult.grid.map { col -> col.clone() }.toTypedArray()
                        currentTelemetry = simResult.telemetry
                        if (simResult.logs.isNotEmpty()) {
                            accumulatedLogs.addAll(simResult.logs)
                        }
                        currentTick++
                        
                        // Core queuing + clock cycle throttling delay simulating physical hardware limits
                        val coreDelay = when {
                            state.allocatedCores == 1 -> 15L
                            state.allocatedCores == 2 -> 6L
                            state.allocatedCores == 3 -> 3L
                            else -> 0L
                        }
                        val clockDelay = when {
                            state.clockMhz < 1000 -> 24L
                            state.clockMhz < 1800 -> 10L
                            state.clockMhz < 2600 -> 3L
                            else -> 0L
                        }
                        val throttledDelay = coreDelay + clockDelay
                        if (throttledDelay > 0L) {
                            delay(throttledDelay)
                        }
                    }

                    _uiState.update { current ->
                        if (current.width == state.width && current.height == state.height) {
                            current.copy(
                                grid = currentGrid, 
                                telemetry = currentTelemetry,
                                logs = if (accumulatedLogs.isNotEmpty()) (current.logs + accumulatedLogs).takeLast(50) else current.logs,
                                simulationTick = currentTick
                            )
                        } else {
                            current
                        }
                    }
                    elapsed = System.currentTimeMillis() - startTime
                }
                
                val clockMhz = state.clockMhz
                val delayTime = when {
                    clockMhz >= 3600 -> 24L
                    clockMhz >= 2400 -> 50L
                    clockMhz >= 1600 -> 80L
                    clockMhz >= 1000 -> 140L
                    else -> 280L // highly-throttled CPU limit
                }
                delay(maxOf(delayTime - elapsed, 10L))
            }
        }
    }
    
    fun toggleSimulation() {
        _uiState.update { it.copy(isSimulationRunning = !it.isSimulationRunning) }
    }

    fun selectTool(type: ComponentType) {
        _uiState.update { state ->
            state.copy(selectedTool = type, selectedCategory = type.category)
        }
    }
    
    fun selectCategory(category: ComponentCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun resizeGrid(newWidth: Int, newHeight: Int) {
        val newGrid = Array(newWidth) { Array(newHeight) { GridComponent() } }
        _uiState.update { 
            it.copy(grid = newGrid, width = newWidth, height = newHeight, telemetry = Telemetry(), showSettingsDialog = false) 
        }
    }

    fun onCellClicked(x: Int, y: Int) {
        val currentState = _uiState.value
        val tool = currentState.selectedTool
        
        if (tool == ComponentType.PAN) return
        
        val actualWidth = currentState.grid.size
        val actualHeight = if (actualWidth > 0) currentState.grid[0].size else 0
        if (x !in 0 until actualWidth || y !in 0 until actualHeight) return
        
        if (tool == ComponentType.INSPECT) {
            val comp = currentState.grid[x][y]
            if (comp.type != ComponentType.EMPTY) {
                _uiState.update { it.copy(inspectCoordinates = Pair(x, y)) }
            }
            return
        }
        
        if (tool == ComponentType.MULTIMETER) {
            val comp = currentState.grid[x][y]
            if (comp.type != ComponentType.EMPTY) {
                _uiState.update { it.copy(multimeterCoordinates = Pair(x, y)) }
            } else {
                _uiState.update { it.copy(multimeterCoordinates = null) }
            }
            return
        }

        _uiState.update { current ->
            val gridCopy = current.grid.map { it.clone() }.toTypedArray()

            val isSingleOnly = tool == ComponentType.ROTATE || tool == ComponentType.PUSH_BUTTON || tool == ComponentType.INSPECT || tool == ComponentType.MULTIMETER
            val bRange = if (isSingleOnly) 0 else (current.brushSize / 2)

            for (dx in -bRange..bRange) {
                for (dy in -bRange..bRange) {
                    val cx = x + dx
                    val cy = y + dy
                    if (cx in 0 until actualWidth && cy in 0 until actualHeight) {
                        val cell = gridCopy[cx][cy]

                        if (tool == ComponentType.EMPTY) {
                            if (cell.type == ComponentType.DOUBLE_DOOR) {
                                val dir = cell.direction
                                val nx = cx + Physical.getDx(dir)
                                val ny = cy + Physical.getDy(dir)
                                if (nx in 0 until actualWidth && ny in 0 until actualHeight && gridCopy[nx][ny].type == ComponentType.DOUBLE_DOOR) {
                                    gridCopy[nx][ny] = GridComponent()
                                }
                            }
                            gridCopy[cx][cy] = GridComponent()
                        } else if (tool == ComponentType.ROTATE) {
                            if (Physical.canRotate(cell.type)) {
                                gridCopy[cx][cy] = Physical.rotate(cell)
                            }
                        } else if (tool == ComponentType.SWITCH_OPEN || tool == ComponentType.SWITCH_CLOSED) {
                            val currentType = cell.type
                            if (currentType == ComponentType.SWITCH_OPEN) {
                                gridCopy[cx][cy] = cell.copy(type = ComponentType.SWITCH_CLOSED)
                            } else if (currentType == ComponentType.SWITCH_CLOSED) {
                                gridCopy[cx][cy] = cell.copy(type = ComponentType.SWITCH_OPEN)
                            } else {
                                gridCopy[cx][cy] = GridComponent(tool)
                            }
                        } else if (tool == ComponentType.PUSH_BUTTON) {
                            val currentType = cell.type
                            if (currentType == ComponentType.PUSH_BUTTON) {
                                gridCopy[cx][cy] = cell.copy(logicState = !cell.logicState)
                            } else {
                                gridCopy[cx][cy] = GridComponent(tool)
                            }
                        } else if (current.selectedTool == ComponentType.WIRE_ANY && 
                                   (cell.type == ComponentType.SWITCH_OPEN || cell.type == ComponentType.SWITCH_CLOSED)) {
                            val currentType = cell.type
                            gridCopy[cx][cy] = cell.copy(type = if(currentType == ComponentType.SWITCH_OPEN) ComponentType.SWITCH_CLOSED else ComponentType.SWITCH_OPEN)
                        } else {
                            if (cell.type == tool && Physical.canRotate(tool)) {
                                val rotatedCell = Physical.rotate(cell)
                                gridCopy[cx][cy] = rotatedCell
                                if (tool == ComponentType.DOUBLE_DOOR) {
                                    val oldDir = cell.direction
                                    val oldNx = cx + Physical.getDx(oldDir)
                                    val oldNy = cy + Physical.getDy(oldDir)
                                    if (oldNx in 0 until actualWidth && oldNy in 0 until actualHeight && gridCopy[oldNx][oldNy].type == ComponentType.DOUBLE_DOOR) {
                                        gridCopy[oldNx][oldNy] = GridComponent()
                                    }
                                    val newDir = rotatedCell.direction
                                    val newNx = cx + Physical.getDx(newDir)
                                    val newNy = cy + Physical.getDy(newDir)
                                    if (newNx in 0 until actualWidth && newNy in 0 until actualHeight) {
                                        val oppDir = when (newDir) {
                                            Direction.UP -> Direction.DOWN
                                            Direction.DOWN -> Direction.UP
                                            Direction.LEFT -> Direction.RIGHT
                                            Direction.RIGHT -> Direction.LEFT
                                        }
                                        gridCopy[newNx][newNy] = GridComponent(type = ComponentType.DOUBLE_DOOR, direction = oppDir)
                                    }
                                }
                            } else {
                                gridCopy[cx][cy] = GridComponent(tool)
                                if (tool == ComponentType.DOUBLE_DOOR) {
                                    val dir = gridCopy[cx][cy].direction
                                    val tDx = Physical.getDx(dir)
                                    val tDy = Physical.getDy(dir)
                                    val nx = cx + tDx
                                    val ny = cy + tDy
                                    if (nx in 0 until actualWidth && ny in 0 until actualHeight) {
                                        val oppDir = when (dir) {
                                            Direction.UP -> Direction.DOWN
                                            Direction.DOWN -> Direction.UP
                                            Direction.LEFT -> Direction.RIGHT
                                            Direction.RIGHT -> Direction.LEFT
                                        }
                                        gridCopy[nx][ny] = GridComponent(type = ComponentType.DOUBLE_DOOR, direction = oppDir)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val updatedState = if (!current.isSimulationRunning) {
                val simResult = engine.calculatePower(
                    gridCopy, 
                    current.width, 
                    current.height, 
                    current.simulationTick,
                    ramGb = current.allocatedRamGbytes,
                    cores = current.allocatedCores,
                    clockMhz = current.clockMhz
                )
                current.copy(grid = simResult.grid, telemetry = simResult.telemetry)
            } else {
                current.copy(grid = gridCopy)
            }
            
            updatedState
        }
    }

    fun updateComponentData(x: Int, y: Int, data: String, rechargeRepair: Boolean = false) {
        _uiState.update { current ->
            val actualWidth = current.grid.size
            val actualHeight = if (actualWidth > 0) current.grid[0].size else 0
            if (x !in 0 until actualWidth || y !in 0 until actualHeight) return@update current
            
            val gridCopy = current.grid.map { it.clone() }.toTypedArray()
            var newComp = gridCopy[x][y].copy(extraData = data)
            if (rechargeRepair) {
                newComp = newComp.copy(isOverloaded = false, charge = -1f) // -1f recalculates max charge
            }
            gridCopy[x][y] = newComp
            
            val updatedState = if (!current.isSimulationRunning) {
                val simResult = engine.calculatePower(
                    gridCopy, 
                    current.width, 
                    current.height, 
                    current.simulationTick,
                    ramGb = current.allocatedRamGbytes,
                    cores = current.allocatedCores,
                    clockMhz = current.clockMhz
                )
                current.copy(grid = simResult.grid, telemetry = simResult.telemetry, inspectCoordinates = null)
            } else {
                current.copy(grid = gridCopy, inspectCoordinates = null)
            }
            updatedState
        }
    }
    
    fun changeLanguage(lang: AppLanguage) {
        _uiState.update { it.copy(appLanguage = lang) }
    }

    fun dismissInspect() {
        _uiState.update { it.copy(inspectCoordinates = null) }
    }
    
    fun dismissMultimeter() {
        _uiState.update { it.copy(multimeterCoordinates = null) }
    }
    
    fun dismissEasterEgg() {
        _uiState.update { it.copy(isEasterEggActive = false) }
    }

    fun clearGrid() {
        val currentState = _uiState.value
        val newGrid = Array(currentState.width) { Array(currentState.height) { GridComponent() } }
        _uiState.update { it.copy(grid = newGrid, telemetry = Telemetry()) }
    }

    fun showSaveDialog() { _uiState.update { it.copy(showSaveDialog = true) } }
    fun showLoadDialog() { 
        _uiState.update { it.copy(showLoadDialog = true) }
        if (!com.example.netauth.NetAuthManager.isGuest) {
            viewModelScope.launch {
                val listResult = com.example.netauth.NetAuthManager.listFiles()
                if (listResult.isSuccess) {
                    _uiState.update { it.copy(cloudSchemes = listResult.getOrNull() ?: emptyList()) }
                }
            }
        } else {
            _uiState.update { it.copy(cloudSchemes = emptyList()) }
        }
    }
    fun showSettingsDialog() { _uiState.update { it.copy(showSettingsDialog = true) } }

    fun updateHardwareSettings(ramGb: Int, cores: Int, mhz: Int, canvasStyle: String) {
        _uiState.update { current ->
            current.copy(
                allocatedRamGbytes = ramGb.coerceIn(1, 16),
                allocatedCores = cores.coerceIn(1, 8),
                clockMhz = mhz.coerceIn(500, 3600),
                canvasStyle = canvasStyle
            )
        }
    }

    fun dismissSettingsAndApply() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(alertMessage = null) }
    }

    fun dismissDialogs() {
        _uiState.update { it.copy(showLoadDialog = false, showSaveDialog = false, showSettingsDialog = false) }
    }

    fun saveScheme(name: String) {
        val currentState = _uiState.value
        
        var isEmpty = true
        for (col in currentState.grid) {
            for (comp in col) {
                if (comp.type != ComponentType.EMPTY) {
                    isEmpty = false
                    break
                }
            }
        }
        
        if (isEmpty && !com.example.netauth.NetAuthManager.isGuest) {
            _uiState.update { it.copy(alertMessage = "Нельзя загрузить пустую схему") }
            return
        }

        val gridData = engine.serializeGrid(currentState.grid, currentState.width, currentState.height)
        viewModelScope.launch {
            if (!com.example.netauth.NetAuthManager.isGuest) {
                val uploadResult = com.example.netauth.NetAuthManager.uploadFile("$name.json", gridData.toByteArray())
                if (uploadResult.isFailure) {
                    val ex = uploadResult.exceptionOrNull()
                    if (ex?.message == "413_PAYLOAD_TOO_LARGE") {
                        _uiState.update { it.copy(alertMessage = "Недостаточно места в облаке NetAuth (Лимит 512 МБ)") }
                        return@launch
                    } else {
                        _uiState.update { it.copy(alertMessage = "Связь с NetAuth потеряна. Сохраняем локально.") }
                    }
                }
            }
            
            repository.insert(CircuitScheme(
                name = name, 
                gridData = gridData,
                width = currentState.width,
                height = currentState.height
            ))
        }
        dismissDialogs()
    }

    fun loadScheme(scheme: CircuitScheme) {
        val newGrid = engine.deserializeGrid(scheme.gridData, scheme.width, scheme.height)
        viewModelScope.launch(Dispatchers.Default) {
             val currentHardware = _uiState.value
             val simResult = engine.calculatePower(
                 newGrid, 
                 scheme.width, 
                 scheme.height,
                 ramGb = currentHardware.allocatedRamGbytes,
                 cores = currentHardware.allocatedCores,
                 clockMhz = currentHardware.clockMhz
             )
            _uiState.update { 
                it.copy(
                    grid = simResult.grid, 
                    telemetry = simResult.telemetry,
                    width = scheme.width,
                    height = scheme.height,
                    showLoadDialog = false 
                ) 
            }
        }
    }
    
    fun loadCloudScheme(fileName: String) {
        viewModelScope.launch {
            val downloadResult = com.example.netauth.NetAuthManager.downloadFile(fileName)
            if (downloadResult.isSuccess) {
                val gridData = String(downloadResult.getOrNull()!!)
                // we assume cloud saves preserve the size, or we can guess/detect it. 
                // Wait, CircuitEngine.deserializeGrid usually takes width/height. We might need to parse or use current.
                // Let's use 16x16 as fallback or what if gridData has no dimensions?
                // Actually `importShareableString` does `deserializeGrid(actualData, _uiState.value.width, _uiState.value.height)`
                importShareableString(gridData)
                dismissDialogs()
            } else {
                _uiState.update { it.copy(alertMessage = "Ошибка при загрузке облачной схемы. Связь с NetAuth потеряна") }
            }
        }
    }
    
    fun deleteScheme(id: Int) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun setTimeMultiplier(multiplier: Int) {
        _uiState.update { it.copy(timeMultiplier = multiplier.coerceIn(1, 20)) }
    }

    fun setBrushSize(size: Int) {
        _uiState.update { it.copy(brushSize = size.coerceIn(1, 9)) }
    }
}
