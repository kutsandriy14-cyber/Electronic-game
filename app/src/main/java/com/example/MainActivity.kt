package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.db.AppDatabase
import com.example.db.CircuitRepository
import com.example.netauth.NetAuthManager
import com.example.ui.LoginScreen
import com.example.ui.SimulatorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SimulatorViewModel
import com.example.viewmodel.SimulatorViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize our multi-language asset-based parsing system
    com.example.lang.Lang.init(applicationContext)
    NetAuthManager.init(applicationContext)
    
    val database = AppDatabase.getDatabase(this)
    val repository = CircuitRepository(database.schemeDao())
    val viewModelFactory = SimulatorViewModelFactory(repository)
    val viewModel: SimulatorViewModel by viewModels { viewModelFactory }
    
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = if (NetAuthManager.isGuest && NetAuthManager.currentUserId == null) "login" else "simulator") {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("simulator") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onGuestLogin = {
                        navController.navigate("simulator") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("simulator") {
                SimulatorScreen(viewModel = viewModel)
            }
        }
      }
    }
  }
}


