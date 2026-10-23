package lab
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
@OptIn(ExperimentalCoroutinesApi::class)
class DemonstrationsTest {
 @Test fun suspendingReadUsesImportedRecords() = runTest { assertEquals(2,suspendingCount(listOf("a","b"))); assertEquals(25,currentTime) }
 @Test fun catalogueHas43OriginalTeachingEntries() { assertEquals(43,Demonstrations.all.size); assertEquals(9,Demonstrations.all.map { it.module }.distinct().size); assertTrue(Demonstrations.all.none { it.title.contains("Homework") }) }
 @Test fun launchedWorkAndFlowCompleteInVirtualTime() = runTest { val result=async { delay(100); flowOf("alpha","b").map { it.length }.toList() }; advanceUntilIdle(); assertEquals(listOf(5,1),result.await()); assertEquals(100,currentTime) }
 @Test fun runningDemoRetainsCapturedRecordsWhenInputChanges() = runTest {
  val input=mutableListOf("original")
  val run=DemoRun(input,{})
  input[0]="replacement"
  assertEquals(listOf("original"),run.records)
 }
 @Test fun failureActuallyCancelsAnAlreadyStartedSibling() = runTest {
  val events=mutableListOf<LabEvent>()
  Demonstrations.all.first { it.id==18 }.run(DemoRun(listOf("fixture"),{events+=it}))
  assertEquals(listOf("Sibling stopped","Parent observed Invalid imported row"),events.map { it.message })
  assertTrue(Demonstrations.all.first { it.id==18 }.source.indexOf("awaitCancellation()") < Demonstrations.all.first { it.id==18 }.source.indexOf("error("))
 }
 @Test fun everyPortableDemoTerminatesAndProducesEvents() = runTest {
  Dispatchers.setMain(StandardTestDispatcher(testScheduler))
  try { Demonstrations.all.forEach { demo ->
   val events=MutableStateFlow(emptyList<LabEvent>())
   println("Executing demo ${demo.id}: ${demo.title}")
   withContext(Dispatchers.Default) { withTimeout(5000) { demo.run(DemoRun(listOf("alpha","beta","gamma"), { event -> events.update { it+event } }, flowOf("fixture-a","fixture-b","fixture-c"))) } }
   assertTrue(events.value.isNotEmpty(),demo.title)
  } } finally { Dispatchers.resetMain() }
 }
}
