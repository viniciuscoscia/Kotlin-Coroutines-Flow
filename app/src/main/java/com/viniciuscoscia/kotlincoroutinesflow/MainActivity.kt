package com.viniciuscoscia.kotlincoroutinesflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import com.viniciuscoscia.kotlincoroutinesflow._1coroutinebasics.CoroutineBasicsLessons
import com.viniciuscoscia.kotlincoroutinesflow.ui.theme.KotlinCoroutinesFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        CoroutineBasicsLessons()
        setContent {
            KotlinCoroutinesFlowTheme {
                CoroutineBasicsLessons().run()
                Text("Test")
            }
        }
    }
}