package lab
import android.app.Activity
import android.content.*
import android.hardware.*
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import java.io.File
import okhttp3.*

@Composable actual fun ImportFile(enabled:Boolean,onImporting:(Boolean)->Unit,onImported:(String,String)->Unit) {
 val context=LocalContext.current; val scope=rememberCoroutineScope()
 val ownership=remember { ImportOwnership() }
 val latestEnabled by rememberUpdatedState(enabled)
 val latestImporting by rememberUpdatedState(onImporting)
 val latestImported by rememberUpdatedState(onImported)
 var busy by remember { mutableStateOf(false) }
 var pending by remember { mutableStateOf<Int?>(null) }
 var task by remember { mutableStateOf<Job?>(null) }
 var error by remember { mutableStateOf<String?>(null) }
 fun finish(ticket:Int) { if(ownership.finish(ticket)) { busy=false; latestImporting(false) } }
 DisposableEffect(Unit) { onDispose { ownership.dispose(); task?.cancel() } }
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  val ticket=pending; pending=null
  if(ticket!=null && ownership.isCurrent(ticket)) {
   if(uri==null || !latestEnabled) finish(ticket)
   else task=scope.launch {
    var copy:File?=null; var published=false
    try {
     val owner=currentCoroutineContext()[Job]!!
     val text=runInterruptible(Dispatchers.IO.limitedParallelism(2)) {
      val bytes=context.contentResolver.openInputStream(uri)!!.use { input ->
       val output=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
       while(output.size()<=1_048_576) {
        owner.ensureActive()
        val count=input.read(buffer,0,minOf(buffer.size,1_048_577-output.size()))
        if(count<0) break
        output.write(buffer,0,count)
       }; output.toByteArray()
      }
      require(bytes.size<=1_048_576) { "File exceeds 1 MiB" }
      owner.ensureActive()
      val sandbox=File(context.filesDir,"laboratory/imports").apply { mkdirs() }
      val target=File(sandbox,"import-${java.util.UUID.randomUUID()}.txt"); copy=target
      owner.ensureActive(); target.writeBytes(bytes); bytes.toString(Charsets.UTF_8)
     }
     ensureActive()
     if(ownership.isCurrent(ticket) && latestEnabled) { latestImported(uri.lastPathSegment?:"import",text); published=true }
    } catch(cancelled:CancellationException) { throw cancelled }
    catch(failure:Exception) { if(ownership.isCurrent(ticket)) error=failure.message }
    finally { try { if(!published) copy?.delete() } finally { finish(ticket) } }
   }
  }
 }
 Button(enabled=enabled && !busy,onClick={
  ownership.begin()?.let { ticket ->
   pending=ticket; busy=true; error=null; latestImporting(true)
   try { launcher.launch(arrayOf("text/*")) } catch(failure:Exception) { error=failure.message; finish(ticket) }
  }
 }) { Text("Import text file") }
 error?.let { Text(it) }
}
@Composable actual fun DeviceObservations() {
 val context=LocalContext.current; var enabled by remember { mutableStateOf(false) }; var observation by remember { mutableStateOf("Android observations stopped") }
 Button(onClick={enabled=!enabled}) { Text(if(enabled) "Stop Android observations" else "Observe Android battery / accelerometer") }
 DisposableEffect(enabled) {
  val manager=context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  val receiver=object:BroadcastReceiver() { override fun onReceive(c:Context?,intent:Intent?) { observation="Battery ${intent?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)}%" } }
  val listener=object:SensorEventListener {
   private var last=0L
   override fun onSensorChanged(event:SensorEvent) { if(event.timestamp-last>500_000_000) { last=event.timestamp; observation="Accelerometer ${event.values.take(3).joinToString()}" } }
   override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int) {}
  }
  if(enabled) { context.registerReceiver(receiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED)); manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { manager.registerListener(listener,it,SensorManager.SENSOR_DELAY_NORMAL) } }
  onDispose { if(enabled) { context.unregisterReceiver(receiver); manager.unregisterListener(listener) } }
 }; Text(observation)
}
actual fun connectBackend(url:String,onEvent:(String)->Unit):()->Unit {
 val gate=BackendEventGate(onEvent)
 val client=OkHttpClient.Builder().connectTimeout(5,java.util.concurrent.TimeUnit.SECONDS).build()
 val socket=client.newWebSocket(Request.Builder().url(url).build(),object:WebSocketListener() {
  override fun onOpen(ws:WebSocket,response:Response) { gate.emit("WebSocket connected") }
  override fun onMessage(ws:WebSocket,text:String) { if(text.length<=8192) gate.emit(text) else ws.close(1009,"Oversized event") }
  override fun onFailure(ws:WebSocket,t:Throwable,response:Response?) { gate.emit("WebSocket failure: ${t.message}") }
 })
 return { gate.close(); socket.cancel(); client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll() }
}
