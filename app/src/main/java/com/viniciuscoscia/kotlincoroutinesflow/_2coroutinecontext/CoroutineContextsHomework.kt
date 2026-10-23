package com.viniciuscoscia.kotlincoroutinesflow._2coroutinecontext

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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

object CoroutineContextsHomework1 {

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

object CoroutineContextsHomework2{
	fun run() {

	}
}
