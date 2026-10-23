package lab
import kotlinx.coroutines.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.config.annotation.*
import jakarta.annotation.PreDestroy
import java.nio.file.*
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootApplication
class LaboratoryServer
fun main(args:Array<String>) { runApplication<LaboratoryServer>(*args) }

@RestController
class FileController(@Value("\${lab.sandbox}") sandbox:String, private val environment:Environment) {
 private val root=Path.of(sandbox).toAbsolutePath().normalize().resolve("imports")
 private val io=Dispatchers.IO.limitedParallelism(2)
 init { Files.createDirectories(root) }
 @GetMapping("/ready") fun ready()=Readiness(true,environment.activeProfiles.firstOrNull()?:"dev")
 @PostMapping("/files") suspend fun importCopy(@RequestParam("file") file:MultipartFile):FileSummary=withContext(io) {
  require(file.size<=1_048_576) { "Maximum file size is 1 MiB" }
  val bytes=file.inputStream.use { it.readNBytes(1_048_577) }; require(bytes.size<=1_048_576)
  val text=try {
   Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
  } catch(failure:CharacterCodingException) {
   throw ResponseStatusException(HttpStatus.BAD_REQUEST,"File must contain valid UTF-8",failure)
  }
  val id="import-${UUID.randomUUID()}.txt"
  Files.write(root.resolve(id),bytes,StandardOpenOption.CREATE_NEW)
  FileSummary(id,bytes.size.toLong(),text.lineSequence().count())
 }
 @GetMapping("/files/{id}", produces=["text/plain;charset=UTF-8"])
 suspend fun readCopy(@PathVariable("id") id:String):String = withContext(io) {
  require(id.matches(Regex("import-[0-9a-f-]{36}\\.txt"))) { "Invalid sandbox identifier" }
  Files.readString(root.resolve(id))
 }
 @GetMapping("/files") suspend fun list():List<FileSummary> = withContext(io) {
  Files.list(root).use { stream -> stream.limit(100).map { path -> FileSummary(path.fileName.toString(),Files.size(path),Files.readString(path).lineSequence().count()) }.toList() }
 }
}
@Configuration
@EnableWebSocket
class EventConfiguration:WebSocketConfigurer {
 private val handler=FileEvents()
 override fun registerWebSocketHandlers(registry:WebSocketHandlerRegistry) {
  registry.addHandler(handler,"/events").setAllowedOrigins("http://localhost:8080","http://127.0.0.1:8080","http://localhost:8081","http://127.0.0.1:8081")
 }
 @PreDestroy fun close()=handler.close()
}
class FileEvents:TextWebSocketHandler() {
 private val sessions=ConcurrentHashMap<String,WebSocketSession>()
 private val scheduler=Executors.newSingleThreadScheduledExecutor()
 private var sequence=0L
 init { scheduler.scheduleAtFixedRate({
  val message=TextMessage("{\"sequence\":${++sequence},\"event\":\"laboratory heartbeat\"}")
  sessions.values.forEach { session ->
   try { if(session.isOpen) session.sendMessage(message) } catch(_:Exception) { sessions.remove(session.id); runCatching { session.close() } }
  }
 },0,1,TimeUnit.SECONDS) }
 @Synchronized override fun afterConnectionEstablished(session:WebSocketSession) {
  if(sessions.size>=8) { session.close(CloseStatus.SERVICE_OVERLOAD); return }
  session.textMessageSizeLimit=8192
  sessions[session.id]=org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator(session,1000,8192)
 }
 override fun handleTextMessage(session:WebSocketSession,message:TextMessage) {
  val probe=message.payload
  if(!probe.matches(Regex("[A-Za-z0-9-]{1,80}"))) { session.close(CloseStatus.BAD_DATA); return }
  sessions[session.id]?.sendMessage(TextMessage("{\"event\":\"ack\",\"probe\":\"$probe\"}"))
 }
 override fun afterConnectionClosed(session:WebSocketSession,status:CloseStatus) { sessions.remove(session.id) }
 fun close() { scheduler.shutdownNow(); sessions.values.forEach { runCatching { it.close() } }; sessions.clear() }
}
