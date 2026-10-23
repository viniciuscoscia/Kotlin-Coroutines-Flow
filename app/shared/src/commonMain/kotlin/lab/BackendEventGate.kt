package lab
/** close is a publication barrier: callbacks cannot publish after it returns. */
internal expect class BackendEventGate(onEvent:(String)->Unit) {
 fun emit(message:String)
 fun close()
}
