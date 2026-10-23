package lab
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.browser.document
import kotlinx.browser.window

internal fun newBrowserImportCopyName():String = "import-${js("crypto.randomUUID()") as String}.txt"

/** Checks ownership at every promise boundary and removes an unpublished copy on cancellation. */
internal fun persistBrowserCopy(text:String,name:String,resources:dynamic,isCurrent:()->Boolean):dynamic {
 val persist=js("""(function(text,name,resources,isCurrent) {
  var directory=null, writer=null;
  function ensureCurrent() { if(!isCurrent()) throw new Error('Import cancelled'); }
  return navigator.storage.getDirectory().then(function(root) {
   ensureCurrent(); return root.getDirectoryHandle('coroutine-laboratory',{create:true});
  }).then(function(dir) {
   directory=dir; resources.directory=dir; ensureCurrent(); return dir.getFileHandle(name,{create:true});
  }).then(function(file) {
   ensureCurrent(); return file.createWritable();
  }).then(function(output) {
   writer=output; resources.writer=output; ensureCurrent(); return output.write(text);
  }).then(function() {
   ensureCurrent(); return writer.close();
  }).then(function() {
   resources.writer=null; ensureCurrent();
  }).catch(function(error) {
   resources.writer=null;
   var abort=writer ? writer.abort().catch(function(){}) : Promise.resolve();
   return abort.then(function() {
    return directory ? directory.removeEntry(name).catch(function(){}) : Promise.resolve();
   }).then(function() { throw error; });
  });
 })""")
 return persist(text,name,resources,isCurrent)
}

@Composable actual fun ImportFile(enabled:Boolean,onImporting:(Boolean)->Unit,onImported:(String,String)->Unit) {
 val ownership=remember { ImportOwnership() }
 val resources=remember { js("({input:null,reader:null,writer:null,directory:null})") }
 val latestEnabled by rememberUpdatedState(enabled)
 val latestImporting by rememberUpdatedState(onImporting)
 val latestImported by rememberUpdatedState(onImported)
 var busy by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<String?>(null) }
 fun finish(ticket:Int) {
  if(ownership.finish(ticket)) {
   resources.input?.onchange=null; resources.input?.oncancel=null; resources.input=null
   resources.reader=null; busy=false; latestImporting(false)
  }
 }
 DisposableEffect(Unit) { onDispose {
  ownership.dispose()
  resources.input?.onchange=null; resources.input?.oncancel=null; resources.input=null
  resources.reader?.onload=null; resources.reader?.onerror=null
  resources.reader?.abort(); resources.reader=null
  resources.writer?.abort()?.catch({ _:dynamic -> Unit }); resources.writer=null
 } }
 Button(enabled=enabled && !busy,onClick={
  ownership.begin()?.let { ticket ->
   busy=true; error=null; latestImporting(true)
   val input=document.createElement("input").asDynamic(); resources.input=input
   input.type="file"; input.accept="text/*"
   input.oncancel={ _:dynamic -> finish(ticket) }
   input.onchange={ _:dynamic ->
    if(ownership.isCurrent(ticket)) {
     val file=input.files[0]
     if(file==null || !latestEnabled) finish(ticket)
     else if((file.size as Double)>1_048_576) { error="File exceeds 1 MiB"; finish(ticket) }
     else {
      val reader=js("new FileReader()"); resources.reader=reader
      reader.onerror={ _:dynamic -> if(ownership.isCurrent(ticket)) { error="Unable to read imported file"; finish(ticket) } }
      reader.onload={ _:dynamic ->
       if(ownership.isCurrent(ticket) && latestEnabled) {
        try {
         val text=reader.result as String; val name=newBrowserImportCopyName()
         persistBrowserCopy(text,name,resources) { ownership.isCurrent(ticket) && latestEnabled }.then({ _:dynamic ->
          if(ownership.isCurrent(ticket) && latestEnabled) { latestImported(file.name as String,text); error=null }
          else resources.directory?.removeEntry(name)?.catch({ _:dynamic -> Unit })
          finish(ticket)
         }, { failure:dynamic ->
          if(ownership.isCurrent(ticket)) error="Sandbox import failed: $failure"
          finish(ticket)
         })
        } catch(failure:Exception) { if(ownership.isCurrent(ticket)) { error=failure.message; finish(ticket) } }
       } else finish(ticket)
      }
      reader.readAsText(file)
     }
    }
   }
   try { input.click() } catch(failure:Exception) { error=failure.message; finish(ticket) }
  }
 }) { Text("Import text file") }
 error?.let { Text(it) }
}
@Composable actual fun DeviceObservations() {}
actual fun connectBackend(url:String,onEvent:(String)->Unit):()->Unit {
 val gate=BackendEventGate(onEvent)
 val socket=js("new WebSocket(url)")
 socket.onopen={ _:dynamic -> gate.emit("WebSocket connected") }
 socket.onmessage={ event:dynamic -> val text=event.data as String; if(text.length<=8192) gate.emit(text) else socket.close(1009,"Oversized event") }
 socket.onerror={ _:dynamic -> gate.emit("WebSocket error; check private tunnel and readiness") }
 return { gate.close(); socket.onopen=null; socket.onmessage=null; socket.onerror=null; socket.close(1000,"Leaving laboratory") }
}
