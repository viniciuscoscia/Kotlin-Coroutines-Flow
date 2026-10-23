package lab

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties=["server.address=127.0.0.1"])
@DirtiesContext
class FileUploadHttpTest {
 @LocalServerPort private var port:Int=0
 companion object {
  private val root=Path.of("build","http-upload-review-${UUID.randomUUID()}").toAbsolutePath()
  @JvmStatic @DynamicPropertySource fun properties(registry:DynamicPropertyRegistry) { registry.add("lab.sandbox") { root.toString() } }
 }
 @Test fun malformedUploadIsHttp400AndCreatesNoSandboxCopy() {
  val boundary="review-${UUID.randomUUID()}"
  val body=("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"bad.txt\"\r\nContent-Type: text/plain\r\n\r\n".toByteArray()+byteArrayOf(0xC3.toByte(),0x28)+"\r\n--$boundary--\r\n".toByteArray())
  HttpClient.newHttpClient().use { client ->
   val request=HttpRequest.newBuilder(URI("http://127.0.0.1:$port/files"))
    .header("Content-Type","multipart/form-data; boundary=$boundary")
    .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build()
   val response=client.send(request,HttpResponse.BodyHandlers.ofString())
   assertEquals(400,response.statusCode(),response.body())
   Files.list(root.resolve("imports")).use { assertEquals(0,it.count()) }
  }
 }
}
