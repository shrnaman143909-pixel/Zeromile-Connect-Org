package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme
import com.example.zeromile.ui.admin.AdminScreen
import com.example.zeromile.ui.screens.MainScreen
import com.example.zeromile.ui.viewmodel.AdminViewModel
import com.example.zeromile.ui.viewmodel.CivicViewModel

enum class AppExperience {
  CITIZEN,
  ADMIN
}

class MainActivity : ComponentActivity() {
  private val civicViewModel: CivicViewModel by viewModels()
  private val adminViewModel: AdminViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          var currentExperience by remember { mutableStateOf(AppExperience.CITIZEN) }

          when (currentExperience) {
            AppExperience.CITIZEN -> {
              MainScreen(
                viewModel = civicViewModel,
                onOpenAdminPortal = { currentExperience = AppExperience.ADMIN }
              )
            }
            AppExperience.ADMIN -> {
              AdminScreen(
                adminViewModel = adminViewModel,
                onSwitchToCitizenApp = { currentExperience = AppExperience.CITIZEN }
              )
            }
          }
        }
      }
    }
  }
}
