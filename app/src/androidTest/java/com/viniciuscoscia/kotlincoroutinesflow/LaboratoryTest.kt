package com.viniciuscoscia.kotlincoroutinesflow
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import lab.*
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.Collections

class LaboratoryTest {
 @Test fun mainImmediateRunsInlineOnActualAndroidDispatcher() = runBlocking {
  val events=Collections.synchronizedList(mutableListOf<LabEvent>())
  Demonstrations.all.first { it.id==10 }.run(DemoRun(listOf("real dispatcher"),{events+=it}))
  assertEquals(listOf("Immediate child inline","UI caller continues","Main child dispatched"),events.map { it.message })
 }
 @Test fun realSandboxCopyRunsAll43DemonstrationsOnAndroid() = runBlocking {
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  val root=File(instrumentation.targetContext.filesDir,"laboratory/verification").apply { mkdirs() }
  val copied=File(root,"imported-copy.txt")
  instrumentation.context.assets.open("laboratory-input.txt").use { input -> copied.outputStream().use { input.copyTo(it) } }
  val records=copied.readLines()
  assertEquals(listOf("alpha","beta","gamma"),records)
  for(demo in Demonstrations.all) {
   val events=Collections.synchronizedList(mutableListOf<LabEvent>())
   withTimeout(5000) { demo.run(DemoRun(records,{ events+=it },flowOf("explicit-network-fixture-1","explicit-network-fixture-2","explicit-network-fixture-3"))) }
   assertTrue("No observable events for ${demo.id}: ${demo.title}",events.isNotEmpty())
  }
 }
}
