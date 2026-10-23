package lab
internal actual class BackendEventGate actual constructor(private val onEvent:(String)->Unit) {
 private val lock=Any()
 private var active=true
 actual fun emit(message:String) { synchronized(lock) { if(active) onEvent(message) } }
 actual fun close() { synchronized(lock) { active=false } }
}
