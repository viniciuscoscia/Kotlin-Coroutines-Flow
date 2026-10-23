package lab

/** UI-thread ownership token: late picker/reader callbacks cannot publish after exit. */
internal class ImportOwnership {
    private var generation = 0
    private var disposed = false
    var busy = false
        private set
    fun begin(): Int? {
        if (disposed || busy) return null
        busy = true
        return ++generation
    }
    fun isCurrent(ticket: Int): Boolean = !disposed && busy && generation == ticket
    fun finish(ticket: Int): Boolean {
        if (!isCurrent(ticket)) return false
        busy = false
        return true
    }
    fun dispose() { disposed = true; busy = false; generation++ }
}
