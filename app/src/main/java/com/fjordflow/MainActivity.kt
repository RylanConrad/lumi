package com.fjordflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.fjordflow.ui.navigation.AppNavigation
import com.fjordflow.ui.theme.FjordFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val db = (application as FjordFlowApp).database

        setContent {
            FjordFlowTheme {
                AppNavigation(db = db)
            }
        }
    }
}
