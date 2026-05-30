package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.CircuitRepository
import com.example.db.CircuitScheme
import com.example.engine.CircuitEngine
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
    val inspectCoordinates: Pair<Int, Int>? = null, // Open inspect dialog for this cell
    val isEasterEggActive: Boolean = false,
    val isSimulationRunning: Boolean = true,
    val simulationTick: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimulatorState

        if (!grid.contentDeepEquals(other.grid)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (telemetry != other.telemetry) return false
        if (selectedTool != other.selectedTool) return false
        if (selectedCategory != other.selectedCategory) return false
        if (showLoadDialog != other.showLoadDialog) return false
        if (showSaveDialog != other.showSaveDialog) return false
        if (showSettingsDialog != other.showSettingsDialog) return false
        if (inspectCoordinates != other.inspectCoordinates) return false
        if (isEasterEggActive != other.isEasterEggActive) return false
        if (isSimulationRunning != other.isSimulationRunning) return false
        if (simulationTick != other.simulationTick) return false

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
        result = 31 * result + isEasterEggActive.hashCode()
        result = 31 * result + isSimulationRunning.hashCode()
        result = 31 * result + simulationTick.hashCode()
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
        return engine.serializeGrid(state.grid, state.width, state.height)
    }

    private fun startSimulationLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val state = _uiState.value
                if (state.isSimulationRunning) {
                    val simResult = engine.calculatePower(state.grid, state.width, state.height, state.simulationTick)
                    _uiState.update { 
                        it.copy(
                            grid = simResult.grid, 
                            telemetry = simResult.telemetry,
                            simulationTick = it.simulationTick + 1
                        ) 
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
        
        if (tool == ComponentType.INSPECT) {
            val comp = currentState.grid[x][y]
            if (comp.type != ComponentType.EMPTY) {
                _uiState.update { it.copy(inspectCoordinates = Pair(x, y)) }
            }
            return
        }

        val newGrid = currentState.grid.map { it.clone() }.toTypedArray()

        if (tool == ComponentType.EMPTY) {
            newGrid[x][y] = GridComponent()
        } else if (tool == ComponentType.ROTATE) {
            val cell = newGrid[x][y]
            if (cell.type != ComponentType.EMPTY) {
                newGrid[x][y] = cell.copy(direction = cell.direction.next())
            }
        } else if (tool == ComponentType.SWITCH_OPEN || tool == ComponentType.SWITCH_CLOSED) {
            val currentType = newGrid[x][y].type
            if (currentType == ComponentType.SWITCH_OPEN) {
                newGrid[x][y] = newGrid[x][y].copy(type = ComponentType.SWITCH_CLOSED)
            } else if (currentType == ComponentType.SWITCH_CLOSED) {
                newGrid[x][y] = newGrid[x][y].copy(type = ComponentType.SWITCH_OPEN)
            } else {
                newGrid[x][y] = GridComponent(tool)
            }
        } else if (tool == ComponentType.PUSH_BUTTON) {
            val currentType = newGrid[x][y].type
            if (currentType == ComponentType.PUSH_BUTTON) {
                newGrid[x][y] = newGrid[x][y].copy(logicState = !newGrid[x][y].logicState)
            } else {
                newGrid[x][y] = GridComponent(tool)
            }
        } else if (currentState.selectedTool == ComponentType.WIRE_ANY && 
                   (newGrid[x][y].type == ComponentType.SWITCH_OPEN || newGrid[x][y].type == ComponentType.SWITCH_CLOSED)) {
             val currentType = newGrid[x][y].type
             newGrid[x][y] = newGrid[x][y].copy(type = if(currentType == ComponentType.SWITCH_OPEN) ComponentType.SWITCH_CLOSED else ComponentType.SWITCH_OPEN)
        } else {
            // Check for easter eggs
            if (tool == ComponentType.BATTERY && newGrid[x][y].type == ComponentType.BATTERY) {
                var batteryCount = 0
                for (i in 0 until currentState.width) {
                    for (j in 0 until currentState.height) {
                        if (newGrid[i][j].type == ComponentType.BATTERY) batteryCount++
                    }
                }
                if (batteryCount > 8) {
                    _uiState.update { it.copy(isEasterEggActive = true) }
                }
            }
            newGrid[x][y] = GridComponent(tool)
        }

        viewModelScope.launch(Dispatchers.Default) {
            val simResult = engine.calculatePower(newGrid, currentState.width, currentState.height)
            _uiState.update { it.copy(grid = simResult.grid, telemetry = simResult.telemetry) }
        }
    }
    
    fun updateComponentData(x: Int, y: Int, data: String, rechargeRepair: Boolean = false) {
        val currentState = _uiState.value
        val newGrid = currentState.grid.map { it.clone() }.toTypedArray()
        var newComp = newGrid[x][y].copy(extraData = data)
        if (rechargeRepair) {
             newComp = newComp.copy(isOverloaded = false, charge = -1f) // -1f recalculates max charge
        }
        newGrid[x][y] = newComp
        _uiState.update { it.copy(grid = newGrid, inspectCoordinates = null) }
        
        viewModelScope.launch(Dispatchers.Default) {
            val simResult = engine.calculatePower(newGrid, currentState.width, currentState.height)
            _uiState.update { it.copy(telemetry = simResult.telemetry) }
        }
    }
    
    fun dismissInspect() {
        _uiState.update { it.copy(inspectCoordinates = null) }
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
}
