package com.viniciuscoscia.kotlincoroutinesflow
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import lab.Laboratory
@Composable fun LaboratoryEntry() {
 var legacy by remember { mutableStateOf(true) }
 BackHandler(enabled=!legacy) { legacy=true }
 if(legacy) Column {
  Box(Modifier.weight(1f)) { LessonSelector() }
  TextButton(onClick={legacy=false}) { Text("Open multiplatform laboratory") }
 } else Laboratory(onLegacy={legacy=true})
}
