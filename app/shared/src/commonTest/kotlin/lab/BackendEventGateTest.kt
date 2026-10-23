package lab
import kotlin.test.*
class BackendEventGateTest {
 @Test fun closedGenerationCannotPublishIntoReconnectedConsumer() {
  val events=mutableListOf<String>()
  val old=BackendEventGate { events+=it }; old.emit("before"); old.close()
  val next=BackendEventGate { events+=it }
  old.emit("stale"); next.emit("new"); next.close(); next.emit("late")
  assertEquals(listOf("before","new"),events)
 }
}
