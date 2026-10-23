package com.viniciuscoscia.kotlincoroutinesflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.viniciuscoscia.kotlincoroutinesflow._2coroutinecontext.CoroutineContextLessons
import com.viniciuscoscia.kotlincoroutinesflow.ui.theme.KotlinCoroutinesFlowTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
//            CoroutineContextLessons.queryDatabase()
            CoroutineContextLessons.unconfinedDispatcher()
        }

        setContent {
            KotlinCoroutinesFlowTheme {
                LaboratoryEntry()
            }
        }
    }
}