package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.PdfDatabase
import com.example.ui.DashboardScreen
import com.example.ui.PdfViewModel
import com.example.ui.PdfViewModelFactory
import com.example.ui.Screen
import com.example.ui.ViewerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = PdfDatabase.getDatabase(applicationContext)
    val viewModel: PdfViewModel by viewModels {
      PdfViewModelFactory(database.recentPdfDao())
    }

    setContent {
      MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
          val state by viewModel.uiState.collectAsState()

          when (state.currentScreen) {
            Screen.Dashboard -> {
              DashboardScreen(viewModel = viewModel)
            }
            Screen.Viewer -> {
              ViewerScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}
