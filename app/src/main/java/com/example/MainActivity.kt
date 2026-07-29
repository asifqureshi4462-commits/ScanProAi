package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainScreen
import com.example.ui.ScanProViewModel
import com.example.ui.theme.ScanProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ScanProViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()

            ScanProTheme(themeMode = themeMode) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

