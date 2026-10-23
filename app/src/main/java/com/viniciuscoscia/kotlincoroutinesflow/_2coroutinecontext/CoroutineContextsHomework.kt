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
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.plus
import kotlin.time.Duration.Companion.seconds

/**
 * Assignment #1
 * Throughout your day, you came up with a name for each bird: Tweety, Zazu, and Woodstock.
 * Instructions
 * Continuing from the previous program you’ve written in Coroutine Basics, give each coroutine a name and print them out alongside the sounds they make.
 */
object CoroutineContextsHomework1 {

	@Composable
	fun Assignment(modifier: Modifier = Modifier) {
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
				Button(onClick = { selectedBird = Bird.Tweety }) {
					Text("Tweety")
				}
				Button(onClick = { selectedBird = Bird.Zazu }) {
					Text("Zazu")
				}
				Button(onClick = { selectedBird = Bird.Woodstock }) {
					Text("Woodstock")
				}

				when (selectedBird) {
					Bird.Tweety -> Tweety()
					Bird.Zazu -> Zazu()
					Bird.Woodstock -> Woodstock()
					null -> Text("Choose a bird to hear its sound")
				}
			}
		}
	}

	@Composable
	private fun Tweety() {
		Text("Selected bird: Tweety")

		LaunchedEffect(Unit) {
			this + CoroutineName("Tweety")
			while (isActive) {
				delay(1.seconds)
				println("Coo")
				println("${coroutineContext[CoroutineName]}")
			}
		}
	}

	@Composable
	private fun Zazu() {
		Text("Selected bird: Zazu")

		LaunchedEffect(Unit) {
			while (isActive) {
				delay(2.seconds)
				println("Caw")
			}
		}
	}

	@Composable
	private fun Woodstock() {
		Text("Selected bird: Woodstock")

		LaunchedEffect(Unit) {
			while (isActive) {
				delay(3.seconds)
				println("Chirp")
			}
		}
	}

	private enum class Bird {
		Tweety,
		Zazu,
		Woodstock
	}
}

/**
 * Assignment #2
 * You’ve taken a couple of pictures of the birds you hear each morning, and you decide to write an application to select one of these pictures and then set the app’s background to the predominant color in the image.
 * Instructions
 * Take the existing project shared below with a single screen containing a button and the RotatingBox composable. Tapping the button should launch a picker that allows the user to select an image. Once an image is selected, use the function findDominantColor to get the primary color in the image. Change the app’s background to this new color.
 */
object CoroutineContextsHomework2{
	fun run() {

	}
}
