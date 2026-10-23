package com.viniciuscoscia.kotlincoroutinesflow._1coroutinebasics

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.viniciuscoscia.kotlincoroutinesflow.commons.RotatingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object CoroutineBasicsLessons {

	fun ComponentActivity.jobsAndDeferred() {
		val job = lifecycleScope.launch {
            val innerJob1 = launch {
                delay(2000L)
                println("Inner job 1 finished")
            }

            val innerJob2 = launch {
                delay(1000L)
                println("Inner job 2 finished")
            }

			val timeInMillis = measureTimeMillis {
				innerJob1.join()
				innerJob2.join()
			}

			println("Execution took $timeInMillis") // Result == 2 seconds

			val profileDeferred = async {
				println("Fetching profile data...")
				delay(2000L.milliseconds)
				"profile"
			}
			val postsDeferred = async {
				println("Fetching profile posts...")
				delay(3000L.milliseconds)
				"posts"
			}

			val timeMillis = measureTimeMillis {
				val posts = postsDeferred.await()
				val profile = profileDeferred.await()

				println("Profile loaded: $profile, $posts")
			}
			println("Jobs took $timeMillis milliseconds.")
		}
	}

	@Composable
	fun example3() {
		LaunchedEffect(true) {
			GlobalScope.launch(Dispatchers.Main) {
				// It won't affect the rotating box
				delay(3.seconds)
				Thread.sleep(3000L)
			}
		}

		RotatingBox()
	}

	fun example2() {
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

	fun example1() {
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