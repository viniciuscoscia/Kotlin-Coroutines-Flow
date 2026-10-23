package lab
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.nio.file.*
import java.net.URI
import java.net.http.*
import java.util.concurrent.CompletionStage
import javax.swing.JFileChooser

@Composable actual fun ImportFile(enabled:Boolean,onImporting:(Boolean)->Unit,onImported:(String,String)->Unit) {
 val scope=rememberCoroutineScope(); val ownership=remember { ImportOwnership() }
 val latestEnabled by rememberUpdatedState(enabled)
 val latestImporting by rememberUpdatedState(onImporting)
 val latestImported by rememberUpdatedState(onImported)
 var busy by remember { mutableStateOf(false) }
 var task by remember { mutableStateOf<Job?>(null) }
 var chooser by remember { mutableStateOf<JFileChooser?>(null) }
 var error by remember { mutableStateOf<String?>(null) }
 fun finish(ticket:Int) { if(ownership.finish(ticket)) { busy=false; latestImporting(false) } }
 DisposableEffect(Unit) { onDispose { ownership.dispose(); chooser?.cancelSelection(); task?.cancel() } }
 Button(enabled=enabled && !busy,onClick={
  ownership.begin()?.let { ticket ->
   busy=true; error=null; latestImporting(true)
   task=scope.launch {
    var copy:Path?=null; var published=false
    try {
     val picker=JFileChooser(); chooser=picker
     if(picker.showOpenDialog(null)!=JFileChooser.APPROVE_OPTION) return@launch
     ensureActive()
     if(!ownership.isCurrent(ticket) || !latestEnabled) return@launch
     val source=picker.selectedFile.toPath(); val owner=currentCoroutineContext()[Job]!!
     val text=runInterruptible(Dispatchers.IO.limitedParallelism(2)) {
      require(Files.size(source)<=1_048_576) { "File exceeds 1 MiB" }
      val bytes=Files.newInputStream(source).use { it.readNBytes(1_048_577) }; require(bytes.size<=1_048_576)
      owner.ensureActive()
      val sandbox=Path.of(System.getProperty("user.home"),".coroutine-laboratory","imports"); Files.createDirectories(sandbox)
      val target=sandbox.resolve("import-${java.util.UUID.randomUUID()}.txt"); copy=target
      owner.ensureActive(); Files.write(target,bytes,StandardOpenOption.CREATE_NEW); bytes.toString(Charsets.UTF_8)
     }
     ensureActive()
     if(ownership.isCurrent(ticket) && latestEnabled) { latestImported(source.fileName.toString(),text); published=true }
    } catch(cancelled:CancellationException) { throw cancelled }
    catch(failure:Exception) { if(ownership.isCurrent(ticket)) error=failure.message }
    finally { try { if(!published) copy?.let { Files.deleteIfExists(it) } } finally { chooser=null; finish(ticket) } }
   }
  }
 }) { Text("Import text file") }
 error?.let { Text(it) }
}
@Composable actual fun DeviceObservations() {}
actual fun connectBackend(url:String,onEvent:(String)->Unit):()->Unit {
 val uri=URI(url); require(uri.scheme in listOf("ws","wss"))
 val gate=BackendEventGate(onEvent)
 val client=HttpClient.newHttpClient()
 val socket=client.newWebSocketBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).buildAsync(uri,object:WebSocket.Listener {
  private val partial=StringBuilder()
  override fun onOpen(ws:WebSocket) { gate.emit("WebSocket connected"); ws.request(1) }
  override fun onText(ws:WebSocket,data:CharSequence,last:Boolean):CompletionStage<*>? {
   if(partial.length+data.length>8192) { ws.abort(); gate.emit("Rejected oversized event"); return null }
   partial.append(data); if(last) { gate.emit(partial.toString()); partial.clear() }; ws.request(1); return null
  }
  override fun onError(ws:WebSocket,error:Throwable) { gate.emit("WebSocket error: ${error.message}") }
 }); socket.exceptionally { gate.emit("Connection failed: ${it.message}"); null }
 return { gate.close(); socket.thenAccept { it.abort() }; client.shutdownNow() }
}
