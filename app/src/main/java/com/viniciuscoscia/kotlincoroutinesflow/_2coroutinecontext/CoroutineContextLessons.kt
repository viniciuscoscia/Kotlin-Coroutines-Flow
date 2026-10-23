@file:OptIn(ExperimentalStdlibApi::class)

package com.viniciuscoscia.kotlincoroutinesflow._2coroutinecontext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

object CoroutineContextLessons {

	/**
	 * CoroutineScope holds a CoroutineContext. There's no big difference between then
	 * Just for better understanding and readability
	 *
	 * Practical Use:
	 * Combine dispatcher's main with a supervisor job for complex coroutine management.
	 *
	 * withContext  is a suspending function that enables context switching, allowing coroutines to execute code on different threads.
	 * Practical Use Case:
	 * Switching between threads is crucial for performing I.O. operations, then updating the UI with the results using
	 * withContext.
	 *
	 */
	suspend fun queryDatabase() {
		val job = currentCoroutineContext()[Job]
		val name = currentCoroutineContext()[CoroutineName]
		val handler = currentCoroutineContext()[CoroutineExceptionHandler]
		val dispatcher = currentCoroutineContext()[CoroutineDispatcher]

		CoroutineScope(Dispatchers.Main + CoroutineName("Vinicius Coroutine Name")).launch {
			println("Coroutine Name: ${currentCoroutineContext()[CoroutineName]}")
			println("Dispatcher: ${currentCoroutineContext()[CoroutineDispatcher]}")
		}

		println("Job: $job")
		println("Name: $name")
		println("Handler: $handler")
		println("Dispatcher: $dispatcher")
	}

	/**
	 *
	 *
	 * Thread: main
	 * Thread: main
	 * Thread: DefaultDispatcher-worker-2
	 * Thread: DefaultDispatcher-worker-2
	 */
	fun unconfinedDispatcher() {
		GlobalScope.launch {
			withContext(Dispatchers.Main) {
				println("Thread: ${Thread.currentThread().name}")
				withContext(Dispatchers.Unconfined) {
					println("Thread: ${Thread.currentThread().name}")
					withContext(Dispatchers.IO) {
						println("Thread: ${Thread.currentThread().name}")
					}
					println("Thread: ${Thread.currentThread().name}")
				}
			}
		}
	}
}