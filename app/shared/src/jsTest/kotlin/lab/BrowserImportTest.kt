package lab
import kotlin.test.*
import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest

class BrowserImportTest {
 private suspend fun read(name:String):String {
  val load=js("(function(name) { return navigator.storage.getDirectory().then(function(root) { return root.getDirectoryHandle('coroutine-laboratory'); }).then(function(dir) { return dir.getFileHandle(name); }).then(function(file) { return file.getFile(); }).then(function(file) { return file.text(); }); })")
  return load(name).unsafeCast<Promise<String>>().await()
 }
 private suspend fun remove(name:String) {
  val cleanup=js("(function(name) { return navigator.storage.getDirectory().then(function(root) { return root.getDirectoryHandle('coroutine-laboratory'); }).then(function(dir) { return dir.removeEntry(name).catch(function(){}); }); })")
  cleanup(name).unsafeCast<Promise<Unit>>().await()
 }
 @Test fun distinctRealOpfsCopiesPreserveBothImports() = runTest {
  val first=newBrowserImportCopyName(); val second=newBrowserImportCopyName()
  assertNotEquals(first,second)
  try {
   persistBrowserCopy("first content",first,js("({})")) { true }.unsafeCast<Promise<Unit>>().await()
   persistBrowserCopy("second content",second,js("({})")) { true }.unsafeCast<Promise<Unit>>().await()
   assertEquals("first content",read(first)); assertEquals("second content",read(second))
  } finally { remove(first); remove(second) }
 }
 @Test fun cancelledMountedTicketSettlesAndAllowsNextRealImport() = runTest {
  val ownership=ImportOwnership(); val cancelled=ownership.begin()!!
  val abandoned=newBrowserImportCopyName(); val nextName=newBrowserImportCopyName()
  val resources=js("({})"); var importing=true
  fun settle(ticket:Int) { if(ownership.finish(ticket)) importing=false }
  try {
   val rejected=runCatching {
    persistBrowserCopy("abandoned",abandoned,resources) {
     if(resources.writer!=null) settle(cancelled)
     ownership.isCurrent(cancelled)
    }.unsafeCast<Promise<Unit>>().await()
   }
   settle(cancelled) // Promise rejection settlement must not strand a mounted picker.
   assertTrue(rejected.isFailure); assertFalse(importing); assertFalse(ownership.busy)
   assertTrue(runCatching { read(abandoned) }.isFailure)
   val next=ownership.begin()!!; importing=true
   persistBrowserCopy("next retained",nextName,js("({})")) { ownership.isCurrent(next) }.unsafeCast<Promise<Unit>>().await()
   settle(next); assertFalse(importing); assertEquals("next retained",read(nextName))
  } finally { remove(abandoned); remove(nextName) }
 }
 @Test fun cancelledImportRemovesItsUnpublishedOpfsCopy() = runTest {
  val name=newBrowserImportCopyName(); val resources=js("({})")
  val result=runCatching {
   persistBrowserCopy("cancelled content",name,resources) { resources.writer==null }.unsafeCast<Promise<Unit>>().await()
  }
  assertTrue(result.isFailure)
  assertTrue(runCatching { read(name) }.isFailure)
 }
}
