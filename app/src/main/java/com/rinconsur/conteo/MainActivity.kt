package com.rinconsur.conteo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rinconsur.conteo.navigation.AppNavigation
import com.rinconsur.conteo.ui.theme.AppRinconTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRinconTheme {
                AppNavigation()
            }
        }
    }
}
