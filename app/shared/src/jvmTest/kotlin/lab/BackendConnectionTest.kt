package lab

import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.*

class BackendConnectionTest {
 @Test fun actualSocketCloseWaitsForInFlightCallbackAndRejectsLateEvents() {
  val before="{\"event\":\"before\"}"; val late="{\"event\":\"late\"}"
  val entered=CountDownLatch(1); val release=CountDownLatch(1); val sendLate=CountDownLatch(1)
  val closeStarted=CountDownLatch(1); val closeFinished=CountDownLatch(1)
  val closed=AtomicBoolean(false); val events=Collections.synchronizedList(mutableListOf<String>())
  val afterClose=Collections.synchronizedList(mutableListOf<String>())
  val server=ServerSocket(0,1,InetAddress.getByName("127.0.0.1")).apply { soTimeout=5000 }
  val worker=Executors.newSingleThreadExecutor(); val closer=Executors.newSingleThreadExecutor()
  var disconnect:(()->Unit)?=null
  try {
   val fixture=worker.submit {
    server.accept().use { socket ->
     socket.soTimeout=5000
     val reader=socket.getInputStream().bufferedReader(StandardCharsets.US_ASCII)
     val headers=mutableListOf<String>()
     while(true) { val line=reader.readLine()?:error("Connection closed during handshake"); if(line.isEmpty()) break; headers+=line }
     val key=headers.first { it.startsWith("Sec-WebSocket-Key:",ignoreCase=true) }.substringAfter(':').trim()
     val accept=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray(StandardCharsets.US_ASCII)))
     val output=socket.getOutputStream()
     output.write("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: $accept\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)); output.flush()
     fun frame(text:String) { val bytes=text.toByteArray(); output.write(byteArrayOf(0x81.toByte(),bytes.size.toByte())); output.write(bytes); output.flush() }
     frame(before)
     check(sendLate.await(5,TimeUnit.SECONDS))
     runCatching { frame(late) } // Real peer attempts a frame after the client close barrier.
    }
   }
   disconnect=connectBackend("ws://127.0.0.1:${server.localPort}/events") { event ->
    if(closed.get()) afterClose+=event
    events+=event
    if(event==before) { entered.countDown(); check(release.await(5,TimeUnit.SECONDS)) }
   }
   assertTrue(entered.await(5,TimeUnit.SECONDS),"Actual WebSocket callback did not arrive")
   val closeTask=closer.submit { closeStarted.countDown(); disconnect.invoke(); closed.set(true); closeFinished.countDown() }
   assertTrue(closeStarted.await(2,TimeUnit.SECONDS))
   assertFalse(closeFinished.await(100,TimeUnit.MILLISECONDS),"Close must not return while a publishing callback owns the gate")
   release.countDown(); closeTask.get(5,TimeUnit.SECONDS)
   sendLate.countDown(); fixture.get(5,TimeUnit.SECONDS)
   assertTrue(events.contains(before)); assertFalse(events.contains(late)); assertTrue(afterClose.isEmpty())
  } finally {
   release.countDown(); sendLate.countDown(); disconnect?.invoke(); server.close()
   worker.shutdownNow(); closer.shutdownNow()
  }
 }
}
