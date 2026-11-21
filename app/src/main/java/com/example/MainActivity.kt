package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.data.AiRepository
import com.example.data.QuizzMeRepository
import com.example.data.SettingsRepository
import com.example.data.local.AppDatabase
import com.example.ui.navigation.QuizzMeApp
import com.example.ui.theme.QuizzMeTheme

class MainActivity : ComponentActivity() {

    private val repository by lazy {
        val db = AppDatabase.getInstance(applicationContext)
        QuizzMeRepository(
            lectureDao = db.lectureDao(),
            pastPaperDao = db.pastPaperDao(),
            trackerTaskDao = db.trackerTaskDao(),
            settingsRepository = SettingsRepository(applicationContext),
            aiRepository = AiRepository()
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // STEP 2: Initialize UI
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            QuizzMeTheme {
                QuizzMeApp(
                    repository = repository,
                    widthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }
}
