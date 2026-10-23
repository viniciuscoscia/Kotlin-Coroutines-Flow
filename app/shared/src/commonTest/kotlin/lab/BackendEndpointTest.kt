package lab
import kotlin.test.*
import kotlinx.coroutines.CancellationException
class BackendEndpointTest {
 @Test fun malformedInputReportsFailureAndLeavesRetryAvailable() {
  for (url in listOf("not a url","http://x","ws://bad host")) {
   val events=mutableListOf<String>()
   assertNull(openBackendSafely(url,events::add))
   assertEquals(1,events.size)
   assertTrue(events.single().startsWith("Connection failed"))
  }
 }
 @Test fun platformConstructorFailureIsReportedAndSuccessfulRetryCanDisconnect() {
  val events=mutableListOf<String>()
  assertNull(openBackendSafely("ws://localhost:8080/events",events::add) { _,_->throw IllegalArgumentException("Invalid platform URI") })
  var closed=false
  val close=openBackendSafely("ws://localhost:8080/events",events::add) { _,_-> {closed=true} }
  assertNotNull(close);close();assertTrue(closed)
 }
 @Test fun cancellationIsPropagated() {
  assertFailsWith<CancellationException> {openBackendSafely("ws://localhost/events",{}) { _,_->throw CancellationException("Leaving")}}
 }
}
