package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.db.AppDatabase
import com.example.db.CircuitRepository
import com.example.ui.SimulatorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SimulatorViewModel
import com.example.viewmodel.SimulatorViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize our multi-language asset-based parsing system
    com.example.lang.Lang.init(applicationContext)
    
    val database = AppDatabase.getDatabase(this)
    val repository = CircuitRepository(database.schemeDao())
    val viewModelFactory = SimulatorViewModelFactory(repository)
    val viewModel: SimulatorViewModel by viewModels { viewModelFactory }
    
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        SimulatorScreen(viewModel = viewModel)
      }
    }
  }
}

