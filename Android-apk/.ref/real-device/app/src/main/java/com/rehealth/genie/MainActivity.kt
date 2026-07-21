package com.rehealth.genie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rehealth.genie.ui.ReHealthApp
import com.rehealth.genie.ui.theme.ReHealthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReHealthTheme {
                ReHealthApp()
            }
        }
    }
}
