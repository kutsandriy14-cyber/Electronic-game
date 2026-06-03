package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.CircuitRepository
import com.example.db.CircuitScheme
import com.example.engine.CircuitEngine
import com.example.lang.AppLanguage
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.GridComponent
import com.example.model.Telemetry
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
    val timeMultiplier: Int = 1 // Acceleration from 1 to 20
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

    init {
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
                if (state.isSimulationRunning) {
                    val stepsCount = state.timeMultiplier.coerceIn(1, 20)
                    var currentGrid = state.grid
                    var currentTelemetry = state.telemetry
                    val accumulatedLogs = mutableListOf<String>()
                    var currentTick = state.simulationTick
                    
                    for (step in 0 until stepsCount) {
                        val simResult = engine.calculatePower(currentGrid, state.width, state.height, currentTick)
                        currentGrid = simResult.grid
                        currentTelemetry = simResult.telemetry
                        if (simResult.logs.isNotEmpty()) {
                            accumulatedLogs.addAll(simResult.logs)
                        }
                        currentTick++
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
                }
                delay(50) // 50ms simulation step for smoother physics
            }
        }
    }
    
    fun toggleSimulation() {
        _uiState.update { it.copy(isSimulationRunning = !it.isSimulationRunning) }
    }

    fun selectTool(type: ComponentType) {
        _uiState.update { it.copy(selectedTool = type, selectedCategory = type.category) }
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
            val cell = gridCopy[x][y]

            if (tool == ComponentType.EMPTY) {
                gridCopy[x][y] = GridComponent()
            } else if (tool == ComponentType.ROTATE) {
                if (cell.type != ComponentType.EMPTY) {
                    gridCopy[x][y] = cell.copy(direction = cell.direction.next())
                }
            } else if (tool == ComponentType.SWITCH_OPEN || tool == ComponentType.SWITCH_CLOSED) {
                val currentType = cell.type
                if (currentType == ComponentType.SWITCH_OPEN) {
                    gridCopy[x][y] = cell.copy(type = ComponentType.SWITCH_CLOSED)
                } else if (currentType == ComponentType.SWITCH_CLOSED) {
                    gridCopy[x][y] = cell.copy(type = ComponentType.SWITCH_OPEN)
                } else {
                    gridCopy[x][y] = GridComponent(tool)
                }
            } else if (tool == ComponentType.PUSH_BUTTON) {
                val currentType = cell.type
                if (currentType == ComponentType.PUSH_BUTTON) {
                    gridCopy[x][y] = cell.copy(logicState = !cell.logicState)
                } else {
                    gridCopy[x][y] = GridComponent(tool)
                }
            } else if (current.selectedTool == ComponentType.WIRE_ANY && 
                       (cell.type == ComponentType.SWITCH_OPEN || cell.type == ComponentType.SWITCH_CLOSED)) {
                val currentType = cell.type
                gridCopy[x][y] = cell.copy(type = if(currentType == ComponentType.SWITCH_OPEN) ComponentType.SWITCH_CLOSED else ComponentType.SWITCH_OPEN)
            } else {
                // Feature: "зделай нормальние компаненти штоби их можно било поварачувать"
                // If they click with a tool on a component of the exact SAME type, we ROTATE it!
                if (cell.type == tool && tool != ComponentType.EMPTY) {
                    gridCopy[x][y] = cell.copy(direction = cell.direction.next())
                } else {
                    if (tool == ComponentType.BATTERY) {
                        var batteryCount = 0
                        for (i in 0 until actualWidth) {
                            for (j in 0 until actualHeight) {
                                if (gridCopy[i][j].type == ComponentType.BATTERY) batteryCount++
                            }
                        }
                        if (batteryCount > 8) {
                            _uiState.update { it.copy(isEasterEggActive = true) }
                        }
                    }
                    gridCopy[x][y] = GridComponent(tool)
                }
            }

            val updatedState = if (!current.isSimulationRunning) {
                val simResult = engine.calculatePower(gridCopy, current.width, current.height, current.simulationTick)
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
                val simResult = engine.calculatePower(gridCopy, current.width, current.height, current.simulationTick)
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
    fun showLoadDialog() { _uiState.update { it.copy(showLoadDialog = true) } }
    fun showSettingsDialog() { _uiState.update { it.copy(showSettingsDialog = true) } }

    fun dismissDialogs() {
        _uiState.update { it.copy(showLoadDialog = false, showSaveDialog = false, showSettingsDialog = false) }
    }

    fun saveScheme(name: String) {
        val currentState = _uiState.value
        val gridData = engine.serializeGrid(currentState.grid, currentState.width, currentState.height)
        viewModelScope.launch {
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
             val simResult = engine.calculatePower(newGrid, scheme.width, scheme.height)
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
    
    fun deleteScheme(id: Int) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun setTimeMultiplier(multiplier: Int) {
        _uiState.update { it.copy(timeMultiplier = multiplier.coerceIn(1, 20)) }
    }
}
