package lab
import kotlinx.coroutines.*
actual fun platformIO():CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2)
