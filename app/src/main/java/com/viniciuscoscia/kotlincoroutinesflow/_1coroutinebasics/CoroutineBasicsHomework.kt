package com.viniciuscoscia.kotlincoroutinesflow._1coroutinebasics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viniciuscoscia.kotlincoroutinesflow.ui.theme.KotlinCoroutinesFlowTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

object CoroutineBasicsHomework {

    /**
     * Assignment #1: each bird calls four times, starting after its first interval.
     * Call from a coroutine; this suspends until the slowest bird finishes (12 seconds).
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun assignment1() {
        // GlobalScope is used here to follow the exercise's hint.
        // In an app, prefer a scope whose lifetime belongs to the caller.
        val pigeon = GlobalScope.launch {
            repeat(4) {
                delay(1.seconds)
                println("Coo")
            }
        }
        val crow = GlobalScope.launch {
            repeat(4) {
                delay(2.seconds)
                println("Caw")
            }
        }
        val sparrow = GlobalScope.launch {
            repeat(4) {
                delay(3.seconds)
                println("Chirp")
            }
        }

        val birds = listOf(pigeon, crow, sparrow)
        try {
            birds.joinAll()
        } finally {
            // Global jobs need explicit cleanup if the waiting caller is cancelled.
            birds.forEach { it.cancel() }
        }
    }

    /**
     * Assignment #2: the birds repeat until their parent job is cancelled after 10 seconds.
     * Call from a coroutine; cancelling the caller also cancels all three birds.
     */
    suspend fun assignment2() = coroutineScope {
        val birds = launch {
            launch {
                while (isActive) {
                    delay(1.seconds)
                    println("Coo")
                }
            }
            launch {
                while (isActive) {
                    delay(2.seconds)
                    println("Caw")
                }
            }
            launch {
                while (isActive) {
                    delay(3.seconds)
                    println("Chirp")
                }
            }
        }

        delay(10.seconds)
        birds.cancelAndJoin()
    }

    /**
     * Assignment #3: place this inside setContent to try the bird simulator.
     * Only the selected bird is in the composition, so switching cancels its effect.
     */
    @Composable
    fun Assignment3(modifier: Modifier = Modifier) {
        var selectedBird by rememberSaveable { mutableStateOf<Bird?>(null) }

        Surface(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(
                    space = 16.dp,
                    alignment = Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { selectedBird = Bird.Pigeon }) {
                    Text("Pigeon")
                }
                Button(onClick = { selectedBird = Bird.Crow }) {
                    Text("Crow")
                }
                Button(onClick = { selectedBird = Bird.Sparrow }) {
                    Text("Sparrow")
                }

                when (selectedBird) {
                    Bird.Pigeon -> Pigeon()
                    Bird.Crow -> Crow()
                    Bird.Sparrow -> Sparrow()
                    null -> Text("Choose a bird to hear its sound")
                }
            }
        }
    }

    @Composable
    private fun Pigeon() {
        Text("Selected bird: Pigeon")

        LaunchedEffect(Unit) {
            while (isActive) {
                delay(1.seconds)
                println("Coo")
            }
        }
    }

    @Composable
    private fun Crow() {
        Text("Selected bird: Crow")

        LaunchedEffect(Unit) {
            while (isActive) {
                delay(2.seconds)
                println("Caw")
            }
        }
    }

    @Composable
    private fun Sparrow() {
        Text("Selected bird: Sparrow")

        LaunchedEffect(Unit) {
            while (isActive) {
                delay(3.seconds)
                println("Chirp")
            }
        }
    }

    private enum class Bird {
        Pigeon,
        Crow,
        Sparrow
    }
}

@Preview(showBackground = true)
@Composable
private fun Assignment3Preview() {
    KotlinCoroutinesFlowTheme {
        CoroutineBasicsHomework.Assignment3()
    }
}
