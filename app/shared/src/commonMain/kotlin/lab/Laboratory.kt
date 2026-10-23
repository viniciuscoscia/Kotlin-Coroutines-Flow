package lab
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.BufferOverflow

@Composable expect fun ImportFile(enabled:Boolean, onImporting:(Boolean)->Unit, onImported:(String,String)->Unit)
@Composable expect fun DeviceObservations()
expect fun connectBackend(url:String,onEvent:(String)->Unit):()->Unit

internal fun openBackendSafely(
 url:String,
 onEvent:(String)->Unit,
 connector:(String,(String)->Unit)->(()->Unit)=::connectBackend
): (()->Unit)? = try {
 require((url.startsWith("ws://") || url.startsWith("wss://")) && url.none { it.isWhitespace() })
 connector(url,onEvent)
} catch (cancelled:CancellationException) {
 throw cancelled
} catch (_:Exception) {
 onEvent("Connection failed. Check the ws:// or wss:// URL and try again.")
 null
}

@Composable fun Laboratory(onLegacy:(()->Unit)?=null) {
 val scope=rememberCoroutineScope()
 var selected by remember { mutableStateOf(Demonstrations.all.first()) }
 var importing by remember { mutableStateOf(false) }
 var imported by remember { mutableStateOf<String?>(null) }
 var records by remember { mutableStateOf(emptyList<String>()) }
 var events by remember { mutableStateOf(listOf<String>()) }
 var job by remember { mutableStateOf<Job?>(null) }
 var endpoint by remember { mutableStateOf("ws://127.0.0.1:8080/events") }
 var disconnect by remember { mutableStateOf<(() -> Unit)?>(null) }
 val network=remember { MutableSharedFlow<String>(extraBufferCapacity=16,onBufferOverflow=BufferOverflow.DROP_OLDEST) }
 val bus=remember { MutableSharedFlow<String>(extraBufferCapacity=128,onBufferOverflow=BufferOverflow.DROP_OLDEST) }
 LaunchedEffect(Unit) { bus.collect { events=(events+it).takeLast(200) } }
 DisposableEffect(Unit) { onDispose { disconnect?.invoke() } }
 MaterialTheme {
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text("Coroutine & Flow Laboratory",style=MaterialTheme.typography.headlineMedium)
   onLegacy?.let { TextButton(onClick=it) { Text("Open existing lessons") } }
   Text("Import a UTF-8 text file (up to 1 MiB). Only a sandbox copy is used; at most 2,000 lines are loaded.")
   ImportFile(enabled=job?.isActive!=true,onImporting={importing=it}) { name,text -> imported=name; records=text.lineSequence().take(2000).toList(); bus.tryEmit("Imported sandbox copy: $name; ${records.size} lines") }
   Text(imported?:"No file imported. Run is enabled after importing a real file.")
   Text("Course modules",style=MaterialTheme.typography.titleMedium)
   FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    Demonstrations.all.map { it.module }.distinct().forEach { module ->
     FilterChip(selected=selected.module==module,onClick={job?.cancel();selected=Demonstrations.all.first { it.module==module }},label={Text(module)})
    }
   }
   FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    Demonstrations.all.filter { it.module==selected.module }.forEach { demo ->
     FilterChip(selected=selected.id==demo.id,onClick={job?.cancel();selected=demo},label={Text("${demo.id}. ${demo.title}")})
    }
   }
   Text(selected.title,style=MaterialTheme.typography.titleLarge)
   if(selected.id==26) Text("Portable block: single-owner coroutine confinement. JVM/Android additionally execute a dedicated single-thread dispatcher; browser uses its event loop.")
   Text(selected.source,fontFamily=FontFamily.Monospace)
   platformTeachingSource(selected.id).takeIf { it.isNotEmpty() }?.let { Text(it,fontFamily=FontFamily.Monospace) }
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    Button(enabled=records.isNotEmpty() && !importing && job?.isActive!=true,onClick={
     events=emptyList(); val demo=selected; val runRecords=records.toList()
     job=scope.launch {
      try { withTimeout(8000) { demo.run(DemoRun(runRecords, { bus.tryEmit(it.message) }, network)) }; bus.tryEmit("Completed ${demo.id}") }
      catch (e:CancellationException) { bus.tryEmit("Run cancelled; owned children released"); throw e }
      catch (e:Exception) { bus.tryEmit("Failed: ${e.message}") }
     }
    }) { Text("Run demonstration") }
    OutlinedButton(onClick={job?.cancel()}) { Text("Cancel") }
   }
   OutlinedTextField(value=endpoint,onValueChange={endpoint=it},label={Text("Private / loopback Spring WebSocket URL")},modifier=Modifier.fillMaxWidth())
   Row { Button(enabled=disconnect==null,onClick={disconnect=openBackendSafely(endpoint,onEvent={ if(it.startsWith("{")) network.tryEmit(it); bus.tryEmit(it) })}) { Text("Connect backend") }; TextButton(onClick={if(selected.id==37) job?.cancel(); disconnect?.invoke(); disconnect=null}) { Text("Disconnect") } }
   Text("Network events are real WebSocket messages. Start Spring explicitly and use a private tunnel on another machine.")
   DeviceObservations()
   Text("Observable events (last 200)",style=MaterialTheme.typography.titleMedium)
   events.forEachIndexed { index,line -> Text("${index+1}. $line",fontFamily=FontFamily.Monospace) }
  }
 }
}
