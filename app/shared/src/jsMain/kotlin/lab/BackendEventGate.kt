package lab
internal actual class BackendEventGate actual constructor(private val onEvent:(String)->Unit) {
 private var active=true
 actual fun emit(message:String) { if(active) onEvent(message) }
 actual fun close() { active=false }
}
