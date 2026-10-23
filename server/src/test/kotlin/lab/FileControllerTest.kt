package lab
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Assertions.*
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.*
class FileControllerTest {
 @TempDir lateinit var temp:Path
 @Test fun importsCopyWithoutUsingClientPath() = runBlocking {
  val controller=FileController(temp.toString(),MockEnvironment())
  val result=controller.importCopy(MockMultipartFile("file","../../escape.txt","text/plain","one\ntwo".toByteArray()))
  assertEquals(2,result.lines); assertEquals(7,result.bytes)
  assertTrue(Files.exists(temp.resolve("imports").resolve(result.name)))
  assertFalse(result.name.contains("..")); assertTrue(controller.ready().ready)
  assertEquals("one\ntwo",controller.readCopy(result.name)); assertEquals(2,controller.list().single().lines)
 }
 @Test fun rejectsMalformedUtf8BeforeCreatingACopy() {
  val controller=FileController(temp.toString(),MockEnvironment())
  val failure=assertThrows(org.springframework.web.server.ResponseStatusException::class.java) {
   runBlocking { controller.importCopy(MockMultipartFile("file",byteArrayOf(0xC3.toByte(),0x28))) }
  }
  assertEquals(400,failure.statusCode.value())
  Files.list(temp.resolve("imports")).use { assertEquals(0,it.count()) }
 }
 @Test fun existingReadPathAlreadyRejectsMalformedStoredBytes() {
  val controller=FileController(temp.toString(),MockEnvironment())
  val id="import-${java.util.UUID.randomUUID()}.txt"
  Files.write(temp.resolve("imports").resolve(id),byteArrayOf(0xC3.toByte(),0x28))
  assertThrows(java.nio.charset.MalformedInputException::class.java) { runBlocking { controller.readCopy(id) } }
 }
 @Test fun preservesValidMultibyteUtf8Exactly() = runBlocking {
  val controller=FileController(temp.toString(),MockEnvironment())
  val text="café\n東京"; val bytes=text.toByteArray(Charsets.UTF_8)
  val receipt=controller.importCopy(MockMultipartFile("file",bytes))
  assertEquals(text,controller.readCopy(receipt.name)); assertEquals(bytes.size.toLong(),receipt.bytes)
  assertEquals(2,receipt.lines)
 }
 @Test fun rejectsOversizeImport() {
  val controller=FileController(temp.toString(),MockEnvironment())
  assertThrows(IllegalArgumentException::class.java) { runBlocking { controller.importCopy(MockMultipartFile("file",ByteArray(1_048_577))) } }
 }
}
