package com.viniciuscoscia.kotlincoroutinesflow._1coroutinebasics

import android.util.Log
import androidx.activity.ComponentActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CoroutineBasicsLessons {

	fun run() {
		GlobalScope.launch {
			launch {
				launch {
					delay(1.seconds)
					println("Innermost coroutine finished!")
				}

				delay(500.milliseconds)
				println("Inner coroutine finished!")
			}
			println("Outermost coroutine finished")
		}
	}

	private fun example1() {
		GlobalScope.launch {
			repeat(1000) {
				delay(100L)
				println("Hello from coroutine1")
			}
		}

		GlobalScope.launch {
			repeat(1000) {
				println("Hello from coroutine2")
			}
		}
	}

}
